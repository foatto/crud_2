package foatto.core.model

import kotlinx.serialization.Serializable

@Serializable
class AppMessage(
    val fromUserId: Int,
    val toUserId: Int,
    var message: String,
)