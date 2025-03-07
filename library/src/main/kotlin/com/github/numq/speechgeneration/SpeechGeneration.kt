package com.github.numq.speechgeneration

import com.github.numq.speechgeneration.bark.BarkSpeechGeneration
import com.github.numq.speechgeneration.bark.NativeBarkSpeechGeneration
import com.github.numq.speechgeneration.piper.NativePiperSpeechGeneration
import com.github.numq.speechgeneration.piper.PhonemeType
import com.github.numq.speechgeneration.piper.PiperConfiguration
import com.github.numq.speechgeneration.piper.PiperSpeechGeneration
import com.github.numq.speechgeneration.piper.model.DefaultPiperOnnxModel
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path

interface SpeechGeneration : AutoCloseable {
    /**
     * The sample rate of the audio data in Hertz (Hz).
     */
    val sampleRate: Int

    /**
     * The number of audio channels.
     */
    val channels: Int

    /**
     * Generates speech from the given text.
     *
     * The input text is processed to create phonemes, which are then synthesized into speech audio.
     *
     * @param text the input text to be phonemized and synthesized into speech.
     * @return a [Result] containing the byte array of generated speech audio if successful.
     */
    suspend fun generate(text: String): Result<ByteArray>

    interface Bark : SpeechGeneration {
        companion object {
            private const val DEFAULT_TEMPERATURE = .7f
            private const val DEFAULT_SEED = 0L

            private sealed interface LoadState {
                data object Unloaded : LoadState

                data object CPU : LoadState

                data object CUDA : LoadState
            }

            @Volatile
            private var loadState: LoadState = LoadState.Unloaded

            /**
             * Loads the CPU-based native libraries required for Bark speech generation.
             *
             * This method must be called before creating a Bark instance.
             *
             * @param ggmlBase The path to the `ggml-base` binary.
             * @param ggmlCpu The path to the `ggml-cpu` binary.
             * @param ggml The path to the `ggml` binary.
             * @param speechGenerationBark The path to the `speech-generation-bark` binary.
             * @return A [Result] indicating the success or failure of the operation.
             */
            fun loadCPU(
                ggmlBase: String,
                ggmlCpu: String,
                ggml: String,
                speechGenerationBark: String,
            ) = runCatching {
                check(loadState is LoadState.Unloaded) { "Native binaries have already been loaded as ${loadState::class.simpleName}" }

                System.load(ggmlBase)
                System.load(ggmlCpu)
                System.load(ggml)
                System.load(speechGenerationBark)

                loadState = LoadState.CPU
            }

            /**
             * Loads the CUDA-based native libraries required for Bark speech generation.
             *
             * This method must be called before creating a Bark instance.
             *
             * @param ggmlBase The path to the `ggml-base` binary.
             * @param ggmlCpu The path to the `ggml-cpu` binary.
             * @param ggmlCuda The path to the `ggml-cuda` binary.
             * @param ggml The path to the `ggml` binary.
             * @param speechGenerationBark The path to the `speech-generation-bark` binary.
             * @return A [Result] indicating the success or failure of the operation.
             */
            fun loadCUDA(
                ggmlBase: String,
                ggmlCpu: String,
                ggmlCuda: String,
                ggml: String,
                speechGenerationBark: String,
            ) = runCatching {
                check(loadState is LoadState.Unloaded) { "Native binaries have already been loaded as ${loadState::class.simpleName}" }

                System.load(ggmlBase)
                System.load(ggmlCpu)
                System.load(ggmlCuda)
                System.load(ggml)
                System.load(speechGenerationBark)

                loadState = LoadState.CUDA
            }

            /**
             * Creates a new instance of [SpeechGeneration] using the Bark implementation.
             *
             * This method initializes the Bark speech generation with the specified model.
             *
             * @param modelPath the path to the Bark model file.
             * @param temperature the temperature parameter controls the randomness of the generated speech.
             *        Higher values result in more diverse but potentially less coherent outputs.
             * @param seed the random seed to ensure deterministic outputs. If set to the same value,
             *        it will generate the same audio output for the same input text.
             * @return a [Result] containing the created instance if successful.
             * @throws IllegalStateException if the native libraries are not loaded or if there is an issue with the underlying native libraries.
             */
            fun create(
                modelPath: String,
                temperature: Float = DEFAULT_TEMPERATURE,
                seed: Long = DEFAULT_SEED,
            ): Result<Bark> = runCatching {
                check(loadState !is LoadState.Unloaded) { "Native binaries were not loaded" }

                BarkSpeechGeneration(
                    nativeBarkSpeechGeneration = NativeBarkSpeechGeneration(
                        modelPath = modelPath,
                        temperature = temperature,
                        seed = seed
                    )
                )
            }
        }

        override fun close() {
            loadState = LoadState.Unloaded
        }
    }

    interface Piper : SpeechGeneration {
        /**
         * The total number of available speakers.
         *
         * @return The number of speakers supported by the engine.
         */
        val numSpeakers: Int

        /**
         * The currently selected speaker identifier.
         *
         * @return The identifier of the currently selected speaker.
         */
        val selectedSpeakerId: Long

        /**
         * Selects a speaker identifier for speech synthesis.
         *
         * @param speakerId The identifier of the speaker to select.
         * @return a [Result] indicating success or failure of the operation.
         */
        suspend fun selectSpeakerId(speakerId: Long): Result<Unit>

        companion object {
            private const val DEFAULT_SPEAKER_ID = 0L

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
             * Loads the native libraries required for Piper speech generation.
             *
             * This method must be called before creating a Piper instance.
             *
             * @param espeak The path to the `espeak-ng` binary.
             * @param speechGenerationPiper The path to the `speech-generation-piper` binary.
             * @return A [Result] indicating the success or failure of the operation.
             */
            fun load(
                espeak: String,
                speechGenerationPiper: String,
            ) = runCatching {
                System.load(espeak)
                System.load(speechGenerationPiper)
            }.onSuccess {
                isLoaded = true
            }

            /**
             * Creates a new instance of [SpeechGeneration] using the Piper implementation.
             *
             * This method initializes the Piper speech generation with the specified model.
             *
             * @param modelPath the path to the Piper model file.
             * @param configurationPath the path to the Piper configuration file.
             * @param speakerId the speaker id. Default is `0`.
             * @return a [Result] containing the created instance if successful.
             * @throws IllegalStateException if the native libraries are not loaded or if there is an issue with the underlying native libraries.
             */
            fun create(
                modelPath: String,
                configurationPath: String,
                speakerId: Long = DEFAULT_SPEAKER_ID,
            ): Result<Piper> =
                runCatching {
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

                    PiperSpeechGeneration(
                        nativePiperSpeechGeneration = NativePiperSpeechGeneration(dataPath = tempDir.absolutePath),
                        model = DefaultPiperOnnxModel(modelPath = modelPath),
                        configuration = PiperConfiguration(
                            modelConfig = modelConfig, phonemeConfig = phonemeConfig, synthesisConfig = synthesisConfig
                        ),
                        speakerId = speakerId
                    )
                }
        }
    }
}