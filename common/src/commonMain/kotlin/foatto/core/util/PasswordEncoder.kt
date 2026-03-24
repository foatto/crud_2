package foatto.core.util

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.SHA1
import kotlin.io.encoding.Base64

@OptIn(DelicateCryptographyApi::class)
suspend fun encodePassword2(password: String): String {
    val bytes = CryptographyProvider.Default
        .get(SHA1)
        .hasher()
        .hash(password.encodeToByteArray())

    return Base64.encode(bytes)
}