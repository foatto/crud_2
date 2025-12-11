package foatto.compose

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import foatto.compose.composable.GenerateMenuBody
import foatto.compose.composable.MainMenu
import foatto.compose.composable.PasswordChangeDialog
import foatto.compose.composable.StardartDialog
import foatto.compose.composable.TabPanel
import foatto.compose.composable.getClientSubMenus
import foatto.compose.control.TableControl
import foatto.compose.model.MenuDataClient
import foatto.compose.model.TabInfo
import foatto.compose.utils.SETTINGS_LOGIN
import foatto.compose.utils.SETTINGS_LOGON_EXPIRE
import foatto.compose.utils.SETTINGS_PASSWORD
import foatto.compose.utils.encodePassword
import foatto.compose.utils.getFullUrl
import foatto.compose.utils.getScaledWindowWidth
import foatto.compose.utils.openFileByUrl
import foatto.compose.utils.settings
import foatto.core.ActionType
import foatto.core.i18n.LanguageEnum
import foatto.core.i18n.LocalizedMessages
import foatto.core.i18n.getLocalizedMessage
import foatto.core.model.AppAction
import foatto.core.model.AppUserConfig
import foatto.core.model.request.ChangeLanguageRequest
import foatto.core.model.request.ChangePasswordRequest
import foatto.core.model.request.LogoffRequest
import foatto.core.model.response.ChangeLanguageResponse
import foatto.core.model.response.ChangePasswordResponse
import foatto.core.model.response.LogoffResponse
import io.kamel.core.getOrElse
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.launch

var defaultStartModule: String? = null

open class Root {

    companion object {
        //--- новый рекорд: Samsung A52, inner width = 509, outer width = 412
        private const val WIDE_SCREEN_WIDTH = 600
    }

    var defaultLang: LanguageEnum = LanguageEnum.RU

    var scaledWindowWidth: Int = 0
        private set
    var isWideScreen: Boolean by mutableStateOf(false)
        private set
    var scaleKoef: Float = 1.0f
        private set

    var appUserConfig: AppUserConfig by mutableStateOf(
        AppUserConfig(
            currentUserName = "",
            isAdmin = false,
            timeOffset = 0,
            lang = defaultLang,
            userProperties = mutableMapOf(),
        )
    )

    private var isShowMainMenuButton: Boolean by mutableStateOf(false)
    private val alMenuDataClient = mutableStateListOf<MenuDataClient>()
    private var isShowMainMenu by mutableStateOf(false)

    private var isTabPanelVisible by mutableStateOf(false)
    private var selectedTabIndex by mutableIntStateOf(0)
    private val tabInfos = mutableStateListOf<TabInfo>()

    private val controls = mutableStateListOf<AppControl>()
    var selectorControl: TableControl? by mutableStateOf(null)

    private var waitCount by mutableIntStateOf(0)

    private var lastTabId: Int = 0

    var dialogActionFun: suspend () -> Unit = {}
    var dialogContent by mutableStateOf<@Composable (() -> Unit)>({})
    var showDialogCancel by mutableStateOf(false)
    var showDialog by mutableStateOf(false)
    private val dialogButtonOkText by mutableStateOf(getLocalizedMessage(LocalizedMessages.OK, appUserConfig.lang))
    private val dialogButtonCancelText by mutableStateOf(getLocalizedMessage(LocalizedMessages.CANCEL, appUserConfig.lang))

    private var showPasswordChangeDialog by mutableStateOf(false)

    private val mainMenuVerticalScrollState = ScrollState(0)

    @Composable
    fun Content() {
        setCustomColors()

        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()

        scaledWindowWidth = getScaledWindowWidth()
        isWideScreen = scaledWindowWidth >= WIDE_SCREEN_WIDTH
        //--- инициализируем ДО того, как будет использоваться в Map/Scheme/Chart/Dashboard
        scaleKoef = density.density

        MaterialTheme {
            //--- define the all urgent size modifiers explicitly
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .paint(
                        painter = asyncPainterResource(getFullUrl("/images/index_2.png")).getOrElse(
                            onLoading = { ColorPainter(colorIndexBackgroundDefault) },
                            onFailure = { ColorPainter(colorIndexBackgroundDefault) },
                        ),
                        contentScale = ContentScale.FillBounds,
                    )
            ) {
                if (isWideScreen) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        //AnimatedVisibility(visible = isShowMainMenu) { - притормаживает со сложной графикой (с дашбоардами, например)
                        if (isShowMainMenu) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(widthMainMenu)
                                    .background(colorMainMenuDefault)
                            ) {
                                Row(
                                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    KamelImage(
                                        modifier = Modifier.padding(horizontal = 16.dp).size(18.dp),
                                        resource = { asyncPainterResource(data = getFullUrl("/images/ic_account_circle_black_18dp.png")) },
                                        colorFilter = ColorFilter.tint(colorUserName),
                                        contentDescription = "",
                                    )
                                    Text(
                                        modifier = Modifier.weight(1.0f),
                                        color = colorUserName,
                                        text = appUserConfig.currentUserName,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                KamelImage(
                                    modifier = Modifier
                                        .width(widthLogo)
                                        .height(heightLogo),
                                    resource = { asyncPainterResource(data = getFullUrl("/images/logo_2.png")) },
                                    contentDescription = null,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Column(
                                    modifier = Modifier
                                        .weight(1.0f)
                                        .verticalScroll(state = mainMenuVerticalScrollState)
                                ) {
                                    GenerateMenuBody(
                                        isMainMenu = true,
                                        alMenuDataClient = alMenuDataClient,
                                        level = 0,
                                        closeMenu = {},
                                        menuClick = { action: AppAction, inNewTab: Boolean ->
                                            coroutineScope.launch {
                                                openTab(action)
                                            }
                                        },
                                    )
                                }
                            }
                        }
                        RootBody()
                    }
                } else {
                    RootBody()
                }
                selectorControl?.Body()

                if (showPasswordChangeDialog) {
                    PasswordChangeDialog(
                        lang = appUserConfig.lang,
                        onOkClick = { newPassword ->
                            showPasswordChangeDialog = false
                            coroutineScope.launch {
                                invokeRequest(ChangePasswordRequest(encodePassword(newPassword))) { changePasswordResponse: ChangePasswordResponse ->
                                    showAlert(getLocalizedMessage(LocalizedMessages.PASSWORD_CHANGED_SUCCESSFULLY, appUserConfig.lang))
                                }
                            }
                        },
                        onCancelClick = {
                            showPasswordChangeDialog = false
                        }
                    )
                }
                if (waitCount > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color = Color.White.copy(alpha = 0.7f))
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(128.dp).align(Alignment.Center),
                            strokeWidth = 8.dp,
                            color = colorWait ?: ProgressIndicatorDefaults.circularColor,
                            trackColor = colorWaitTrack ?: ProgressIndicatorDefaults.circularIndeterminateTrackColor,
                        )
                    }
                }
                if (showDialog) {
                    StardartDialog(
                        content = dialogContent,
                        buttonOkText = dialogButtonOkText,
                        buttonCancelText = dialogButtonCancelText,
                        showCancelButton = showDialogCancel,
                        onOkClick = {
                            showDialog = false
                            coroutineScope.launch {
                                dialogActionFun()
                            }
                        },
                        onCancelClick = { showDialog = false },
                    )
                }
            }
        }
    }

    @Composable
    private fun RootBody() {
        val coroutineScope = rememberCoroutineScope()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorMainBack0)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column {
                    if (isShowMainMenuButton) {
                        FilledIconButton(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(0.dp),   // чтобы кнопка заполнила всё пространство
                            colors = colorIconButton ?: IconButtonDefaults.iconButtonColors(),
                            onClick = {
                                isShowMainMenu = !isShowMainMenu
                            }
                        ) {
                            Icon(
                                imageVector = if (isShowMainMenu) {
                                    Icons.AutoMirrored.Default.KeyboardArrowLeft
                                } else {
                                    Icons.Default.Menu
                                },
                                contentDescription = null,
                            )
                        }
                    }
                    if (!isWideScreen) {
                        MainMenu(
                            isMainMenu = false,
                            isShowMainMenu = isShowMainMenu,
                            alMenuDataClient = alMenuDataClient,
                            closeMenu = {
                                isShowMainMenu = false
                            },
                            menuClick = { action: AppAction, inNewTab: Boolean ->
                                coroutineScope.launch {
                                    openTab(action)
                                }
                            }
                        )
                    }
                }
                TabPanel(
                    isTabPanelVisible = isTabPanelVisible,
                    selectedTabIndex = selectedTabIndex,
                    tabInfos = tabInfos,
                    onTabClick = { index ->
                        selectedTabIndex = index
                    },
                    onTabCloseClick = { index ->
                        closeTabByIndex(index)
                    },
                )
            }
            controls.getOrNull(selectedTabIndex)?.Body()
        }
    }

    @Composable
    open fun setCustomColors() {
    }

    suspend fun start() {
        defaultStartModule?.let { startModule ->
            addTabComp(
                AppAction(
                    type = ActionType.MODULE_TABLE,
                    module = startModule,
                )
            )
        }
    }

    suspend fun openTab(action: AppAction) {
        when (action.type) {
            ActionType.FILE -> {
                action.url?.let { fileUrl ->
                    openFileByUrl(fileUrl)
                }
            }

            ActionType.CHANGE_PASSWORD -> {
                showPasswordChangeDialog = true
            }

            ActionType.LOGOFF -> {
                settings.remove(SETTINGS_LOGIN)
                settings.remove(SETTINGS_PASSWORD)
                settings.remove(SETTINGS_LOGON_EXPIRE)

                invokeRequest(LogoffRequest()) { logoffResponse: LogoffResponse ->
                    isShowMainMenuButton = false
                    isShowMainMenu = false

                    alMenuDataClient.clear()

                    isTabPanelVisible = false
                    selectedTabIndex = 0
                    tabInfos.clear()
                    controls.clear()

                    appUserConfig = AppUserConfig(
                        currentUserName = "",
                        isAdmin = false,
                        timeOffset = 0,
                        lang = defaultLang,
                        userProperties = mutableMapOf(),
                    )

                    start()
                }
            }

            ActionType.SET_LANGUAGE -> {
                action.module?.let { module ->
                    val lang = LanguageEnum.valueOf(module)

                    invokeRequest(ChangeLanguageRequest(lang)) { changeLanguageResponse: ChangeLanguageResponse ->
                        appUserConfig.lang = lang
                    }
                }
            }

            else -> {
                addTabComp(action)
            }
        }
    }

    private suspend fun addTabComp(action: AppAction) {
        lastTabId++

        val appControl = AppControl(this, action, lastTabId)

        tabInfos += TabInfo(lastTabId)
        controls += appControl

        selectedTabIndex = tabInfos.lastIndex

        appControl.start()
    }

    fun setTabInfo(tabId: Int, tabText: String) {
        tabInfos.find { tabInfo ->
            tabInfo.id == tabId
        }?.let { tabInfo ->
            tabInfo.texts.clear()
            tabInfo.texts.addAll(
                tabText.split('\n').filter { tabWord ->
                    tabWord.isNotBlank()
                }.map { tabWord ->
                    tabWord
                }
            )
        }
    }

    fun closeTabById(tabId: Int) {
        val indexForClose = tabInfos.indexOfFirst { tabInfo ->
            tabInfo.id == tabId
        }
        closeTabByIndex(indexForClose)
    }

    fun closeTabByIndex(indexForClose: Int) {
        //--- for last tab removing case
        if (selectedTabIndex == tabInfos.lastIndex) {
            selectedTabIndex--
        }

        controls.removeAt(indexForClose)
        tabInfos.removeAt(indexForClose)
    }

    fun setMenuBarData(alMenuData: List<MenuDataClient>, scaledWindowWidth: Int) {
        isShowMainMenuButton = true

        alMenuDataClient.clear()
        alMenuDataClient.addAll(alMenuData)
        alMenuDataClient.addAll(getClientSubMenus(appUserConfig, scaledWindowWidth, scaleKoef))

        isTabPanelVisible = true
    }

    fun setWait(isWait: Boolean) {
        waitCount += (if (isWait) 1 else -1)
    }

    fun showAlert(message: String) {
        dialogActionFun = {}
        dialogContent = { Text(text = message) }
        showDialogCancel = false
        showDialog = true
    }

}
