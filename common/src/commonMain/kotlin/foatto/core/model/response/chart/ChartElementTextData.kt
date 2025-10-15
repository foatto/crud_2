package foatto.core.model.response.chart

import kotlinx.serialization.Serializable

@Serializable
class ChartElementTextData(
    val x1: Int,
    val x2: Int,
    val fillColor: Int,
    val borderColor: Int,
    val textColor: Int,
    val text: String,
)
