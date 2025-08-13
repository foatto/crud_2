package foatto.core.model.response.chart

import kotlinx.serialization.Serializable

@Serializable
class ChartElementTextData(
    val x1: Int,
    val x2: Int,
    val fillColor: ULong,
    val borderColor: ULong,
    val textColor: ULong,
    val text: String,
)
