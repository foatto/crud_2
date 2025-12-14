package foatto.server

import foatto.core.ActionType
import foatto.core.AppModule
import foatto.core.i18n.LanguageEnum
import foatto.core.i18n.LocalizedMessages
import foatto.core.i18n.getLocalizedMessage
import foatto.core.model.response.MenuData
import foatto.core_mms.AppModuleMMS
import foatto.core_mms.addAppModuleUrls
import foatto.core_mms.i18n.LocalizedMMSMessages
import foatto.core_mms.i18n.getLocalizedMMSMessage
import foatto.server.model.AppModuleConfig
import foatto.server.model.AppRoleConfig
import foatto.server.model.Permission
import foatto.server.service.ObjectService
import foatto.server.util.AdvancedLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener

@SpringBootApplication(
    scanBasePackages = ["foatto.server"],
)
class MMSSpringApp : SpringApp() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<MMSSpringApp>(*args)
        }
    }

    @Value("\${log_dir}")
    val logDir: String = ""

    @Value("\${log_options}")
    val logOptions: String = ""

    @EventListener(ApplicationReadyEvent::class)
    override fun init() {
        super.init()

        AdvancedLogger.init(logDir, logOptions.contains("error"), logOptions.contains("info"), logOptions.contains("debug"))
        AdvancedLogger.info("==================== MMSSpringApp started ====================")

        addAppModuleUrls()
        //--- строго после addAppModuleUrls
        initAppRoleConfigs()
        initAppModuleConfigs()
        initMenuInit()
    }

    private fun initAppRoleConfigs() {
        appRoleConfigs[AppRoleMMS.SUPPORT] = AppRoleConfig(
            redirectOnLogon = null,
        )

        appRoleConfigs[AppRoleMMS.USER_FIXED_OBJECTS] = AppRoleConfig(
            redirectOnLogon = null,
        )
        appRoleConfigs[AppRoleMMS.USER_MOBILE_OBJECTS] = AppRoleConfig(
            redirectOnLogon = null,
        )
    }

    private fun initAppModuleConfigs() {
        //--- add disabled permissions for SUPPORT role to some system modules
        appModuleConfigs[AppModule.ACTION_LOG]?.disabledAccessRoles?.add(AppRoleMMS.SUPPORT)
        appModuleConfigs[AppModule.USER]?.disabledFormAddRoles?.add(AppRoleMMS.SUPPORT)
        addDisabledRoles(AppModule.USER, ActionType.FORM_EDIT, AppRoleMMS.SUPPORT)
        addDisabledRoles(AppModule.USER, ActionType.FORM_DELETE, AppRoleMMS.SUPPORT)

        appModuleConfigs[AppModuleMMS.OBJECT] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Objects", LanguageEnum.RU to "Объекты"),
            pageSize = AppModuleConfig.DEFAULT_PAGE_SIZE,
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
            enabledFormAddRoles = mutableSetOf(AppRole.ADMIN),
            disabledFormAddRoles = mutableSetOf(AppRole.USER),
            rowPermissions = mutableMapOf(
                ActionType.MODULE_TABLE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                        getOrPut(UserRelationEnum.WORKER) { mutableSetOf() } += AppRole.USER
                    },
                ),
                ActionType.MODULE_FORM to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                        getOrPut(UserRelationEnum.WORKER) { mutableSetOf() } += AppRole.USER
                    },
                ),
                ActionType.FORM_EDIT to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
                ActionType.FORM_DELETE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
            ),
        )

        appModuleConfigs[AppModuleMMS.DEPARTMENT] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Departments", LanguageEnum.RU to "Подразделения"),
            pageSize = AppModuleConfig.DEFAULT_PAGE_SIZE,
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
            enabledFormAddRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledFormAddRoles = mutableSetOf(),
            rowPermissions = mutableMapOf(
                ActionType.MODULE_TABLE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                        getOrPut(UserRelationEnum.WORKER) { mutableSetOf() } += AppRole.USER
                    },
                ),
                ActionType.MODULE_FORM to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                        getOrPut(UserRelationEnum.WORKER) { mutableSetOf() } += AppRole.USER
                    },
                ),
                ActionType.FORM_EDIT to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                    },
                ),
                ActionType.FORM_DELETE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                    },
                ),
            ),
        )
        appModuleConfigs[AppModuleMMS.GROUP] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Groups", LanguageEnum.RU to "Группы"),
            pageSize = AppModuleConfig.DEFAULT_PAGE_SIZE,
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
            enabledFormAddRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledFormAddRoles = mutableSetOf(),
            rowPermissions = mutableMapOf(
                ActionType.MODULE_TABLE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                        getOrPut(UserRelationEnum.WORKER) { mutableSetOf() } += AppRole.USER
                    },
                ),
                ActionType.MODULE_FORM to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                        getOrPut(UserRelationEnum.WORKER) { mutableSetOf() } += AppRole.USER
                    },
                ),
                ActionType.FORM_EDIT to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                    },
                ),
                ActionType.FORM_DELETE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                    },
                ),
            ),
        )

        appModuleConfigs[AppModuleMMS.SENSOR] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Sensors", LanguageEnum.RU to "Датчики"),
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN),
            disabledAccessRoles = mutableSetOf(AppRole.USER),
            enabledFormAddRoles = mutableSetOf(AppRole.ADMIN),
            disabledFormAddRoles = mutableSetOf(AppRole.USER),
            rowPermissions = mutableMapOf(
                ActionType.MODULE_TABLE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
                ActionType.MODULE_FORM to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
                ActionType.FORM_EDIT to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
                ActionType.FORM_DELETE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
            ),
        )
        appModuleConfigs[AppModuleMMS.SENSOR_CALIBRATION] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Sensor calibration", LanguageEnum.RU to "Тарировка датчика"),
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN),
            disabledAccessRoles = mutableSetOf(AppRole.USER),
            enabledFormAddRoles = mutableSetOf(AppRole.ADMIN),
            disabledFormAddRoles = mutableSetOf(AppRole.USER),
            rowPermissions = mutableMapOf(
                ActionType.MODULE_TABLE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
                ActionType.MODULE_FORM to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
                ActionType.FORM_EDIT to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
                ActionType.FORM_DELETE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
            ),
        )

        appModuleConfigs[AppModuleMMS.OBJECT_DATA] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Object data", LanguageEnum.RU to "Данные по объекту"),
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN),
            disabledAccessRoles = mutableSetOf(AppRole.USER),
//            enabledFormAddRoles = mutableSetOf(AppRole.ADMIN),
//            disabledFormAddRoles = mutableSetOf(AppRole.USER),
            rowPermissions = mutableMapOf(
                ActionType.MODULE_TABLE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
//                ActionType.MODULE_FORM to Permission(
//                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
//                    disabledRoles = getRoleAllPermissions(AppRole.USER),
//                ),
//                ActionType.FORM_EDIT to Permission(
//                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
//                    disabledRoles = getRoleAllPermissions(AppRole.USER),
//                ),
//                ActionType.FORM_DELETE to Permission(
//                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
//                    disabledRoles = getRoleAllPermissions(AppRole.USER),
//                ),
            ),
        )
        appModuleConfigs[AppModuleMMS.SENSOR_DATA] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Sensor data", LanguageEnum.RU to "Данные по датчику"),
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN),
            disabledAccessRoles = mutableSetOf(AppRole.USER),
//            enabledFormAddRoles = mutableSetOf(AppRole.ADMIN),
//            disabledFormAddRoles = mutableSetOf(AppRole.USER),
            rowPermissions = mutableMapOf(
                ActionType.MODULE_TABLE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
//                ActionType.MODULE_FORM to Permission(
//                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
//                    disabledRoles = getRoleAllPermissions(AppRole.USER),
//                ),
//                ActionType.FORM_EDIT to Permission(
//                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
//                    disabledRoles = getRoleAllPermissions(AppRole.USER),
//                ),
//                ActionType.FORM_DELETE to Permission(
//                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
//                    disabledRoles = getRoleAllPermissions(AppRole.USER),
//                ),
            ),
        )

        appModuleConfigs[AppModuleMMS.DEVICE] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Devices", LanguageEnum.RU to "Контроллеры"),
            pageSize = AppModuleConfig.DEFAULT_PAGE_SIZE,
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN),
            disabledAccessRoles = mutableSetOf(AppRole.USER),
            enabledFormAddRoles = mutableSetOf(AppRole.ADMIN),
            disabledFormAddRoles = mutableSetOf(AppRole.USER),
            rowPermissions = mutableMapOf(
                ActionType.MODULE_TABLE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                        getOrPut(UserRelationEnum.WORKER) { mutableSetOf() } += AppRole.USER
                    },
                ),
                ActionType.MODULE_FORM to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                        getOrPut(UserRelationEnum.WORKER) { mutableSetOf() } += AppRole.USER
                    },
                ),
                ActionType.FORM_EDIT to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
                ActionType.FORM_DELETE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
            ),
        )
        appModuleConfigs[AppModuleMMS.DEVICE_MANAGE] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Device control", LanguageEnum.RU to "Управление контроллером"),
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN),
            disabledAccessRoles = mutableSetOf(AppRole.USER),
            enabledFormAddRoles = mutableSetOf(AppRole.ADMIN),
            disabledFormAddRoles = mutableSetOf(AppRole.USER),
            rowPermissions = mutableMapOf(
                ActionType.MODULE_TABLE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
                ActionType.MODULE_FORM to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
                ActionType.FORM_EDIT to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
                ActionType.FORM_DELETE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
            ),
        )

        appModuleConfigs[AppModuleMMS.DAY_WORK] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Daily work log", LanguageEnum.RU to "Журнал суточных работ"),
            pageSize = AppModuleConfig.DEFAULT_PAGE_SIZE,
//            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
//            disabledAccessRoles = mutableSetOf(),
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN),
            disabledAccessRoles = mutableSetOf(AppRoleMMS.SUPPORT, AppRole.USER),
            enabledFormAddRoles = mutableSetOf(AppRole.ADMIN),
            disabledFormAddRoles = mutableSetOf(AppRole.USER),
            rowPermissions = mutableMapOf(
                ActionType.MODULE_TABLE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                        getOrPut(UserRelationEnum.WORKER) { mutableSetOf() } += AppRole.USER
                    },
                ),
                ActionType.MODULE_FORM to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
                ActionType.FORM_EDIT to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
                ActionType.FORM_DELETE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
            ),
        )

        appModuleConfigs[AppModuleMMS.CHART_SENSOR] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Chart by sensor", LanguageEnum.RU to "График по датчику"),
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRoleMMS.USER_FIXED_OBJECTS),
            disabledAccessRoles = mutableSetOf(),
        )
//        appModuleConfigs[AppModuleMMS.CHART_LIQUID_LEVEL] = AppModuleConfig(
//            caption = "Графики уровня топлива",
//            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRoleMMS.USER_FIXED_OBJECTS),
//            disabledAccessRoles = mutableSetOf(),
//        )

        appModuleConfigs[AppModuleMMS.MAP_TRACE] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Trajectory on the map", LanguageEnum.RU to "Траектория на карте"),
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRoleMMS.USER_MOBILE_OBJECTS),
            disabledAccessRoles = mutableSetOf(),
        )

        appModuleConfigs[AppModuleMMS.SCHEME_ANALOGUE_INDICATOR_STATE] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Analog indicator of the object sensor", LanguageEnum.RU to "Аналоговый индикатор датчика объекта"),
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
        )
        appModuleConfigs[AppModuleMMS.SCHEME_COUNTER_INDICATOR_STATE] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Object counter sensor indicator", LanguageEnum.RU to "Индикатор счётного датчика объекта"),
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
        )
        appModuleConfigs[AppModuleMMS.SCHEME_WORK_INDICATOR_STATE] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Object operation sensor indicator", LanguageEnum.RU to "Индикатор датчика работы объекта"),
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
        )

        appModuleConfigs[AppModuleMMS.OBJECT_SCHEME_DASHBOARD] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Object control indicators: all sensors", LanguageEnum.RU to "Контрольные показатели объекта: все датчики"),
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
        )
        appModuleConfigs[AppModuleMMS.OBJECT_SCHEME_LIST_DASHBOARD] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Object control indicators", LanguageEnum.RU to "Контрольные показатели объектов"),
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
        )
        appModuleConfigs[AppModuleMMS.OBJECT_CHART_DASHBOARD] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Object control charts: all sensors", LanguageEnum.RU to "Контрольные графики объекта: все датчики"),
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
        )
        appModuleConfigs[AppModuleMMS.OBJECT_CHART_LIST_DASHBOARD] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Control charts of objects", LanguageEnum.RU to "Контрольные графики объектов"),
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
        )

        appModuleConfigs[AppModuleMMS.REPORT_SUMMARY] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Summary report", LanguageEnum.RU to "Суммарный отчёт"),
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN),   //, AppRole.USER),
            disabledAccessRoles = mutableSetOf(AppRoleMMS.SUPPORT),
            enabledFormAddRoles = mutableSetOf(AppRole.ADMIN),  //, AppRole.USER),
            disabledFormAddRoles = mutableSetOf(),
            rowPermissions = mutableMapOf(
//                ActionType.MODULE_TABLE to Permission(
//                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
//                    disabledRoles = getRoleAllPermissions(AppRole.USER),
//                ),
                ActionType.MODULE_FORM to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN, AppRole.USER),
//                    disabledRoles = getRoleAllPermissions(),
                ),
//                ActionType.FORM_EDIT to Permission(
//                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
//                    disabledRoles = getRoleAllPermissions(AppRole.USER),
//                ),
//                ActionType.FORM_DELETE to Permission(
//                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
//                    disabledRoles = getRoleAllPermissions(AppRole.USER),
//                ),
            ),
        )

    }

    private fun initMenuInit() {
        menuInit = { serverUserConfig ->
            val alMenu = mutableListOf<MenuData>()

            mutableListOf<MenuData>().apply {

                addMenuItem(
                    alMenu = this,
                    serverUserConfig = serverUserConfig,
                    module = AppModuleMMS.OBJECT_SCHEME_LIST_DASHBOARD,
                    actionType = ActionType.MODULE_COMPOSITE,
                    iconUrl = "/images/icons8-dashboard-gauge-24.png",
                    iconSize = 24,
                )
                addMenuItem(
                    alMenu = this,
                    serverUserConfig = serverUserConfig,
                    module = AppModuleMMS.OBJECT_CHART_LIST_DASHBOARD,
                    actionType = ActionType.MODULE_COMPOSITE,
                    iconUrl = "/images/icons8-combo-chart-24.png",
                    iconSize = 24,
                )

                if (size > 0) {
                    alMenu += MenuData(
                        caption = getLocalizedMMSMessage(LocalizedMMSMessages.CONTROL, serverUserConfig.lang),
                        action = null,
                        subMenuDatas = this
                    )
                }
            }

            mutableListOf<MenuData>().apply {
                addMenuItem(
                    alMenu = this,
                    serverUserConfig = serverUserConfig,
                    module = AppModuleMMS.OBJECT,
                    actionType = ActionType.MODULE_TABLE,
                    id = null,
                    alterCaption = "Мобильные объекты",
                    params = mutableMapOf(ObjectService.FIELD_TYPE to ObjectType.MOBILE.name),
                    iconUrl = "/images/icons8-truck-24.png",
                    iconSize = 24,
                )
                addMenuItem(
                    alMenu = this,
                    serverUserConfig = serverUserConfig,
                    module = AppModuleMMS.OBJECT,
                    actionType = ActionType.MODULE_TABLE,
                    id = null,
                    alterCaption = "Стационарные объекты",
                    params = mutableMapOf(ObjectService.FIELD_TYPE to ObjectType.STATIONARY.name),
                    iconUrl = "/images/icons8-oil-pump-24.png",
                    iconSize = 24,
                )
                addMenuItem(
                    alMenu = this,
                    serverUserConfig = serverUserConfig,
                    module = AppModuleMMS.DAY_WORK,
                    actionType = ActionType.MODULE_TABLE,
                    iconUrl = "/images/icons8-logbook-24.png",
                    iconSize = 24,
                )

                if (size > 0) {
                    alMenu += MenuData(
                        caption = getLocalizedMMSMessage(LocalizedMMSMessages.ACCOUNTING, serverUserConfig.lang),
                        action = null,
                        subMenuDatas = this
                    )
                }
            }

            mutableListOf<MenuData>().apply {

                addMenuItem(
                    alMenu = this,
                    serverUserConfig = serverUserConfig,
                    module = AppModuleMMS.REPORT_SUMMARY,
                    actionType = ActionType.MODULE_FORM,
                    iconUrl = "/images/icons8-statistics-report-24.png",
                    iconSize = 24,
                )

                if (size > 0) {
                    alMenu += MenuData(
                        caption = getLocalizedMMSMessage(LocalizedMMSMessages.REPORTS, serverUserConfig.lang),
                        action = null,
                        subMenuDatas = this
                    )
                }
            }

            mutableListOf<MenuData>().apply {

                addMenuItem(
                    alMenu = this,
                    serverUserConfig = serverUserConfig,
                    module = AppModuleMMS.DEPARTMENT,
                    actionType = ActionType.MODULE_TABLE,
                    iconUrl = "/images/icons8-organization-chart-people-24.png",
                    iconSize = 24,
                )
                addMenuItem(
                    alMenu = this,
                    serverUserConfig = serverUserConfig,
                    module = AppModuleMMS.GROUP,
                    actionType = ActionType.MODULE_TABLE,
                    iconUrl = "/images/icons8-organization-chart-people-24.png",
                    iconSize = 24,
                )

                if (size > 0) {
                    alMenu += MenuData(
                        caption = getLocalizedMMSMessage(LocalizedMMSMessages.REFERENCES, serverUserConfig.lang),
                        action = null,
                        subMenuDatas = this
                    )
                }
            }

            mutableListOf<MenuData>().apply {

                addMenuItem(
                    alMenu = this,
                    serverUserConfig = serverUserConfig,
                    module = AppModuleMMS.DEVICE,
                    actionType = ActionType.MODULE_TABLE,
                    iconUrl = "/images/icons8-network-gateway-24.png",
                    iconSize = 24,
                )

                if (size > 0) {
                    alMenu += MenuData(
                        caption = getLocalizedMMSMessage(message = LocalizedMMSMessages.DEVICES, lang = serverUserConfig.lang),
                        action = null,
                        subMenuDatas = this,
                    )
                }
            }

            mutableListOf<MenuData>().apply {

                addMenuItem(
                    alMenu = this,
                    serverUserConfig = serverUserConfig,
                    module = AppModule.USER_PROPERTY_EDIT,
                    actionType = ActionType.MODULE_FORM,
                    id = serverUserConfig.id,
                    iconUrl = "/images/icons8-users-settings-24.png",
                    iconSize = 24,
                )
                addMenuItem(
                    alMenu = this,
                    serverUserConfig = serverUserConfig,
                    module = AppModule.USER,
                    actionType = ActionType.MODULE_TABLE,
                    iconUrl = "/images/icons8-users-24.png",
                    iconSize = 24,
                )
                addMenuItem(
                    alMenu = this,
                    serverUserConfig = serverUserConfig,
                    module = AppModule.ACTION_LOG,
                    actionType = ActionType.MODULE_TABLE,
                    iconUrl = "/images/icons8-log-24.png",
                    iconSize = 24,
                )

                if (size > 0) {
                    alMenu += MenuData(
                        caption = getLocalizedMessage(LocalizedMessages.SYSTEM, serverUserConfig.lang),
                        action = null,
                        subMenuDatas = this
                    )
                }
            }

            alMenu
        }
    }

}
