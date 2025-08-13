package foatto.core.model.response

import foatto.core.model.response.chart.ChartData
import kotlinx.serialization.Serializable

@Serializable
class ChartActionResponse(
    override val responseCode: ResponseCode,

    val charts: List<ChartData> = emptyList(),
) : BaseResponse()

