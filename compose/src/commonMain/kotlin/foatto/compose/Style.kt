package foatto.compose

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TimePickerColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import foatto.core.i18n.LanguageEnum

//--- по умолчанию - тёмные иконки на светлом фоне кнопок,
//--- но в material 3 design (да и в прочих дизайнах) - контраст к основному фону - светлые иконки/текст на тёмном фоне иконок
var styleToolbarDarkIcon: Boolean = true
var styleTableRowDarkIcon: Boolean = true
var styleOtherDarkIcon: Boolean = true

//--- по умолчанию - иконки размером 36dp (пока только на toolbar'ах, в остальных местах 48)
const val styleToolbarIconSize: Int = 36
const val styleTableRowIconSize: Int = 36 //!!! хорошо бы попробовать 24, но надо загрузить все 24-размерные варианты иконок
const val styleOtherIconSize: Int = 48
const val styleOtherIconSizeNarrow: Int = 36

//--- суффикс наименовани типовой иконки material design
fun getStyleToolbarIconNameSuffix(): String = (if (styleToolbarDarkIcon) "black" else "white") + "_" + styleToolbarIconSize + "dp"
fun getStyleTableRowIconNameSuffix(): String = (if (styleTableRowDarkIcon) "black" else "white") + "_" + styleTableRowIconSize + "dp"
fun getStyleOtherIconNameSuffix(): String = (if (styleOtherDarkIcon) "black" else "white") + "_" + styleOtherIconSize + "dp"
fun getStyleOtherIconNameSuffixNarrow(): String = (if (styleOtherDarkIcon) "black" else "white") + "_" + styleOtherIconSizeNarrow + "dp"

//--- different gray tones by default
private const val MAIN_BACK_LIGHTNESS_0 = 0.97f
private const val MAIN_BACK_LIGHTNESS_1 = 0.94f
private const val MAIN_BACK_LIGHTNESS_2 = 0.88f
private const val MAIN_BACK_LIGHTNESS_3 = 0.82f

val colorMainBack0: Color = Color.hsl(0.0f, 0.0f, 1.0f)                             // input field, etc.
val colorMainBack1: Color = Color.hsl(0.0f, 0.0f, MAIN_BACK_LIGHTNESS_0)    // form panel
val colorMainBack2: Color = Color.hsl(0.0f, 0.0f, MAIN_BACK_LIGHTNESS_1)    // active tab + header + toolbar
private val colorMainBack3: Color = Color.hsl(0.0f, 0.0f, MAIN_BACK_LIGHTNESS_2)    // non-active tab
val colorMainBack4: Color = Color.hsl(0.0f, 0.0f, MAIN_BACK_LIGHTNESS_3)    // ?

var colorIndexBackgroundDefault: Color = colorMainBack0

var colorWait: Color? = null
var colorWaitTrack: Color? = null

val colorMainText: Color = Color.Black
val colorControlBack: Color = colorMainBack0
var colorIconTint: Color = colorMainText

var colorTextButton: ButtonColors? = null
var colorTextButtonDefault: ButtonColors? = null
var colorIconButton: IconButtonColors? = null
var colorOutlinedTextInput: TextFieldColors? = null
var colorCheckBox: CheckboxColors? = null
var colorRadioButton: RadioButtonColors? = null

var colorLogonBack: Color = colorMainBack0

var colorMainMenuDefault: Color = colorMainBack1
var colorUserName: Color = colorMainText

var colorMainMenuText: Color = colorMainText
var colorMainMenuIcon: Color = colorMainText
var colorPopupMenuText: Color = colorMainText
var colorPopupMenuIcon: Color = colorMainText

val colorTabActive: Color = colorMainBack2
val colorTabInactive: Color = colorMainBack3
var colorTabSelected: Color? = null
var colorTabUnselected: Color? = null

val colorHeader: Color = colorTabActive
val colorToolBar: Color = colorHeader
val colorFormBack: Color = colorMainBack1
val colorBottomBar: Color = colorHeader
val colorScrollBarBack: Color = colorFormBack   //colorMainBack2
val colorScrollBarFore: Color = Color.hsl(0f, 0f, MAIN_BACK_LIGHTNESS_3 - (MAIN_BACK_LIGHTNESS_2 - MAIN_BACK_LIGHTNESS_3) / 2)

var colorTableCaptionBar: Color = colorHeader
var colorTableCaptionSortCurrent: Color = colorMainText
var colorTableCellBorder: Color = colorMainBack3

//--- table-group-back цвета не должны совпадать с основными фоновыми цветами - поэтому располагаем их между ними посередине
var colorTableGroupBack0: Color = Color.hsl(0f, 0f, (MAIN_BACK_LIGHTNESS_2 + MAIN_BACK_LIGHTNESS_3) / 2)
var colorTableGroupBack1: Color = Color.hsl(0f, 0f, MAIN_BACK_LIGHTNESS_3 - (MAIN_BACK_LIGHTNESS_2 - MAIN_BACK_LIGHTNESS_3) / 2)
var colorTableRowBack0: Color = colorMainBack0
var colorTableRowBack1: Color = colorMainBack1
var colorTableCurrentRow: Color = colorMainBack4    // по дефолту - максимальмо мрачный цвет, чтобы сразу переделать захотелось
val colorTableSelectorBack: Color = colorMainBack0  //.copy(alpha = 0.9f)
var colorTablePageButton: ButtonColors? = null

var colorFormNarrowLabelCellBack: Color = colorMainBack1

@OptIn(ExperimentalMaterial3Api::class)
var colorDatePicker: DatePickerColors? = null

@OptIn(ExperimentalMaterial3Api::class)
var colorTimePicker: TimePickerColors? = null

var widthLogo: Dp = 0.dp
var heightLogo: Dp = 0.dp
var widthMainMenu: Dp = 300.dp

var textSizeMainMenuFolder: TextUnit = TextUnit.Unspecified
var textSizeMainMenuItem: TextUnit = TextUnit.Unspecified
var textSizePopupMenuFolder: TextUnit = TextUnit.Unspecified
var textSizePopupMenuItem: TextUnit = TextUnit.Unspecified
val textSizeTableText: TextUnit = 12.sp

var textWeightMainMenuFolder: FontWeight = FontWeight.Bold
var textWeightPopupMenuFolder: FontWeight = FontWeight.Bold

val alertDialogShape: Shape = RoundedCornerShape(8.dp)
val singleButtonShape: Shape = RoundedCornerShape(4.dp)

var colorCompositeMovedBlockBack: Color = Color.White

var logonFormTitles: Map<LanguageEnum, String> = emptyMap()