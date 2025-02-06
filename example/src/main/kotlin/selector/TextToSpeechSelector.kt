package selector

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import interaction.TextToSpeechItem

@Composable
fun TextToSpeechSelector(
    modifier: Modifier,
    selectedTextToSpeech: TextToSpeechItem,
    selectTextToSpeech: (TextToSpeechItem) -> Unit,
) {
    Selector(
        modifier = modifier,
        items = TextToSpeechItem.entries.map { mode ->
            when (mode) {
                TextToSpeechItem.BARK -> "Bark"

                TextToSpeechItem.PIPER -> "Piper"
            }
        },
        selectedIndex = TextToSpeechItem.entries.indexOf(selectedTextToSpeech),
        selectIndex = { index -> selectTextToSpeech(TextToSpeechItem.entries.elementAt(index)) }
    )
}