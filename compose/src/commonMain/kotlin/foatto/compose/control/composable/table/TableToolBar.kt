package foatto.compose.control.composable.table

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerSelectionMode
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import foatto.compose.alertDialogShape
import foatto.compose.colorControlBack
import foatto.compose.colorDatePicker
import foatto.compose.colorOutlinedTextInput
import foatto.compose.colorTextButton
import foatto.compose.colorTextButtonDefault
import foatto.compose.colorTimePicker
import foatto.compose.colorToolBar
import foatto.compose.control.composable.ToolBarBlock
import foatto.compose.control.composable.ToolBarIconButton
import foatto.compose.getStyleToolbarIconNameSuffix
import foatto.compose.singleButtonShape
import foatto.core.model.AppAction
import foatto.core.model.response.ClientActionButton
import foatto.core.model.response.ServerActionButton
import foatto.core.util.getDateTimeDMYHMSString
import foatto.core.util.getLocalDateTime
import foatto.core.util.getTimeZone
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class)
@Composable
fun TableToolBar(
    isWideScreen: Boolean,

    isSelectorMode: Boolean,
    selectorCancelAction: (() -> Unit)?,

    isFindTextVisible: Boolean,
    findText: String,
    onFindInput: (newText: String) -> Unit,

    timeOffset: Int,
    isDateTimeIntervalPanelVisible: Boolean,
    withTime: Boolean,
    begDateTimeValue: Int?,
    onBegDateTimeClick: (newTime: Int) -> Unit,
    endDateTimeValue: Int?,
    onEndDateTimeClick: (newTime: Int) -> Unit,

    doFindAndFilter: () -> Unit,

    commonServerButtons: SnapshotStateList<ServerActionButton>,
    commonClientButtons: SnapshotStateList<ClientActionButton>,
    rowServerButtons: SnapshotStateList<ServerActionButton>,
    rowClientButtons: SnapshotStateList<ClientActionButton>,

    isRefreshEnabled: Boolean,
    tableAction: AppAction,

    clientAction: (action: AppAction) -> Unit,
    call: (action: AppAction, inNewTab: Boolean) -> Unit,
) {

    val begDatePickerState = rememberDatePickerState(
        initialDisplayMode = DisplayMode.Picker,
    )
    val begTimePickerState = rememberTimePickerState()

    var isShowBegDatePicker by remember { mutableStateOf(false) }
    var isShowBegTimePicker by remember { mutableStateOf(false) }

    val endDatePickerState = rememberDatePickerState(
        initialDisplayMode = DisplayMode.Picker,
    )
    val endTimePickerState = rememberTimePickerState()

    var isShowEndDatePicker by remember { mutableStateOf(false) }
    var isShowEndTimePicker by remember { mutableStateOf(false) }

    if (isShowBegDatePicker) {
        val millis = begDateTimeValue?.plus(timeOffset)?.toLong()?.times(1000)
        begDatePickerState.selectedDateMillis = millis
        begDatePickerState.displayedMonthMillis = millis ?: Clock.System.now().toEpochMilliseconds()

        AlertDialog(
            shape = alertDialogShape,
            text = {
                DatePicker(
                    colors = colorDatePicker ?: DatePickerDefaults.colors(),
                    state = begDatePickerState,
                )
            },
            onDismissRequest = {
                isShowBegDatePicker = false
            },
            confirmButton = {
                TextButton(
                    shape = singleButtonShape,
                    colors = colorTextButtonDefault ?: ButtonDefaults.textButtonColors(),
                    onClick = {
                        begDatePickerState.selectedDateMillis?.let { millis ->
                            onBegDateTimeClick((millis / 1000).toInt() - timeOffset)
                        }
                        isShowBegDatePicker = false
                    }
                ) {
                    Text(text = "OK")
                }
            },
            dismissButton = {
                TextButton(
                    shape = singleButtonShape,
                    colors = colorTextButton ?: ButtonDefaults.textButtonColors(),
                    onClick = {
                        isShowBegDatePicker = false
                    }
                ) {
                    Text(text = "Отмена")
                }
            },
        )
    }

    if (isShowBegTimePicker) {
        val localDateTime = getLocalDateTime(
            timeOffset = timeOffset,
            seconds = begDateTimeValue ?: Clock.System.now().epochSeconds.toInt()
        )

        begTimePickerState.selection = TimePickerSelectionMode.Hour
        begTimePickerState.is24hour = true
        begTimePickerState.hour = localDateTime.hour
        begTimePickerState.minute = localDateTime.minute

        AlertDialog(
            shape = alertDialogShape,
            text = {
                TimePicker(
                    colors = colorTimePicker ?: TimePickerDefaults.colors(),
                    state = begTimePickerState,
                )
            },
            onDismissRequest = {
                isShowBegTimePicker = false
            },
            confirmButton = {
                TextButton(
                    shape = singleButtonShape,
                    colors = colorTextButtonDefault ?: ButtonDefaults.textButtonColors(),
                    onClick = {
                        onBegDateTimeClick(
                            LocalDateTime(
                                year = localDateTime.year,
                                month = localDateTime.month.number,
                                day = localDateTime.day,
                                hour = begTimePickerState.hour,
                                minute = begTimePickerState.minute,
                                second = 0
                            ).toInstant(getTimeZone(timeOffset)).epochSeconds.toInt()
                        )
                        isShowBegTimePicker = false
                    }
                ) {
                    Text(text = "OK")
                }
            },
            dismissButton = {
                TextButton(
                    shape = singleButtonShape,
                    colors = colorTextButton ?: ButtonDefaults.textButtonColors(),
                    onClick = {
                        isShowBegTimePicker = false
                    }
                ) {
                    Text(text = "Отмена")
                }
            },
        )
    }

    if (isShowEndDatePicker) {
        val millis = endDateTimeValue?.plus(timeOffset)?.toLong()?.times(1000)
        endDatePickerState.selectedDateMillis = millis
        endDatePickerState.displayedMonthMillis = millis ?: Clock.System.now().toEpochMilliseconds()

        AlertDialog(
            shape = alertDialogShape,
            text = {
                DatePicker(
                    colors = colorDatePicker ?: DatePickerDefaults.colors(),
                    state = endDatePickerState,
                )
            },
            onDismissRequest = {
                isShowEndDatePicker = false
            },
            confirmButton = {
                TextButton(
                    shape = singleButtonShape,
                    colors = colorTextButtonDefault ?: ButtonDefaults.textButtonColors(),
                    onClick = {
                        endDatePickerState.selectedDateMillis?.let { millis ->
                            onEndDateTimeClick((millis / 1000).toInt() - timeOffset)
                        }
                        isShowEndDatePicker = false
                    }
                ) {
                    Text(text = "OK")
                }
            },
            dismissButton = {
                TextButton(
                    shape = singleButtonShape,
                    colors = colorTextButton ?: ButtonDefaults.textButtonColors(),
                    onClick = {
                        isShowEndDatePicker = false
                    }
                ) {
                    Text(text = "Отмена")
                }
            },
        )
    }

    if (isShowEndTimePicker) {
        val localDateTime = getLocalDateTime(
            timeOffset = timeOffset,
            seconds = endDateTimeValue ?: Clock.System.now().epochSeconds.toInt()
        )

        endTimePickerState.selection = TimePickerSelectionMode.Hour
        endTimePickerState.is24hour = true
        endTimePickerState.hour = localDateTime.hour
        endTimePickerState.minute = localDateTime.minute

        AlertDialog(
            shape = alertDialogShape,
            text = {
                TimePicker(
                    colors = colorTimePicker ?: TimePickerDefaults.colors(),
                    state = endTimePickerState,
                )
            },
            onDismissRequest = {
                isShowEndTimePicker = false
            },
            confirmButton = {
                TextButton(
                    shape = singleButtonShape,
                    colors = colorTextButtonDefault ?: ButtonDefaults.textButtonColors(),
                    onClick = {
                        onEndDateTimeClick(
                            LocalDateTime(
                                year = localDateTime.year,
                                month = localDateTime.month.number,
                                day = localDateTime.day,
                                hour = endTimePickerState.hour,
                                minute = endTimePickerState.minute,
                                second = 0
                            ).toInstant(getTimeZone(timeOffset)).epochSeconds.toInt()
                        )
                        isShowEndTimePicker = false
                    }
                ) {
                    Text(text = "OK")
                }
            },
            dismissButton = {
                TextButton(
                    shape = singleButtonShape,
                    colors = colorTextButton ?: ButtonDefaults.textButtonColors(),
                    onClick = {
                        isShowEndTimePicker = false
                    }
                ) {
                    Text(text = "Отмена")
                }
            },
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorToolBar)
            .padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolBarBlock {
            ToolBarIconButton(
                isVisible = selectorCancelAction != null,
                name = "/images/ic_reply_all_${getStyleToolbarIconNameSuffix()}.png",
            ) {
                selectorCancelAction?.invoke()
            }
            if (isFindTextVisible) {
                OutlinedTextField(
                    modifier = Modifier
                        .background(colorControlBack)
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.key == Key.Enter) {
                                doFindAndFilter()
                                true
                            } else {
                                false
                            }
                        },
                    colors = colorOutlinedTextInput ?: OutlinedTextFieldDefaults.colors(),
                    value = findText,
                    onValueChange = { newText ->
                        onFindInput(newText)
                    },
                    //label = { Text("Поиск...") }, - появляется паразитный белый фон вокруг рамки
                    placeholder = { Text("Поиск...") },
                    singleLine = true,
                    trailingIcon = if (isDateTimeIntervalPanelVisible) {
                        null
                    } else {
                        {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                modifier = Modifier.clickable {
                                    doFindAndFilter()
                                }
                            )
                        }
                    },
                )
            }

            if (isDateTimeIntervalPanelVisible) {
                val (begDateString, begTimeString) = begDateTimeValue?.let { curDateTimeValue ->
                    val dateTimeString = getDateTimeDMYHMSString(timeOffset, curDateTimeValue)
                    if (withTime) {
                        dateTimeString.substring(0, 10) to dateTimeString.substring(11)
                    } else {
                        dateTimeString.substring(0, 10) to null
                    }
                } ?: run {
                    if (withTime) {
                        "00.00.0000" to "00:00"
                    } else {
                        "00.00.0000" to null
                    }
                }
                val (endDateString, endTimeString) = endDateTimeValue?.let { curDateTimeValue ->
                    val dateTimeString = getDateTimeDMYHMSString(timeOffset, curDateTimeValue)
                    if (withTime) {
                        dateTimeString.substring(0, 10) to dateTimeString.substring(11)
                    } else {
                        dateTimeString.substring(0, 10) to null
                    }
                } ?: run {
                    if (withTime) {
                        "00.00.0000" to "00:00"
                    } else {
                        "00.00.0000" to null
                    }
                }

                Text(
                    modifier = Modifier.padding(start = 16.dp),
                    text = begDateString,
                )

                ToolBarIconButton(
                    name = "/images/ic_today_${getStyleToolbarIconNameSuffix()}.png",
                ) {
                    val curDateTime = Clock.System.now().toEpochMilliseconds()

                    begDatePickerState.displayedMonthMillis = curDateTime
                    begDatePickerState.selectedDateMillis = curDateTime

                    isShowBegDatePicker = true
                }

                begTimeString?.let {
                    Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = begTimeString,
                    )

                    ToolBarIconButton(
                        name = "/images/ic_query_builder_${getStyleToolbarIconNameSuffix()}.png",
                    ) {
                        isShowBegTimePicker = true
                    }
                }

                Text(
                    modifier = Modifier.padding(start = 16.dp),
                    text = endDateString,
                )

                ToolBarIconButton(
                    name = "/images/ic_today_${getStyleToolbarIconNameSuffix()}.png",
                ) {
                    val curDateTime = Clock.System.now().toEpochMilliseconds()

                    endDatePickerState.displayedMonthMillis = curDateTime
                    endDatePickerState.selectedDateMillis = curDateTime

                    isShowEndDatePicker = true
                }

                endTimeString?.let {
                    Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = endTimeString,
                    )

                    ToolBarIconButton(
                        name = "/images/ic_query_builder_${getStyleToolbarIconNameSuffix()}.png",
                    ) {
                        isShowEndTimePicker = true
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                ToolBarIconButton(
                    name = "/images/ic_search_${getStyleToolbarIconNameSuffix()}.png",
                ) {
                    doFindAndFilter()
                }
            }
        }

        if (!isSelectorMode) {
            ToolBarBlock {
                for (serverButton in commonServerButtons) {
                    ToolBarIconButton(
                        isVisible = isWideScreen || !serverButton.isForWideScreenOnly,
                        name = serverButton.name,
                    ) {
                        call(serverButton.action, serverButton.inNewTab)
                    }
                }
            }
            ToolBarBlock {
                for (clientButton in commonClientButtons) {
                    ToolBarIconButton(
                        isVisible = isWideScreen || !clientButton.isForWideScreenOnly,
                        name = clientButton.name
                    ) {
                        clientAction(clientButton.action)
                    }
                }
            }
            ToolBarBlock {
                for (serverButton in rowServerButtons) {
                    ToolBarIconButton(
                        isVisible = isWideScreen || !serverButton.isForWideScreenOnly,
                        name = serverButton.name,
                    ) {
                        call(serverButton.action, serverButton.inNewTab)
                    }
                }
            }
            ToolBarBlock {
                for (clientButton in rowClientButtons) {
                    ToolBarIconButton(
                        isVisible = isWideScreen || !clientButton.isForWideScreenOnly,
                        name = clientButton.name
                    ) {
                        clientAction(clientButton.action)
                    }
                }
            }
        }

        ToolBarBlock {
            if (isRefreshEnabled/*isWideScreen*/) {
                ToolBarIconButton(
                    name = "/images/ic_sync_${getStyleToolbarIconNameSuffix()}.png",
                ) {
                    call(tableAction, false)
                }
            }
        }
    }
}

/*
                getToolBarSpan {
                    Input(InputType.Text) {
                        style {
                            border(width = 0.px)
                            outline("none")
                            backgroundColor(hsla(0, 0, 0, 0))
                            color(hsla(0, 0, 0, 0))
                        }
                        id("table_cursor_$tabId")
                        readOnly()
                        size(1)
                        onInput { syntheticInputEvent ->
                            findText.value = syntheticInputEvent.value
                        }
                        onKeyUp { syntheticKeyboardEvent ->
                            when (syntheticKeyboardEvent.key) {
                                "Enter" -> doKeyEnter()
                                "Escape" -> doKeyEsc()
                                "ArrowUp" -> doKeyUp()
                                "ArrowDown" -> doKeyDown()
                                "Home" -> doKeyHome()
                                "End" -> doKeyEnd()
                                "PageUp" -> doKeyPageUp()
                                "PageDown" -> doKeyPageDown()
                                "F4" -> closeTabById()
                            }
                        }
                    }
 */