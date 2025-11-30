package foatto.compose.control.composable

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import foatto.compose.colorIconButton
import foatto.compose.colorTextButton
import foatto.compose.composable.ImageOrTextFromNameControl
import foatto.compose.singleButtonShape
import foatto.compose.styleToolbarIconSize

@Composable
fun ToolBarIconButton(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    isEnabled: Boolean = true,
    name: String,
    onClick: () -> Unit,
) {
    if (isVisible) {
        ImageOrTextFromNameControl(
            name = name,
            iconSize = styleToolbarIconSize,
            imageButton = { func ->
                FilledIconButton(
                    modifier = modifier,
                    shape = singleButtonShape,
                    colors = colorIconButton ?: IconButtonDefaults.iconButtonColors(),
                    enabled = isEnabled,
                    onClick = {
                        onClick()
                    }
                ) {
                    func()
                }
            },
            textButton = { text ->
                TextButton(
                    modifier = modifier,
                    shape = singleButtonShape,
                    colors = colorTextButton ?: ButtonDefaults.textButtonColors(),
                    enabled = isEnabled,
                    onClick = {
                        onClick()
                    }
                ) {
                    Text(text)
                }
            },
        )
    }
}

