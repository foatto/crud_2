package foatto.core.model.response.chart

import kotlinx.serialization.Serializable

@Serializable
class ChartElementLineData(
    val x: Int,
    var y: Float,
    val color: ULong,
)
