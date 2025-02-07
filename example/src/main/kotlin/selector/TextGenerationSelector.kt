package selector

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import interaction.TextGenerationItem

@Composable
fun TextGenerationSelector(
    modifier: Modifier,
    selectedTextGeneration: TextGenerationItem,
    selectTextGeneration: (TextGenerationItem) -> Unit,
) {
    Selector(
        modifier = modifier,
        items = TextGenerationItem.entries.map { mode ->
            when (mode) {
                TextGenerationItem.BARK -> "Bark"

                TextGenerationItem.PIPER -> "Piper"
            }
        },
        selectedIndex = TextGenerationItem.entries.indexOf(selectedTextGeneration),
        selectIndex = { index -> selectTextGeneration(TextGenerationItem.entries.elementAt(index)) }
    )
}