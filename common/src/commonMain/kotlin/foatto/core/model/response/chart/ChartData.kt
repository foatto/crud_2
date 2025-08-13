package foatto.core.model.response.chart

import kotlinx.serialization.Serializable

@Serializable
class ChartData(
    val title: String,
    val axises: List<ChartAxisData>,
    val elements: List<ChartElementData>,
    val legends: List<ChartLegendData>,
)

