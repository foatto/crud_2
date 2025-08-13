package foatto.core.model.response.chart

import kotlinx.serialization.Serializable

@Serializable
class ChartElementData(
    val isReversedY: Boolean,
    val axisIndex: Int? = null,
    val lineWidth: Int = 0,
    val backs: List<ChartElementBackData>? = null,
    val lines: List<ChartElementLineData>? = null,
    val texts: List<ChartElementTextData>? = null,
) {
    fun isNotEmpty(): Boolean = backs?.isNotEmpty() ?: false || lines?.isNotEmpty() ?: false || texts?.isNotEmpty() ?: false
}
