package foatto.compose.control

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import foatto.compose.AppControl
import foatto.compose.Root
import foatto.compose.colorCompositeMovedBlockBack
import foatto.compose.colorIconButton
import foatto.compose.colorMainBack2
import foatto.compose.colorScrollBarBack
import foatto.compose.colorScrollBarFore
import foatto.compose.control.composable.composite.CompositeToolBar
import foatto.compose.control.composable.onPointerEvents
import foatto.compose.control.model.CompositeListItem
import foatto.compose.control.model.composite.CompositeBlockControl
import foatto.compose.control.model.composite.CompositeWorkMode
import foatto.compose.getStyleToolbarIconNameSuffix
import foatto.compose.invokeRequest
import foatto.compose.singleButtonShape
import foatto.compose.styleToolbarIconSize
import foatto.compose.utils.getFullUrl
import foatto.core.ActionType
import foatto.core.model.request.CompositeActionRequest
import foatto.core.model.request.SaveUserPropertyRequest
import foatto.core.model.response.CompositeActionResponse
import foatto.core.model.response.SaveUserPropertyResponse
import foatto.core.model.response.composite.CompositeBlock
import foatto.core.model.response.composite.CompositeLayoutData
import foatto.core.model.response.composite.CompositeListItemData
import foatto.core.model.response.composite.CompositeResponse
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.math.max
import kotlin.math.min

class CompositeControl(
    protected val root: Root,
    private val appControl: AppControl,
    protected val compositeResponse: CompositeResponse,
    tabId: Int,
) : AbstractControl(tabId) {

    companion object {
        private val SCROLL_BAR_TICKNESS = 16.dp

        //--- 350 - узковато - не помещаются некоторые наименования, при этом 6-й столбец на стандартный FullHD уже не помещается;
        //--- 400 - ширина достаточна, при этом 5-й столбец на стандартный FullHD помещается не полностью;
        //--- 380 == (1920 - 16) / 5 (с учётом вертикального тулбара)
        const val BLOCK_MIN_WIDTH: Int = 360

        //--- 200 - низковато, соседние заголовки и подписи располагаются слишком близко
        const val BLOCK_MIN_HEIGHT: Int = 240
    }

    private var isToolBarsVisible by mutableStateOf(true)

    private var isPanButtonEnabled by mutableStateOf(false)
    private var isLayoutButtonEnabled by mutableStateOf(true)

    private var isBlocksVisibilityButtonVisible by mutableStateOf(false)
    private var isShowBlocksList by mutableStateOf(false)
    private var isLayoutSaveButtonVisible by mutableStateOf(false)

    private var isRefreshButtonsVisible by mutableStateOf(true)
    private var refreshInterval: Int by mutableIntStateOf(0)

    private var canvasWidth: Float by mutableFloatStateOf(0.0f)
    private var canvasHeight: Float by mutableFloatStateOf(0.0f)

    private var oneBlockWidth by mutableFloatStateOf(0.0f)
    private var oneBlockHeight by mutableFloatStateOf(0.0f)

    private var vScrollBarLength: Float by mutableFloatStateOf(0.0f)
    private var hScrollBarLength: Float by mutableFloatStateOf(0.0f)

    private var screenOffsetX: Float by mutableFloatStateOf(0.0f)
    private var screenOffsetY: Float by mutableFloatStateOf(0.0f)

    private var curMode = CompositeWorkMode.PAN

    private var listItems: List<CompositeListItem>? = null
    private val verticalScrollState = ScrollState(0)

    private val blocks = mutableStateListOf<CompositeBlockControl>()

    private var layoutSaveKey: String? = null

    private var movingBlock: CompositeBlockControl? by mutableStateOf(null)

    @Composable
    override fun Body() {
        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()
//        val textMeasurer = rememberTextMeasurer()

        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            key(listItems, verticalScrollState, blocks) {
                listItems?.let { items ->
                    Column(
                        modifier = Modifier
                            .width(360.dp)
                            .fillMaxHeight()
                            .background(color = colorMainBack2)
                            .padding(16.dp)
                            .verticalScroll(state = verticalScrollState)
                    ) {
                        GenerateItemList(items, 0)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight()
                ) {
                    GetHeader()

                    CompositeToolBar(
                        isWideScreen = root.isWideScreen,
                        isToolBarsVisible = isToolBarsVisible,
                        isPanButtonEnabled = isPanButtonEnabled,
                        isLayoutButtonEnabled = isLayoutButtonEnabled,
                        isBlocksVisibilityButtonVisible = isBlocksVisibilityButtonVisible,
                        isShowBlocksList = isShowBlocksList,
                        blocks = blocks,
                        isLayoutSaveButtonVisible = isLayoutSaveButtonVisible,
                        isRefreshButtonsVisible = isRefreshButtonsVisible,
                        refreshInterval = refreshInterval,
                        setMode = { compositeWorkMode: CompositeWorkMode -> setMode(compositeWorkMode) },
                        onShowBlocksList = { isShowBlocksList = !isShowBlocksList },
                        doCloseBlocksList = { isShowBlocksList = false },
                        onBlocksListClick = { block: CompositeBlockControl ->
                            changeBlockVisibility(block)
                        },
                        saveLayout = { saveLayout() },
                        setInterval = { interval: Int -> coroutineScope.launch { setInterval(interval) } },
                    )

                    Row(
                        modifier = Modifier
                            .weight(1.0f)
                            .fillMaxWidth()
                    ) {
                        if (blocks.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1.0f)
                                    .fillMaxSize()
                                    .clipToBounds()
                                    .onSizeChanged { size ->
                                        canvasWidth = size.width.toFloat()
                                        canvasHeight = size.height.toFloat()

                                        oneBlockWidth = max(BLOCK_MIN_WIDTH * root.scaleKoef, canvasWidth / getBlockColSum())
                                        oneBlockHeight = max(BLOCK_MIN_HEIGHT * root.scaleKoef, canvasHeight / getBlockRowSum())
                                    }
                                    .onPointerEvents(
                                        withInteractive = true,
                                        onPointerDown = { pointerInputChange -> },
                                        onPointerUp = { pointerInputChange -> onPointerUp(pointerInputChange) },
                                        onDragStart = { offset -> },
                                        onDrag = { pointerInputChange, offset -> onDrag(pointerInputChange, offset.x, offset.y) },
                                        onDragEnd = { },
                                        onDragCancel = { },
                                    ),
                            ) {
                                for (block in blocks) {
                                    BlockBody(block) { blockX, blockY, blockW, blockH ->
                                        Box(
                                            modifier = Modifier
                                                .offset(
                                                    x = with(density) { blockX.toDp() },
                                                    y = with(density) { blockY.toDp() },
                                                )
                                                .size(
                                                    width = with(density) { blockW.toDp() },
                                                    height = with(density) { blockH.toDp() },
                                                )
                                        ) {
                                            block.chartBlock?.MainChartBody(false)
                                                ?: block.mapBlock?.getXyElementTemplate(false)
                                                ?: block.schemeBlock?.getXyElementTemplate(false)
                                                ?: block.tableBlock?.TableBody(Modifier)
                                                ?: run {
                                                    Text(text = "Не задан тип блока!")
                                                }
                                        }
                                    }
                                }

                                movingBlock?.let { block ->
                                    BlockBody(block) { blockX, blockY, blockW, blockH ->
                                        Box(
                                            modifier = Modifier
                                                .offset(
                                                    x = with(density) { blockX.toDp() },
                                                    y = with(density) { blockY.toDp() },
                                                )
                                                .size(
                                                    width = with(density) { blockW.toDp() },
                                                    height = with(density) { blockH.toDp() },
                                                )
                                                .background(color = colorCompositeMovedBlockBack)
                                        )

                                        val iconSize = styleToolbarIconSize

                                        val blockCenterX = blockX + blockW / 2 - iconSize / 2
                                        val blockCenterY = blockY + blockH / 2 - iconSize / 2

                                        LayoutButton(
                                            modifier = Modifier.offset(
                                                x = with(density) { blockCenterX.toDp() },
                                                y = with(density) { blockCenterY.toDp() },
                                            ),
                                            iconName = "/images/ic_done_${getStyleToolbarIconNameSuffix()}.png",
                                            isEnabled = true
                                        ) {
                                            movingBlock = null
                                            isToolBarsVisible = true
                                        }

                                        LayoutButton(
                                            modifier = Modifier.offset(
                                                x = with(density) { blockCenterX.toDp() },
                                                y = with(density) { (blockCenterY - iconSize * 2).toDp() },
                                            ),
                                            iconName = "/images/ic_arrow_upward_${getStyleToolbarIconNameSuffix()}.png",
                                            isEnabled = true
                                        ) {
                                            onBlockMoveUp(block)
                                        }

                                        LayoutButton(
                                            modifier = Modifier.offset(
                                                x = with(density) { (blockCenterX - iconSize * 2).toDp() },
                                                y = with(density) { blockCenterY.toDp() },
                                            ),
                                            iconName = "/images/ic_arrow_back_${getStyleToolbarIconNameSuffix()}.png",
                                            isEnabled = true
                                        ) {
                                            onBlockMoveLeft(block)
                                        }

                                        LayoutButton(
                                            modifier = Modifier.offset(
                                                x = with(density) { (blockCenterX + iconSize * 2).toDp() },
                                                y = with(density) { blockCenterY.toDp() },
                                            ),
                                            iconName = "/images/ic_arrow_forward_${getStyleToolbarIconNameSuffix()}.png",
                                            isEnabled = true
                                        ) {
                                            onBlockMoveRight(block)
                                        }

                                        LayoutButton(
                                            modifier = Modifier.offset(
                                                x = with(density) { blockCenterX.toDp() },
                                                y = with(density) { (blockCenterY + iconSize * 2).toDp() },
                                            ),
                                            iconName = "/images/ic_arrow_downward_${getStyleToolbarIconNameSuffix()}.png",
                                            isEnabled = true
                                        ) {
                                            onBlockMoveDown(block)
                                        }

                                        LayoutButton(
                                            modifier = Modifier.offset(
                                                x = with(density) { (blockCenterX + iconSize * 3).toDp() },
                                                y = with(density) { (blockCenterY + iconSize * 2).toDp() },
                                            ),
                                            iconName = "/images/ic_visibility_off_${getStyleToolbarIconNameSuffix()}.png",
                                            isEnabled = true
                                        ) {
                                            block.isHidden = true
                                            block.x = -block.x - 1    // добавим -1 с учётом возможных координат 0,0
                                            block.y = -block.y - 1

                                            movingBlock = null
                                            isToolBarsVisible = true
                                        }
                                    }
                                }
                            }
                            VerticalScrollBody()
                        }
                    }
                    if (blocks.isNotEmpty()) {
                        HorizontalScrollBody()
                    }
                }
            }
        }

        coroutineScope.launch {
            while (refreshInterval > 0) {
                refreshAll()
                delay(refreshInterval * 1000L)
            }
        }
    }

    @Composable
    fun GenerateItemList(
        items: List<CompositeListItem>,
        level: Int,
    ) {
        for (item in items) {
            item.subListDatas?.let { subItems ->
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            modifier = Modifier.clickable(
                                onClick = {
                                    item.isExpanded.value = !item.isExpanded.value
                                }
                            ),
                            imageVector = if (item.isExpanded.value) {
                                Icons.Default.KeyboardArrowUp
                            } else {
                                Icons.Default.KeyboardArrowDown
                            },
                            contentDescription = null,
                        )
                    },
                    text = {
                        Text(
                            text = item.text,
                            fontWeight = if (level == 0) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                        )
                    },
                    contentPadding = PaddingValues(start = getMenuPadding(level), end = 16.dp),
                    onClick = {
                        compositeBlocksReload(
                            id = item.itemId,
                            parentModule = item.itemModule,
                        )
                    },
                )
                if (item.isExpanded.value) {
                    GenerateItemList(
                        items = subItems,
                        level = level + 1,
                    )
                }
            } ?: run {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item.text,
                            fontWeight = if (level == 0) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                        )
                    },
                    contentPadding = PaddingValues(start = getMenuPadding(level), end = 16.dp),
                    onClick = {
                        compositeBlocksReload(
                            id = item.itemId,
                            parentModule = item.itemModule,
                        )
                    },
                )
            }
        }
    }

    private fun getMenuPadding(level: Int) = ((1 + level) * 16).dp

    @Composable
    fun BlockBody(
        block: CompositeBlockControl,
        content: @Composable (blockX: Float, blockY: Float, blockW: Float, blockH: Float) -> Unit,
    ) {
        val blockX = block.x * oneBlockWidth + screenOffsetX
        val blockY = block.y * oneBlockHeight + screenOffsetY

        val blockW = block.w * oneBlockWidth
        val blockH = block.h * oneBlockHeight

        content(blockX, blockY, blockW, blockH)
    }

    @Composable
    fun LayoutButton(
        modifier: Modifier,
        iconName: String,
        isEnabled: Boolean,
        onClick: () -> Unit,
    ) {
        FilledIconButton(
            modifier = modifier,
            shape = singleButtonShape,
            colors = colorIconButton ?: IconButtonDefaults.iconButtonColors(),
            enabled = isEnabled,
            onClick = {
                onClick()
            }
        ) {
            KamelImage(
                modifier = Modifier.size(styleToolbarIconSize.dp),
                resource = asyncPainterResource(data = getFullUrl(iconName)),
                contentDescription = iconName,
            )
        }
    }

    @Composable
    private fun VerticalScrollBody() {
        val density = LocalDensity.current

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
                        val pixEndY = getBlockRowSum() * oneBlockHeight
                        val incScaleY = pixEndY / vScrollBarLength
                        onDrag(pointerInputChange, 0.0f, -offset.y * incScaleY)
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
            val pixEndY = getBlockRowSum() * oneBlockHeight
            val decScaleY = min(1.0f, canvasHeight / pixEndY)
            val scrollBarH = vScrollBarLength * decScaleY
            val scrollBarY = if (canvasHeight >= pixEndY) {
                0.0f
            } else {
                (vScrollBarLength - scrollBarH) * (-screenOffsetY) / (pixEndY - canvasHeight)
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
                        val pixEndX = getBlockColSum() * oneBlockWidth
                        val incScaleX = pixEndX / hScrollBarLength
                        onDrag(pointerInputChange, -offset.x * incScaleX, 0.0f)
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
            val pixEndX = getBlockColSum() * oneBlockWidth
            val decScaleX = min(1.0f, canvasWidth / pixEndX)
            val scrollBarW = hScrollBarLength * decScaleX
            val scrollBarX = if (canvasWidth >= pixEndX) {
                0.0f
            } else {
                (hScrollBarLength - scrollBarW) * (-screenOffsetX) / (pixEndX - canvasWidth)
            }
            drawRect(
                topLeft = Offset(scrollBarX, 0.0f),
                size = Size(scrollBarW, with(density) { SCROLL_BAR_TICKNESS.toPx() }),
                color = colorScrollBarFore,
                style = Fill,
            )
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun start() {
        root.setTabInfo(tabId, compositeResponse.tabCaption)

        compositeResponse.items?.let { items ->
            listItems = items.map { item ->
                mapListItems(item)
            }
        }

        GlobalScope.launch {
            compositeBlocksReload(
                id = compositeResponse.items?.let {
                    null
                } ?: compositeResponse.action.id,
                parentModule = compositeResponse.items?.let {
                    null
                } ?: compositeResponse.action.parentModule,
            )
        }
        /*
            !!! setInterval(10)
         */
    }

    private fun mapListItems(compositeListItemData: CompositeListItemData): CompositeListItem = CompositeListItem(
        text = compositeListItemData.text,
        itemId = compositeListItemData.itemId,
        itemModule = compositeListItemData.itemModule,
        subListDatas = compositeListItemData.subListDatas?.map { subListData ->
            mapListItems(subListData)
        },
    )

    private fun compositeBlocksReload(
        id: Int?,
        parentModule: String?,
    ) {
        root.setWait(true)
        invokeRequest(
            CompositeActionRequest(
                action = compositeResponse.action.copy(
                    type = ActionType.GET_ELEMENTS,
                    id = id,
                    parentModule = parentModule,
                ),
            )
        ) { compositeActionResponse: CompositeActionResponse ->

            //--- сбрасываем горизонтальный скроллинг/смещение
            screenOffsetX = 0.0f
            screenOffsetY = 0.0f

            headerData = compositeActionResponse.headerData

            blocks.clear()
            compositeActionResponse.blocks.forEach { blockControlData ->
                readBlock(blockControlData)
            }

            layoutSaveKey = compositeActionResponse.layoutSaveKey

            root.setWait(false)
        }
    }

    private suspend fun readBlock(blockControlData: CompositeBlock) {
        blocks += CompositeBlockControl(
            id = blockControlData.id,
            chartBlock = blockControlData.chartResponse?.let { chartResponse ->
                ChartControl(root, appControl, blockControlData.action, chartResponse, tabId).apply {
                    startChartBody()
                }
            },
            mapBlock = blockControlData.mapResponse?.let { mapResponse ->
                MapControl(root, appControl, blockControlData.action, mapResponse, tabId).apply {
                    startMapBody(curScale = this.minXyScale)
                }
            },
            schemeBlock = blockControlData.schemeResponse?.let { schemeResponse ->
                SchemeControl(root, appControl, blockControlData.action, schemeResponse, tabId).apply {
                    startSchemeBody(
                        startExpandKoef = SchemeControl.SCHEME_START_EXPAND_KOEF,
                        isCentered = true,
                        curScale = 1,
                    )
                }
            },
            tableBlock = blockControlData.tableResponse?.let { tableResponse ->
                TableControl(root, appControl, blockControlData.action, tableResponse, tabId).apply {
                    startTableBody()
                }
            },
        ).apply {
            isHidden = blockControlData.isHidden

            x = blockControlData.x
            y = blockControlData.y
            w = blockControlData.w
            h = blockControlData.h
        }
    }

    private suspend fun setInterval(sec: Int) {
        refreshInterval = sec

        if (sec == 0) {
            refreshAll()
        }
    }

    private suspend fun refreshAll() {
        for (block in blocks) {
            if (block.isHidden) {
                continue
            }
            block.chartBlock?.doChartRefresh(null, false)
//                ?: run {
//                    block.chartBlock?.doChartRefresh(null, false)
                ?: block.mapBlock?.mapRefreshView(null, false)
                ?: block.schemeBlock?.schemeRefreshView(null, false)
                ?: block.tableBlock?.refreshTableFromComposite()
                ?: println("refreshAll: не задан тип блока!")
//                }
        }
    }

    private fun setMode(newMode: CompositeWorkMode) {
        when (curMode) {
            CompositeWorkMode.PAN -> {
                isPanButtonEnabled = true
            }

            CompositeWorkMode.LAYOUT -> {
                isLayoutButtonEnabled = true

                movingBlock = null

                isBlocksVisibilityButtonVisible = false
                isLayoutSaveButtonVisible = false
            }
        }

        when (newMode) {
            CompositeWorkMode.PAN -> {
                isPanButtonEnabled = false

                isRefreshButtonsVisible = true
            }

            CompositeWorkMode.LAYOUT -> {
                isLayoutButtonEnabled = false

                isBlocksVisibilityButtonVisible = true
                isLayoutSaveButtonVisible = true

                isRefreshButtonsVisible = false
            }
        }
        curMode = newMode

        refreshInterval = 0
    }

    private fun onPointerUp(pointerInputChange: PointerInputChange) {
        val mouseX = pointerInputChange.position.x
        val mouseY = pointerInputChange.position.y

        when (curMode) {
            CompositeWorkMode.LAYOUT -> {
                for (block in blocks) {
                    if (block.isHidden) {
                        continue
                    }

                    val blockX = block.x * oneBlockWidth + screenOffsetX
                    val blockY = block.y * oneBlockHeight + screenOffsetY

                    val blockW = block.w * oneBlockWidth
                    val blockH = block.h * oneBlockHeight

                    if (mouseX > blockX &&
                        mouseX < blockX + blockW &&
                        mouseY > blockY &&
                        mouseY < blockY + blockH
                    ) {
                        isToolBarsVisible = false
                        movingBlock = block
                        break
                    }
                }
            }

            else -> {}
        }
    }

    private fun onDrag(pointerInputChange: PointerInputChange, dx: Float, dy: Float) {
        when (curMode) {
            CompositeWorkMode.PAN, CompositeWorkMode.LAYOUT -> {
                val pixEndX = getBlockColSum() * oneBlockWidth
                val pixEndY = getBlockRowSum() * oneBlockHeight

                val newOffsetX = screenOffsetX + dx
                val newOffsetY = screenOffsetY + dy

                if (newOffsetX <= 0) {
                    screenOffsetX = if (newOffsetX >= canvasWidth - pixEndX) {
                        newOffsetX
                    } else {
                        canvasWidth - pixEndX
                    }
                } else {
                    screenOffsetX = 0.0f
                }

                if (newOffsetY <= 0) {
                    screenOffsetY = if (newOffsetY >= canvasHeight - pixEndY) {
                        newOffsetY
                    } else {
                        canvasHeight - pixEndY
                    }
                } else {
                    screenOffsetY = 0.0f
                }
            }

            else -> {}
        }
    }

    private fun onBlockMoveLeft(block: CompositeBlockControl) {
        if (block.x > 0) {
            val nearBlocks = blocks.filter { b ->
                !b.isHidden && b.isIntersect(block.x - 1, block.y, block.w, block.h)
            }
            if (nearBlocks.isEmpty()) {
                block.x--
            } else {
                nearBlocks.forEach { b ->
                    b.x += b.w + block.w - 1
                    //!!!проверить последующее пересечение перемещаемых блоков с другими блоками и
                    // при необходимости двигать в правый/левый/нижний/верхний конец схемы, не трогая остальные элементы
                    // (или сделать циклическое/рекурсивное смещение всех участвующих элементов вправо/влевоЭвниз/вверх)
                }
                block.x--
            }
        }
    }

    private fun onBlockMoveRight(block: CompositeBlockControl) {
        val nearBlocks = blocks.filter { b ->
            !b.isHidden && b.isIntersect(block.x + 1, block.y, block.w, block.h)
        }
        if (nearBlocks.isEmpty()) {
            block.x++
        } else {
            nearBlocks.forEach { b ->
                b.x -= b.w + block.w - 1
                //!!!проверить последующее пересечение перемещаемых блоков с другими блоками и
                // при необходимости двигать в правый/левый/нижний/верхний конец схемы, не трогая остальные элементы
                // (или сделать циклическое/рекурсивное смещение всех участвующих элементов вправо/влевоЭвниз/вверх)
            }
            block.x++
        }
    }

    private fun onBlockMoveUp(block: CompositeBlockControl) {
        if (block.y > 0) {
            val nearBlocks = blocks.filter { b ->
                !b.isHidden && b.isIntersect(block.x, block.y - 1, block.w, block.h)
            }
            if (nearBlocks.isEmpty()) {
                block.y--
            } else {
                nearBlocks.forEach { b ->
                    b.y += b.h + block.h - 1
                    //!!!проверить последующее пересечение перемещаемых блоков с другими блоками и
                    // при необходимости двигать в правый/левый/нижний/верхний конец схемы, не трогая остальные элементы
                    // (или сделать циклическое/рекурсивное смещение всех участвующих элементов вправо/влевоЭвниз/вверх)
                }
                block.y--
            }
        }
    }

    private fun onBlockMoveDown(block: CompositeBlockControl) {
        val nearBlocks = blocks.filter { b ->
            !b.isHidden && b.isIntersect(block.x, block.y + 1, block.w, block.h)
        }
        if (nearBlocks.isEmpty()) {
            block.y++
        } else {
            nearBlocks.forEach { b ->
                b.y -= b.h + block.h - 1
                //!!!проверить последующее пересечение перемещаемых блоков с другими блоками и
                // при необходимости двигать в правый/левый/нижний/верхний конец схемы, не трогая остальные элементы
                // (или сделать циклическое/рекурсивное смещение всех участвующих элементов вправо/влевоЭвниз/вверх)
            }
            block.y++
        }
    }

    private fun getBlockColSum(): Int = blocks
        .filterNot { block -> block.isHidden }
        .maxOf { block ->
            block.x + block.w
        }

    private fun getBlockRowSum(): Int = blocks
        .filterNot { block -> block.isHidden }
        .maxOf { block ->
            block.y + block.h
        }

    private fun changeBlockVisibility(block: CompositeBlockControl) {
        if (block.isHidden) {
            block.x = -(block.x + 1)
            block.y = -(block.y + 1)

            var x = block.x
            var y = block.y
            while (true) {
                val samePlacedBlocks = blocks.filter { b ->
                    !b.isHidden && b.isIntersect(x, y, block.w, block.h)
                }
                if (samePlacedBlocks.isEmpty()) {
                    block.x = x
                    block.y = y
                    break
                } else {
                    //--- двигаемся к краю, который поближе
                    if (getBlockColSum() <= getBlockRowSum()) {
                        x++
                    } else {
                        y++
                    }
                }
            }
        } else {
            block.x = -(block.x + 1)    // добавим -1 с учётом возможных координат 0,0
            block.y = -(block.y + 1)
        }
        block.isHidden = !block.isHidden
    }

    private fun saveLayout() {
        val compositeLayoutDatas = blocks.associate { block ->
            block.id to CompositeLayoutData(
                isHidden = block.isHidden,
                x = block.x,
                y = block.y,
                w = block.w,
                h = block.h,
            )
        }

        layoutSaveKey?.let { saveKey ->
            invokeRequest(
                SaveUserPropertyRequest(
                    name = saveKey,
                    value = Json.encodeToString(compositeLayoutDatas),
                )
            ) { _: SaveUserPropertyResponse ->
                root.showAlert("Сохранено")
            }
        }
//            root.appUserConfig.userProperties[propertyName] = propertyValue
//        }
    }
}