package foatto.core.model.response

import kotlinx.serialization.Serializable

@Serializable
class ChangeLanguageResponse(
    override val responseCode: ResponseCode = ResponseCode.NOTHING,
) : BaseResponse()
