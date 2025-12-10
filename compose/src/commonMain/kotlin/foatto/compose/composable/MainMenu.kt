package foatto.compose.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import foatto.compose.colorMainMenuText
import foatto.compose.colorPopupMenuText
import foatto.compose.model.MenuDataClient
import foatto.compose.textSizeMainMenuFolder
import foatto.compose.textSizeMainMenuItem
import foatto.compose.textSizePopupMenuFolder
import foatto.compose.textSizePopupMenuItem
import foatto.compose.textWeightMainMenuFolder
import foatto.compose.textWeightPopupMenuFolder
import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.AppUserConfig


@Composable
internal fun MainMenu(
    isMainMenu: Boolean,
    isShowMainMenu: Boolean,
    alMenuDataClient: SnapshotStateList<MenuDataClient>,
    closeMenu: () -> Unit,
    menuClick: (action: AppAction, inNewTab: Boolean) -> Unit,
) {
    DropdownMenu(
        expanded = isShowMainMenu,
        onDismissRequest = closeMenu,
    ) {
        GenerateMenuBody(
            isMainMenu = isMainMenu,
            alMenuDataClient = alMenuDataClient,
            level = 0,
            closeMenu = closeMenu,
            menuClick = menuClick,
        )
    }
}

@Composable
internal fun GenerateMenuBody(
    isMainMenu: Boolean,
    alMenuDataClient: List<MenuDataClient>,
    level: Int,
    closeMenu: () -> Unit,
    menuClick: (action: AppAction, inNewTab: Boolean) -> Unit,
) {
    for (menuDataClient in alMenuDataClient) {
        menuDataClient.alSubMenu?.let { alSubMenu ->
            DropdownMenuItem(
                trailingIcon = {
                    Icon(
                        modifier = Modifier.clickable(
                            onClick = {
                                menuDataClient.isExpanded.value = !menuDataClient.isExpanded.value
                            }
                        ),
                        imageVector = if (menuDataClient.isExpanded.value) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        tint = if (isMainMenu) {
                            colorMainMenuText
                        } else {
                            colorPopupMenuText
                        },
                        contentDescription = null,
                    )
                },
                text = {
                    Text(
                        text = menuDataClient.caption,
                        fontSize = if (isMainMenu) {
                            textSizeMainMenuFolder
                        } else {
                            textSizePopupMenuFolder
                        },
                        fontWeight = if (isMainMenu) {
                            textWeightMainMenuFolder
                        } else {
                            textWeightPopupMenuFolder
                        },
                        color = if (isMainMenu) {
                            colorMainMenuText
                        } else {
                            colorPopupMenuText
                        },
                    )
                },
                contentPadding = PaddingValues(start = getMenuPadding(level), end = 16.dp),
                onClick = {
                    menuDataClient.isExpanded.value = !menuDataClient.isExpanded.value
                },
            )
            if (menuDataClient.isExpanded.value) {
                GenerateMenuBody(
                    isMainMenu = isMainMenu,
                    alMenuDataClient = alSubMenu,
                    level = level + 1,
                    menuClick = menuClick,
                    closeMenu = closeMenu,
                )
            }
        } ?: run {
            if (menuDataClient.action != null || menuDataClient.caption.isNotEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = menuDataClient.caption,
                            fontSize = if (isMainMenu) {
                                textSizeMainMenuItem
                            } else {
                                textSizePopupMenuItem
                            },
                            color = if (isMainMenu) {
                                colorMainMenuText
                            } else {
                                colorPopupMenuText
                            },
                        )
                    },
                    contentPadding = PaddingValues(start = getMenuPadding(level), end = 16.dp),
                    onClick = {
                        closeMenu()
                        menuDataClient.action?.let { menuAction ->
                            menuClick(menuAction, menuDataClient.inNewTab)
                        }
                    },
                )
            } else {
                HorizontalDivider(
                    modifier = Modifier.padding(start = getMenuPadding(level)),
                )
            }
        }
    }
}

private fun getMenuPadding(level: Int) = ((1 + level) * 16).dp

internal fun getClientSubMenus(
    appUserConfig: AppUserConfig,
    scaledWindowWidth: Int,
    scaleKoef: Float,
): List<MenuDataClient> {
    val alClientSubMenu = mutableListOf<MenuDataClient>()

    alClientSubMenu += MenuDataClient(caption = "Сменить пароль", action = AppAction(ActionType.CHANGE_PASSWORD))
    alClientSubMenu += MenuDataClient(caption = "Выход из системы", action = AppAction(ActionType.LOGOFF))

    if (appUserConfig.isAdmin) {
        alClientSubMenu += MenuDataClient(caption = "")

        alClientSubMenu += MenuDataClient(caption = "scaled window width = $scaledWindowWidth")
        alClientSubMenu += MenuDataClient(caption = "device pixel ratio = $scaleKoef")
//    alClientSubMenu.add(MenuDataClient(url = "", text = "touch screen = ${getStyleIsTouchScreen()}"))
    }

//    alClientSubMenu += MenuDataClient(caption = "")
//    LanguageEnum.entries.forEach { lang ->
//        alClientSubMenu += MenuDataClient(caption = lang.descr, action = AppAction(type = ActionType.SET_LANGUAGE, module = lang.name))
//    }

    return alClientSubMenu
}