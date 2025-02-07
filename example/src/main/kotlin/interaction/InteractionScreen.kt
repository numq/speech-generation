package interaction

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import com.github.numq.speechgeneration.SpeechGeneration
import generation.GenerationResult
import kotlinx.coroutines.*
import playback.PlaybackService
import playback.PlaybackState
import selector.SpeechGenerationSelector
import javax.sound.sampled.AudioFormat
import kotlin.time.measureTime

@Composable
fun InteractionScreen(
    bark: SpeechGeneration.Bark,
    piper: SpeechGeneration.Piper,
    playbackService: PlaybackService,
    handleThrowable: (Throwable) -> Unit,
) {
    val generationScope = rememberCoroutineScope { Dispatchers.Default }

    var generationJob by remember { mutableStateOf<Job?>(null) }

    var playbackJob by remember { mutableStateOf<Job?>(null) }

    var selectedSpeechGeneration by remember { mutableStateOf(SpeechGenerationItem.BARK) }

    val speechGeneration = remember(selectedSpeechGeneration) {
        when (selectedSpeechGeneration) {
            SpeechGenerationItem.BARK -> bark

            SpeechGenerationItem.PIPER -> piper
        }
    }

    val sampleRate = remember(speechGeneration) {
        speechGeneration.sampleRate
    }

    var generationResult by remember { mutableStateOf<GenerationResult>(GenerationResult.Empty) }

    var state by remember { mutableStateOf<PlaybackState>(PlaybackState.Empty) }

    LaunchedEffect(generationResult) {
        state = when (generationResult) {
            is GenerationResult.Empty -> PlaybackState.Empty

            is GenerationResult.Content -> with(generationResult as GenerationResult.Content) {
                PlaybackState.Generated.Playing(pcmBytes, sampleRate)
            }
        }
    }

    val (text, setText) = remember { mutableStateOf("") }

    var isGenerating by remember { mutableStateOf(false) }

    var requestCancellation by remember { mutableStateOf(false) }

    fun generateSpeech(text: String) {
        generationJob = generationScope.launch {
            isGenerating = true

            var pcmBytes: ByteArray? = null

            val elapsedTime = measureTime {
                speechGeneration.generate(text = text).onSuccess {
                    pcmBytes = it
                }.onFailure(handleThrowable)
            }

            generationResult = pcmBytes?.let { bytes ->
                GenerationResult.Content(text, bytes, speechGeneration.sampleRate, elapsedTime)
            } ?: GenerationResult.Empty

            generationJob = null

            isGenerating = false
        }
    }

    LaunchedEffect(requestCancellation) {
        if (requestCancellation) {
            generationJob?.cancelAndJoin()
            generationJob = null

            isGenerating = false

            requestCancellation = false
        }
    }

    LaunchedEffect(state) {
        playbackJob?.cancelAndJoin()
        playbackJob = when (state) {
            is PlaybackState.Generated.Playing -> launch {
                playbackService.stop().onFailure(handleThrowable)

                val pcmBytes = (state as PlaybackState.Generated.Playing).pcmBytes

                withContext(Dispatchers.IO) {
                    val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)

                    playbackService.start(format).mapCatching {
                        playbackService.play(pcmBytes).getOrThrow()
                    }.onFailure(handleThrowable)
                }

                playbackJob?.invokeOnCompletion {
                    state = PlaybackState.Generated.Stopped(pcmBytes, sampleRate)
                }
            }

            else -> null
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.CenterVertically)
        ) {
            SpeechGenerationSelector(
                modifier = Modifier.fillMaxWidth(), selectedSpeechGeneration = selectedSpeechGeneration
            ) { speechGeneration ->
                requestCancellation = true

                selectedSpeechGeneration = speechGeneration
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    (generationResult as? GenerationResult.Content)?.let { result ->
                        Text(result.text)
                    }

                    when (val currentState = state) {
                        is PlaybackState.Empty -> Unit

                        is PlaybackState.Generated -> when (currentState) {
                            is PlaybackState.Generated.Stopped -> IconButton(onClick = {
                                state = with((state as PlaybackState.Generated.Stopped)) {
                                    PlaybackState.Generated.Playing(
                                        pcmBytes = pcmBytes,
                                        sampleRate = sampleRate
                                    )
                                }
                            }) {
                                Icon(Icons.Default.PlayCircle, null)
                            }

                            is PlaybackState.Generated.Playing -> IconButton(onClick = {
                                state = with((state as PlaybackState.Generated.Stopped)) {
                                    PlaybackState.Generated.Stopped(
                                        pcmBytes = pcmBytes,
                                        sampleRate = sampleRate
                                    )
                                }
                            }) {
                                Icon(Icons.Default.StopCircle, null)
                            }
                        }
                    }
                    (generationResult as? GenerationResult.Content)?.let { result ->
                        Text("Generation time: ${result.elapsedTime}")
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(value = text, onValueChange = setText, modifier = Modifier.weight(1f).onKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Enter) {
                        generateSpeech(text)

                        return@onKeyEvent true
                    }

                    false
                }, singleLine = true, trailingIcon = {
                    IconButton(onClick = {
                        setText("")
                    }, enabled = text.isNotBlank()) {
                        Icon(Icons.Default.Clear, null)
                    }
                }, enabled = !isGenerating
                )
                Box(modifier = Modifier.padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                    when {
                        isGenerating -> IconButton(onClick = {
                            requestCancellation = true
                        }) {
                            Icon(Icons.Default.Cancel, null)
                        }

                        else -> IconButton(onClick = {
                            generateSpeech(text)
                        }, enabled = text.isNotBlank()) {
                            Icon(Icons.AutoMirrored.Filled.Send, null)
                        }
                    }
                }
            }
        }
    }
}