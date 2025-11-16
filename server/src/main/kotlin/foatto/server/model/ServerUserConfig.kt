package foatto.server.model

import foatto.core.i18n.LanguageEnum
import foatto.core.model.AppUserConfig
import foatto.server.AppRole
import foatto.server.UserRelationEnum

class ServerUserConfig(
    val id: Int,
    val currentUserName: String,
    val roles: Set<String>,
    val timeOffset: Int,
    val lang: LanguageEnum,
    val fullNames: Map<Int, String>,
    val shortNames: Map<Int, String>,
    val relatedUserIds: Map<Int, UserRelationEnum>,
    val userProperties: MutableMap<String, String>
) {
    fun toAppUserConfig(): AppUserConfig = AppUserConfig(
        currentUserName = currentUserName,
        isAdmin = roles.contains(AppRole.ADMIN),
        timeOffset = timeOffset,
        lang = lang,
        userProperties = userProperties,
    )
}