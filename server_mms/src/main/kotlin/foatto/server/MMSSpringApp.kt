package foatto.server

import foatto.core.ActionType
import foatto.core.AppModule
import foatto.core.i18n.LanguageEnum
import foatto.core.i18n.LocalizedMessages
import foatto.core.i18n.getLocalizedMessage
import foatto.core.model.AppAction
import foatto.core.model.response.MenuData
import foatto.core.model.response.table.TablePopup
import foatto.core_mms.AppModuleMMS
import foatto.core_mms.addAppModuleUrls
import foatto.core_mms.i18n.LocalizedMMSMessages
import foatto.core_mms.i18n.getLocalizedMMSMessage
import foatto.server.model.AppModuleConfig
import foatto.server.model.AppRoleConfig
import foatto.server.model.Permission
import foatto.server.model.ServerUserConfig
import foatto.server.repository.DayWorkRepository
import foatto.server.repository.DepartmentRepository
import foatto.server.repository.DeviceManageRepository
import foatto.server.repository.DeviceRepository
import foatto.server.repository.GroupRepository
import foatto.server.repository.ObjectRepository
import foatto.server.repository.WorkShiftRepository
import foatto.server.service.UserService
import foatto.server.util.AdvancedLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener

@SpringBootApplication(
    scanBasePackages = ["foatto.server"],
)
class MMSSpringApp(
    private val objectRepository: ObjectRepository,
    private val departmentRepository: DepartmentRepository,
    private val groupRepository: GroupRepository,
    private val deviceRepository: DeviceRepository,
    private val dayWorkRepository: DayWorkRepository,
    private val workShiftRepository: WorkShiftRepository,
    private val deviceManageRepository: DeviceManageRepository,
) : SpringApp() {

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
        setServicesCustomFuns()
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

        appModuleConfigs[AppModuleMMS.ALL_OBJECT] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "All objects", LanguageEnum.RU to "Все объекты"),
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

        appModuleConfigs[AppModuleMMS.MOBILE_OBJECT] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Mobile objects", LanguageEnum.RU to "Мобильные объекты"),
            pageSize = AppModuleConfig.DEFAULT_PAGE_SIZE,
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRoleMMS.USER_MOBILE_OBJECTS),
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

        appModuleConfigs[AppModuleMMS.STATIONARY_OBJECT] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Stationary objects", LanguageEnum.RU to "Стационарные объекты"),
            pageSize = AppModuleConfig.DEFAULT_PAGE_SIZE,
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRoleMMS.USER_FIXED_OBJECTS),
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

        appModuleConfigs[AppModuleMMS.DAY_ALL_WORK] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Daily all works log", LanguageEnum.RU to "Журнал всех суточных работ"),
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
        appModuleConfigs[AppModuleMMS.DAY_MOBILE_WORK] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Daily mobile works log", LanguageEnum.RU to "Журнал мобильных суточных работ"),
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
        appModuleConfigs[AppModuleMMS.DAY_STATIONARY_WORK] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Daily stationary works log", LanguageEnum.RU to "Журнал стационарных суточных работ"),
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
        appModuleConfigs[AppModuleMMS.WORK_SHIFT] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Shift work log", LanguageEnum.RU to "Журнал посменных работ"),
            pageSize = AppModuleConfig.DEFAULT_PAGE_SIZE,
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRoleMMS.USER_FIXED_OBJECTS),
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

        appModuleConfigs[AppModuleMMS.CHART_SENSOR] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Chart by sensor", LanguageEnum.RU to "График по датчику"),
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
        )
        appModuleConfigs[AppModuleMMS.CHART_ALL_SENSORS] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Charts for all sensors", LanguageEnum.RU to "Графики по всем датчикам"),
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
        )
        appModuleConfigs[AppModuleMMS.CHART_ENERGO_SENSORS] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Charts for energo sensors", LanguageEnum.RU to "Графики по электросчётчикам"),
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
        )
        appModuleConfigs[AppModuleMMS.CHART_LIQUID_LEVEL] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Combined charts for fuel tanks", LanguageEnum.RU to "Комбинированные графики уровня топлива"),
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRoleMMS.USER_FIXED_OBJECTS),
            disabledAccessRoles = mutableSetOf(),
        )

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
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
            enabledFormAddRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledFormAddRoles = mutableSetOf(),
            rowPermissions = mutableMapOf(
                ActionType.MODULE_FORM to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN, AppRole.USER),
                ),
            ),
        )
        appModuleConfigs[AppModuleMMS.REPORT_DAY_WORK] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Day work report", LanguageEnum.RU to "Отчёт по суточным работам"),
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
            enabledFormAddRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledFormAddRoles = mutableSetOf(),
            rowPermissions = mutableMapOf(
                ActionType.MODULE_FORM to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN, AppRole.USER),
                ),
            ),
        )
        appModuleConfigs[AppModuleMMS.REPORT_WORK_SHIFT] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Work shift report", LanguageEnum.RU to "Отчёт по рабочим сменам"),
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRoleMMS.USER_FIXED_OBJECTS),
            disabledAccessRoles = mutableSetOf(),
            enabledFormAddRoles = mutableSetOf(AppRole.ADMIN, AppRoleMMS.USER_FIXED_OBJECTS),
            disabledFormAddRoles = mutableSetOf(),
            rowPermissions = mutableMapOf(
                ActionType.MODULE_FORM to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN, AppRoleMMS.USER_FIXED_OBJECTS),
                ),
            ),
        )

        //--- упрощённая настройка телеграм-модулей - модифицируем настройки классических модулей
        appModuleConfigs[AppModuleMMS.T_OBJECT] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Objects", LanguageEnum.RU to "Объекты"),
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
        )
        appModuleConfigs[AppModuleMMS.T_DEVICE] = AppModuleConfig(
            captions = mapOf(LanguageEnum.EN to "Devices", LanguageEnum.RU to "Приборы"),
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN),
            disabledAccessRoles = mutableSetOf(AppRole.USER),
        )
    }

    private fun setServicesCustomFuns() {
        UserService.addCustomTablePopups = { userConfig: ServerUserConfig,
                                             userId: Int,
                                             popupDatas: MutableList<TablePopup> ->

            addPopupData(LocalizedMMSMessages.OBJECTS, AppModuleMMS.ALL_OBJECT, userConfig, userId, popupDatas)
            addPopupData(LocalizedMMSMessages.OBJECTS, AppModuleMMS.MOBILE_OBJECT, userConfig, userId, popupDatas)
            addPopupData(LocalizedMMSMessages.OBJECTS, AppModuleMMS.STATIONARY_OBJECT, userConfig, userId, popupDatas)
            addPopupData(LocalizedMMSMessages.WORK_LOGS, AppModuleMMS.DAY_ALL_WORK, userConfig, userId, popupDatas)
            addPopupData(LocalizedMMSMessages.WORK_LOGS, AppModuleMMS.DAY_MOBILE_WORK, userConfig, userId, popupDatas)
            addPopupData(LocalizedMMSMessages.WORK_LOGS, AppModuleMMS.DAY_STATIONARY_WORK, userConfig, userId, popupDatas)
            addPopupData(LocalizedMMSMessages.WORK_LOGS, AppModuleMMS.WORK_SHIFT, userConfig, userId, popupDatas)
            addPopupData(LocalizedMMSMessages.REFERENCES, AppModuleMMS.DEPARTMENT, userConfig, userId, popupDatas)
            addPopupData(LocalizedMMSMessages.REFERENCES, AppModuleMMS.GROUP, userConfig, userId, popupDatas)
            addPopupData(LocalizedMMSMessages.DEVICES, AppModuleMMS.DEVICE, userConfig, userId, popupDatas)
            addPopupData(LocalizedMMSMessages.DEVICES, AppModuleMMS.DEVICE_MANAGE, userConfig, userId, popupDatas)
        }
        UserService.checkCustomDepencies = { userId: Int ->
            objectRepository.findByUserId(userId).isNotEmpty() ||
                    departmentRepository.findByUserId(userId).isNotEmpty() ||
                    groupRepository.findByUserId(userId).isNotEmpty() ||
                    deviceRepository.findByUserId(userId).isNotEmpty()
        }
        UserService.customActionDelete = { userId: Int ->
            dayWorkRepository.deleteByUserId(userId)
            workShiftRepository.deleteByUserId(userId)
            deviceManageRepository.deleteByUserId(userId)
        }
    }

    private fun addPopupData(
        group: LocalizedMMSMessages?,
        childModule: String,
        userConfig: ServerUserConfig,
        userId: Int,
        popupDatas: MutableList<TablePopup>
    ) {
        popupDatas += TablePopup(
            group = group?.let { getLocalizedMMSMessage(group, userConfig.lang) },
            action = AppAction(
                type = ActionType.MODULE_TABLE,
                module = childModule,
                parentModule = AppModule.USER,
                parentId = userId,
            ),
            text = appModuleConfigs[childModule]?.captions?.let { captions ->
                getLocalizedMessage(captions, userConfig.lang)
            } ?: "(неизвестный тип модуля: '$childModule')",
            inNewTab = true,
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
//                addMenuItem(
//                    alMenu = this,
//                    serverUserConfig = serverUserConfig,
//                    module = AppModuleMMS.OBJECT_CHART_LIST_DASHBOARD,
//                    actionType = ActionType.MODULE_COMPOSITE,
//                    iconUrl = "/images/icons8-combo-chart-24.png",
//                    iconSize = 24,
//                )

                if (size > 0) {
                    alMenu += MenuData(
                        caption = getLocalizedMMSMessage(LocalizedMMSMessages.CONTROL, serverUserConfig.lang),
                        action = null,
                        subMenuDatas = this
                    )
                }
            }

            mutableListOf<MenuData>().apply {
                if (
                    !serverUserConfig.roles.contains(AppRoleMMS.USER_MOBILE_OBJECTS) &&
                    !serverUserConfig.roles.contains(AppRoleMMS.USER_FIXED_OBJECTS)
                ) {
                    addMenuItem(
                        alMenu = this,
                        serverUserConfig = serverUserConfig,
                        module = AppModuleMMS.ALL_OBJECT,
                        actionType = ActionType.MODULE_TABLE,
                        iconUrl = "/images/icons8-compressor-24.png",
                        iconSize = 24,
                    )
                }
                addMenuItem(
                    alMenu = this,
                    serverUserConfig = serverUserConfig,
                    module = AppModuleMMS.MOBILE_OBJECT,
                    actionType = ActionType.MODULE_TABLE,
                    iconUrl = "/images/icons8-truck-24.png",
                    iconSize = 24,
                )
                addMenuItem(
                    alMenu = this,
                    serverUserConfig = serverUserConfig,
                    module = AppModuleMMS.STATIONARY_OBJECT,
                    actionType = ActionType.MODULE_TABLE,
                    iconUrl = "/images/icons8-oil-pump-24.png",
                    iconSize = 24,
                )

                if (size > 0) {
                    alMenu += MenuData(
                        caption = getLocalizedMMSMessage(LocalizedMMSMessages.OBJECTS, serverUserConfig.lang),
                        action = null,
                        subMenuDatas = this
                    )
                }
            }

            mutableListOf<MenuData>().apply {
                addMenuItem(
                    alMenu = this,
                    serverUserConfig = serverUserConfig,
                    module = AppModuleMMS.DAY_ALL_WORK,
                    actionType = ActionType.MODULE_TABLE,
                    iconUrl = "/images/icons8-logbook-24.png",
                    iconSize = 24,
                )
                addMenuItem(
                    alMenu = this,
                    serverUserConfig = serverUserConfig,
                    module = AppModuleMMS.DAY_MOBILE_WORK,
                    actionType = ActionType.MODULE_TABLE,
                    iconUrl = "/images/icons8-logbook-24.png",
                    iconSize = 24,
                )
                addMenuItem(
                    alMenu = this,
                    serverUserConfig = serverUserConfig,
                    module = AppModuleMMS.DAY_STATIONARY_WORK,
                    actionType = ActionType.MODULE_TABLE,
                    iconUrl = "/images/icons8-logbook-24.png",
                    iconSize = 24,
                )
                addMenuItem(
                    alMenu = this,
                    serverUserConfig = serverUserConfig,
                    module = AppModuleMMS.WORK_SHIFT,
                    actionType = ActionType.MODULE_TABLE,
                    iconUrl = "/images/icons8-logbook-24.png",
                    iconSize = 24,
                )

                if (size > 0) {
                    alMenu += MenuData(
                        caption = getLocalizedMMSMessage(LocalizedMMSMessages.WORK_LOGS, serverUserConfig.lang),
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
                addMenuItem(
                    alMenu = this,
                    serverUserConfig = serverUserConfig,
                    module = AppModuleMMS.REPORT_DAY_WORK,
                    actionType = ActionType.MODULE_FORM,
                    iconUrl = "/images/icons8-statistics-report-24.png",
                    iconSize = 24,
                )
                addMenuItem(
                    alMenu = this,
                    serverUserConfig = serverUserConfig,
                    module = AppModuleMMS.REPORT_WORK_SHIFT,
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

            mutableListOf<MenuData>().apply {
                val langSubMenus = mutableListOf<MenuData>()
                LanguageEnum.entries.forEach { lang ->
                    langSubMenus += MenuData(caption = lang.descr, action = AppAction(type = ActionType.SET_LANGUAGE, module = lang.name))
                }
                add(MenuData(caption = LanguageEnum.entries.joinToString(separator = " / ") { le -> le.descr }, subMenuDatas = langSubMenus))

                addMenuItem(
                    alMenu = this,
                    serverUserConfig = serverUserConfig,
                    module = AppModule.USER_PROPERTY_EDIT,
                    actionType = ActionType.MODULE_FORM,
                    id = serverUserConfig.id,
                    iconUrl = "/images/icons8-users-settings-24.png",
                    iconSize = 24,
                )
                add(
                    MenuData(
                        caption = getLocalizedMessage(LocalizedMessages.CHANGE_PASSWORD, serverUserConfig.lang),
                        action = AppAction(ActionType.CHANGE_PASSWORD),
                        iconUrl = "/images/icons8-password-24.png",
                        iconSize = 24,
                    )
                )
                add(
                    MenuData(
                        caption = getLocalizedMessage(LocalizedMessages.LOGOUT, serverUserConfig.lang),
                        action = AppAction(ActionType.LOGOFF),
                        iconUrl = "/images/icons8-logout-24.png",
                        iconSize = 24,
                    )
                )

                if (size > 0) {
                    alMenu += MenuData(
                        caption = getLocalizedMessage(LocalizedMessages.ADDITIONAL, serverUserConfig.lang),
                        subMenuDatas = this
                    )
                }
            }

            alMenu
        }
    }

}
