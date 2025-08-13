package foatto.core.model.response.chart

import kotlinx.serialization.Serializable

@Serializable
class ChartLegendData(
    val fillColor: ULong?,
    val borderColor: ULong,
    val textColor: ULong,
    val text: String,
)
