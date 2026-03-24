package foatto.compose.utils

import java.security.MessageDigest
import kotlin.io.encoding.Base64

internal actual fun encodePassword(password: String): String {
    val bytes = MessageDigest
        .getInstance("SHA-1")
        .digest(password.toByteArray())

    return Base64.encode(bytes)
}
