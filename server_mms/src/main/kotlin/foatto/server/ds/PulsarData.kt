package foatto.server.ds

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

//--- outer JSON-class, don't rename fields!
@OptIn(ExperimentalTime::class)
@Serializable
data class PulsarData(
    @Contextual
    val dateTime: Instant? = null,

    val deviceID: String? = null,
    val blockID: String? = null,

    val idx: Int? = null,
    val vals: List<Map<String, Double>>? = null,
)

