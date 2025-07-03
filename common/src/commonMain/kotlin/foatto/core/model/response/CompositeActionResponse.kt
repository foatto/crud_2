package foatto.core.model.response

import foatto.core.model.response.composite.CompositeBlock
import kotlinx.serialization.Serializable

@Serializable
class CompositeActionResponse(
    override val responseCode: ResponseCode,

    val headerData: HeaderData,
    val blocks: List<CompositeBlock>,
    val layoutSaveKey: String,

//    //--- additional custom parameters for any commands
//    val hmParam: Map<String, String> = emptyMap(),
) : BaseResponse()

