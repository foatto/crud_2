package foatto.core.model.response.chart

import kotlinx.serialization.Serializable

@Serializable
class ChartLegendData(
    val fillColor: Int?,
    val borderColor: Int,
    val textColor: Int,
    val text: String,
)
