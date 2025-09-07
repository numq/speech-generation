<h1 align="center">Speech generation</h1>

<br>

<div align="center" style="display: grid; justify-content: center;">

|                                                                  🌟                                                                   |                  Support this project                   |               
|:-------------------------------------------------------------------------------------------------------------------------------------:|:-------------------------------------------------------:|
|  <img src="https://raw.githubusercontent.com/ErikThiart/cryptocurrency-icons/master/32/bitcoin.png" alt="Bitcoin (BTC)" width="32"/>  | <code>bc1qs6qq0fkqqhp4whwq8u8zc5egprakvqxewr5pmx</code> | 
| <img src="https://raw.githubusercontent.com/ErikThiart/cryptocurrency-icons/master/32/ethereum.png" alt="Ethereum (ETH)" width="32"/> | <code>0x3147bEE3179Df0f6a0852044BFe3C59086072e12</code> |
|  <img src="https://raw.githubusercontent.com/ErikThiart/cryptocurrency-icons/master/32/tether.png" alt="USDT (TRC-20)" width="32"/>   |     <code>TKznmR65yhPt5qmYCML4tNSWFeeUkgYSEV</code>     |

</div>

<br>

<p align="center">JVM library for speech generation written in Kotlin and based on the C++ libraries <a href="https://github.com/PABannier/bark.cpp">bark.cpp</a> and <a href="https://github.com/rhasspy/piper">piper</a></p>

### See also

- [Stretch](https://github.com/numq/stretch) *to change the speed of audio without changing the pitch*


- [Voice Activity Detection](https://github.com/numq/voice-activity-detection) *to extract speech from audio*


- [Speech recognition](https://github.com/numq/speech-recognition) *to transcribe audio to text*


- [Text generation](https://github.com/numq/text-generation) *to generate text from prompt*


- [Noise reduction](https://github.com/numq/noise-reduction) *to remove noise from audio*

## Features

- Generates PCM speech audio data from a string
- Supports any sampling rate and number of channels due to resampling and downmixing

## Installation

- Download latest [release](https://github.com/numq/speech-generation/releases)

- Add library dependency

   ```kotlin
   dependencies {
       implementation(file("/path/to/jar"))
   }
   ```

- Unzip binaries

### Piper

- Add dependencies
   ```kotlin
   dependencies {
        implementation("com.microsoft.onnxruntime:onnxruntime:1.20.0")
        implementation("com.google.code.gson:gson:2.11.0")
   }
   ```

- Download one of the voices [here](https://huggingface.co/rhasspy/piper-voices) or use any other compatible voice

## Usage

### TL;DR

> See the [example](example) module for implementation details

- Call `generate` to process the input string and get the generated speech

### Step-by-step

- Load binaries

    - Bark

        - CPU

          ```kotlin
          SpeechGeneration.Bark.loadCPU(
              ggmlBase = "/path/to/ggml-base", 
              ggmlCpu = "/path/to/ggml-cpu",
              ggml = "/path/to/ggml",
              speechGenerationBark = "/path/to/speech-generation-bark",
          )
          ```

        - CUDA

          ```kotlin
          SpeechGeneration.Bark.loadCUDA(
              ggmlBase = "/path/to/ggml-base", 
              ggmlCpu = "/path/to/ggml-cpu",
              ggmlCuda = "/path/to/ggml-cuda",
              ggml = "/path/to/ggml",
              speechGenerationBark = "/path/to/speech-generation-bark",
          )
          ```

    - Piper

      ```kotlin
      SpeechGeneration.Piper.load(
        espeak = "/path/to/espeak-ng",
        speechGenerationPiper = "/path/to/speech-generation-piper",
      )
      ```

- Create an instance

    - Bark

      ```kotlin
      SpeechGeneration.Bark.create(
          modelPath = "/path/to/model",
      )
      ```

    - Piper

      ```kotlin
      SpeechGeneration.Piper.create(
          modelPath = "/path/to/model",
          configurationPath = "/path/to/configuration",
      )
      ```

- Call `sampleRate` to get the audio producer sample rate


- Call `generate` to process the input string and get the generated speech


- Call `close` to release resources

## Requirements

- JVM version 9 or higher

## License

This project is licensed under the [Apache License 2.0](LICENSE)

## Acknowledgments

- [bark.cpp](https://github.com/PABannier/bark.cpp)
- [piper](https://github.com/rhasspy/piper)
