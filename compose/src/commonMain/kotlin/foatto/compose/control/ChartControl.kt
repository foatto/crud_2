package foatto.compose.control

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.hsl
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import foatto.compose.AppControl
import foatto.compose.Root
import foatto.compose.colorMainText
import foatto.compose.control.composable.chart.ChartToolBar
import foatto.compose.control.composable.onPointerEvents
import foatto.compose.control.model.MouseRectData
import foatto.compose.control.model.chart.ChartDrawData
import foatto.compose.control.model.chart.ChartLineData
import foatto.compose.control.model.chart.ChartLineDrawData
import foatto.compose.control.model.chart.ChartRectDrawData
import foatto.compose.control.model.chart.ChartTextDrawData
import foatto.compose.control.model.chart.ChartTimeLabelData
import foatto.compose.control.model.chart.ChartViewCoord
import foatto.compose.control.model.chart.ChartWorkMode
import foatto.compose.control.model.chart.ChartYData
import foatto.compose.invokeRequest
import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.request.ChartActionRequest
import foatto.core.model.response.ChartActionResponse
import foatto.core.model.response.chart.ChartData
import foatto.core.model.response.chart.ChartResponse
import foatto.core.model.response.xy.geom.XyRect
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getDateTimeDMYHMSString
import foatto.core.util.getSplittedDouble
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.io.println
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt

class ChartControl(
    private val root: Root,
    private val appControl: AppControl,
    private val chartAction: AppAction,
    private val chartResponse: ChartResponse,
    tabId: Int,
) : AbstractControl(tabId) {

    companion object {
        private val COLOR_CHART_TIME_LINE = hsl(180.0f, 1.0f, 0.5f)
        private val COLOR_CHART_LABEL_BACK = hsl(60.0f, 1.0f, 0.5f)
        private val COLOR_CHART_LABEL_BORDER = hsl(60.0f, 1.0f, 0.25f)
        private val COLOR_CHART_AXIS_DEFAULT = hsl(0.0f, 0.0f, 0.5f)

        private val COLOR_CHART_DATA_BACK = hsl(60.0f, 1.0f, 0.5f)   // 0.7f
        private const val COLOR_CHART_LINE_WIDTH = 1.0f

        private const val MARGIN_LEFT = 100     // на каждую ось Y
        private const val MARGIN_RIGHT = 20.0f
        private const val MARGIN_TOP = 40
        private const val MARGIN_BOTTOM = 60

        private const val MIN_GRID_STEP_X = 40  // минимальный шаг между линиями сетки в пикселях
        private const val MIN_GRID_STEP_Y = 40  // минимальный шаг между линиями сетки в пикселях

        private const val MIN_SCALE_X = 15 * 60     // минимальный разрешённый масштаб - диапазон не менее 15 мин
        private const val MAX_SCALE_X = 32 * 86400  // максимальный разрешённый масштаб - диапазон не более 32 дней (чуть более месяца)

        private const val GRAPHIC_TEXT_HEIGHT = 20              // высота текстового блока
        private const val GRAPHIC_TEXT_MIN_VISIBLE_WIDTH = 4    // минимальная ширина видимого текстового блока

        private val arrGridStepX = arrayOf(
            1, 5, 15,                           // 1 - 5 - 15 seconds
            1 * 60, 5 * 60, 15 * 60,            // 1 - 5 - 15 minutes
            1 * 3_600, 3 * 3_600, 6 * 3_600,    // 1 - 3 - 6 hours
            1 * 86_400, 3 * 86_400, 9 * 86_400, // 1 - 3 - 9 days
            27 * 86_400, 81 * 86_400            // 27 - 81 days
        )

        private val arrGridStepY = arrayOf(
            0.001f, 0.002f, 0.0025f, 0.005f,
            0.01f, 0.02f, 0.025f, 0.05f,
            0.1f, 0.2f, 0.25f, 0.5f,
            1.0f, 2.0f, 2.5f, 5.0f,
            10.0f, 20.0f, 25.0f, 50.0f,
            100.0f, 200.0f, 250.0f, 500.0f,
            1_000.0f, 2_000.0f, 2_500.0f, 5_000.0f,
            10_000.0f, 20_000.0f, 25_000.0f, 50_000.0f,
            100_000.0f, 200_000.0f, 250_000.0f, 500_000.0f,
            1_000_000.0f, 2_000_000.0f, 2_500_000.0f, 5_000_000.0f,
            10_000_000.0f, 20_000_000.0f, 25_000_000.0f, 50_000_000.0f,
        )

        private val arrPrecY = arrayOf(
            3, 3, 4, 3,
            2, 2, 3, 2,
            1, 1, 2, 1,
            0, 0, 1, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )
    }

    private var isPanButtonEnabled by mutableStateOf(false)
    private var isZoomButtonEnabled by mutableStateOf(true)

    private var canvasWidth: Float by mutableFloatStateOf(0.0f)
    private var canvasHeight: Float by mutableFloatStateOf(0.0f)

    private var axisCanvasWidth: Float by mutableFloatStateOf(0.0f)
    private var bodyCanvasWidth: Float by mutableFloatStateOf(0.0f)
    private var legendCanvasWidth: Float by mutableFloatStateOf(0.0f)

    private var screenOffsetX: Float by mutableFloatStateOf(0.0f)

    //--- нужен для извлечения/показа величин на графике
    private val chartDatas = mutableListOf<ChartData>()

    private val chartDrawDatas = mutableStateListOf<ChartDrawData>()
    private val yDrawDatas = mutableListOf<ChartYData>()

    private val mouseRect = MouseRectData()     // contains state-fields

    private val timeLabels = mutableStateListOf(ChartTimeLabelData(), ChartTimeLabelData(), ChartTimeLabelData())

//    private val grTooltipVisible = mutableStateOf(false)
//    private val grTooltipText = mutableStateOf("")
//    private val grTooltipLeft = mutableStateOf(0.px)
//    private val grTooltipTop = mutableStateOf(0.px)

    private val grTimeLine = ChartLineData(
        isVisible = mutableStateOf(false),
        x1 = mutableFloatStateOf(0.0f),
        y1 = mutableFloatStateOf(0.0f),
        x2 = mutableFloatStateOf(0.0f),
        y2 = mutableFloatStateOf(0.0f),
        width = mutableFloatStateOf(1.0f),
    )

    private var curMode = ChartWorkMode.PAN

    private var refreshInterval by mutableIntStateOf(0)

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private var chartViewCoord = ChartViewCoord(0, 0)
//    private var grTooltipOffTime = 0.0

    @Composable
    override fun Body() {
//        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            GetHeader()

            ChartToolBar(
                isWideScreen = root.isWideScreen,
                isPanButtonEnabled = isPanButtonEnabled,
                isZoomButtonEnabled = isZoomButtonEnabled,
                refreshInterval = refreshInterval,
                setMode = { chartWorkMode: ChartWorkMode -> setMode(chartWorkMode) },
                zoomIn = { coroutineScope.launch { zoomIn() } },
                zoomOut = { coroutineScope.launch { zoomOut() } },
            ) { interval: Int -> coroutineScope.launch { setInterval(interval) } }

            MainChartBody(withInteractive = true)
        }

        coroutineScope.launch {
            while (refreshInterval > 0) {
                doChartRefresh(null, false)
                delay(refreshInterval * 1000L)
            }
        }
    }

    @Composable
    fun MainChartBody(withInteractive: Boolean) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()

        Row(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    canvasWidth = size.width.toFloat()
                    canvasHeight = size.height.toFloat()
                }
        ) {
            YAxisBody(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(with(density) { axisCanvasWidth.toDp() })
                    .clipToBounds(),
                textMeasurer = textMeasurer,
                withInteractive = withInteractive,
            )
            ChartBody(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxHeight()
                    .width(with(density) { bodyCanvasWidth.toDp() })
                    .clipToBounds(),
                textMeasurer = textMeasurer,
                withInteractive = withInteractive,
            )
            LegendBody(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(with(density) { legendCanvasWidth.toDp() })
                    .clipToBounds(),
                textMeasurer = textMeasurer,
                withInteractive = withInteractive,
            )
        }
    }

    @Composable
    private fun YAxisBody(
        modifier: Modifier,
        textMeasurer: TextMeasurer,
        withInteractive: Boolean,
    ) {
        Canvas(
            modifier = modifier.onPointerEvents(
                withInteractive = withInteractive,
                onPointerDown = { pointerInputChange -> onPointerDown(pointerInputChange) },
                onPointerUp = { pointerInputChange -> onPointerUp(pointerInputChange) },
                onDragStart = { offset -> onDragStart(offset) },
                onDrag = { pointerInputChange, offset -> onDrag(pointerInputChange, offset) },
                onDragEnd = { onDragEnd() },
                onDragCancel = { onDragCancel() },
            ),
        ) {
            for (chartGroup in chartDrawDatas) {
                for (axisLine in chartGroup.yAxisLines) {
                    drawLineOnCanvas(
                        canvasWidth = axisCanvasWidth,
                        canvasHeight = canvasHeight,
                        x1 = axisLine.x1,
                        y1 = axisLine.y1,
                        x2 = axisLine.x2,
                        y2 = axisLine.y2,
                        color = axisLine.strokeColor,
                        strokeWidth = axisLine.strokeWidth,
                        strokeDash = axisLine.strokeDash,
                    )
                }
                for (axisText in chartGroup.yAxisTexts) {
                    drawTextOnCanvas(
                        scaleKoef = root.scaleKoef,
                        canvasWidth = axisCanvasWidth,
                        canvasHeight = canvasHeight,
                        textMeasurer = textMeasurer,
                        text = axisText.text,
                        fontSize = 12,
                        textIsBold = false,
                        x = axisText.x,
                        y = axisText.y,
                        textLimitWidth = null,
                        textLimitHeight = null,
                        rotateDegree = axisText.rotateDegree,
                        fillColor = axisText.fillColor,
                        fillAlpha = 1.0f,
                        strokeColor = axisText.strokeColor,
                        strokeWidth = axisText.strokeWidth,
                        textAnchor = axisText.textAnchor,
                        textColor = axisText.textColor,
                    )
//                                fontSize((1.0 * scaleKoef).cssRem)
                }
            }
        }
    }

    @Composable
    private fun ChartBody(
        modifier: Modifier,
        textMeasurer: TextMeasurer,
        withInteractive: Boolean,
    ) {
        Canvas(
            modifier = modifier.onPointerEvents(
                withInteractive = withInteractive,
                onPointerDown = { pointerInputChange -> onPointerDown(pointerInputChange) },
                onPointerUp = { pointerInputChange -> onPointerUp(pointerInputChange) },
                onDragStart = { offset -> onDragStart(offset) },
                onDrag = { pointerInputChange, offset -> onDrag(pointerInputChange, offset) },
                onDragEnd = { onDragEnd() },
                onDragCancel = { onDragCancel() },
            ),
        ) {
            for (chartGroup in chartDrawDatas) {
                drawTextOnCanvas(
                    scaleKoef = root.scaleKoef,
                    canvasWidth = bodyCanvasWidth,
                    canvasHeight = canvasHeight,
                    textMeasurer = textMeasurer,
                    text = chartGroup.title.text,
                    fontSize = 12,
                    textIsBold = false,
                    x = chartGroup.title.x + screenOffsetX,
                    y = chartGroup.title.y,
                    textLimitWidth = null,
                    textLimitHeight = null,
                    rotateDegree = null,
                    fillColor = chartGroup.title.fillColor,
                    fillAlpha = 1.0f,
                    strokeColor = chartGroup.title.strokeColor,
                    strokeWidth = chartGroup.title.strokeWidth,
                    textAnchor = chartGroup.title.textAnchor,
                    textColor = chartGroup.title.textColor,
                )
//                            fontSize((1.0 * scaleKoef).cssRem)
                for (graphicBack in chartGroup.chartBacks) {
                    drawRectOnCanvas(
                        x = graphicBack.x + screenOffsetX,
                        y = graphicBack.y,
                        width = graphicBack.width,
                        height = graphicBack.height,
                        fillColor = graphicBack.fillColor,
                        fillAlpha = 0.25f,
                        strokeColor = null,
                        strokeAlpha = 1.0f,
                        strokeStyle = null,
                    )
                }
                for (axisLine in chartGroup.xAxisLines) {
                    drawLineOnCanvas(
                        canvasWidth = bodyCanvasWidth,
                        canvasHeight = canvasHeight,
                        x1 = axisLine.x1 + screenOffsetX,
                        y1 = axisLine.y1,
                        x2 = axisLine.x2 + screenOffsetX,
                        y2 = axisLine.y2,
                        color = axisLine.strokeColor,
                        strokeWidth = axisLine.strokeWidth,    // 2.dp.toPx()
                        strokeDash = axisLine.strokeDash,
                    )
                }
                for (axisText in chartGroup.xAxisTexts) {
                    drawTextOnCanvas(
                        scaleKoef = root.scaleKoef,
                        canvasWidth = bodyCanvasWidth,
                        canvasHeight = canvasHeight,
                        textMeasurer = textMeasurer,
                        text = axisText.text,
                        fontSize = 12,
                        textIsBold = false,
                        x = axisText.x + screenOffsetX,
                        y = axisText.y,
                        textLimitWidth = null,
                        textLimitHeight = null,
                        rotateDegree = null,
                        fillColor = axisText.fillColor,
                        fillAlpha = 1.0f,
                        strokeColor = axisText.strokeColor,
                        strokeWidth = axisText.strokeWidth,
                        textAnchor = axisText.textAnchor,
                        textColor = axisText.textColor,
                    )
//                                fontSize((1.0 * scaleKoef).cssRem)
                }
//--- пока не используется
//                for (graphicPoint in chartGroup.alChartPoint) {
//                    drawCircleOnCanvas(
//                        drawScope = this,
//                        x = graphicPoint.cx,
//                        y = graphicPoint.cy,
//                        radius = graphicPoint.radius,
//                        fillAlpha = 1.0f,   //graphicPoint.alpha,
//                        fillColor = graphicPoint.fillColor,
//                        strokeColor = graphicPoint.strokeColor,
//                        strokeAlpha = 1.0f,
//                        strokeStyle = Stroke(width = 1.0f),
//                    )
////                            onMouseEnter { syntheticMouseEvent ->
////                                onGrMouseOver(syntheticMouseEvent, graphicPoint)
////                            }
////                            onMouseLeave {
////                                onGrMouseOut()
////                            }
//                }
                for (graphicLine in chartGroup.chartLines) {
                    drawLineOnCanvas(
                        canvasWidth = bodyCanvasWidth,
                        canvasHeight = canvasHeight,
                        x1 = graphicLine.x1 + screenOffsetX,
                        y1 = graphicLine.y1,
                        x2 = graphicLine.x2 + screenOffsetX,
                        y2 = graphicLine.y2,
                        color = graphicLine.strokeColor,
                        strokeWidth = graphicLine.strokeWidth,    // 2.dp.toPx()
                        strokeDash = graphicLine.strokeDash,
                    )
//                            onMouseEnter { syntheticMouseEvent ->
//                                onGrMouseOver(syntheticMouseEvent, graphicLine)
//                            }
//                            onMouseLeave {
//                                onGrMouseOut()
//                            }
                }
                for (graphicText in chartGroup.chartTexts) {
                    drawTextOnCanvas(
                        scaleKoef = root.scaleKoef,
                        canvasWidth = bodyCanvasWidth,
                        canvasHeight = canvasHeight,
                        textMeasurer = textMeasurer,
                        text = graphicText.text,
                        fontSize = 12,
                        textIsBold = false,
                        x = graphicText.x + screenOffsetX,
                        y = graphicText.y,
                        textLimitWidth = graphicText.textLimitWidth,
                        textLimitHeight = graphicText.textLimitHeight,
                        rotateDegree = null,
                        fillColor = graphicText.fillColor,
                        fillAlpha = 0.7f,
                        strokeColor = graphicText.strokeColor,
                        strokeWidth = graphicText.strokeWidth,
                        textAnchor = graphicText.textAnchor,
                        textColor = graphicText.textColor,
                    )
//                                onMouseEnter { syntheticMouseEvent ->
//                                    onGrMouseOver(syntheticMouseEvent, graphicText)
//                                }
//                                onMouseLeave {
//                                    onGrMouseOut()
//                                }
                }
            }

            if (grTimeLine.isVisible.value) {
                drawLineOnCanvas(
                    canvasWidth = bodyCanvasWidth,
                    canvasHeight = canvasHeight,
                    x1 = grTimeLine.x1.value,
                    y1 = grTimeLine.y1.value,
                    x2 = grTimeLine.x2.value,
                    y2 = grTimeLine.y2.value,
                    color = COLOR_CHART_TIME_LINE,
                    strokeWidth = grTimeLine.width.value,
                )
            }

            if (withInteractive && refreshInterval == 0 && mouseRect.isVisible.value) {
                drawRectOnCanvas(
                    x = min(mouseRect.x1.value, mouseRect.x2.value),
                    y = min(mouseRect.y1.value, mouseRect.y2.value),
                    width = abs(mouseRect.x2.value - mouseRect.x1.value),
                    height = abs(mouseRect.y2.value - mouseRect.y1.value),
                    fillColor = COLOR_CHART_TIME_LINE,
                    fillAlpha = 0.25f,
                    strokeColor = COLOR_CHART_TIME_LINE,
                    strokeAlpha = 1.0f,
                    strokeStyle = Stroke(width = COLOR_CHART_LINE_WIDTH * root.scaleKoef),
                )
            }

            if (refreshInterval == 0 && withInteractive) {
                //--- Time Labels
                for (timeLabel in timeLabels) {
                    if (timeLabel.isVisible.value) {
                        drawTextOnCanvas(
                            scaleKoef = root.scaleKoef,
                            canvasWidth = bodyCanvasWidth,
                            canvasHeight = canvasHeight,
                            textMeasurer = textMeasurer,
                            text = timeLabel.text.value,
                            fontSize = 12,
                            textIsBold = false,
                            x = timeLabel.x.value,
                            y = canvasHeight,
                            textLimitWidth = null,
                            textLimitHeight = null,
                            rotateDegree = null,
                            fillColor = COLOR_CHART_LABEL_BACK,
                            fillAlpha = 1.0f,
                            strokeColor = COLOR_CHART_LABEL_BORDER,
                            strokeWidth = 1.0f,
                            textAnchor = when {
                                timeLabel.x.value < bodyCanvasWidth / 8 -> {
                                    Alignment.BottomStart
                                }

                                timeLabel.x.value > bodyCanvasWidth * 7 / 8 -> {
                                    Alignment.BottomEnd
                                }

                                else -> {
                                    Alignment.BottomCenter
                                }
                            },
                            textColor = colorMainText,
                        )
                    }
                }

//                //--- Tooltip
//                if (grTooltipVisible.value) {
//                    Div(
//                        attrs = {
//                            style {
//                                position(Position.Absolute)
//                                color(COLOR_MAIN_TEXT)
//                                backgroundColor(COLOR_CHART_LABEL_BACK)
//                                setBorder(color = COLOR_CHART_LABEL_BORDER, radius = styleButtonBorderRadius)
//                                setPaddings(arrStyleControlTooltipPadding)
//                                userSelect("none")
//                                left(grTooltipLeft.value)
//                                top(grTooltipTop.value)
//                            }
//                        }
//                    ) {
//                        getMultilineText(grTooltipText.value)
//                    }
//                }
            }
        }
    }

    @Composable
    private fun LegendBody(
        modifier: Modifier,
        textMeasurer: TextMeasurer,
        withInteractive: Boolean,
    ) {
        Canvas(
            modifier = modifier.onPointerEvents(
                withInteractive = withInteractive,
                onPointerDown = { pointerInputChange -> onPointerDown(pointerInputChange) },
                onPointerUp = { pointerInputChange -> onPointerUp(pointerInputChange) },
                onDragStart = { offset -> onDragStart(offset) },
                onDrag = { pointerInputChange, offset -> onDrag(pointerInputChange, offset) },
                onDragEnd = { onDragEnd() },
                onDragCancel = { onDragCancel() },
            ),
        ) {
            for (chartGroup in chartDrawDatas) {
                for (legendBack in chartGroup.legendBacks) {
                    drawRectOnCanvas(
                        x = legendBack.x,
                        y = legendBack.y,
                        width = legendBack.width,
                        height = legendBack.height,
                        fillColor = legendBack.fillColor,
                        fillAlpha = 1.0f,
                        strokeColor = legendBack.strokeColor,
                        strokeAlpha = 1.0f,
                        strokeStyle = Stroke(width = legendBack.strokeWidth),
                    )
                }
                for (legendText in chartGroup.legendTexts) {
                    drawTextOnCanvas(
                        scaleKoef = root.scaleKoef,
                        canvasWidth = legendCanvasWidth,
                        canvasHeight = canvasHeight,
                        textMeasurer = textMeasurer,
                        text = legendText.text,
                        fontSize = 12,
                        textIsBold = true,
                        x = legendText.x,
                        y = legendText.y,
                        textLimitWidth = null,
                        textLimitHeight = null,
                        rotateDegree = legendText.rotateDegree,
                        fillColor = legendText.fillColor,
                        fillAlpha = 1.0f,
                        strokeColor = legendText.strokeColor,
                        strokeWidth = legendText.strokeWidth,
                        textAnchor = legendText.textAnchor,
                        textColor = legendText.textColor,
                    )
//                                fontSize((1.0 * scaleKoef).cssRem)
                }
            }
        }
    }

    override suspend fun start() {
        root.setTabInfo(tabId, chartResponse.tabCaption)

        startChartBody()
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun startChartBody() {
        //--- вынесено сюда для использования во внешнем CompositeControl
        headerData = chartResponse.headerData

        val newViewCoord = ChartViewCoord(
            t1 = if (chartAction.timeRangeType == 0) {
                chartAction.begTime ?: (getCurrentTimeInt() - 86_400)
            } else {
                getCurrentTimeInt() - chartAction.timeRangeType
            },
            t2 = if (chartAction.timeRangeType == 0) {
                chartAction.endTime ?: getCurrentTimeInt()
            } else {
                getCurrentTimeInt()
            },
        )
        GlobalScope.launch {
            while (canvasWidth == 0.0f) {
                yield()
            }
            doChartRefresh(
                aView = newViewCoord,
                withWait = true,
            )
        }
    }

    private suspend fun setInterval(sec: Int) {
        refreshInterval = sec

        if (sec == 0) {
            doChartRefresh(null, false)
        }
    }

    private suspend fun chartRefreshView(aView: ChartViewCoord?) {
        doChartRefresh(aView = aView, withWait = true)
    }

    suspend fun doChartRefresh(
        aView: ChartViewCoord?,
        withWait: Boolean,
    ) {
        aView?.let {
            chartViewCoord = aView
        } ?: run {
            if (chartAction.timeRangeType != 0 && refreshInterval != 0) {
                chartViewCoord = ChartViewCoord(
                    t1 = getCurrentTimeInt() - chartAction.timeRangeType,
                    t2 = getCurrentTimeInt(),
                )
            }
        }

        if (withWait) {
            root.setWait(true)
        }
        invokeRequest(
            ChartActionRequest(
                action = chartAction.copy(type = ActionType.GET_ELEMENTS),
                times = Pair(chartViewCoord.t1, chartViewCoord.t2),
                viewSize = bodyCanvasWidth / root.scaleKoef to canvasHeight / root.scaleKoef,
            )
        ) { chartActionResponse: ChartActionResponse ->

            //--- сбрасываем горизонтальный скроллинг/смещение
            screenOffsetX = 0.0f

            chartDatas.clear()
            chartDatas.addAll(chartActionResponse.charts)

            var maxMarginLeft = 0.0f
            var maxMarginRight = 0.0f

            //--- prerare data for Y-reversed charts
            chartDatas.forEach { chartData ->
                chartData.axises.forEach { axisYData ->
                    if (axisYData.isReversedY) {
                        //--- во избежание перекрёстных изменений
                        val minY = axisYData.min
                        val maxY = axisYData.max
                        axisYData.min = -maxY
                        axisYData.max = -minY
                    }
                }
                chartData.elements.forEach { gdc ->
                    if (gdc.isReversedY) {
                        gdc.lines?.forEach { gld ->
                            gld.y = -gld.y
                        }
                    }
                }

                //--- переинициализировать значение левого поля
                maxMarginLeft = max(maxMarginLeft, chartData.axises.size * MARGIN_LEFT * root.scaleKoef)
                maxMarginRight = max(maxMarginRight, max(MARGIN_RIGHT, chartData.legends.size * getLegendWidth(root.scaleKoef)))
            }


            //--- установка динамической (зависящей от scaleKoef) ширины области с вертикальными осями
            axisCanvasWidth = maxMarginLeft
            legendCanvasWidth = maxMarginRight
            bodyCanvasWidth = canvasWidth - axisCanvasWidth - legendCanvasWidth

            //--- реальная высота одной единицы относительной высоты
            val oneChartHeight = if (chartDatas.isEmpty()) {
                0.0f
            } else {
                canvasHeight / chartDatas.size * root.scaleKoef
            }

            chartDrawDatas.clear()
            yDrawDatas.clear()

            var localPixEndY = 0.0f
            chartDatas.forEach { chartData ->
                outChart(
                    t1 = chartViewCoord.t1,
                    t2 = chartViewCoord.t2,
                    chartData = chartData,
                    oneChartHeight = oneChartHeight,
                    pixStartY = localPixEndY,
                )
                localPixEndY += oneChartHeight
            }
//        //--- перезагрузка данных может быть связана с изменением показываемого временнОго диапазона,
//        //--- поэтому переотобразим его
//        val arrBegDT = DateTime_Arr( appContainer.timeZone, grModel.viewCoord.t1 )
//        arrTxtDateTime[ 2 ]!!.text = arrBegDT[ 0 ].toString()
//        arrTxtDateTime[ 1 ]!!.text = arrBegDT[ 1 ].toString()
//        arrTxtDateTime[ 0 ]!!.text = arrBegDT[ 2 ].toString()
//        arrTxtDateTime[ 3 ]!!.text = arrBegDT[ 3 ].toString()
//        arrTxtDateTime[ 4 ]!!.text = arrBegDT[ 4 ].toString()
//        val arrEndDT = DateTime_Arr( appContainer.timeZone, grModel.viewCoord.t2 )
//        arrTxtDateTime[ 7 ]!!.text = arrEndDT[ 0 ].toString()
//        arrTxtDateTime[ 6 ]!!.text = arrEndDT[ 1 ].toString()
//        arrTxtDateTime[ 5 ]!!.text = arrEndDT[ 2 ].toString()
//        arrTxtDateTime[ 8 ]!!.text = arrEndDT[ 3 ].toString()
//        arrTxtDateTime[ 9 ]!!.text = arrEndDT[ 4 ].toString()
//
//        onRequestFocus()

            if (withWait) {
                root.setWait(false)
            }
        }
    }

    private fun getLegendWidth(scaleKoef: Float): Float = 24 * scaleKoef

    private fun outChart(
        t1: Int,
        t2: Int,
        chartData: ChartData,
        oneChartHeight: Float,
        pixStartY: Float,
    ) {
        //--- maxMarginLeft уходит на левую панель, к оси Y
        val pixDrawHeight = oneChartHeight - (MARGIN_TOP + MARGIN_BOTTOM) * root.scaleKoef
        val pixDrawY0 = pixStartY + oneChartHeight - MARGIN_BOTTOM * root.scaleKoef   // "нулевая" ось Y
        val pixDrawTopY = pixStartY + MARGIN_TOP * root.scaleKoef  // верхний край графика

        val yAxisLines = mutableListOf<ChartLineDrawData>()
        val yAxisTexts = mutableListOf<ChartTextDrawData>()
        val xAxisLines = mutableListOf<ChartLineDrawData>()
        val xAxisTexts = mutableListOf<ChartTextDrawData>()
        val chartBacks = mutableListOf<ChartRectDrawData>()
        val chartLines = mutableListOf<ChartLineDrawData>()
        // val alChartPoint = mutableListOf<ChartCircleDrawData>() - пока не используется
        val chartTexts = mutableListOf<ChartTextDrawData>()
        val legendBacks = mutableListOf<ChartRectDrawData>()
        val legendTexts = mutableListOf<ChartTextDrawData>()

        //--- заголовок

        val titleData = ChartTextDrawData(
            x = MIN_GRID_STEP_X * root.scaleKoef,
            y = pixDrawTopY - 4 * root.scaleKoef,
            textAnchor = Alignment.BottomStart,
            text = chartData.title,
            textColor = colorMainText,
        )

        //--- ось X ---

        drawTimePane(
            t1 = t1,
            t2 = t2,
            pixWidth = bodyCanvasWidth,
            pixDrawY0 = pixDrawY0,
            pixDrawTopY = pixDrawTopY,
            axisLines = xAxisLines,
            axisTexts = xAxisTexts
        )

        //--- оси Y

        val alAxisYDataIndex = mutableListOf<Int>()
        for (i in chartData.axises.indices) {
            val ayd = chartData.axises[i]

            val precY = drawAxisY(
                element = chartData,
                axisIndex = i,
                pixDrawWidth = axisCanvasWidth,
                pixDrawHeight = pixDrawHeight,
                pixDrawY0 = pixDrawY0,
                pixDrawTopY = pixDrawTopY,
                pixBodyWidth = bodyCanvasWidth,
                alAxisLine = yAxisLines,
                alAxisText = yAxisTexts,
                alBodyLine = xAxisLines,    // для горизонтальной линии на самом графике
            )

            ayd.prec = precY

            val axisYDataIndex = yDrawDatas.size
            alAxisYDataIndex.add(axisYDataIndex)

            yDrawDatas += ChartYData(
                y1 = pixDrawY0,
                y2 = pixDrawTopY,
                value1 = ayd.min,
                value2 = ayd.max,
                prec = precY,
                isReversedY = ayd.isReversedY,
            )
        }

        //--- легенда ---

        for (i in chartData.legends.indices) {
            drawLegend(
                element = chartData,
                legendIndex = i,
                pixDrawHeight = pixDrawHeight,
                pixDrawY0 = pixDrawY0,
                pixDrawTopY = pixDrawTopY,
                alLegendBack = legendBacks,
                alLegendText = legendTexts,
            )
        }

        //--- графики ---

        val prevTextBounds = mutableListOf<XyRect>()

        for (chartElementData in chartData.elements) {
            chartElementData.backs?.let {
                for (grd in chartElementData.backs) {
                    val drawX1 = bodyCanvasWidth * (grd.x1 - t1) / (t2 - t1)
                    val drawX2 = bodyCanvasWidth * (grd.x2 - t1) / (t2 - t1)

                    chartBacks.add(
                        ChartRectDrawData(
                            x = drawX1,
                            y = pixDrawTopY,
                            width = drawX2 - drawX1,
                            height = pixDrawY0 - pixDrawTopY,
                            fillColor = Color(grd.color),
                        )
                    )
                }
            } ?: chartElementData.lines?.let {
                val axisIndex = chartElementData.axisIndex ?: 0

                var prevDrawX = -1.0f
                var prevDrawY = -1.0f
                var prevDrawColor: Int? = null
                val ayd = chartData.axises[axisIndex]
                val yDiff = ayd.max - ayd.min

                for (gld in chartElementData.lines) {
                    val drawX = bodyCanvasWidth * (gld.x - t1) / (t2 - t1)
                    val drawY = pixDrawY0 - pixDrawHeight * (gld.y - ayd.min) / yDiff
                    prevDrawColor?.let {
                        chartLines.add(
                            ChartLineDrawData(
                                x1 = prevDrawX,
                                y1 = prevDrawY,
                                x2 = drawX,
                                y2 = drawY,
                                strokeColor = Color(gld.color),
                                strokeWidth = chartElementData.lineWidth * root.scaleKoef,
                                tooltip = alAxisYDataIndex[axisIndex].toString(),
                            )
                        )
                    }
                    prevDrawX = drawX
                    prevDrawY = drawY
                    prevDrawColor = gld.color
                }
            } ?: chartElementData.texts?.let {
                for (gtd in chartElementData.texts) {
                    val drawX1 = bodyCanvasWidth * (gtd.x1 - t1) / (t2 - t1)
                    val drawX2 = bodyCanvasWidth * (gtd.x2 - t1) / (t2 - t1)
                    val drawWidth = drawX2 - drawX1
                    val drawHeight = GRAPHIC_TEXT_HEIGHT * root.scaleKoef

                    //--- смысла нет показывать коротенькие блоки
                    if (drawWidth <= GRAPHIC_TEXT_MIN_VISIBLE_WIDTH * root.scaleKoef) {
                        continue
                    }

                    val rect = XyRect(drawX1, pixDrawTopY, drawWidth, drawHeight)

                    //--- обеспечим отсутствие накладок текстов/прямоугольников
                    //--- (многопроходной алгоритм, учитывающий "смену обстановки" после очередного сдвига)
                    while (true) {
                        var crossNotFound = true
                        for (otherRect in prevTextBounds) {
                            //--- если блок текста пересекается с кем-то предыдущим, опустимся ниже его
                            if (rect.isIntersects(otherRect)) {
                                rect.y += rect.height
                                crossNotFound = false
                                break
                            }
                        }
                        if (crossNotFound) {
                            break
                        }
                    }
                    //--- для следующих текстов
                    prevTextBounds.add(rect)
                    chartTexts.add(
                        ChartTextDrawData(
                            x = rect.x.toFloat(),
                            y = rect.y.toFloat(),
                            textLimitWidth = rect.width.toFloat(),
                            textLimitHeight = rect.height.toFloat(),
                            textAnchor = Alignment.TopStart,
                            text = gtd.text,
                            fillColor = Color(gtd.fillColor),
                            strokeColor = Color(gtd.borderColor),
                            strokeWidth = 1 * root.scaleKoef,
                            //radius = (2 * scaleKoef).px,
                            textColor = Color(gtd.textColor),
//                                fontSize ((1.0 * scaleKoef).cssRem)
//                                userSelect ("none")
                            tooltip = gtd.text,
                        )
                    )
                }
            }
        }

        chartDrawDatas += ChartDrawData(
            title = titleData,
            yAxisLines = yAxisLines,
            yAxisTexts = yAxisTexts,
            xAxisLines = xAxisLines,
            xAxisTexts = xAxisTexts,
            chartBacks = chartBacks,
            chartLines = chartLines,
//                alChartPoint = alChartPoint,
            chartTexts = chartTexts,
            legendBacks = legendBacks,
            legendTexts = legendTexts,
        )
    }

    private fun drawTimePane(
        t1: Int,
        t2: Int,
        pixWidth: Float,
        pixDrawY0: Float,
        pixDrawTopY: Float,
        axisLines: MutableList<ChartLineDrawData>,
        axisTexts: MutableList<ChartTextDrawData>,
    ) {
        val timeOffset = root.appUserConfig.timeOffset

        val timeWidth = t2 - t1

        //--- сетка, насечки, надписи по оси X
        val minStepX: Int = (timeWidth * MIN_GRID_STEP_X * root.scaleKoef / pixWidth).roundToInt()
        var notchStepX = 0   // шаг насечек
        var labelStepX = 0   // шаг подписей под насечками
        for (i in arrGridStepX.indices) {
            if (arrGridStepX[i] >= minStepX) {
                notchStepX = arrGridStepX[i]
                //--- подписи по шкале X делаются реже, чем насечки
                labelStepX = arrGridStepX[if (i == arrGridStepX.lastIndex) i else i + 1]
                break
            }
        }
        //--- если подходящий шаг насечек не нашелся, берем максимальный (хотя такой ситуации не должно быть)
        if (notchStepX == 0) {
            notchStepX = arrGridStepX[arrGridStepX.lastIndex]
            labelStepX = arrGridStepX[arrGridStepX.lastIndex]
        }

        var notchX = (t1 + timeOffset) / notchStepX * notchStepX - timeOffset
        while (notchX <= t2) {

            if (notchX < t1) {
                notchX += notchStepX
                continue
            }
            //--- в double и обратно из-за ошибок округления
            val pixDrawX = 1.0f * pixWidth * (notchX - t1) / timeWidth
            //--- вертикальная линия сетки, переходящая в насечку
            val line = ChartLineDrawData(
                x1 = pixDrawX,
                y1 = pixDrawTopY,
                x2 = pixDrawX,
                y2 = pixDrawY0 + (2 * root.scaleKoef).roundToInt(),
                strokeColor = COLOR_CHART_AXIS_DEFAULT,
                strokeWidth = max(1.0f, root.scaleKoef),
                //--- если насечка переходит в линию сетки, то возможно меняется стиль линии
                strokeDash = if (pixDrawTopY < pixDrawY0 && (notchX + timeOffset) % labelStepX != 0) {
                    floatArrayOf(root.scaleKoef * 2, root.scaleKoef * 2)
                } else {
                    null
                }
            )
            axisLines.add(line)

            //--- текст метки по оси X
            if ((notchX + timeOffset) % labelStepX == 0) {
                val text = getDateTimeDMYHMSString(timeOffset, notchX).replace(" ", "\n  ")
                axisTexts.add(
                    ChartTextDrawData(
                        x = pixDrawX,
                        y = pixDrawY0 + 2 * root.scaleKoef,
                        textAnchor = Alignment.TopCenter,
                        text = text,
                        textColor = COLOR_CHART_AXIS_DEFAULT,
                    )
                )
            }
            notchX += notchStepX
        }
        //--- первую и последнюю метки (дата и время) перевыровнять к началу и к концу соответственно
        axisTexts[0].textAnchor = Alignment.TopStart
        axisTexts[axisTexts.lastIndex].textAnchor = Alignment.TopEnd

        //--- ось X
        val line = ChartLineDrawData(
            x1 = 0.0f,
            y1 = pixDrawY0,
            x2 = pixWidth,
            y2 = pixDrawY0,
            strokeColor = COLOR_CHART_AXIS_DEFAULT,
            strokeWidth = max(1.0f, root.scaleKoef),
            //--- если насечка переходит в линию сетки, то возможно меняется стиль линии
            strokeDash = if (pixDrawTopY < pixDrawY0 && (notchX + timeOffset) % labelStepX != 0) {
                floatArrayOf(root.scaleKoef * 2, root.scaleKoef * 2)
            } else {
                null
            }
        )
        axisLines.add(line)
    }

    private fun drawAxisY(
        element: ChartData,
        axisIndex: Int,
        pixDrawWidth: Float,
        pixDrawHeight: Float,
        pixDrawY0: Float,
        pixDrawTopY: Float,
        pixBodyWidth: Float,      // ширина основного графика
        alAxisLine: MutableList<ChartLineDrawData>,
        alAxisText: MutableList<ChartTextDrawData>,
        alBodyLine: MutableList<ChartLineDrawData>
    ): Int {
        val ayd = element.axises[axisIndex]
        val grHeight = ayd.max - ayd.min
        val axisX = pixDrawWidth - axisIndex * MARGIN_LEFT * root.scaleKoef

        //--- сетка, насечки, надписи по оси Y
        val minGraphicStepY = grHeight * MIN_GRID_STEP_Y * root.scaleKoef / pixDrawHeight
        var notchGraphicStepY = 0.0f   // шаг насечек
        var labelGraphicStepY = 0.0f   // шаг подписей под насечками
        var precY = 0
        for (i in arrGridStepY.indices) {
            val gridStepY = arrGridStepY[i]
            if (gridStepY >= minGraphicStepY) {
                notchGraphicStepY = gridStepY
                //--- подписи по шкале Y делаются на каждой насечке
                labelGraphicStepY = gridStepY  //element.getGridStepX(  i == element.getGridStepXCount() - 1 ? i : i + 1  );
                //--- сразу же определим precY
                precY = arrPrecY[i]
                break
            }
        }
        //--- если подходящий шаг насечек не нашелся, берем максимальный (хотя такой ситуации не должно быть)
        if (notchGraphicStepY <= 0.0) {
            notchGraphicStepY = arrGridStepY.last()
            labelGraphicStepY = arrGridStepY.last()
            //--- сразу же определим precY
            precY = arrPrecY.last()
        }
        //--- для последующего корректного вычисления остатка от деления дробных чисел будем приводить их к целым числам путём умножения на квадрат минимального шага
        val mult = round(1.0 / arrGridStepY[0] / arrGridStepY[0])

        var notchY = floor(ayd.min / notchGraphicStepY) * notchGraphicStepY
        while (notchY <= ayd.max) {

            if (notchY < ayd.min) {
                notchY += notchGraphicStepY
                continue
            }
            val drawY = pixDrawY0 - pixDrawHeight * (notchY - ayd.min) / grHeight

            //--- горизонтальная линия сетки, переходящая в насечку
            val line = ChartLineDrawData(
                x1 = axisX - MARGIN_LEFT * root.scaleKoef / 2,
                y1 = drawY,
                x2 = /*if( axisIndex == 0 ) pixDrawWidth else*/ axisX,
                y2 = drawY,
                strokeColor = COLOR_CHART_AXIS_DEFAULT,
                strokeWidth = max(1.0f, root.scaleKoef),
                //--- если насечка переходит в линию сетки, то возможно меняется стиль линии
                strokeDash = floatArrayOf(root.scaleKoef * 2, root.scaleKoef * 2)
            )
            alAxisLine.add(line)

            //--- горизонтальная линия сетки на основном графике
            if (axisIndex == 0) {
                alBodyLine.add(
                    ChartLineDrawData(
                        x1 = 0.0f,
                        y1 = drawY,
                        x2 = pixBodyWidth,
                        y2 = drawY,
                        strokeColor = COLOR_CHART_AXIS_DEFAULT,
                        strokeWidth = max(1.0f, root.scaleKoef),
                        //--- если насечка переходит в линию сетки, то возможно меняется стиль линии
                        strokeDash = floatArrayOf(root.scaleKoef * 2, root.scaleKoef * 2)
                    )
                )
            }

            //--- текст метки по оси Y
            if (round(notchY * mult).toLong() % round(labelGraphicStepY * mult).toLong() == 0L) {
                val value = if (ayd.isReversedY) {
                    -notchY
                } else {
                    notchY
                }
                val axisText = ChartTextDrawData(
                    x = axisX - 2 * root.scaleKoef,
                    y = drawY - 2 * root.scaleKoef,
                    text = getSplittedDouble(value.toDouble(), precY, true, '.'),
                    textColor = Color(ayd.color),
                    textAnchor = Alignment.BottomEnd,
                )
                alAxisText.add(axisText)
            }
            notchY += notchGraphicStepY
        }
        //--- ось Y
        val line = ChartLineDrawData(
            x1 = axisX,
            y1 = pixDrawY0,
            x2 = axisX,
            y2 = pixDrawTopY,
            strokeColor = Color(ayd.color),
            strokeWidth = max(1.0f, root.scaleKoef)
        )
        alAxisLine.add(line)

        //--- подпись оси Y - подпись отодвинем подальше от цифр, чтобы не перекрывались
        val axisTextX = axisX - (MARGIN_LEFT * root.scaleKoef * 5 / 6).roundToInt()
        val axisTextY = pixDrawY0 - pixDrawHeight / 2
        val axisText = ChartTextDrawData(
            x = axisTextX,
            y = axisTextY,
            text = ayd.title,
            textColor = Color(ayd.color),
            textAnchor = Alignment.TopCenter,
            rotateDegree = -90.0f,
        )

        alAxisText.add(axisText)

        return precY
    }

    private fun drawLegend(
        element: ChartData,
        legendIndex: Int,
        pixDrawHeight: Float,
        pixDrawY0: Float,
        pixDrawTopY: Float,
        alLegendBack: MutableList<ChartRectDrawData>,
        alLegendText: MutableList<ChartTextDrawData>,
    ) {
        val chartLegend = element.legends[legendIndex]

        val width = getLegendWidth(root.scaleKoef)

        val x1 = width * legendIndex
        val y1 = pixDrawTopY
        //val x2 = x1 + width
        val y2 = pixDrawY0

        val legendBack = ChartRectDrawData(
            x = x1,
            y = y1,
            width = width - 2.0f * root.scaleKoef,
            height = pixDrawHeight,
            strokeColor = Color(chartLegend.borderColor),
            fillColor = chartLegend.fillColor?.let { fillColor -> Color(fillColor) },
//            rx = 2,
//            ry = 2,
            //tooltip: String = "" - на случай, если будем выводить только прямоугольники без текста (для мобильной версии, например)
        )
        alLegendBack += legendBack

        val legendText = ChartTextDrawData(
            x = x1 + 1.0f * root.scaleKoef,
            y = (y1 + y2) / 2,
            text = chartLegend.text,
            textAnchor = Alignment.TopCenter,
            rotateDegree = -90.0f,
            textColor = chartLegend.fillColor?.let {
                colorMainText
            } ?: Color(chartLegend.textColor),
        )
        alLegendText += legendText
    }

    private fun setMode(newMode: ChartWorkMode) {
        when (newMode) {
            ChartWorkMode.PAN -> {
                isPanButtonEnabled = false
                isZoomButtonEnabled = true
            }

            ChartWorkMode.ZOOM_BOX -> {
                isPanButtonEnabled = true
                isZoomButtonEnabled = false
            }
        }
        curMode = newMode

        refreshInterval = 0
    }

    private suspend fun zoomIn() {
        val t1 = chartViewCoord.t1
        val t2 = chartViewCoord.t2
        val grWidth = chartViewCoord.width

        val newT1 = t1 + grWidth / 4
        val newT2 = t2 - grWidth / 4

        if (newT2 - newT1 >= MIN_SCALE_X) {
            chartRefreshView(ChartViewCoord(newT1, newT2))
        }

    }

    private suspend fun zoomOut() {
        val t1 = chartViewCoord.t1
        val t2 = chartViewCoord.t2
        val grWidth = chartViewCoord.width

        val newT1 = t1 - grWidth / 2
        val newT2 = t2 + grWidth / 2
        if (newT2 - newT1 <= MAX_SCALE_X) {
            chartRefreshView(ChartViewCoord(newT1, newT2))
        }
    }

    private fun onPointerDown(pointerInputChange: PointerInputChange) {
        when (curMode) {
            ChartWorkMode.PAN -> {}

            ChartWorkMode.ZOOM_BOX -> {}
        }
    }

    private fun onDragStart(offset: Offset) {
        //--- при нажатой кнопке мыши положение курсора не отслеживается
        disableCursorLinesAndLabels()

        when (curMode) {
            ChartWorkMode.PAN -> {}

            ChartWorkMode.ZOOM_BOX -> {
                // case SELECT_FOR_PRINT:
                mouseRect.apply {
                    isVisible.value = true
                    x1.value = offset.x
                    y1.value = 0.0f
                    x2.value = offset.x
                    y2.value = canvasHeight - root.scaleKoef
                }

                setTimeLabel(offset.x, timeLabels[1])
                setTimeLabel(offset.x, timeLabels[2])
            }
        }
    }

    private fun onDrag(pointerInputChange: PointerInputChange, dragAmount: Offset) {
        val mouseX = pointerInputChange.position.x
        val mouseY = pointerInputChange.position.y

        when (curMode) {
            ChartWorkMode.PAN -> {
                screenOffsetX += dragAmount.x
            }

            ChartWorkMode.ZOOM_BOX -> {
                if (mouseRect.isVisible.value && mouseX >= 0 && mouseX <= bodyCanvasWidth && mouseY >= 0 && mouseY <= canvasHeight) {
                    mouseRect.x2.value = mouseX
                    setTimeLabel(mouseX, timeLabels[2])
                }
            }
        }
        /*
            //--- mouse moved
            else {
                when (grCurMode.value) {
                    GraphicWorkMode.PAN, GraphicWorkMode.ZOOM_BOX -> {
                        //                    case SELECT_FOR_PRINT:
                        if (mouseX in 0..grSvgBodyWidth.value) {
                            val arrViewBoxBody = getGraphicViewBoxBody()

                            grTimeLine.isVisible.value = true
                            grTimeLine.x1.value = mouseX
                            grTimeLine.y1.value = arrViewBoxBody[1]
                            grTimeLine.x2.value = mouseX
                            grTimeLine.y2.value = arrViewBoxBody[1] + arrViewBoxBody[3]

                            setTimeLabel(svgBodyLeft, grSvgBodyWidth.value, mouseX, alTimeLabel[0])

                            if (isShowGraphicDataVisible.value) {
                                fillGraphicData(mouseX)
                            }
                        } else {
                            disableCursorLinesAndLabels()
                        }
                    }
                }
            }
        }
         */
    }

    private suspend fun onDragEnd() {
        when (curMode) {
            ChartWorkMode.PAN -> {
                //--- перезагружаем график, только если был горизонтальный сдвиг
                if (abs(screenOffsetX) >= 1) {
                    //--- именно в этом порядке операндов, чтобы:
                    //--- не было всегда 0 из-за целочисленного деления panDX / svgBodyWidth
                    //--- и не было возможного переполнения из-за умножения viewCoord.width * panDX
                    val deltaT = getTimeFromX(-screenOffsetX, bodyCanvasWidth, 0, chartViewCoord.width)
                    chartViewCoord.moveRel(deltaT)
                    chartRefreshView(chartViewCoord)
                }
            }

            ChartWorkMode.ZOOM_BOX /*, ChartWorkMode.SELECT_FOR_PRINT*/ -> {
                if (mouseRect.isVisible.value) {
                    mouseRect.isVisible.value = false
                    timeLabels[1].isVisible.value = false
                    timeLabels[2].isVisible.value = false

                    //--- если размер прямоугольника меньше MIN_USER_RECT_SIZE pix, то это видимо ошибка - игнорируем
                    if (abs(mouseRect.x2.value - mouseRect.x1.value) >= MIN_USER_RECT_SIZE * root.scaleKoef &&
                        abs(mouseRect.y2.value - mouseRect.y1.value) >= MIN_USER_RECT_SIZE * root.scaleKoef
                    ) {
                        //--- именно в этом порядке операндов, чтобы:
                        //--- не было всегда 0 из-за целочисленного деления min( mouseRect.x1, mouseRect.x2 ) / svgBodyWidth
                        //--- и не было возможного переполнения из-за умножения viewCoord.width * min( mouseRect.x1, mouseRect.x2 )
                        val newT1 = getTimeFromX(min(mouseRect.x1.value, mouseRect.x2.value), bodyCanvasWidth, chartViewCoord.t1, chartViewCoord.width)
                        val newT2 = getTimeFromX(max(mouseRect.x1.value, mouseRect.x2.value), bodyCanvasWidth, chartViewCoord.t1, chartViewCoord.width)
                        if (newT2 - newT1 >= MIN_SCALE_X) {
                            if (curMode == ChartWorkMode.ZOOM_BOX) {
                                chartRefreshView(ChartViewCoord(newT1, newT2))
                            } else {
                                //!!! пока пусть будет сразу печать с текущими границами, без возможности их отдельного определения перед печатью (а оно надо ли ?)
                                //outRect = mouseRectangle.getBoundsReal(  null  );
                                //outViewStage1();
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onDragCancel() {
        when (curMode) {
            ChartWorkMode.PAN -> {}

            ChartWorkMode.ZOOM_BOX -> {}
        }
    }

    private fun onPointerUp(pointerInputChange: PointerInputChange) {
        when (curMode) {
            ChartWorkMode.PAN -> {}

            ChartWorkMode.ZOOM_BOX -> {}
        }
    }

    private fun disableCursorLinesAndLabels() {
        grTimeLine.isVisible.value = false
        timeLabels.forEach { timeLabelData ->
            timeLabelData.isVisible.value = false
        }
    }

    private fun setTimeLabel(x: Float, timeLabelData: ChartTimeLabelData) {
        val cursorTime = getTimeFromX(x, bodyCanvasWidth, chartViewCoord.t1, chartViewCoord.width)

        timeLabelData.isVisible.value = true
        timeLabelData.x.value = x
        timeLabelData.text.value = getDateTimeDMYHMSString(root.appUserConfig.timeOffset, cursorTime).replace(" ", "\n  ")
    }

    //--- в double и обратно из-за ошибок округления
    private fun getTimeFromX(pixX: Float, pixWidth: Float, timeStart: Int, timeWidth: Int): Int = (1.0 * timeWidth * pixX / pixWidth + timeStart).roundToInt()

}

/*

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

private fun onGrMouseOver(syntheticMouseEvent: SyntheticMouseEvent, graphicElement: SvgElementData) {
    val mouseOffsetY = syntheticMouseEvent.offsetY.toInt()
    val mouseClientX = syntheticMouseEvent.clientX
    val mouseClientY = syntheticMouseEvent.clientY

    val arrViewBoxBody = getGraphicViewBoxBody()

    if (graphicElement is SvgLineData) {
        val yData = alYData[graphicElement.tooltip.toInt()]
        //--- именно в таком порядке, чтобы не нарваться на 0 при целочисленном делении
        //--- (yData.y1 - нижняя/большая координата, yData.y2 - верхняя/меньшая координата)
        var value = (yData.value2 - yData.value1) * (yData.y1 - (mouseOffsetY + arrViewBoxBody[1])) / (yData.y1 - yData.y2) + yData.value1
        if (yData.isReversedY) {
            value = -value
        }
        val tooltipValue = getSplittedDouble(value, yData.prec, true, '.')

        val (tooltipX, tooltipY) = getGraphixAndXyTooltipCoord(mouseClientX, mouseClientY)

        grTooltipVisible.value = true
        grTooltipText.value = tooltipValue
        grTooltipLeft.value = tooltipX.px
        grTooltipTop.value = tooltipY.px
        grTooltipOffTime = Date.now() + 3000

    } else if (graphicElement.tooltip.isNotEmpty()) {
        val (tooltipX, tooltipY) = getGraphixAndXyTooltipCoord(mouseClientX, mouseClientY)

        grTooltipVisible.value = true
        grTooltipText.value = graphicElement.tooltip
        grTooltipLeft.value = tooltipX.px
        grTooltipTop.value = tooltipY.px
        grTooltipOffTime = Date.now() + 3000
    } else {
        grTooltipVisible.value = false
    }
}

private fun onGrMouseOut() {
    //--- через 3 сек выключить тултип, если не было других активаций тултипов
    //--- причина: баг (?) в том, что mouseleave вызывается сразу после mouseenter,
    //--- причём после ухода с графика других mouseleave не вызывается.
    setGraphicAndXyTooltipOffTimeout(grTooltipOffTime, grTooltipVisible)
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

private fun fillGraphicData(mouseX: Int) {
    val timeOffset = root.timeOffset
    val cursorTime = getTimeFromX(mouseX, grSvgBodyWidth.value, chartViewCoord.t1, chartViewCoord.width)

    alGraphicDataData.clear()
    alGraphicDataData += "Дата/время: " + DateTime_YMDHMS(timeOffset, cursorTime)

    alElement.forEach { (_, element) ->

        element.alGDC.map { gdc ->
            gdc to element.alAxisYData[gdc.axisYIndex]
        }.filter { (gdc, _) ->
            gdc.type == ChartElementTypeDTO.LINE
        }.forEach { (gdc, yData) ->

            val index = gdc.alGLD.indexOfFirst { gld ->
                gld.x >= cursorTime
            }
            val s = (if (index != -1) {
                val gld = gdc.alGLD[index]
                if (gld.x == cursorTime) {
                    gld.y
                } else if (index > 0) {
                    val gldPrev = gdc.alGLD[index - 1]
                    1.0 * (cursorTime - gldPrev.x) / (gld.x - gldPrev.x) * (gld.y - gldPrev.y) + gldPrev.y
                } else {
                    null
                }
            } else {
                null
            })?.let { yValue ->
                yValue * if (gdc.isReversedY) {
                    -1
                } else {
                    1
                }
            }?.let { y ->
                getSplittedDouble(y, yData.prec, true, '.')
            } ?: "-"
            alGraphicDataData += "${yData.title} = $s"
        }
    }
}

private fun onGrMouseWheel(syntheticWheelEvent: SyntheticWheelEvent) {
    val isCtrl = syntheticWheelEvent.ctrlKey
    val mouseX = syntheticWheelEvent.offsetX.toInt()
    val deltaY = syntheticWheelEvent.deltaY.toInt()

    val (svgBodyLeft, svgBodyTop) = calcBodyLeftAndTop()

    if (grCurMode.value == GraphicWorkMode.PAN && !isMouseDown || grCurMode.value == GraphicWorkMode.ZOOM_BOX && !isMouseDown) {
        //|| grControl.curMode == GraphicModel.WorkMode.SELECT_FOR_PRINT && grControl.selectorX1 < 0  ) {
        //--- масштабирование
        if (isCtrl) {
            val t1 = chartViewCoord.t1
            val t2 = chartViewCoord.t2
            //--- вычисляем текущую координату курсора в реальных координатах
            val curT = getTimeFromX(mouseX, grSvgBodyWidth.value, t1, chartViewCoord.width)

            val newT1 = if (deltaY < 0) {
                curT - (curT - t1) / 2
            } else {
                curT - (curT - t1) * 2
            }
            val newT2 = if (deltaY < 0) {
                curT + (t2 - curT) / 2
            } else {
                curT + (t2 - curT) * 2
            }

            if (newT2 - newT1 in MIN_SCALE_X..MAX_SCALE_X) {
                grRefreshView(GraphicViewCoord(newT1, newT2))
            }
        }
        //--- вертикальная прокрутка
        else {
            val arrViewBoxAxis = getGraphicViewBoxAxis()
            val arrViewBoxBody = getGraphicViewBoxBody()
            val arrViewBoxLegend = getGraphicViewBoxLegend()

            val dy = (deltaY * scaleKoef).roundToInt()

            if (dy < 0) {
                listOf(arrViewBoxAxis, arrViewBoxBody, arrViewBoxLegend).forEach { arr ->
                    arr[1] += dy
                    if (arr[1] < 0) {
                        arr[1] = 0
                    }
                }
            } else if (dy > 0 && pixStartY - arrViewBoxAxis[1] > grSvgHeight.value) {
                listOf(arrViewBoxAxis, arrViewBoxBody, arrViewBoxLegend).forEach { arr ->
                    arr[1] += dy
                }
            }

            setGraphicViewBoxAxis(arrayOf(arrViewBoxAxis[0], arrViewBoxAxis[1], arrViewBoxAxis[2], arrViewBoxAxis[3]))
            setGraphicViewBoxBody(arrayOf(arrViewBoxBody[0], arrViewBoxBody[1], arrViewBoxBody[2], arrViewBoxBody[3]))
            setGraphicViewBoxLegend(arrayOf(arrViewBoxLegend[0], arrViewBoxLegend[1], arrViewBoxLegend[2], arrViewBoxLegend[3]))

            setGraphicTextOffset(svgBodyLeft, svgBodyTop)
        }
    }
*/

//    /*
//                    else if( comp == butShowForTime ) {
//                        try {
//                            val begTime = Arr_DateTime( appContainer.timeZone, arrayOf(
//                                Integer.parseInt( arrTxtDateTime[ 2 ]!!.text ), Integer.parseInt( arrTxtDateTime[ 1 ]!!.text ), Integer.parseInt( arrTxtDateTime[ 0 ]!!.text ),
//                                Integer.parseInt( arrTxtDateTime[ 3 ]!!.text ), Integer.parseInt( arrTxtDateTime[ 4 ]!!.text ), 0 ) )
//                            val endTime = Arr_DateTime( appContainer.timeZone, arrayOf(
//                                Integer.parseInt( arrTxtDateTime[ 7 ]!!.text ), Integer.parseInt( arrTxtDateTime[ 6 ]!!.text ), Integer.parseInt( arrTxtDateTime[ 5 ]!!.text ),
//                                Integer.parseInt( arrTxtDateTime[ 8 ]!!.text ), Integer.parseInt( arrTxtDateTime[ 9 ]!!.text ), 0 ) )
//                            val graphicWidth = endTime - begTime
//
//                            if( graphicWidth < GraphicModel.MIN_SCALE_X ) showError( "Ошибка задания периода", "Слишком короткий период для показа графика" )
//                            else if( graphicWidth > GraphicModel.MAX_SCALE_X ) showError( "Ошибка задания периода", "Слишком большой период для показа графика" )
//                            else grRefreshView( GraphicViewCoord( begTime, endTime ), 0 )
//                        }
//                        catch( nfe: NumberFormatException ) {
//                            showError( "Ошибка задания периода", "Неправильно заданы дата/время" )
//                        }
//
//                    }
//
//     */
//        ).add(
//            getGraphicSpecificComponentData()
//        )
//    }
//}

/*
private val panTimeBar = HBox( iAppContainer.DEFAULT_SPACING )
private val arrTxtDateTime = arrayOfNulls<TextField>( 10 )
private lateinit var butShowForTime: Button

    //--- нижняя панель временнОго масштабирования
    for( i in arrTxtDateTime.indices ) {
        arrTxtDateTime[ i ] = TextField()
        arrTxtDateTime[ i ]!!.setPrefColumnCount( if( i == 2 || i == 7 ) 4 else 2 )
        arrTxtDateTime[ i ]!!.setAlignment( Pos.CENTER )
        arrTxtDateTime[ i ]!!.setFont( curControlFont )
        arrTxtDateTime[ i ]!!.setOnKeyPressed( this )
    }

    butShowForTime = TextButton( "Показать" )
    butShowForTime.tooltip = Tooltip( "Показать график на заданный период" )
    butShowForTime.setOnAction( this )

    panTimeBar.children.addAll( Label( "Начало:" ), arrTxtDateTime[ 0 ], Label( "." ), arrTxtDateTime[ 1 ], Label( "." ), arrTxtDateTime[ 2 ], Label( " " ), arrTxtDateTime[ 3 ], Label( ":" ), arrTxtDateTime[ 4 ], Label( "Окончание:" ), arrTxtDateTime[ 5 ], Label( "." ), arrTxtDateTime[ 6 ], Label( "." ), arrTxtDateTime[ 7 ], Label( " " ), arrTxtDateTime[ 8 ], Label( ":" ), arrTxtDateTime[ 9 ], butShowForTime )

*/
