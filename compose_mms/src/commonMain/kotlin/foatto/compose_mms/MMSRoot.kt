package foatto.compose_mms

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import foatto.compose.Root
import foatto.compose.colorTableCaptionBar
import foatto.compose.colorCheckBox
import foatto.compose.colorCompositeMovedBlockBack
import foatto.compose.colorDatePicker
import foatto.compose.colorIconButton
import foatto.compose.colorMainBack0
import foatto.compose.colorMainText
import foatto.compose.colorOutlinedTextInput
import foatto.compose.colorRadioButton
import foatto.compose.colorTabSelected
import foatto.compose.colorTabUnselected
import foatto.compose.colorTableCaptionSortCurrent
import foatto.compose.colorTableCurrentRow
import foatto.compose.colorTablePageButton
import foatto.compose.colorTextButton
import foatto.compose.colorTextButtonDefault
import foatto.compose.colorTimePicker
import foatto.compose.colorWait
import foatto.compose.colorWaitTrack
import foatto.compose.defaultStartModule
import foatto.compose.utils.SETTINGS_SERVER_ADDRESS
import foatto.compose.utils.SETTINGS_SERVER_PORT
import foatto.compose.utils.SETTINGS_SERVER_PROTOCOL
import foatto.compose.utils.settings
import foatto.core_mms.AppModuleMMS
import foatto.core_mms.addAppModuleUrls

//--- фирменный тёмно-синий         #0C386D = hsl(213,80%,24%) и градиент до #209dcb = hsl(196,73%,46%)
private const val MMS_FIRM_COLOR_1_H = 213f
private const val MMS_FIRM_COLOR_1_S = 0.80f
private const val MMS_FIRM_COLOR_1_L = 0.40f    //0.24f <- слишком тяжёлый/сочный/тёмный цвет - 0.60f
//--- фирменный тёмно-синий         #0D54A2 = hsl(211,85%,34%) - с сайта - получается светлее и не очень контрастным с рыжим
//private const val MMS_FIRM_COLOR_1_H = 211f
//private const val MMS_FIRM_COLOR_1_S = 0.85f
//private const val MMS_FIRM_COLOR_1_L = 0.34f

//--- фирменный терракотовый        #F7AA47 = hsl(34,92%,62%)
private const val MMS_FIRM_COLOR_2_H = 34f
private const val MMS_FIRM_COLOR_2_S = 0.92f
private const val MMS_FIRM_COLOR_2_L = 0.70f    //0.62f <- слишком тяжёлый/сочный/тёмный цвет - 0.80f

//--- фирменный тёмно-красный       #BF0D0E = hsl(359.7,87.3%,40%) - logon button
private const val MMS_FIRM_COLOR_3_H = 360f
private const val MMS_FIRM_COLOR_3_S = 0.87f
private const val MMS_FIRM_COLOR_3_L = 0.40f

class MMSRoot : Root() {
    init {
        settings.putString(SETTINGS_SERVER_PROTOCOL, "http")
        settings.putString(SETTINGS_SERVER_ADDRESS, "192.168.0.44")
        settings.putInt(SETTINGS_SERVER_PORT, 19998)

//        defaultStartModule = AppModuleMMS.OBJECT_SCHEME_LIST_DASHBOARD
        defaultStartModule = AppModuleMMS.OBJECT

        addAppModuleUrls()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun setCustomColors() {
        super.setCustomColors()

        val darkBlueColor = Color.hsl(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, MMS_FIRM_COLOR_1_L)
        val orangeColor = Color.hsl(MMS_FIRM_COLOR_2_H, MMS_FIRM_COLOR_2_S, MMS_FIRM_COLOR_2_L)

        colorWait = darkBlueColor
        colorWaitTrack = orangeColor

        colorTabSelected = darkBlueColor
        colorTabUnselected = Color.hsl(0.0f, 0.0f, 0.5f)

        colorTextButton = ButtonDefaults.textButtonColors().copy(
            containerColor = darkBlueColor,
            contentColor = colorMainBack0,
        )
        colorTextButtonDefault = ButtonDefaults.textButtonColors().copy(
            containerColor = orangeColor,
            contentColor = colorMainText,
        )
        colorIconButton = IconButtonDefaults.iconButtonColors().copy(
            containerColor = orangeColor,
            contentColor = colorMainText,
        )

        colorOutlinedTextInput = OutlinedTextFieldDefaults.colors().copy(
            focusedIndicatorColor = darkBlueColor,
            focusedLabelColor = darkBlueColor,
        )
        colorCheckBox = CheckboxDefaults.colors().copy(
            checkedBoxColor = darkBlueColor,
            checkedBorderColor = darkBlueColor,
        )
        colorRadioButton = RadioButtonDefaults.colors().copy(
            selectedColor = darkBlueColor,
        )

        colorTableCaptionBar = darkBlueColor
        colorTableCaptionSortCurrent = orangeColor
        //--- чуть светлее брендового цвета, чтобы на его фоне были видны кнопки селекторов
        colorTableCurrentRow = Color.hsl(MMS_FIRM_COLOR_2_H, MMS_FIRM_COLOR_2_S, (MMS_FIRM_COLOR_2_L + 1.0f) / 2)
        colorTablePageButton = ButtonDefaults.buttonColors().copy(
            containerColor = darkBlueColor,
            contentColor = colorMainBack0,
            disabledContentColor = colorMainText,
        )

        colorDatePicker = DatePickerDefaults.colors().copy(
//            titleContentColor = ,
//            headlineContentColor = ,
//            weekdayContentColor = ,
//            subheadContentColor = ,
//            navigationContentColor = ,
//            yearContentColor = ,
//            disabledYearContentColor = ,
            currentYearContentColor = darkBlueColor,
            selectedYearContentColor = darkBlueColor,
//            disabledSelectedYearContentColor = ,
            selectedYearContainerColor = orangeColor,
//            disabledSelectedYearContainerColor = ,
//            dayContentColor = ,
//            disabledDayContentColor = ,
            selectedDayContentColor = darkBlueColor,
//            disabledSelectedDayContentColor = ,
            selectedDayContainerColor = orangeColor,
//            disabledSelectedDayContainerColor = ,
            todayContentColor = darkBlueColor,
            todayDateBorderColor = darkBlueColor,
            dayInSelectionRangeContentColor = darkBlueColor,
            dayInSelectionRangeContainerColor = orangeColor,
            //dividerColor = darkBlueColor, - родной цвет более аккуратный
            // dateTextFieldColors = , - слишком муторно задавать множество параметров из-за пары изменяемых значений
        )
        colorTimePicker = TimePickerDefaults.colors(
            //clockDialColor = , - лучше оставить дефолтный
            clockDialSelectedContentColor = darkBlueColor,
//            clockDialUnselectedContentColor = ,
            selectorColor = orangeColor,
            //containerColor = Color.Red, - не удалось выяснить
//            periodSelectorBorderColor = ,
//            periodSelectorSelectedContainerColor = ,
//            periodSelectorUnselectedContainerColor = ,
//            periodSelectorSelectedContentColor = ,
//            periodSelectorUnselectedContentColor = ,
            timeSelectorSelectedContainerColor = orangeColor,
//            timeSelectorUnselectedContainerColor = ,
            timeSelectorSelectedContentColor = darkBlueColor,
//            timeSelectorUnselectedContentColor = ,
        )

        colorCompositeMovedBlockBack = darkBlueColor.copy(alpha = 0.7f)
    }
}