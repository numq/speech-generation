package application

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import com.github.numq.tts.TextToSpeech
import interaction.InteractionScreen
import playback.PlaybackService

const val APP_NAME = "Text-To-Speech"

fun main(args: Array<String>) {
    val (barkModelPath, piperModelPath, piperConfigurationPath) = args

    val pathToBinariesBark = Thread.currentThread().contextClassLoader.getResource("bin/bark")?.file

    checkNotNull(pathToBinariesBark) { "Binaries not found" }

    val pathToBinariesPiper = Thread.currentThread().contextClassLoader.getResource("bin/piper")?.file

    checkNotNull(pathToBinariesPiper) { "Binaries not found" }

    TextToSpeech.Bark.loadCUDA(
        ggmlbase = "$pathToBinariesBark\\ggml-base.dll",
        ggmlcpu = "$pathToBinariesBark\\ggml-cpu.dll",
        ggmlcuda = "$pathToBinariesBark\\ggml-cuda.dll",
        ggml = "$pathToBinariesBark\\ggml.dll",
        libttsbark = "$pathToBinariesBark\\libtts_bark.dll"
    ).getOrThrow()

    TextToSpeech.Piper.load(
        espeakng = "$pathToBinariesPiper\\espeak-ng.dll",
        libttspiper = "$pathToBinariesPiper\\libtts_piper.dll"
    ).getOrThrow()

    singleWindowApplication(state = WindowState(width = 512.dp, height = 512.dp), title = APP_NAME) {
        val bark = remember {
            TextToSpeech.Bark.create(modelPath = barkModelPath).getOrThrow()
        }

        val piper = remember {
            TextToSpeech.Piper.create(
                modelPath = piperModelPath,
                configurationPath = piperConfigurationPath
            ).getOrThrow()
        }

        val playbackService = remember { PlaybackService.create().getOrThrow() }

        val (throwable, setThrowable) = remember { mutableStateOf<Throwable?>(null) }

        DisposableEffect(Unit) {
            onDispose {
                playbackService.close()
                bark.close()
                piper.close()
            }
        }

        MaterialTheme {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                InteractionScreen(
                    bark = bark,
                    piper = piper,
                    playbackService = playbackService,
                    handleThrowable = setThrowable
                )
                throwable?.let { t ->
                    Snackbar(
                        modifier = Modifier.padding(8.dp),
                        action = {
                            Button(onClick = { setThrowable(null) }) { Text("Dismiss") }
                        }
                    ) { Text(t.localizedMessage) }
                }
            }
        }
    }
}