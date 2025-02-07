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
import com.github.numq.textgeneration.SpeechGeneration
import interaction.InteractionScreen
import playback.PlaybackService

const val APP_NAME = "Text generation"

fun main(args: Array<String>) {
    val (barkModelPath, piperModelPath, piperConfigurationPath) = args

    val pathToBinariesBark = Thread.currentThread().contextClassLoader.getResource("bin/bark")?.file

    checkNotNull(pathToBinariesBark) { "Binaries not found" }

    val pathToBinariesPiper = Thread.currentThread().contextClassLoader.getResource("bin/piper")?.file

    checkNotNull(pathToBinariesPiper) { "Binaries not found" }

    SpeechGeneration.Bark.loadCUDA(
        ggmlBase = "$pathToBinariesBark\\ggml-base.dll",
        ggmlCpu = "$pathToBinariesBark\\ggml-cpu.dll",
        ggmlCuda = "$pathToBinariesBark\\ggml-cuda.dll",
        ggml = "$pathToBinariesBark\\ggml.dll",
        textGenerationBark = "$pathToBinariesBark\\text-generation-bark.dll"
    ).getOrThrow()

    SpeechGeneration.Piper.load(
        espeak = "$pathToBinariesPiper\\espeak-ng.dll",
        textGenerationPiper = "$pathToBinariesPiper\\text-generation-piper.dll"
    ).getOrThrow()

    singleWindowApplication(state = WindowState(width = 512.dp, height = 512.dp), title = APP_NAME) {
        val bark = remember {
            SpeechGeneration.Bark.create(modelPath = barkModelPath).getOrThrow()
        }

        val piper = remember {
            SpeechGeneration.Piper.create(
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