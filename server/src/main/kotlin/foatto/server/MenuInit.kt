package foatto.server

import foatto.core.i18n.getLocalizedMessage
import foatto.core.model.AppAction
import foatto.core.model.response.MenuData
import foatto.server.model.ServerUserConfig

var menuInit: (serverUserConfig: ServerUserConfig) -> List<MenuData> = { serverUserConfig -> mutableListOf() }

fun addMenuItem(
    alMenu: MutableList<MenuData>,
    serverUserConfig: ServerUserConfig,
    module: String,
    actionType: String,
    id: Int? = null,
    alterCaption: String? = null,
    params: MutableMap<String, String> = mutableMapOf(),
) {
    appModuleConfigs[module]?.let { moduleConfig ->
        if (checkAccessPermission(module, serverUserConfig.roles)) {
            alMenu += MenuData(
                caption = alterCaption ?: getLocalizedMessage(moduleConfig.captions, serverUserConfig.lang),
                action = AppAction(
                    type = actionType,
                    module = module,
                    id = id,
                    params = params,
                ),
            )
        }
    }
}

fun addSeparator(alMenu: MutableList<MenuData>) {
    alMenu += MenuData("", null)
}
