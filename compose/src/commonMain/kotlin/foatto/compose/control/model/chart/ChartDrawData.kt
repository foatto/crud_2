package foatto.compose.control.model.chart

internal class ChartDrawData(
    val title: ChartTextDrawData,
    val yAxisLines: MutableList<ChartLineDrawData>,
    val yAxisTexts: MutableList<ChartTextDrawData>,
    val xAxisLines: MutableList<ChartLineDrawData>,
    val xAxisTexts: MutableList<ChartTextDrawData>,
    val chartBacks: MutableList<ChartRectDrawData>,
    val chartLines: MutableList<ChartLineDrawData>,
//    val alChartPoint: MutableList<ChartCircleDrawData>, - пок не используется (?)
    val chartTexts: MutableList<ChartTextDrawData>,
    val legendBacks: MutableList<ChartRectDrawData>,
    val legendTexts: MutableList<ChartTextDrawData>,
)
