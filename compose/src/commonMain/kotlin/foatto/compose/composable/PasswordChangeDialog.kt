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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import foatto.compose.alertDialogShape
import foatto.compose.colorOutlinedTextInput
import foatto.compose.colorTextButton
import foatto.compose.colorTextButtonDefault
import foatto.compose.singleButtonShape
import foatto.core.i18n.LanguageEnum
import foatto.core.i18n.LocalizedMessages
import foatto.core.i18n.getLocalizedMessage

@Composable
fun PasswordChangeDialog(
    lang: LanguageEnum,
    onOkClick: (String) -> Unit,
    onCancelClick: () -> Unit,
) {
    var password1 by remember { mutableStateOf("") }
    var password2 by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    AlertDialog(
        shape = alertDialogShape,
        text = {
            Column {
                Text(text = "${getLocalizedMessage(LocalizedMessages.ENTER_NEW_PASSWORD, lang)}:")
                OutlinedTextField(
                    colors = colorOutlinedTextInput ?: OutlinedTextFieldDefaults.colors(),
                    value = password1,
                    onValueChange = { newPass ->
                        password1 = newPass
                        errorMessage = ""
                    },
                    label = { Text(getLocalizedMessage(LocalizedMessages.NEW_PASSWORD, lang)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                OutlinedTextField(
                    colors = colorOutlinedTextInput ?: OutlinedTextFieldDefaults.colors(),
                    value = password2,
                    onValueChange = { newPass ->
                        password2 = newPass
                        errorMessage = ""
                    },
                    label = { Text(getLocalizedMessage(LocalizedMessages.AGAIN, lang)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Text(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    text = errorMessage,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
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
                    if (password1 != password2) {
                        errorMessage = getLocalizedMessage(LocalizedMessages.DIFFERENT_PASSWORD, lang)
                    } else if (password1.length < 3) {
                        errorMessage = getLocalizedMessage(LocalizedMessages.TOO_SHORT_PASSWORD, lang)
                    } else {
                        errorMessage = ""
                        onOkClick(password1)
                    }
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
                    errorMessage = ""
                    onCancelClick()
                }
            ) {
                Text(text = getLocalizedMessage(LocalizedMessages.CANCEL, lang))
            }
        },
    )
}

/*
//--- проверку комплексности пароля пока пропустим. Не любят этого пользователи.
if(  pwd.length() < 8  ) return false;
if(  pwd.equals(  pwd.toUpperCase()  )  ) return false;
if(  pwd.equals(  pwd.toLowerCase()  )  ) return false;
boolean haveDigit = false;
for(  int i = 0; i < 10; i++  )
    if(  pwd.indexOf(  "" + i  ) >= 0  ) {
        haveDigit = true;
        break;
    }
return haveDigit;
 */