package foatto.compose.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import foatto.compose.alertDialogShape
import foatto.compose.colorOutlinedTextInput
import foatto.compose.colorTextButton
import foatto.compose.colorTextButtonDefault
import foatto.compose.singleButtonShape
import foatto.core.i18n.LanguageEnum
import foatto.core.i18n.LocalizedMessages
import foatto.core.i18n.getLocalizedMessage

@Composable
fun MessageDialog(
    lang: LanguageEnum,
    onOkClick: (String) -> Unit,
    onCancelClick: () -> Unit,
) {
    var message by remember { mutableStateOf("") }

    AlertDialog(
        shape = alertDialogShape,
        text = {
            Column {
                Text(text = "${getLocalizedMessage(LocalizedMessages.ENTER_MESSAGE, lang)}:")
                OutlinedTextField(
                    colors = colorOutlinedTextInput ?: OutlinedTextFieldDefaults.colors(),
                    value = message,
                    onValueChange = { newMessage ->
                        message = newMessage
                    },
                    label = { Text(getLocalizedMessage(LocalizedMessages.MESSAGE, lang)) },
                    singleLine = false,
                )
            }
        },
        onDismissRequest = {
            onCancelClick()
        },
        confirmButton = {
            TextButton(
                shape = singleButtonShape,
                colors = colorTextButtonDefault ?: ButtonDefaults.textButtonColors(),
                onClick = {
                    onOkClick(message)
                }
            ) {
                Text(text = getLocalizedMessage(LocalizedMessages.OK, lang))
            }
        },
        dismissButton = {
            TextButton(
                shape = singleButtonShape,
                colors = colorTextButton ?: ButtonDefaults.textButtonColors(),
                onClick = {
                    onCancelClick()
                }
            ) {
                Text(text = getLocalizedMessage(LocalizedMessages.CANCEL, lang))
            }
        },
    )
}
