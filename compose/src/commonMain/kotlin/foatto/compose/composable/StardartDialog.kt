package foatto.compose.composable

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun StardartDialog(
    content: @Composable (() -> Unit),
    buttonOkText: String,
    buttonCancelText: String,
    showCancelButton: Boolean,
    onOkClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    AlertDialog(
        text = content,
        onDismissRequest = {
            onCancelClick()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onOkClick()
                }
            ) {
                Text(text = buttonOkText)
            }
        },
        dismissButton = if (showCancelButton) {
            {
                TextButton(
                    onClick = {
                        onCancelClick()
                    }
                ) {
                    Text(text = buttonCancelText)
                }
            }
        } else {
            null
        },
    )
}
