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
        //--- add disabled permissions for SUPPORT role to USER module
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

                addMenuItem(AppModuleMMS.OBJECT_SCHEME_LIST_DASHBOARD, ActionType.MODULE_COMPOSITE, null, serverUserConfig, this)
                addMenuItem(AppModuleMMS.OBJECT_CHART_LIST_DASHBOARD, ActionType.MODULE_COMPOSITE, null, serverUserConfig, this)

                if (size > 0) {
                    alMenu += MenuData(getLocalizedMMSMessage(LocalizedMMSMessages.CONTROL, serverUserConfig.lang), null, this)
                }
            }

            mutableListOf<MenuData>().apply {

                addMenuItem(AppModuleMMS.OBJECT, ActionType.MODULE_TABLE, null, serverUserConfig, this)
                addMenuItem(AppModuleMMS.DAY_WORK, ActionType.MODULE_TABLE, null, serverUserConfig, this)
                addMenuItem(AppModuleMMS.REPORT_SUMMARY, ActionType.MODULE_FORM, null, serverUserConfig, this)

                if (size > 0) {
                    alMenu += MenuData(getLocalizedMMSMessage(LocalizedMMSMessages.ACCOUNTING, serverUserConfig.lang), null, this)
                }
            }

            mutableListOf<MenuData>().apply {

                addMenuItem(AppModuleMMS.DEPARTMENT, ActionType.MODULE_TABLE, null, serverUserConfig, this)
                addMenuItem(AppModuleMMS.GROUP, ActionType.MODULE_TABLE, null, serverUserConfig, this)

                if (size > 0) {
                    alMenu += MenuData(getLocalizedMMSMessage(LocalizedMMSMessages.REFERENCES, serverUserConfig.lang), null, this)
                }
            }

            mutableListOf<MenuData>().apply {

                addMenuItem(AppModuleMMS.DEVICE, ActionType.MODULE_TABLE, null, serverUserConfig, this)

                if (size > 0) {
                    alMenu += MenuData(getLocalizedMMSMessage(LocalizedMMSMessages.DEVICES, serverUserConfig.lang), null, this)
                }
            }

            mutableListOf<MenuData>().apply {

                addMenuItem(AppModule.USER_PROPERTY_EDIT, ActionType.MODULE_FORM, serverUserConfig.id, serverUserConfig, this)
                addMenuItem(AppModule.USER, ActionType.MODULE_TABLE, null, serverUserConfig, this)

                if (size > 0) {
                    alMenu += MenuData(getLocalizedMessage(LocalizedMessages.SYSTEM, serverUserConfig.lang), null, this)
                }
            }

            alMenu
        }
    }

}
