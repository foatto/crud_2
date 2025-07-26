package foatto.compose.control

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import foatto.compose.AppControl
import foatto.compose.Root
import foatto.compose.colorBottomBar
import foatto.compose.colorCheckBox
import foatto.compose.colorIconButton
import foatto.compose.colorMainBack0
import foatto.compose.colorMainText
import foatto.compose.colorScrollBarBack
import foatto.compose.colorScrollBarFore
import foatto.compose.colorTableCellBorder
import foatto.compose.colorTableCurrentRow
import foatto.compose.colorTableGroupBack0
import foatto.compose.colorTableGroupBack1
import foatto.compose.colorTableRowBack0
import foatto.compose.colorTableRowBack1
import foatto.compose.colorTableSelectorBack
import foatto.compose.colorTextButton
import foatto.compose.colorToolBar
import foatto.compose.composable.GenerateMenuBody
import foatto.compose.composable.ImageOrTextFromNameControl
import foatto.compose.control.composable.onPointerEvents
import foatto.compose.control.composable.table.TableImageOrTextCell
import foatto.compose.control.composable.table.TableToolBar
import foatto.compose.control.model.table.cell.TableBaseCellClient
import foatto.compose.control.model.table.cell.TableBooleanCellClient
import foatto.compose.control.model.table.cell.TableButtonCellClient
import foatto.compose.control.model.table.cell.TableCellDataClient
import foatto.compose.control.model.table.cell.TableGridCellClient
import foatto.compose.control.model.table.cell.TableSimpleCellClient
import foatto.compose.invokeRequest
import foatto.compose.model.MenuDataClient
import foatto.compose.singleButtonShape
import foatto.compose.styleOtherIconSize
import foatto.compose.utils.maxDp
import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.request.AppRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.ClientActionButton
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.ServerActionButton
import foatto.core.model.response.table.TableCaption
import foatto.core.model.response.table.TablePageButton
import foatto.core.model.response.table.TablePopup
import foatto.core.model.response.table.TableResponse
import foatto.core.model.response.table.TableRow
import foatto.core.model.response.table.cell.TableBooleanCell
import foatto.core.model.response.table.cell.TableButtonCell
import foatto.core.model.response.table.cell.TableCellAlign
import foatto.core.model.response.table.cell.TableCellBackColorType
import foatto.core.model.response.table.cell.TableGridCell
import foatto.core.model.response.table.cell.TableSimpleCell
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

typealias SelectorFunType = ((selectorData: Map<String, String>) -> Unit)
val tableSelectorFuns: MutableMap<Long, SelectorFunType> = mutableMapOf()

var tableClientActionFun: (
    action: AppAction,
    tableControl: TableControl
) -> Unit = { _: AppAction, _: TableControl ->
}

@OptIn(ExperimentalFoundationApi::class)
class TableControl(
    private val root: Root,
    private val appControl: AppControl,
    private val tableAction: AppAction,
    private val tableResponse: TableResponse,
    tabId: Int,
) : AbstractControl(tabId) {

    companion object {
        private val SCROLL_BAR_TICKNESS = 16.dp

        private val TABLE_CELL_PADDING = 16.dp
    }

    private var isFindTextVisible by mutableStateOf(root.isWideScreen)
    private var findText by mutableStateOf("")

    private val commonServerButtons = mutableStateListOf<ServerActionButton>()
    private val commonClientButtons = mutableStateListOf<ClientActionButton>()
    private val rowServerButtons = mutableStateListOf<ServerActionButton>()
    private val rowClientButtons = mutableStateListOf<ClientActionButton>()
    private val tablePageButtonData = mutableStateListOf<TablePageButton>()

    private var alCaptionData = mutableStateListOf<TableCaption>()

    private val alGridRows = mutableStateListOf<MutableList<TableBaseCellClient?>>()

    private val alRowData = mutableStateListOf<TableRow>()

    private val hmColumnMaxWidth = mutableStateMapOf<Int, Dp>()

    private var currentGridData by mutableStateOf<TableBaseCellClient?>(null)
    private var currentRowNo by mutableStateOf<Int?>(null)

    private var pageUpAction: AppAction? = null
    private var pageDownAction: AppAction? = null

    private var tableBodyWidth: Float by mutableFloatStateOf(0.0f)
    private var tableBodyHeight: Float by mutableFloatStateOf(0.0f)

    private var vScrollBarLength: Float by mutableFloatStateOf(0.0f)
    private val rowHeights = mutableStateMapOf<Int, Float>()

    private var hScrollBarLength: Float by mutableFloatStateOf(0.0f)
    private val colWidths = mutableStateMapOf<Int, Float>()

    private val verticalScrollState = ScrollState(0)
    private val horizontalScrollState = ScrollState(0)

    @Composable
    override fun Body() {
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (tableAction.isSelectorMode) {
                        Modifier.background(colorTableSelectorBack)
                    } else {
                        Modifier
                    }
                ),
        ) {
            GetHeader { action: AppAction ->
                coroutineScope.launch {
                    call(action, false)
                }
            }
            TableToolBar(
                isSelectorMode = tableAction.isSelectorMode,
                isWideScreen = root.isWideScreen,
                selectorCancelAction = if (tableAction.isSelectorMode) {
                    { closeSelector() }
                } else {
                    null
                },
                isFindTextVisible = isFindTextVisible,
                findText = findText,
                commonServerButtons = commonServerButtons,
                commonClientButtons = commonClientButtons,
                rowServerButtons = rowServerButtons,
                rowClientButtons = rowClientButtons,
                tableAction = tableAction,
                onFindInput = { newText: String -> findText = newText },
                doFind = { isClear: Boolean -> coroutineScope.launch { doFind(isClear) } },
                clientAction = { action: AppAction -> clientAction(action) },
                call = { action: AppAction, inNewTab: Boolean -> coroutineScope.launch { call(action, inNewTab) } },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorToolBar)
                    .horizontalScroll(state = horizontalScrollState)
            ) {
                alCaptionData.forEachIndexed { col, captionData ->
                    TextButton(
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(0.dp),   // чтобы получился сплошной ряд заголовков
                        colors = colorTextButton ?: ButtonDefaults.textButtonColors(),
                        modifier = Modifier.then(
                            hmColumnMaxWidth[col]?.let { maxColWidth ->
                                Modifier.width(maxColWidth)
                            } ?: Modifier
                        ),
                        onClick = {
                            captionData.action?.let { action ->
                                coroutineScope.launch {
                                    call(action, false)
                                }
                            }
                        }
                    ) {
                        Text(
                            text = captionData.name,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            TableBody(Modifier.weight(1.0f))

            //--- Page Bar
            if (tablePageButtonData.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorBottomBar),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
//                            borderTop(
//                                width = getStyleTableCaptionBorderTop().width,
//                                lineStyle = getStyleTableCaptionBorderTop().style,
//                                color = getStyleTableCaptionBorderTop().color,
//                            )
//                            backgroundColor(getColorTablePagebarBack())

                    for (pageButton in tablePageButtonData) {
                        TextButton(
                            modifier = Modifier.padding(1.dp),
                            shape = RoundedCornerShape(0.dp),   // чтобы получился почти сплошной ряд кнопок
                            colors = colorTextButton ?: ButtonDefaults.textButtonColors(),
                            enabled = pageButton.action != null,
//                                cursor("pointer")
                            onClick = {
                                pageButton.action?.let { action ->
                                    coroutineScope.launch {
                                        call(action, false)
                                    }
                                }
                            }
                        ) {
                            Text(pageButton.text)
                        }
                    }
                }
            }
        }
//!!!
        //coroutineScope.launch {
        //    gridState.scrollToItem(...)
        //}
    }

    @Composable
    fun TableBody(modifier: Modifier) {
        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()

        //--- bugfix for verticalScrollState/horizontalScrollState value cashing
        key(verticalScrollState, horizontalScrollState) {
            Column(
                modifier = modifier
                    .fillMaxHeight()
            ) {
                Row(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.0f)
                            .fillMaxHeight()
                            .onSizeChanged { size ->
                                tableBodyWidth = size.width.toFloat()
                                tableBodyHeight = size.height.toFloat()
                            }
                            .verticalScroll(state = verticalScrollState)
                            .horizontalScroll(state = horizontalScrollState)
                            .draggable(
                                orientation = Orientation.Horizontal,
                                state = rememberDraggableState { delta ->
                                    coroutineScope.launch {
                                        horizontalScrollState.scrollBy(-delta)
                                    }
                                },
                            )
                            .draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    coroutineScope.launch {
                                        verticalScrollState.scrollBy(-delta)
                                    }
                                },
                            )
                    ) {
                        alGridRows.forEachIndexed { index, alGridRow ->
                            Row(
                                modifier = Modifier
                                    //.width(intrinsicSize = IntrinsicSize.Max) - оставлю на память, чтобы опять не искать, но пользы/разницы не увидел
                                    //--- чтобы ячейки занимали всю отведённую им (общую) высоту (работает только совместно с .fillMaxHeight())
                                    .height(intrinsicSize = IntrinsicSize.Max)
                                    .onSizeChanged { size ->
                                        rowHeights[index] = size.height.toFloat()
                                    }
                            ) {
                                alGridRow.forEachIndexed { col, gridData ->
                                    Box(
                                        contentAlignment = gridData?.align ?: Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxHeight()    // работает только совместно с .height(intrinsicSize = IntrinsicSize.Max)
                                            .background(
                                                gridData?.let {
                                                    if (gridData.dataRow == currentRowNo) {
                                                        colorTableCurrentRow
                                                    } else {
                                                        gridData.backColor
                                                    }
                                                } ?: colorMainBack0
                                            )
                                            .border(BorderStroke(width = 0.1.dp, color = colorTableCellBorder))
                                            //.wrapContentSize(align = Alignment.Center, unbounded = true)  //- ничем не помогает
                                            //--- Непрямой метод поймать правую кнопку мыши. Ждём, когда завезут официальную реакцию на правую мышь.
                                            .pointerInput(Unit) {
                                                awaitEachGesture {
                                                    val event = awaitPointerEvent()
                                                    if (event.buttons.isSecondaryPressed) {
                                                        showPopupMenu(gridData)
                                                    }
                                                }
                                            }
                                            .combinedClickable(
                                                onDoubleClick = {
                                                    gridData?.dataRow?.let { dataRow ->
                                                        val rowData = alRowData[dataRow]
                                                        rowData.rowAction?.let { rowAction ->
                                                            coroutineScope.launch {
                                                                call(rowAction, rowData.isRowUrlInNewTab)
                                                            }
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    currentGridData = gridData
                                                    setCurrentRow(gridData?.dataRow)
                                                },
                                                onLongClick = {
                                                    showPopupMenu(gridData)
                                                },
                                            )
                                            .onSizeChanged { size ->
                                                gridData?.let {
                                                    val componentWidth = with(density) {
                                                        maxDp(size.width.toDp(), gridData.minWidth.dp)
                                                    }
                                                    gridData.componentWidth = maxDp(gridData.componentWidth, componentWidth)

                                                    hmColumnMaxWidth[col] = maxDp(
                                                        hmColumnMaxWidth[col] ?: 0.dp,
                                                        if (gridData.colSpan == 1) {
                                                            componentWidth
                                                        } else {
                                                            8.dp    // minWidth for explicitly empty column (from group-column)
                                                        }
                                                    )

                                                    colWidths[col] = with(density) {
                                                        hmColumnMaxWidth[col]?.toPx() ?: 0.0f
                                                    }
                                                }
                                            }
                                            .then(
                                                gridData?.let {
                                                    var maxColWidth: Dp? = null
                                                    for (c in col until col + gridData.colSpan) {
                                                        hmColumnMaxWidth[c]?.let { cmw ->
                                                            maxColWidth = (maxColWidth ?: 0.dp) + cmw
                                                        }
                                                    }
                                                    maxColWidth?.let { mcw ->
                                                        if (gridData.componentWidth < mcw) {
                                                            Modifier.width(mcw)
                                                        } else {
                                                            Modifier.width(gridData.componentWidth)
                                                        }
                                                    }
                                                } ?: Modifier
                                            )
                                    ) {
                                        when (gridData) {
                                            is TableBooleanCellClient -> {
                                                Checkbox(
                                                    modifier = Modifier.padding(TABLE_CELL_PADDING),    //.background(Color.Transparent), - и без этого хорошо
                                                    colors = colorCheckBox ?: CheckboxDefaults.colors(),
                                                    checked = gridData.value,
                                                    onCheckedChange = null,
                                                )
                                            }

                                            is TableSimpleCellClient -> {
                                                TableImageOrTextCell(
                                                    baseCellData = gridData,
                                                    cellData = gridData.data,
                                                    modifier = Modifier.padding(TABLE_CELL_PADDING),
                                                )
                                            }

                                            is TableButtonCellClient -> {
                                                Column(
                                                    modifier = Modifier.padding(TABLE_CELL_PADDING),
                                                ) {
                                                    gridData.data.forEach { cellData ->
                                                        ImageOrTextFromNameControl(
                                                            name = cellData.name,
                                                            iconSize = styleOtherIconSize,
                                                            imageButton = { func ->
                                                                FilledIconButton(
                                                                    shape = singleButtonShape,
                                                                    colors = colorIconButton ?: IconButtonDefaults.iconButtonColors(),
                                                                    onClick = {
                                                                        cellData.action?.let {
                                                                            coroutineScope.launch {
                                                                                call(cellData.action, cellData.inNewTab)
                                                                            }
                                                                        }
                                                                    }
                                                                ) {
                                                                    func()
                                                                }
                                                            },
                                                            textButton = { caption ->
                                                                TextButton(
                                                                    shape = singleButtonShape,
                                                                    colors = colorTextButton ?: ButtonDefaults.textButtonColors(),
                                                                    onClick = {
                                                                        cellData.action?.let {
                                                                            coroutineScope.launch {
                                                                                call(cellData.action, cellData.inNewTab)
                                                                            }
                                                                        }
                                                                    }
                                                                ) {
                                                                    Text(
                                                                        text = caption,
//                                                                color = gridData.textColor,
                                                                        fontWeight = if (gridData.isBoldText) {
                                                                            FontWeight.Bold
                                                                        } else {
                                                                            null
                                                                        },
                                                                    )
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            }

                                            is TableGridCellClient -> {
                                                Column(
                                                    modifier = Modifier.padding(TABLE_CELL_PADDING),
                                                ) {
                                                    gridData.data.forEach { alRow ->
                                                        Row(
                                                            //horizontalArrangement = Arrangement.SpaceBetween, - don't work properly (?), use Spacer
                                                        ) {
                                                            alRow.forEachIndexed { index, cellData ->
                                                                if (index > 0) {
                                                                    Spacer(modifier = Modifier.weight(1.0f))
                                                                }
                                                                val modifier = if (index > 0) {
                                                                    Modifier.padding(start = TABLE_CELL_PADDING)
                                                                } else {
                                                                    Modifier
                                                                }
                                                                TableImageOrTextCell(
                                                                    baseCellData = gridData,
                                                                    cellData = cellData,
                                                                    modifier = modifier,
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            else -> {}
                                        }
                                        if (!tableAction.isSelectorMode) {
                                            gridData?.let {
                                                DropdownMenu(
                                                    expanded = gridData.isShowPopupMenu,
                                                    onDismissRequest = {
                                                        gridData.isShowPopupMenu = false
                                                    },
                                                ) {
                                                    GenerateMenuBody(
                                                        alMenuDataClient = gridData.alCurrentPopupData!!,
                                                        level = 0,
                                                        closeMenu = {
                                                            gridData.isShowPopupMenu = false
                                                        },
                                                        menuClick = { action: AppAction, inNewTab: Boolean ->
                                                            gridData.isShowPopupMenu = false
                                                            coroutineScope.launch {
                                                                call(action, inNewTab)
                                                            }
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    VerticalScrollBody()
                }
                HorizontalScrollBody()
            }
        }
    }

    @Composable
    private fun VerticalScrollBody() {
        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()

        Canvas(
            modifier = Modifier
                .width(SCROLL_BAR_TICKNESS)
                .fillMaxHeight()
                .onSizeChanged { size ->
                    vScrollBarLength = size.height.toFloat()
                }
                .clipToBounds()
                .onPointerEvents(
                    withInteractive = true,
                    onPointerDown = { pointerInputChange -> },
                    onPointerUp = { pointerInputChange -> },
                    onDragStart = { offset -> },
                    onDrag = { pointerInputChange, offset ->
                        val dy = offset.y
                        if (
                            dy < 0 && verticalScrollState.value > 0 ||
                            dy > 0 && verticalScrollState.value < verticalScrollState.maxValue
                        ) {
                            coroutineScope.launch {
                                val rowHeightSum = rowHeights.values.sum()
                                val incScaleY = max(1.0f, rowHeightSum / vScrollBarLength)
                                verticalScrollState.scrollBy(dy * incScaleY)
                            }
                        }
                    },
                    onDragEnd = { },
                    onDragCancel = { },
                )
        ) {
            drawRect(
                topLeft = Offset(0.0f, 0.0f),
                size = Size(with(density) { SCROLL_BAR_TICKNESS.toPx() }, vScrollBarLength),
                color = colorScrollBarBack,
                style = Fill,
            )
            val rowHeightSum = rowHeights.values.sum()
            val decScaleY = min(1.0f, tableBodyHeight / rowHeightSum)
            val scrollBarH = vScrollBarLength * decScaleY
            val scrollBarY = if (tableBodyHeight >= rowHeightSum) {
                0.0f
            } else {
                (vScrollBarLength - scrollBarH) * verticalScrollState.value / verticalScrollState.maxValue
            }
            drawRect(
                topLeft = Offset(0.0f, scrollBarY),
                size = Size(with(density) { SCROLL_BAR_TICKNESS.toPx() }, scrollBarH),
                color = colorScrollBarFore,
                style = Fill,
            )
        }
    }

    @Composable
    private fun HorizontalScrollBody() {
        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(SCROLL_BAR_TICKNESS)
                .onSizeChanged { size ->
                    hScrollBarLength = size.width.toFloat()
                }
                .clipToBounds()
                .onPointerEvents(
                    withInteractive = true,
                    onPointerDown = { pointerInputChange -> },
                    onPointerUp = { pointerInputChange -> },
                    onDragStart = { offset -> },
                    onDrag = { pointerInputChange, offset ->
                        val dx = offset.x
                        if (
                            dx < 0 && horizontalScrollState.value > 0 ||
                            dx > 0 && horizontalScrollState.value < horizontalScrollState.maxValue
                        ) {
                            coroutineScope.launch {
                                val colWidthSum = colWidths.values.sum()
                                val incScaleX = max(1.0f, colWidthSum / hScrollBarLength)
                                horizontalScrollState.scrollBy(dx * incScaleX)
                                //horizontalScrollState.scrollTo((horizontalScrollState.value + dx * incScaleX).toInt())
                            }
                        }
                    },
                    onDragEnd = { },
                    onDragCancel = { },
                )
        ) {
            drawRect(
                topLeft = Offset(0.0f, 0.0f),
                size = Size(hScrollBarLength, with(density) { SCROLL_BAR_TICKNESS.toPx() }),
                color = colorScrollBarBack,
                style = Fill,
            )
            val colWidthSum = colWidths.values.sum()
            val decScaleX = min(1.0f, tableBodyWidth / colWidthSum)
            val scrollBarW = hScrollBarLength * decScaleX
            val scrollBarX = if (tableBodyWidth >= colWidthSum) {
                0.0f
            } else {
                (hScrollBarLength - scrollBarW) * horizontalScrollState.value / horizontalScrollState.maxValue
            }
            drawRect(
                topLeft = Offset(scrollBarX, 0.0f),
                size = Size(scrollBarW, with(density) { SCROLL_BAR_TICKNESS.toPx() }),
                color = colorScrollBarFore,
                style = Fill,
            )
        }
    }

    override suspend fun start() {
        root.setTabInfo(tabId, tableResponse.tabCaption)

        startTableBody()
    }

    fun startTableBody() {
        //--- вынесено сюда для использования во внешнем CompositeControl
        headerData = tableResponse.headerData

        findText = tableResponse.findText

        commonServerButtons.clear()
        commonServerButtons.addAll(tableResponse.serverActionButtons)

        commonClientButtons.clear()
        commonClientButtons.addAll(tableResponse.clientActionButtons)

        readPageButtons()
        readTable()

        //--- установка текущей строки
        setCurrentRow(tableResponse.selectedRowNo)

        //--- запоминаем текущий appParam для возможной установки в виде стартовой
        root.curAction = tableAction

//        //--- две попытки установить фокус, с минимальными задержками
//        window.setTimeout({
//            if (!focusToCursorField(tabId)) {
//                window.setTimeout({
//                    focusToCursorField(tabId)
//                }, 100)
//            }
//        }, 100)
    }

    private fun readPageButtons() {
        pageUpAction = null
        pageDownAction = null
        tablePageButtonData.clear()

        var isEmptyPassed = false
        //--- вывести новую разметку страниц
        for (pageButton in tableResponse.tablePageButtonData) {
            tablePageButtonData.add(pageButton)

            pageButton.action?.let { action ->
                if (!isEmptyPassed) {
                    pageUpAction = action
                }
                if (isEmptyPassed && pageDownAction == null) {
                    pageDownAction = action
                }
            } ?: run {
                isEmptyPassed = true
            }
        }
    }

    private fun readTable() {
        alCaptionData.clear()
        alCaptionData.addAll(tableResponse.columnCaptions)

        alGridRows.clear()

        var maxColCount = tableResponse.columnCaptions.size

        for (tc in tableResponse.tableCells) {
            maxColCount = max(maxColCount, tc.col + tc.colSpan)

            val backColor = tc.backColor?.let { bc ->
                Color(bc)
            } ?: when (tc.backColorType) {
                TableCellBackColorType.GROUP_0 -> colorTableGroupBack0
                TableCellBackColorType.GROUP_1 -> colorTableGroupBack1
                else -> if (tc.dataRow % 2 == 0) {
                    colorTableRowBack0
                } else {
                    colorTableRowBack1
                }
            }
            val textColor = tc.foreColor?.let { fc ->
                Color(fc)
            } ?: colorMainText

            val align = when (tc.align) {
                TableCellAlign.LEFT -> Alignment.CenterStart
                TableCellAlign.CENTER -> Alignment.Center
                TableCellAlign.RIGHT -> Alignment.CenterEnd
                else -> Alignment.Center
            }

            val tableCellClient = when (tc) {
                is TableBooleanCell -> {
                    TableBooleanCellClient(
                        colSpan = tc.colSpan,
                        dataRow = tc.dataRow,
                        minWidth = tc.minWidth,
                        align = align,
                        backColor = backColor,
                        textColor = textColor,
                        isBoldText = tc.isBoldText,
                        value = tc.value,
                    )
                }

                is TableSimpleCell -> {
                    TableSimpleCellClient(
                        colSpan = tc.colSpan,
                        dataRow = tc.dataRow,
                        minWidth = tc.minWidth,
                        align = align,
                        backColor = backColor,
                        textColor = textColor,
                        isBoldText = tc.isBoldText,
                        //tc.isWordWrap
                        data = TableCellDataClient(
                            name = tc.name,
                        )
                    )
                }

                is TableButtonCell -> {
                    TableButtonCellClient(
                        colSpan = tc.colSpan,
                        dataRow = tc.dataRow,
                        minWidth = tc.minWidth,
                        align = align,
                        backColor = backColor,
                        textColor = textColor,
                        isBoldText = tc.isBoldText,
                        data = tc.values.map { tableButtonCellData ->
                            TableCellDataClient(
                                name = tableButtonCellData.name,
                                action = tableButtonCellData.action,
                                inNewTab = tableButtonCellData.inNewTab,
                            )
                        }
                    )
                }

                is TableGridCell -> {
                    TableGridCellClient(
                        colSpan = tc.colSpan,
                        dataRow = tc.dataRow,
                        minWidth = tc.minWidth,
                        align = align,
                        backColor = backColor,
                        textColor = textColor,
                        isBoldText = tc.isBoldText,
                        data = tc.values.map { alTableGridCellData ->
                            alTableGridCellData.map { name ->
                                TableCellDataClient(
                                    name = name,
                                )
                            }
                        }
                    )
                }

            }

            addCellToGrid(
                alGridRows = alGridRows,
                maxColCount = maxColCount,
                row = tc.row,
                col = tc.col,
                cell = tableCellClient,
            )
        }

        alRowData.clear()
        alRowData.addAll(tableResponse.tableRows)
    }

    private fun closeTabById() {
        root.closeTabById(tabId)
    }

    private suspend fun doFind(isClear: Boolean) {
        if (isClear) {
            findText = ""
        }

        if (!isClear && !isFindTextVisible) {
            isFindTextVisible = true
        } else {
            call(
                newAppAction = tableAction.copy(
                    findText = findText.trim(),
                    pageNo = 0,
                ),
                inNewTab = false,
            )
        }
    }

    private fun setCurrentRow(rowNo: Int?) {
        rowServerButtons.clear()
        rowNo?.let {
            rowServerButtons.addAll(alRowData[rowNo].serverActionButtons)
        }

        rowClientButtons.clear()
        rowNo?.let {
            rowClientButtons.addAll(alRowData[rowNo].clientActionButtons)
        }

        currentRowNo = rowNo

//        focusToCursorField(tabId)
    }

    private fun clientAction(action: AppAction) {
        tableClientActionFun(action, this)
    }

    private suspend fun doKeyEnter() {
        currentRowNo?.let { curRow ->
            if (curRow >= 0 && curRow < alRowData.size) {
                val curRowData = alRowData[curRow]
                curRowData.rowAction?.let { rowAction ->
                    call(rowAction, curRowData.isRowUrlInNewTab)
                }
            }
        }
    }

    private fun doKeyEsc() {
        if (tableAction.isSelectorMode) {
            closeSelector()
        }
    }

    private fun doKeyUp() {
        currentRowNo?.let { curRow ->
            if (curRow > 0) {
                setCurrentRow(curRow - 1)
            }
        }
    }

    private fun doKeyDown() {
        currentRowNo?.let { curRow ->
            if (curRow < alRowData.lastIndex) {
                setCurrentRow(curRow + 1)
            }
        }
    }

    private fun doKeyHome() {
        if (alRowData.isNotEmpty()) {
            setCurrentRow(0)
        }
    }

    private fun doKeyEnd() {
        if (alRowData.isNotEmpty()) {
            setCurrentRow(alRowData.lastIndex)
        }
    }

    private suspend fun doKeyPageUp() {
        pageUpAction?.let { action ->
            call(action, false)
        }
    }

    private suspend fun doKeyPageDown() {
        pageDownAction?.let { action ->
            call(action, false)
        }
    }

    suspend fun refreshTableFromComposite() {
//!!!        call(tableAction, false, false) - сейчас обновляется вся вкладка!
    }

    private suspend fun call(newAppAction: AppAction, inNewTab: Boolean, withWait: Boolean = true) {
        if (tableAction.isSelectorMode) {
            when (newAppAction.type) {
                ActionType.FORM_SELECTOR -> {
                    tableSelectorFuns[tableAction.selectorFunId]?.invoke(newAppAction.selectorData)
                    closeSelector()
                }

                ActionType.MODULE_TABLE -> {
                    val selectorAppAction = newAppAction.copy(
                        prevAction = tableAction,
                        isSelectorMode = true,
                        selectorFunId = tableAction.selectorFunId,
                    )
                    if (withWait) {
                        root.setWait(true)
                    }
                    invokeRequest(
                        AppRequest(
                            action = selectorAppAction
                        )
                    ) { appResponse: AppResponse ->
                        when (appResponse.responseCode) {
                            ResponseCode.MODULE_TABLE -> {
                                root.selectorControl = TableControl(root, appControl, selectorAppAction, appResponse.table!!, tabId)
                                root.selectorControl?.start()
                            }

                            else -> {
                                //!!! dialog с ошибкой
                                println("Wrong response.responseCode in SELECTOR_MODE = ${appResponse.responseCode}")
                            }
                        }
                        if (withWait) {
                            root.setWait(false)
                        }
                    }

                }

                else -> {    //--- невозможная (?) ситуация
                    //!!! dialog с ошибкой
                    println("Wrong call in SELECTOR_MODE = $newAppAction")
                }
            }
        } else {
            if (inNewTab) {
                root.openTab(newAppAction)
            } else {
                appControl.call(
                    AppRequest(
                        action = newAppAction.copy(
                            prevAction = tableAction,
                        )
                    )
                )
            }
        }
    }

    private fun closeSelector() {
        tableSelectorFuns.remove(tableAction.selectorFunId)
        root.selectorControl = null
    }

    private fun showPopupMenu(gridData: TableBaseCellClient?) {
        gridData?.let {
            //--- чтобы строчка выделялась и по правой кнопке мыши тоже
            setCurrentRow(gridData.dataRow)
            gridData.dataRow?.let { dataRow ->
                if (alRowData[dataRow].tablePopups.isNotEmpty()) {
                    convertPopupMenuDataClient(gridData, alRowData[dataRow].tablePopups)
                    gridData.isShowPopupMenu = true
                } else {
                    gridData.isShowPopupMenu = false
                }
            } ?: run {
                gridData.isShowPopupMenu = false
            }
        }
    }

    private fun convertPopupMenuDataClient(gridData: TableBaseCellClient, alMenuDataClient: List<TablePopup>) {
        val alCurPopupData = mutableListOf<MenuDataClient>()

        var i = 0
        while (i < alMenuDataClient.size) {
            val menuDataClient = alMenuDataClient[i]

            menuDataClient.group?.let { groupName ->
                val alPopupSubMenuDataClient = mutableListOf<MenuDataClient>()
                while (i < alMenuDataClient.size) {
                    val subMenuDataClient = alMenuDataClient[i]
                    if (subMenuDataClient.group == null || subMenuDataClient.group != groupName) {
                        break
                    }

                    alPopupSubMenuDataClient.add(
                        MenuDataClient(
                            action = subMenuDataClient.action,
                            caption = subMenuDataClient.text,
                            alSubMenu = null,
                            inNewTab = subMenuDataClient.inNewTab
                        )
                    )
                    i++
                }
                alCurPopupData.add(
                    MenuDataClient(
                        action = null,
                        caption = groupName,
                        alSubMenu = alPopupSubMenuDataClient,
                        inNewTab = false
                    )
                )
            } ?: run {
                alCurPopupData.add(
                    MenuDataClient(
                        action = menuDataClient.action,
                        caption = menuDataClient.text,
                        alSubMenu = null,
                        inNewTab = menuDataClient.inNewTab,
                    )
                )
                i++
            }
        }

        gridData.alCurrentPopupData = alCurPopupData
    }

//    private fun focusToCursorField(tabId: Int): Boolean {
//        document.getElementById("table_cursor_$tabId")?.let { element ->
//            if (element is HTMLElement) {
//                element.focus()
//                return true
//            }
//        }
//        return false
//    }

}