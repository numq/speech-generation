package selector

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import interaction.SpeechGenerationItem

@Composable
fun SpeechGenerationSelector(
    modifier: Modifier,
    selectedSpeechGeneration: SpeechGenerationItem,
    selectSpeechGeneration: (SpeechGenerationItem) -> Unit,
) {
    Selector(
        modifier = modifier,
        items = SpeechGenerationItem.entries.map { mode ->
            when (mode) {
                SpeechGenerationItem.BARK -> "Bark"

                SpeechGenerationItem.PIPER -> "Piper"
            }
        },
        selectedIndex = SpeechGenerationItem.entries.indexOf(selectedSpeechGeneration),
        selectIndex = { index -> selectSpeechGeneration(SpeechGenerationItem.entries.elementAt(index)) }
    )
}