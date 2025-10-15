package foatto.core.model.response.chart

import kotlinx.serialization.Serializable

@Serializable
class ChartAxisData(
    val title: String,
    var min: Float,
    var max: Float,
    val color: Int,
    val isReversedY: Boolean,
) {
    //--- set on client-side
    var prec: Int = 0
}