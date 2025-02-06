package com.github.numq.tts

import com.github.numq.tts.bark.BarkTextToSpeech
import com.github.numq.tts.bark.NativeBarkTextToSpeech
import com.github.numq.tts.piper.NativePiperTextToSpeech
import com.github.numq.tts.piper.PhonemeType
import com.github.numq.tts.piper.PiperConfiguration
import com.github.numq.tts.piper.PiperTextToSpeech
import com.github.numq.tts.piper.model.DefaultPiperOnnxModel
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path

interface TextToSpeech : AutoCloseable {
    val sampleRate: Int
    suspend fun generate(text: String): Result<ByteArray>

    interface Bark : TextToSpeech {
        companion object {
            private sealed interface LoadState {
                data object Unloaded : LoadState

                data object CPU : LoadState

                data object CUDA : LoadState
            }

            @Volatile
            private var loadState: LoadState = LoadState.Unloaded

            /**
             * Loads the CPU-based native libraries required for Bark text-to-speech generation.
             *
             * This method must be called before creating a Bark instance.
             *
             * @param ggmlbase the path to the ggml-base library.
             * @param ggmlcpu the path to the ggml-cpu library.
             * @param ggml the path to the ggml library.
             * @param libttsbark the path to the libtts_bark library.
             * @return a [Result] indicating the success or failure of the operation.
             */
            fun loadCPU(
                ggmlbase: String,
                ggmlcpu: String,
                ggml: String,
                libttsbark: String,
            ) = runCatching {
                check(loadState is LoadState.Unloaded) { "Native binaries have already been loaded as ${loadState::class.simpleName}" }

                System.load(ggmlbase)
                System.load(ggmlcpu)
                System.load(ggml)
                System.load(libttsbark)

                loadState = LoadState.CPU
            }

            /**
             * Loads the CUDA-based native libraries required for Bark text-to-speech generation.
             *
             * This method must be called before creating a Bark instance.
             *
             * @param ggmlbase the path to the ggml-base library.
             * @param ggmlcpu the path to the ggml-cpu library.
             * @param ggmlcuda the path to the ggml-cuda library.
             * @param ggml the path to the ggml library.
             * @param libttsbark the path to the libtts_bark library.
             * @return a [Result] indicating the success or failure of the operation.
             */
            fun loadCUDA(
                ggmlbase: String,
                ggmlcpu: String,
                ggmlcuda: String,
                ggml: String,
                libttsbark: String,
            ) = runCatching {
                check(loadState is LoadState.Unloaded) { "Native binaries have already been loaded as ${loadState::class.simpleName}" }

                System.load(ggmlbase)
                System.load(ggmlcpu)
                System.load(ggmlcuda)
                System.load(ggml)
                System.load(libttsbark)

                loadState = LoadState.CUDA
            }

            /**
             * Creates a new instance of [TextToSpeech] using the Bark implementation.
             *
             * This method initializes the Bark text-to-speech system with the specified model.
             *
             * @param modelPath the path to the Bark model file.
             * @return a [Result] containing the created instance if successful.
             * @throws IllegalStateException if the native libraries are not loaded or if there is an issue with the underlying native libraries.
             */
            fun create(modelPath: String): Result<Bark> = runCatching {
                check(loadState !is LoadState.Unloaded) { "Native binaries were not loaded" }

                BarkTextToSpeech(nativeBarkTextToSpeech = NativeBarkTextToSpeech(modelPath = modelPath))
            }
        }

        override fun close() {
            loadState = LoadState.Unloaded
        }
    }

    interface Piper : TextToSpeech {
        companion object {
            private var isLoaded = false

            private fun parsePhonemeConfig(json: JsonObject, config: PiperConfiguration.PhonemeConfig) {
                json.getAsJsonObject("espeak")?.let { espeakValue ->
                    espeakValue.getAsJsonPrimitive("voice")?.let {
                        config.voice = it.asString
                    }
                }

                json.getAsJsonPrimitive("phoneme_type")?.let {
                    if (it.asString == "text") {
                        config.phonemeType = PhonemeType.ESPEAK
                    }
                }

                json.getAsJsonObject("phoneme_id_map")?.let { phonemeIdMapValue ->
                    for ((fromPhoneme, toIdsJson) in phonemeIdMapValue.entrySet()) {
                        val toIds = Gson().fromJson(toIdsJson, List::class.java) as List<Long>
                        config.phonemeIdMap[fromPhoneme.last()] = toIds
                    }
                }
            }

            private fun parseSynthesisConfig(json: JsonObject, config: PiperConfiguration.SynthesisConfig) {
                json.getAsJsonObject("audio")?.let { audioValue ->
                    audioValue.getAsJsonPrimitive("sample_rate")?.let {
                        config.sampleRate = it.asInt
                    }
                }

                json.getAsJsonObject("audio")?.let { audioValue ->
                    audioValue.getAsJsonPrimitive("channels")?.let {
                        config.channels = it.asInt
                    }
                }

                json.getAsJsonObject("inference")?.let { inferenceValue ->
                    inferenceValue.getAsJsonPrimitive("noise_scale")?.let {
                        config.noiseScale = it.asFloat
                    }
                    inferenceValue.getAsJsonPrimitive("length_scale")?.let {
                        config.lengthScale = it.asFloat
                    }
                    inferenceValue.getAsJsonPrimitive("noise_w")?.let {
                        config.noiseW = it.asFloat
                    }

                    inferenceValue.getAsJsonObject("phoneme_silence")?.let { phonemeSilenceValue ->
                        if (!phonemeSilenceValue.isEmpty) {
                            config.phonemeSilenceSeconds = mutableMapOf()
                        }

                        for ((phoneme, silenceDurationJson) in phonemeSilenceValue.entrySet()) {
                            val silenceDuration = silenceDurationJson.asFloat
                            config.phonemeSilenceSeconds?.set(phoneme.single(), silenceDuration)
                        }
                    }
                }
            }

            private fun parseModelConfig(json: JsonObject, config: PiperConfiguration.ModelConfig) {
                json.getAsJsonPrimitive("num_speakers")?.let {
                    config.numSpeakers = it.asInt
                }

                json.getAsJsonObject("speaker_id_map")?.let { speakerIdMapValue ->
                    for ((speakerName, speakerIdJson) in speakerIdMapValue.entrySet()) {
                        val speakerId = speakerIdJson.asInt
                        config.speakerIdMap[speakerName] = speakerId
                    }
                }
            }

            /**
             * Loads native libraries required for Piper text-to-speech generation.
             *
             * This method must be called before creating a Piper instance.
             *
             * @param espeakng the path to the espeak-ng library.
             * @param libttspiper the path to the libtts_piper library.
             * @return a [Result] indicating the success or failure of the operation.
             */
            fun load(
                espeakng: String,
                libttspiper: String,
            ) = runCatching {
                System.load(espeakng)
                System.load(libttspiper)
            }.onSuccess {
                isLoaded = true
            }

            /**
             * Creates a new instance of [TextToSpeech] using the Piper implementation.
             *
             * This method initializes the Piper text-to-speech system with the specified model.
             *
             * @param modelPath the path to the Piper model file.
             * @param configurationPath the path to the Piper configuration file.
             * @return a [Result] containing the created instance if successful.
             * @throws IllegalStateException if the native libraries are not loaded or if there is an issue with the underlying native libraries.
             */
            fun create(
                modelPath: String,
                configurationPath: String,
            ): Result<Piper> = runCatching {
                check(isLoaded) { "Native binaries were not loaded" }

                val tempDir = Files.createTempDirectory("espeak-ng-data").toFile().apply {
                    deleteOnExit()
                }

                val resourceUrl = Companion::class.java.classLoader.getResource("espeak-ng-data")
                    ?: throw IllegalStateException("Resource directory 'espeak-ng-data' not found")

                val uri = resourceUrl.toURI()

                when (uri.scheme) {
                    "jar" -> FileSystems.newFileSystem(uri, emptyMap<String, Any>()).use { fs ->
                        fs.getPath("espeak-ng-data").let { resourceDirPath ->
                            Files.walk(resourceDirPath).use { paths ->
                                paths.forEach { path ->
                                    val targetPath =
                                        tempDir.toPath().resolve(resourceDirPath.relativize(path).toString())
                                    if (Files.isDirectory(path)) {
                                        Files.createDirectories(targetPath)
                                    } else {
                                        Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING)
                                    }
                                }
                            }
                        }
                    }

                    else -> Paths.get(uri).let { resourceDirPath ->
                        Files.walk(resourceDirPath).use { paths ->
                            paths.forEach { path ->
                                val targetPath = tempDir.toPath().resolve(resourceDirPath.relativize(path).toString())
                                if (Files.isDirectory(path)) {
                                    Files.createDirectories(targetPath)
                                } else {
                                    Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING)
                                }
                            }
                        }
                    }
                }

                require(Files.exists(Path(tempDir.absolutePath))) { "Incorrect eSpeak path" }

                require(Files.exists(Path(modelPath))) { "Incorrect model path" }

                require(Files.exists(Path(configurationPath))) { "Incorrect configuration path" }

                val configurationJson = Gson().fromJson(File(configurationPath).readText(), JsonObject::class.java)

                val modelConfig = PiperConfiguration.ModelConfig().apply {
                    parseModelConfig(json = configurationJson, config = this)
                }

                val phonemeConfig = PiperConfiguration.PhonemeConfig().apply {
                    parsePhonemeConfig(json = configurationJson, config = this)
                }

                val synthesisConfig = PiperConfiguration.SynthesisConfig().apply {
                    parseSynthesisConfig(json = configurationJson, config = this)
                }

                PiperTextToSpeech(
                    nativePiperTextToSpeech = NativePiperTextToSpeech(dataPath = tempDir.absolutePath),
                    model = DefaultPiperOnnxModel(modelPath = modelPath),
                    configuration = PiperConfiguration(
                        modelConfig = modelConfig, phonemeConfig = phonemeConfig, synthesisConfig = synthesisConfig
                    )
                )
            }
        }
    }
}