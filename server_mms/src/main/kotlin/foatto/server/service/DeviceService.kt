package foatto.server.service

import foatto.core.ActionType
import foatto.core.i18n.LanguageEnum
import foatto.core.i18n.LocalizedMessages
import foatto.core.i18n.getLocalizedMessage
import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.form.FormCellVisibility
import foatto.core.model.response.form.FormDateTimeCellMode
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.form.cells.FormBooleanCell
import foatto.core.model.response.form.cells.FormComboCell
import foatto.core.model.response.form.cells.FormDateTimeCell
import foatto.core.model.response.form.cells.FormSimpleCell
import foatto.core.model.response.table.TableCaption
import foatto.core.model.response.table.TablePageButton
import foatto.core.model.response.table.TablePopup
import foatto.core.model.response.table.TableRow
import foatto.core.model.response.table.cell.TableBaseCell
import foatto.core.model.response.table.cell.TableSimpleCell
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getDateTimeDMYHMSString
import foatto.core.util.getDateTimeYMDHMSInts
import foatto.core.util.getTimeZone
import foatto.core_mms.AppModuleMMS
import foatto.core_mms.i18n.LocalizedMMSMessages
import foatto.core_mms.i18n.getLocalizedMMSMessage
import foatto.server.appModuleConfigs
import foatto.server.checkAccessPermission
import foatto.server.checkFormAddPermission
import foatto.server.checkRowPermission
import foatto.server.ds.CoreTelematicFunction
import foatto.server.ds.MMSTelematicFunction
import foatto.server.ds.PortNumbers
import foatto.server.entity.DateEntity
import foatto.server.entity.DeviceEntity
import foatto.server.entity.ObjectEntity
import foatto.server.entity.SensorEntity
import foatto.server.getEnabledUserIds
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.model.sensor.SensorConfig
import foatto.server.model.sensor.SensorConfigCounter
import foatto.server.model.sensor.SensorConfigLiquidLevel
import foatto.server.repository.ActionLogRepository
import foatto.server.repository.DeviceManageRepository
import foatto.server.repository.DeviceRepository
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorCalibrationRepository
import foatto.server.repository.SensorRepository
import foatto.server.repository.UserRepository
import foatto.server.util.getNextId
import jakarta.persistence.EntityManager
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Service
class DeviceService(
    private val entityManager: EntityManager,
    private val deviceRepository: DeviceRepository,
    private val deviceManageRepository: DeviceManageRepository,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
    private val sensorCalibrationRepository: SensorCalibrationRepository,
    private val userRepository: UserRepository,
    private val actionLogRepository: ActionLogRepository,
    private val fileStoreService: FileStoreService,
) : MMSService(
    userRepository = userRepository,
    actionLogRepository = actionLogRepository,
    fileStoreService = fileStoreService,
) {

    companion object {
        //        private const val FIELD_ID = "id"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_TYPE = "type"
        private const val FIELD_INDEX = "index"
        private const val FIELD_SERIAL_NO = "serialNo"
        private const val FIELD_NAME = "name"

        private const val FIELD_CELL_IMEI = "cellImei"
        private const val FIELD_CELL_OWNER = "cellOwner"
        private const val FIELD_CELL_NUMBER = "cellNumber"
        private const val FIELD_CELL_ICC = "cellIcc"
        private const val FIELD_CELL_OPERATOR = "cellOperator"
        private const val FIELD_CELL_IMEI_2 = "cellImei2"
        private const val FIELD_CELL_OWNER_2 = "cellOwner2"
        private const val FIELD_CELL_NUMBER_2 = "cellNumber2"
        private const val FIELD_CELL_ICC_2 = "cellIcc2"
        private const val FIELD_CELL_OPERATOR_2 = "cellOperator2"
        private const val FIELD_FW_VERSION = "fwVersion"
        private const val FIELD_LAST_SESSION_TIME = "lastSessionTime"
        private const val FIELD_LAST_SESSION_STATUS = "lastSessionStatus"
        private const val FIELD_LAST_SESSION_ERROR = "lastSessionError"
        private const val FIELD_USING_START_DATE = "usingStartDate"

        private const val FIELD_OBJECT_ID = "obj.id"
        private const val FIELD_OBJECT_NAME = "obj.name"    // отдельное определение для сортировки в Hibernate
        private const val FIELD_OBJECT_MODEL = "obj.model"  // отдельное определение для сортировки в Hibernate

        private const val FIELD_OWNER_FULL_NAME = "_ownerFullName"   // псевдополе для селектора

        private const val FIELD_COPY_SENSORS = "_copySensors"

        private const val SENSOR_CREATE_ENABLED = "_create_enabled"
        private const val SENSOR_GROUP_NAME = "_group_name_"
        private const val SENSOR_DESCR_PREFIX = "_descr_prefix_"
        private const val SENSOR_DESCR_POSTFIX = "_descr_postfix_"

        //--- 65 устройств по 1000 портов = 65000 портов, что уместится при нумерации от 0 до 65535
        private const val MAX_DEVICE_COUNT_PER_OBJECT = 65

        private const val CELL_OWNER_UNKNOWN = 0
        private const val CELL_OWNER_PLA = 1
        private const val CELL_OWNER_CLIENT = 2

        private val deviceTypes = mapOf(
            MMSTelematicFunction.DEVICE_TYPE_GALILEO to "Galileo",
            MMSTelematicFunction.DEVICE_TYPE_PULSAR_DATA to "Pulsar Data",
        )

        private val cellOwnerNames = mapOf(
            CELL_OWNER_UNKNOWN to mapOf(
                LanguageEnum.EN to "(unknown)",
                LanguageEnum.RU to "(неизвестно)",
                LanguageEnum.KZ to "(белгісіз)",
            ),
            CELL_OWNER_PLA to mapOf(
                LanguageEnum.EN to "Petroline",
                LanguageEnum.RU to "Петролайн",
                LanguageEnum.KZ to "Петролайн",
            ),
            CELL_OWNER_CLIENT to mapOf(
                LanguageEnum.EN to "Client",
                LanguageEnum.RU to "Клиент",
                LanguageEnum.KZ to "Клиент",
            ),
        )

        private const val MAX_PORT_PER_SENSOR = 4

        private const val SENSOR_NAME_EMIS = "emis"
        private const val SENSOR_NAME_ESD = "esd"
        private const val SENSOR_NAME_PMP = "pmp"
        private const val SENSOR_NAME_MERCURY = "mercury"

        private val sensorAutoCreates = mapOf(
            SENSOR_NAME_EMIS to mapOf(
                LanguageEnum.EN to "EMIS",
                LanguageEnum.RU to "ЭМИС",
                LanguageEnum.KZ to "ЭМИС",
            ),
            SENSOR_NAME_ESD to mapOf(
                LanguageEnum.EN to "Euro Sens",
                LanguageEnum.RU to "Euro Sens",
                LanguageEnum.KZ to "Euro Sens",
            ),
            SENSOR_NAME_PMP to mapOf(
                LanguageEnum.EN to "PMP",
                LanguageEnum.RU to "ПМП",
                LanguageEnum.KZ to "ПМП",
            ),
            SENSOR_NAME_MERCURY to mapOf(
                LanguageEnum.EN to "Mercury",
                LanguageEnum.RU to "Меркурий",
                LanguageEnum.KZ to "Меркурий",
            ),
        )
        private val sensorInfos = mapOf(
            SENSOR_NAME_EMIS to listOf(
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_MASS_FLOW,
                    portNum = PortNumbers.EMIS_MASS_FLOW_270,
                    descr = mapOf(
                        LanguageEnum.EN to "Mass flow rate",
                        LanguageEnum.RU to "Массовый расход",
                        LanguageEnum.KZ to "Масса ағынының жылдамдығы",
                    ),
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_DENSITY,
                    portNum = PortNumbers.EMIS_DENSITY_280,
                    descr = mapOf(
                        LanguageEnum.EN to "Density",
                        LanguageEnum.RU to "Плотность",
                        LanguageEnum.KZ to "Тығыздық",
                    ),
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_TEMPERATURE,
                    portNum = PortNumbers.EMIS_TEMPERATURE_290,
                    descr = mapOf(
                        LanguageEnum.EN to "Temperature",
                        LanguageEnum.RU to "Температура",
                        LanguageEnum.KZ to "Температура",
                    ),
                    minView = -100.0,
                    maxView = 100.0
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_VOLUME_FLOW,
                    portNum = PortNumbers.EMIS_VOLUME_FLOW_300,
                    descr = mapOf(
                        LanguageEnum.EN to "Volumetric flow rate",
                        LanguageEnum.RU to "Объёмный расход",
                        LanguageEnum.KZ to "Көлемдік ағын жылдамдығы",
                    ),
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_MASS_ACCUMULATED,
                    portNum = PortNumbers.EMIS_ACCUMULATED_MASS_310,
                    descr = mapOf(
                        LanguageEnum.EN to "Accumulated mass",
                        LanguageEnum.RU to "Накопленная масса",
                        LanguageEnum.KZ to "Жиналған масса",
                    ),
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_VOLUME_ACCUMULATED,
                    portNum = PortNumbers.EMIS_ACCUMULATED_VOLUME_320,
                    descr = mapOf(
                        LanguageEnum.EN to "Accumulated volume",
                        LanguageEnum.RU to "Накопленный объём",
                        LanguageEnum.KZ to "Жинақталған көлем",
                    ),
                ),
            ),
            SENSOR_NAME_ESD to listOf(
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_LIQUID_USING,
                    portNum = PortNumbers.ESD_VOLUME_504,
                    descr = mapOf(
                        LanguageEnum.EN to "Flow meter",
                        LanguageEnum.RU to "Расходомер",
                        LanguageEnum.KZ to "Ағын өлшегіш",
                    ),
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_VOLUME_FLOW,
                    portNum = PortNumbers.ESD_FLOW_508,
                    descr = mapOf(
                        LanguageEnum.EN to "Flow rate",
                        LanguageEnum.RU to "Скорость потока",
                        LanguageEnum.KZ to "Ағын жылдамдығы",
                    ),
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_LIQUID_USING,
                    portNum = PortNumbers.ESD_CAMERA_VOLUME_512,
                    descr = mapOf(
                        LanguageEnum.EN to "Feed chamber flow meter",
                        LanguageEnum.RU to "Расходомер камеры подачи",
                        LanguageEnum.KZ to "Беру камерасының ағын өлшегіші",
                    ),
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_VOLUME_FLOW,
                    portNum = PortNumbers.ESD_CAMERA_FLOW_516,
                    descr = mapOf(
                        LanguageEnum.EN to "Feed chamber flow rate",
                        LanguageEnum.RU to "Скорость потока камеры подачи",
                        LanguageEnum.KZ to "Беру камерасының ағын жылдамдығы",
                    ),
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_TEMPERATURE,
                    portNum = PortNumbers.ESD_CAMERA_TEMPERATURE_520,
                    descr = mapOf(
                        LanguageEnum.EN to "Feed chamber temperature",
                        LanguageEnum.RU to "Температура камеры подачи",
                        LanguageEnum.KZ to "Азықтандыру камерасының температурасы",
                    ),
                    minView = -100.0,
                    maxView = 100.0
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_LIQUID_USING,
                    portNum = PortNumbers.ESD_REVERSE_CAMERA_VOLUME_524,
                    descr = mapOf(
                        LanguageEnum.EN to "Return chamber flow meter",
                        LanguageEnum.RU to "Расходомер камеры обратки",
                        LanguageEnum.KZ to "Қайтару камерасының ағын өлшегіші",
                    ),
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_VOLUME_FLOW,
                    portNum = PortNumbers.ESD_REVERSE_CAMERA_FLOW_528,
                    descr = mapOf(
                        LanguageEnum.EN to "Return chamber flow rate",
                        LanguageEnum.RU to "Скорость потока камеры обратки",
                        LanguageEnum.KZ to "Қайтару камерасының ағын жылдамдығы",
                    ),
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_TEMPERATURE,
                    portNum = PortNumbers.ESD_REVERSE_CAMERA_TEMPERATURE_532,
                    descr = mapOf(
                        LanguageEnum.EN to "Return chamber temperature",
                        LanguageEnum.RU to "Температура камеры обратки",
                        LanguageEnum.KZ to "Қайтару камерасының температурасы",
                    ),
                    minView = -100.0,
                    maxView = 100.0
                ),
            ),
            SENSOR_NAME_PMP to listOf(
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_LIQUID_LEVEL,
                    portNum = PortNumbers.PMP_LEVEL_540,
                    descr = mapOf(
                        LanguageEnum.EN to "Fuel level",
                        LanguageEnum.RU to "Уровень топлива",
                        LanguageEnum.KZ to "Отын деңгейі",
                    ),
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_TEMPERATURE,
                    portNum = PortNumbers.PMP_TEMPERATURE_560,
                    descr = mapOf(
                        LanguageEnum.EN to "Temperature",
                        LanguageEnum.RU to "Температура",
                        LanguageEnum.KZ to "Температура",
                    ),
                    minView = -100.0,
                    maxView = 100.0
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_LIQUID_LEVEL,
                    portNum = PortNumbers.PMP_VOLUME_580,
                    descr = mapOf(
                        LanguageEnum.EN to "Fuel volume",
                        LanguageEnum.RU to "Объём топлива",
                        LanguageEnum.KZ to "Отын көлемі",
                    ),
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_WEIGHT,
                    portNum = PortNumbers.PMP_MASS_600,
                    descr = mapOf(
                        LanguageEnum.EN to "Fuel mass",
                        LanguageEnum.RU to "Масса топлива",
                        LanguageEnum.KZ to "Отын массасы",
                    ),
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_DENSITY,
                    portNum = PortNumbers.PMP_DENSITY_620,
                    descr = mapOf(
                        LanguageEnum.EN to "Density",
                        LanguageEnum.RU to "Плотность",
                        LanguageEnum.KZ to "Тығыздық",
                    ),
                ),
            ),
            SENSOR_NAME_MERCURY to listOf(
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_COUNT_AD,
                    portNum = PortNumbers.MERCURY_COUNT_ACTIVE_DIRECT_160,
                    descr = mapOf(
                        LanguageEnum.EN to "Direct active electrical energy",
                        LanguageEnum.RU to "Электроэнергия активная прямая",
                        LanguageEnum.KZ to "Тікелей белсенді электр энергиясы",
                    ),
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_COUNT_AR,
                    portNum = PortNumbers.MERCURY_COUNT_ACTIVE_REVERSE_164,
                    descr = mapOf(
                        LanguageEnum.EN to "Active reverse electrical energy",
                        LanguageEnum.RU to "Электроэнергия активная обратная",
                        LanguageEnum.KZ to "Белсенді кері электр энергиясы",
                    ),
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_COUNT_RD,
                    portNum = PortNumbers.MERCURY_COUNT_REACTIVE_DIRECT_168,
                    descr = mapOf(
                        LanguageEnum.EN to "Direct reactive electrical energy",
                        LanguageEnum.RU to "Электроэнергия реактивная прямая",
                        LanguageEnum.KZ to "Тікелей реактивті электр энергиясы",
                    ),
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_COUNT_RR,
                    portNum = PortNumbers.MERCURY_COUNT_REACTIVE_REVERSE_172,
                    descr = mapOf(
                        LanguageEnum.EN to "Reactive reverse electrical energy",
                        LanguageEnum.RU to "Электроэнергия реактивная обратная",
                        LanguageEnum.KZ to "Реактивті кері электр энергиясы",
                    ),
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_VOLTAGE,
                    portNum = PortNumbers.MERCURY_VOLTAGE_A_180,
                    descr = mapOf(
                        LanguageEnum.EN to "Voltage in phase A",
                        LanguageEnum.RU to "Напряжение по фазе A",
                        LanguageEnum.KZ to "A фазасындағы кернеу",
                    ),
                    phase = 1,
                    minView = 0.0,
                    maxView = 380.0
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_VOLTAGE,
                    portNum = PortNumbers.MERCURY_VOLTAGE_B_184,
                    descr = mapOf(
                        LanguageEnum.EN to "Voltage in phase B",
                        LanguageEnum.RU to "Напряжение по фазе B",
                        LanguageEnum.KZ to "B фазасындағы кернеу",
                    ),
                    phase = 2,
                    minView = 0.0,
                    maxView = 380.0
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_VOLTAGE,
                    portNum = PortNumbers.MERCURY_VOLTAGE_C_188,
                    descr = mapOf(
                        LanguageEnum.EN to "Voltage in phase C",
                        LanguageEnum.RU to "Напряжение по фазе C",
                        LanguageEnum.KZ to "C фазасындағы кернеу",
                    ),
                    phase = 3,
                    minView = 0.0,
                    maxView = 380.0
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_CURRENT,
                    portNum = PortNumbers.MERCURY_CURRENT_A_200,
                    descr = mapOf(
                        LanguageEnum.EN to "Current in phase A",
                        LanguageEnum.RU to "Ток по фазе A",
                        LanguageEnum.KZ to "A фазасындағы ток",
                    ),
                    phase = 1
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_CURRENT,
                    portNum = PortNumbers.MERCURY_CURRENT_B_204,
                    descr = mapOf(
                        LanguageEnum.EN to "Current in phase B",
                        LanguageEnum.RU to "Ток по фазе B",
                        LanguageEnum.KZ to "B фазасындағы ток",
                    ),
                    phase = 2
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_CURRENT,
                    portNum = PortNumbers.MERCURY_CURRENT_C_208,
                    descr = mapOf(
                        LanguageEnum.EN to "Current in phase C",
                        LanguageEnum.RU to "Ток по фазе C",
                        LanguageEnum.KZ to "C фазасындағы ток",
                    ),
                    phase = 3
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_KOEF,
                    portNum = PortNumbers.MERCURY_POWER_KOEF_A_220,
                    descr = mapOf(
                        LanguageEnum.EN to "Power factor in phase A",
                        LanguageEnum.RU to "Коэффициент мощности по фазе A",
                        LanguageEnum.KZ to "A фазасындағы қуат коэффициенті",
                    ),
                    phase = 1,
                    minView = 0.0,
                    maxView = 1.0
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_KOEF,
                    portNum = PortNumbers.MERCURY_POWER_KOEF_B_224,
                    descr = mapOf(
                        LanguageEnum.EN to "Power factor in phase B",
                        LanguageEnum.RU to "Коэффициент мощности по фазе B",
                        LanguageEnum.KZ to "B фазасындағы қуат коэффициенті",
                    ),
                    phase = 2,
                    minView = 0.0,
                    maxView = 1.0
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_KOEF,
                    portNum = PortNumbers.MERCURY_POWER_KOEF_C_228,
                    descr = mapOf(
                        LanguageEnum.EN to "Power factor in phase C",
                        LanguageEnum.RU to "Коэффициент мощности по фазе C",
                        LanguageEnum.KZ to "C фазасындағы қуат коэффициенті",
                    ),
                    phase = 3,
                    minView = 0.0,
                    maxView = 1.0
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
                    portNum = PortNumbers.MERCURY_POWER_ACTIVE_A_232,
                    descr = mapOf(
                        LanguageEnum.EN to "Active power in phase A",
                        LanguageEnum.RU to "Мощность активная по фазе A",
                        LanguageEnum.KZ to "A фазасындағы белсенді қуат",
                    ),
                    phase = 1
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
                    portNum = PortNumbers.MERCURY_POWER_ACTIVE_B_236,
                    descr = mapOf(
                        LanguageEnum.EN to "Active power in phase B",
                        LanguageEnum.RU to "Мощность активная по фазе B",
                        LanguageEnum.KZ to "B фазасындағы белсенді қуат",
                    ),
                    phase = 2
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
                    portNum = PortNumbers.MERCURY_POWER_ACTIVE_C_240,
                    descr = mapOf(
                        LanguageEnum.EN to "Active power in phase C",
                        LanguageEnum.RU to "Мощность активная по фазе C",
                        LanguageEnum.KZ to "C фазасындағы белсенді қуат",
                    ),
                    phase = 3
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_REACTIVE,
                    portNum = PortNumbers.MERCURY_POWER_REACTIVE_A_244,
                    descr = mapOf(
                        LanguageEnum.EN to "Reactive power in phase A",
                        LanguageEnum.RU to "Мощность реактивная по фазе A",
                        LanguageEnum.KZ to "A фазасындағы реактивті қуат",
                    ),
                    phase = 1
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_REACTIVE,
                    portNum = PortNumbers.MERCURY_POWER_REACTIVE_B_248,
                    descr = mapOf(
                        LanguageEnum.EN to "Reactive power in phase B",
                        LanguageEnum.RU to "Мощность реактивная по фазе B",
                        LanguageEnum.KZ to "B фазасындағы реактивті қуат",
                    ),
                    phase = 2
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_REACTIVE,
                    portNum = PortNumbers.MERCURY_POWER_REACTIVE_C_252,
                    descr = mapOf(
                        LanguageEnum.EN to "Reactive power in phase C",
                        LanguageEnum.RU to "Мощность реактивная по фазе C",
                        LanguageEnum.KZ to "C фазасындағы реактивті қуат",
                    ),
                    phase = 3
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_FULL,
                    portNum = PortNumbers.MERCURY_POWER_FULL_A_256,
                    descr = mapOf(
                        LanguageEnum.EN to "Full power in phase A",
                        LanguageEnum.RU to "Мощность полная по фазе A",
                        LanguageEnum.KZ to "A фазасындағы толық қуат",
                    ),
                    phase = 1
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_FULL,
                    portNum = PortNumbers.MERCURY_POWER_FULL_B_260,
                    descr = mapOf(
                        LanguageEnum.EN to "Full power in phase B",
                        LanguageEnum.RU to "Мощность полная по фазе B",
                        LanguageEnum.KZ to "B фазасындағы толық қуат",
                    ),
                    phase = 2
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_FULL,
                    portNum = PortNumbers.MERCURY_POWER_FULL_C_264,
                    descr = mapOf(
                        LanguageEnum.EN to "Full power in phase C",
                        LanguageEnum.RU to "Мощность полная по фазе C",
                        LanguageEnum.KZ to "C фазасындағы толық қуат",
                    ),
                    phase = 3
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
                    portNum = PortNumbers.MERCURY_POWER_ACTIVE_ABC_330,
                    descr = mapOf(
                        LanguageEnum.EN to "Active power in all phases",
                        LanguageEnum.RU to "Мощность активная по всем фазам",
                        LanguageEnum.KZ to "Барлық фазалардағы белсенді қуат",
                    ),
                    phase = 0
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_REACTIVE,
                    portNum = PortNumbers.MERCURY_POWER_REACTIVE_ABC_340,
                    descr = mapOf(
                        LanguageEnum.EN to "Reactive power in all phases",
                        LanguageEnum.RU to "Мощность реактивная по всем фазам",
                        LanguageEnum.KZ to "Барлық фазалардағы реактивті қуат",
                    ),
                    phase = 0
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_FULL,
                    portNum = PortNumbers.MERCURY_POWER_FULL_ABC_350,
                    descr = mapOf(
                        LanguageEnum.EN to "Full power in all phases",
                        LanguageEnum.RU to "Мощность полная по всем фазам",
                        LanguageEnum.KZ to "Барлық фазаларда толық қуат",
                    ),
                    phase = 0
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_TRANSFORM_KOEF_CURRENT,
                    portNum = PortNumbers.MERCURY_TRANSFORM_KOEF_CURRENT_360,
                    descr = mapOf(
                        LanguageEnum.EN to "Current transformation ratio",
                        LanguageEnum.RU to "Коэффициент трансформации по току",
                        LanguageEnum.KZ to "Ағымдағы трансформация коэффициенті",
                    ),
                ),
                SensorInfo(
                    sensorType = SensorConfig.SENSOR_ENERGO_TRANSFORM_KOEF_VOLTAGE,
                    portNum = PortNumbers.MERCURY_TRANSFORM_KOEF_VOLTAGE_370,
                    descr = mapOf(
                        LanguageEnum.EN to "Voltage transformation ratio",
                        LanguageEnum.RU to "Коэффициент трансформации по напряжению",
                        LanguageEnum.KZ to "Кернеу түрлендіру коэффициенті",
                    ),
                ),
            ),
        )
    }

    //--- на самом деле пока никому не нужно. Просто сделал, чтобы не потерять практики.
    //override fun isDateTimeIntervalPanelVisible(): Boolean = true

    override fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<TableCaption> {
        val alColumnInfo = mutableListOf<Pair<String?, String>>()

        alColumnInfo += null to "" // Device.userId
        alColumnInfo += FIELD_TYPE to getLocalizedMMSMessage(LocalizedMMSMessages.TYPE, userConfig.lang)
        alColumnInfo += FIELD_INDEX to getLocalizedMMSMessage(LocalizedMMSMessages.INDEX_ON_OBJECT, userConfig.lang)
        alColumnInfo += FIELD_SERIAL_NO to getLocalizedMMSMessage(LocalizedMMSMessages.SERIAL_NUMBER, userConfig.lang)
        alColumnInfo += FIELD_NAME to getLocalizedMMSMessage(LocalizedMMSMessages.CONTROLLER_NAME, userConfig.lang)
        alColumnInfo += null to "" // Object.userId
        alColumnInfo += FIELD_OBJECT_NAME to getLocalizedMMSMessage(LocalizedMMSMessages.OBJECT_NAME, userConfig.lang)
        alColumnInfo += FIELD_OBJECT_MODEL to getLocalizedMMSMessage(LocalizedMMSMessages.MODEL, userConfig.lang)

        //--- две строки в каждой ячейке - по обеим симкам
        alColumnInfo += null to "IMEI"
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.SIM_CARD_OWNER, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.PHONE_NUMBER, userConfig.lang)
        alColumnInfo += null to "ICC"
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.MOBILE_OPERATOR, userConfig.lang)

        alColumnInfo += FIELD_FW_VERSION to getLocalizedMMSMessage(LocalizedMMSMessages.FIRMWARE_VERSION, userConfig.lang)
        alColumnInfo += FIELD_LAST_SESSION_TIME to getLocalizedMMSMessage(LocalizedMMSMessages.LAST_SESSION_TIME, userConfig.lang)
        alColumnInfo += FIELD_LAST_SESSION_STATUS to getLocalizedMMSMessage(LocalizedMMSMessages.LAST_SESSION_STATUS, userConfig.lang)
        alColumnInfo += FIELD_LAST_SESSION_ERROR to getLocalizedMMSMessage(LocalizedMMSMessages.LAST_SESSION_ERROR, userConfig.lang)
        alColumnInfo += FIELD_USING_START_DATE to getLocalizedMMSMessage(LocalizedMMSMessages.USING_START, userConfig.lang)

        return getTableColumnCaptionActions(
            action = action,
            columnInfos = alColumnInfo,
        )
    }

    override fun fillTableGridData(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        tableCells: MutableList<TableBaseCell>,
        tableRows: MutableList<TableRow>,
        pageButtons: MutableList<TablePageButton>,
    ): Int? {

        val zoneLocal = getTimeZone(userConfig.timeOffset)

        var currentRowNo: Int? = null
        var row = 0
        var dataRow = 0

        val pageRequest = getTableSortedPageRequest(action, Sort.Order(Sort.Direction.ASC, FIELD_SERIAL_NO))
        val findText = action.findText?.trim() ?: ""

        val parentUserIds = getParentUserIds(action)

        val parentObjectId = getParentObjectId(action)
        val parentObjectEntity = parentObjectId?.let {
            objectRepository.findByIdOrNull(parentObjectId)
        }

        val enabledUserIds = getEnabledUserIds(
            module = action.module,
            actionType = action.type,
            relatedUserIds = userConfig.relatedUserIds,
            roles = userConfig.roles,
        )

        val page: Page<DeviceEntity> = deviceRepository.findByParentUserIdAndObjAndUserIdInAndFilter(
            parentUserIds = parentUserIds,
            obj = parentObjectEntity,
            userIds = enabledUserIds,
            findText = findText,
            timeOffset = userConfig.timeOffset,
            begDateTime = action.begDateTimeValue ?: -1,
            endDateTime = action.endDateTimeValue ?: -1,
            pageRequest = pageRequest,
        )

        fillTablePageButtons(userConfig, action, page.totalPages, pageButtons)
        val deviceEntities = page.content

        for (deviceEntity in deviceEntities) {
            var col = 0

            val rowOwnerShortName = userConfig.shortNames[deviceEntity.userId]
            val rowOwnerFullName = userConfig.fullNames[deviceEntity.userId]

            val isFormEnabled = checkRowPermission(
                module = action.module,
                actionType = ActionType.MODULE_FORM,
                rowUserRelation = userConfig.relatedUserIds[deviceEntity.userId],
                userRoles = userConfig.roles
            )

            tableCells += getTableUserNameCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                userConfig = userConfig,
                rowUserId = deviceEntity.userId,
                rowOwnerShortName = rowOwnerShortName,
                rowOwnerFullName = rowOwnerFullName
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = deviceEntity.type?.let { type ->
                    deviceTypes[type] ?: getLocalizedMMSMessage(LocalizedMMSMessages.UNKNOWN_SENSOR_TYPE, userConfig.lang)
                } ?: "-",
            )
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = dataRow, name = deviceEntity.index?.toString() ?: "-")
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = dataRow, name = deviceEntity.serialNo ?: "-")
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = dataRow, name = deviceEntity.name ?: "-")
            tableCells += getTableUserNameCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                userConfig = userConfig,
                rowUserId = deviceEntity.obj?.userId,
                rowOwnerShortName = rowOwnerShortName,
                rowOwnerFullName = rowOwnerFullName
            )
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = dataRow, name = deviceEntity.obj?.name ?: "-")
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = dataRow, name = deviceEntity.obj?.model ?: "-")

            tableCells += TableSimpleCell(row = row, col = col++, dataRow = dataRow, name = "${deviceEntity.cellImei ?: "-"}\n${deviceEntity.cellImei2 ?: "-"}")
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = "${cellOwnerNames[deviceEntity.cellOwner]?.get(userConfig.lang) ?: "-"}\n${cellOwnerNames[deviceEntity.cellOwner2]?.get(userConfig.lang) ?: "-"}"
            )
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = dataRow, name = "${deviceEntity.cellNumber ?: "-"}\n${deviceEntity.cellNumber2 ?: "-"}")
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = dataRow, name = "${deviceEntity.cellIcc ?: "-"}\n${deviceEntity.cellIcc2 ?: "-"}")
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = dataRow, name = "${deviceEntity.cellOperator ?: "-"}\n${deviceEntity.cellOperator2 ?: "-"}")

            tableCells += TableSimpleCell(row = row, col = col++, dataRow = dataRow, name = deviceEntity.fwVersion ?: "-")
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = deviceEntity.lastSessionTime?.let { lastSessionTime -> getDateTimeDMYHMSString(zoneLocal, lastSessionTime) } ?: "-",
            )
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = dataRow, name = deviceEntity.lastSessionStatus ?: "-")
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = dataRow, name = deviceEntity.lastSessionError ?: "-")
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = dataRow, name = getDateEntityDMYString(deviceEntity.usingStartDate))

            val formOpenAction = action.copy(
                type = ActionType.MODULE_FORM,
                id = deviceEntity.id,
            )

            val popupDatas = mutableListOf<TablePopup>()

            if (isFormEnabled) {
                popupDatas += TablePopup(
                    action = formOpenAction,
                    text = getLocalizedMessage(LocalizedMessages.OPEN, userConfig.lang),
                    inNewTab = false,
                )
            }
            if (checkAccessPermission(AppModuleMMS.DEVICE_MANAGE, userConfig.roles)) {
                popupDatas += TablePopup(
                    action = AppAction(
                        type = ActionType.MODULE_TABLE,
                        module = AppModuleMMS.DEVICE_MANAGE,
                        parentModule = action.module,
                        parentId = deviceEntity.id,
                    ),
                    text = appModuleConfigs[AppModuleMMS.DEVICE_MANAGE]?.captions?.let { captions ->
                        getLocalizedMessage(captions, userConfig.lang)
                    } ?: "(${getLocalizedMessage(LocalizedMessages.UNKNOWN_MODULE_TYPE, userConfig.lang)}: '${AppModuleMMS.DEVICE_MANAGE}')",
                    inNewTab = true,
                )
            }

            tableRows += TableRow(
                rowAction = if (isFormEnabled) {
                    formOpenAction
                } else {
                    null
                },
                isRowUrlInNewTab = false,
                tablePopups = popupDatas,
            )

            if (deviceEntity.id == action.id) {
                currentRowNo = row
            }

            //if (userConfig.isWideScreen) {
            row++
            //}
            dataRow++
        }
        return currentRowNo
    }

    override fun getFormCells(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        addEnabled: Boolean,
        editEnabled: Boolean,
    ): List<FormBaseCell> {
        val formCells = mutableListOf<FormBaseCell>()

        val id = action.id

        val changeEnabled = id?.let { editEnabled } ?: addEnabled

        val deviceEntity = id?.let {
            //--- TODO: ошибка об отсутствии такой записи
            deviceRepository.findByIdOrNull(id) ?: return emptyList()
        }

        val userId = deviceEntity?.let { deviceEntity.userId } ?: getParentUserIds(action)?.singleOrNull() ?: userConfig.id

        val parentObjectId = deviceEntity?.let {
            deviceEntity.obj?.id
        } ?: run {
            getParentObjectId(action) ?: 0
        }

        fillFormUserCells(
            fieldUserId = FIELD_USER_ID,
            fieldOwnerFullName = FIELD_OWNER_FULL_NAME,
            userId = userId,
            userConfig = userConfig,
            changeEnabled = changeEnabled,
            formCells = formCells,
        )
        formCells += FormComboCell(
            name = FIELD_TYPE,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.TYPE, userConfig.lang),
            isEditable = changeEnabled,
            value = (deviceEntity?.type ?: MMSTelematicFunction.DEVICE_TYPE_PULSAR_DATA).toString(),
            values = deviceTypes.map { (key, value) -> key.toString() to value },
            asRadioButtons = true,
        )
        formCells += FormSimpleCell(
            name = FIELD_INDEX,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.INDEX_ON_OBJECT, userConfig.lang),
            isEditable = changeEnabled,
            value = deviceEntity?.index?.toString() ?: "0",
        )
        formCells += FormSimpleCell(
            name = FIELD_SERIAL_NO,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.SERIAL_NUMBER, userConfig.lang),
            isEditable = changeEnabled,
            value = deviceEntity?.serialNo ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_NAME,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.CONTROLLER_NAME, userConfig.lang),
            isEditable = changeEnabled,
            value = deviceEntity?.name ?: "",
        )

        formCells += FormSimpleCell(
            name = FIELD_OBJECT_ID,
            caption = "",
            isEditable = false,
            value = parentObjectId.toString(),
        )
        formCells += FormSimpleCell(
            name = FIELD_OBJECT_NAME,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.OBJECT_NAME, userConfig.lang),
            isEditable = false,
            value = if (parentObjectId == 0) {
                "-"
            } else {
                deviceEntity?.obj?.name ?: getLocalizedMessage(LocalizedMessages.UNKNOWN, userConfig.lang)
            },
            selectorAction = if (changeEnabled) {
                AppAction(
                    type = ActionType.MODULE_TABLE,
                    module = AppModuleMMS.ALL_OBJECT,
                    isSelectorMode = true,
                    selectorPath = mapOf(
                        AbstractObjectService.FIELD_ID to FIELD_OBJECT_ID,
                        AbstractObjectService.FIELD_NAME to FIELD_OBJECT_NAME,
                        AbstractObjectService.FIELD_MODEL to FIELD_OBJECT_MODEL,
                    ),
                    selectorClear = mapOf(
                        FIELD_OBJECT_ID to "0",
                        FIELD_OBJECT_NAME to "",
                        FIELD_OBJECT_MODEL to "",
                    ),
                )
            } else {
                null
            },
        )
        formCells += FormSimpleCell(
            name = FIELD_OBJECT_MODEL,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.MODEL, userConfig.lang),
            isEditable = false,
            value = deviceEntity?.obj?.model ?: "",
        )

        formCells += FormSimpleCell(
            name = FIELD_CELL_IMEI,
            caption = "IMEI",
            isEditable = changeEnabled,
            value = deviceEntity?.cellImei ?: "",
        )
        formCells += FormComboCell(
            name = FIELD_CELL_OWNER,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.SIM_CARD_OWNER, userConfig.lang),
            isEditable = changeEnabled,
            value = (deviceEntity?.cellOwner ?: CELL_OWNER_UNKNOWN).toString(),
            values = cellOwnerNames.map { (key, values) -> key.toString() to (values[userConfig.lang] ?: "-") },
            asRadioButtons = true,
        )
        formCells += FormSimpleCell(
            name = FIELD_CELL_NUMBER,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.PHONE_NUMBER, userConfig.lang),
            isEditable = changeEnabled,
            value = deviceEntity?.cellNumber ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_CELL_ICC,
            caption = "ICC",
            isEditable = changeEnabled,
            value = deviceEntity?.cellIcc ?: "",
        )
        //!!! String-combo с выбором из списка ранее введённых операторов
        /*
        val cellOperators = mutableSetOf("")
        val rs = conn.executeQuery(
            " SELECT cell_operator , cell_operator_2 FROM $modelTableName"
        )
        while (rs.next()) {
            cellOperators += rs.getString(1).trim()
            cellOperators += rs.getString(2).trim()
        }
        rs.close()
        val sortedCellOperators = cellOperators.toSortedSet()
         */
        formCells += FormSimpleCell(
            name = FIELD_CELL_OPERATOR,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.MOBILE_OPERATOR, userConfig.lang),
            isEditable = changeEnabled,
            value = deviceEntity?.cellOperator ?: "",
        )

        formCells += FormSimpleCell(
            name = FIELD_CELL_IMEI_2,
            caption = "IMEI 2",
            isEditable = changeEnabled,
            value = deviceEntity?.cellImei2 ?: "",
        )
        formCells += FormComboCell(
            name = FIELD_CELL_OWNER_2,
            caption = "${getLocalizedMMSMessage(LocalizedMMSMessages.SIM_CARD_OWNER, userConfig.lang)} 2",
            isEditable = changeEnabled,
            value = (deviceEntity?.cellOwner2 ?: CELL_OWNER_UNKNOWN).toString(),
            values = cellOwnerNames.map { (key, values) -> key.toString() to (values[userConfig.lang] ?: "-") },
            asRadioButtons = true,
        )
        formCells += FormSimpleCell(
            name = FIELD_CELL_NUMBER_2,
            caption = "${getLocalizedMMSMessage(LocalizedMMSMessages.PHONE_NUMBER, userConfig.lang)} 2",
            isEditable = changeEnabled,
            value = deviceEntity?.cellNumber2 ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_CELL_ICC_2,
            caption = "ICC 2",
            isEditable = changeEnabled,
            value = deviceEntity?.cellIcc2 ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_CELL_OPERATOR_2,
            caption = "${getLocalizedMMSMessage(LocalizedMMSMessages.MOBILE_OPERATOR, userConfig.lang)} 2",
            isEditable = changeEnabled,
            value = deviceEntity?.cellOperator2 ?: "",
        )

        //--- только для показа, значения не изменяются, перед сохранением берутся свежие из базы
        formCells += FormSimpleCell(
            name = FIELD_FW_VERSION,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.FIRMWARE_VERSION, userConfig.lang),
            isEditable = false,
            value = deviceEntity?.fwVersion ?: "",
        )
        formCells += FormDateTimeCell(
            name = FIELD_LAST_SESSION_TIME,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.LAST_SESSION_TIME, userConfig.lang),
            isEditable = false,
            mode = FormDateTimeCellMode.DMYHMS,
            value = deviceEntity?.lastSessionTime ?: 0,
        )
        formCells += FormSimpleCell(
            name = FIELD_LAST_SESSION_STATUS,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.LAST_SESSION_STATUS, userConfig.lang),
            isEditable = false,
            value = deviceEntity?.lastSessionStatus ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_LAST_SESSION_ERROR,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.LAST_SESSION_ERROR, userConfig.lang),
            isEditable = false,
            value = deviceEntity?.lastSessionError ?: "",
        )
        formCells += FormDateTimeCell(
            name = FIELD_USING_START_DATE,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.USING_START, userConfig.lang),
            isEditable = false,
            mode = FormDateTimeCellMode.DMY,
            value = deviceEntity?.usingStartDate?.let { dt ->
                LocalDateTime(dt.ye ?: 2000, dt.mo ?: 1, dt.da ?: 1, 0, 0, 0).toInstant(getTimeZone(userConfig.timeOffset)).epochSeconds.toInt()
            },
        )

        formCells += FormBooleanCell(
            name = FIELD_COPY_SENSORS,
            caption = if (id == null) "" else getLocalizedMMSMessage(LocalizedMMSMessages.COPY_SENSORS, userConfig.lang),
            isEditable = true,
            value = false,
        )

        sensorAutoCreates.forEach { (name, descr) ->
            val fieldName = name + SENSOR_CREATE_ENABLED

            formCells += FormBooleanCell(
                name = fieldName,
                caption = "${getLocalizedMMSMessage(LocalizedMMSMessages.AUTO_CREATE_SENSORS, userConfig.lang)} $descr",
                isEditable = true,
                value = false,
//--- видимость этих полей зависит от FIELD_COPY_SENSORS, но вследствие отсутствия множественности условий visibility
//--- поля типа "name + SENSOR_GROUP_NAME + si" могут оставаться видимыми при включенной, но невидимой галочке "name + SENSOR_CREATE_ENABLED" :(
//                visibility = FormCellVisibility(
//                    name = FIELD_COPY_SENSORS,
//                    state = true,
//                    values = setOf(false.toString()),
//                ),
            )
            (1..MAX_PORT_PER_SENSOR).forEach { si ->
                formCells += FormSimpleCell(
                    name = name + SENSOR_GROUP_NAME + si,
                    caption = "${getLocalizedMMSMessage(LocalizedMMSMessages.SENSOR_GROUP_NAME, userConfig.lang)} $si",
                    isEditable = true,
                    value = "",
                    visibility = FormCellVisibility(
                        name = fieldName,
                        state = true,
                        values = setOf(true.toString()),
                    ),
                )
                formCells += FormSimpleCell(
                    name = name + SENSOR_DESCR_PREFIX + si,
                    caption = "${getLocalizedMMSMessage(LocalizedMMSMessages.SENSOR_NAME_PREFIX, userConfig.lang)} $si",
                    isEditable = true,
                    value = "",
                    visibility = FormCellVisibility(
                        name = fieldName,
                        state = true,
                        values = setOf(true.toString()),
                    ),
                )
                formCells += FormSimpleCell(
                    name = name + SENSOR_DESCR_POSTFIX + si,
                    caption = "${getLocalizedMMSMessage(LocalizedMMSMessages.SENSOR_NAME_POSTFIX, userConfig.lang)} $si",
                    isEditable = true,
                    value = "",
                    visibility = FormCellVisibility(
                        name = fieldName,
                        state = true,
                        values = setOf(true.toString()),
                    ),
                )
            }
        }

        return formCells
    }

    override fun formActionSave(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        formActionData: Map<String, FormActionData>
    ): FormActionResponse {
        val id = action.id

        val recordUserId = formActionData[FIELD_USER_ID]?.stringValue?.toIntOrNull() ?: 0

        val index = formActionData[FIELD_INDEX]?.stringValue?.toIntOrNull() ?: return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_INDEX to getLocalizedMMSMessage(LocalizedMMSMessages.INDEX_NOT_ENTERED, userConfig.lang)))
        if (index !in 0..<MAX_DEVICE_COUNT_PER_OBJECT) {
            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_INDEX to "${getLocalizedMMSMessage(LocalizedMMSMessages.INDEX_MUST_BE_BETWEEN_0_AND, userConfig.lang)} ${MAX_DEVICE_COUNT_PER_OBJECT - 1}"))
        }

//        val serialNo = formActionData[FIELD_SERIAL_NO]?.stringValue?.trim() ?: return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_SERIAL_NO to "Не введён серийный номер"))
//        if (serialNo.isEmpty()) {
//            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_SERIAL_NO to "Не введён серийный номер"))
//        }
//        if (deviceRepository.findBySerialNo(serialNo).any { se -> se.id != id }) {
//            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_SERIAL_NO to "Такой серийный номер уже существует"))
//        }

        val objectId = formActionData[FIELD_OBJECT_ID]?.stringValue?.toIntOrNull() ?: 0
        val objectEntity = objectRepository.findByIdOrNull(objectId)

        val copySensors = formActionData[FIELD_COPY_SENSORS]?.booleanValue ?: false

        val usingStartDate = getDateTimeYMDHMSInts(getTimeZone(userConfig.timeOffset), Clock.System.now().epochSeconds.toInt())

        val oldDeviceEntity = id?.let {
            deviceRepository.findByIdOrNull(id)
        }

        val recordId = id ?: getNextId { nextId -> deviceRepository.existsById(nextId) }
        val deviceEntity = DeviceEntity(
            id = recordId,
            userId = recordUserId,
            index = index,
            type = formActionData[FIELD_TYPE]?.stringValue?.toIntOrNull() ?: MMSTelematicFunction.DEVICE_TYPE_PULSAR_DATA,
            serialNo = formActionData[FIELD_SERIAL_NO]?.stringValue ?: "",
            name = formActionData[FIELD_NAME]?.stringValue?.trim(),
            obj = objectEntity,
            cellImei = formActionData[FIELD_CELL_IMEI]?.stringValue,
            cellOwner = formActionData[FIELD_CELL_OWNER]?.stringValue?.toIntOrNull() ?: CELL_OWNER_UNKNOWN,
            cellNumber = formActionData[FIELD_CELL_NUMBER]?.stringValue,
            cellIcc = formActionData[FIELD_CELL_ICC]?.stringValue,
            cellOperator = formActionData[FIELD_CELL_OPERATOR]?.stringValue,
            cellImei2 = formActionData[FIELD_CELL_IMEI_2]?.stringValue,
            cellOwner2 = formActionData[FIELD_CELL_OWNER_2]?.stringValue?.toIntOrNull() ?: CELL_OWNER_UNKNOWN,
            cellNumber2 = formActionData[FIELD_CELL_NUMBER_2]?.stringValue,
            cellIcc2 = formActionData[FIELD_CELL_ICC_2]?.stringValue,
            cellOperator2 = formActionData[FIELD_CELL_OPERATOR_2]?.stringValue,
            fwVersion = oldDeviceEntity?.fwVersion ?: "",
            lastSessionTime = oldDeviceEntity?.lastSessionTime ?: 0,
            lastSessionStatus = oldDeviceEntity?.lastSessionStatus ?: "",
            lastSessionError = oldDeviceEntity?.lastSessionError ?: "",
            usingStartDate = oldDeviceEntity?.usingStartDate ?: DateEntity(
                ye = usingStartDate[0],
                mo = usingStartDate[1],
                da = usingStartDate[2],
            ),
        )
        deviceRepository.saveAndFlush(deviceEntity)

        if (copySensors) {
            objectEntity?.let {
                oldDeviceEntity?.obj?.let { oldObjectEntity ->
                    oldDeviceEntity.index?.let { oldIndex ->
                        if (objectEntity.id != 0 && objectEntity.id != oldObjectEntity.id) {
                            copySensors(oldObjectEntity, oldIndex, objectEntity, index)
                        }
                    }
                }
            }
        } else if (objectId != 0) {
            sensorAutoCreates.forEach { (name, _) ->
                val fieldName = name + SENSOR_CREATE_ENABLED

                if (formActionData[fieldName]?.booleanValue == true) {
                    (1..MAX_PORT_PER_SENSOR).forEach { si ->
                        val groupName = formActionData[name + SENSOR_GROUP_NAME + si]?.stringValue?.trim() ?: ""
                        val prefix = formActionData[name + SENSOR_DESCR_PREFIX + si]?.stringValue?.trim() ?: ""
                        val postfix = formActionData[name + SENSOR_DESCR_POSTFIX + si]?.stringValue?.trim() ?: ""

                        if (prefix.isNotEmpty() || postfix.isNotEmpty()) {
                            sensorInfos[name]?.let { sensorInfoList ->
                                sensorInfoList.forEach { sensorInfo ->
                                    addSensor(
                                        obj = objectEntity,
                                        deviceIndex = index,
                                        sensorIndex = si,
                                        groupName = groupName,
                                        descrPrefix = prefix,
                                        descrPostfix = postfix,
                                        sensorType = sensorInfo.sensorType,
                                        portNum = sensorInfo.portNum,
                                        descr = sensorInfo.descr[userConfig.lang] ?: "-",
                                        minView = sensorInfo.minView,
                                        maxView = sensorInfo.maxView,
                                        minLimit = sensorInfo.minLimit,
                                        maxLimit = sensorInfo.maxLimit,
                                        indicatorDelimiterCount = sensorInfo.indicatorDelimiterCount,
                                        indicatorMultiplicator = sensorInfo.indicatorMultiplicator,
                                        phase = sensorInfo.phase,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            sensorRepository.flush()
        }

        return FormActionResponse(
            responseCode = ResponseCode.OK,
            nextAction = action.prevAction?.copy(id = recordId),
        )
    }

    override fun getFormActionPermissions(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): Triple<Boolean, Boolean, Boolean> {
        val id = action.id

        val addEnabled = checkFormAddPermission(moduleConfig, userConfig.roles)

        val deviceEntity = id?.let {
            deviceRepository.findByIdOrNull(id) ?: return Triple(addEnabled, false, false)
        }

        val editEnabled = checkRowPermission(action.module, ActionType.FORM_EDIT, userConfig.relatedUserIds[deviceEntity?.userId], userConfig.roles)
        val deleteEnabled = checkRowPermission(action.module, ActionType.FORM_DELETE, userConfig.relatedUserIds[deviceEntity?.userId], userConfig.roles)

        return Triple(addEnabled, editEnabled, deleteEnabled)
    }

    override fun formActionDelete(userId: Int, id: Int): FormActionResponse {
        deviceRepository.findByIdOrNull(id)?.let { deviceEntity ->
            deviceManageRepository.deleteByDevice(deviceEntity)
        }
        deviceRepository.deleteById(id)

        return FormActionResponse(responseCode = ResponseCode.OK)
    }

    private fun copySensors(oldObjectEntity: ObjectEntity, oldDeviceIndex: Int, newObjectEntity: ObjectEntity, newDeviceIndex: Int) {
        sensorRepository.findByObjAndPortNumBetween(
            obj = oldObjectEntity,
            startPort = oldDeviceIndex * CoreTelematicFunction.MAX_PORT_PER_DEVICE,
            endPort = (oldDeviceIndex + 1) * CoreTelematicFunction.MAX_PORT_PER_DEVICE - 1,
        ).forEach { oldSensorEntity ->
            //--- close sensor's work period
            oldSensorEntity.endTime = getCurrentTimeInt()
            sensorRepository.save(oldSensorEntity)

            oldSensorEntity.id = getNextId { nextId -> sensorRepository.existsById(nextId) }
            oldSensorEntity.obj = newObjectEntity
            oldSensorEntity.portNum = (oldSensorEntity.portNum ?: 0) % CoreTelematicFunction.MAX_PORT_PER_DEVICE + newDeviceIndex * CoreTelematicFunction.MAX_PORT_PER_DEVICE
            //--- open sensor's work period
            oldSensorEntity.begTime = getCurrentTimeInt()
            oldSensorEntity.endTime = null

            sensorRepository.save(oldSensorEntity)
            SensorService.checkAndCreateSensorTables(entityManager, oldSensorEntity.id)

            sensorCalibrationRepository.findBySensorOrderBySensorValue(oldSensorEntity).forEach { oldSensorCalibrationEntity ->
                oldSensorCalibrationEntity.id = getNextId { nextId -> sensorCalibrationRepository.existsById(nextId) }
                oldSensorCalibrationEntity.sensor = oldSensorEntity

                sensorCalibrationRepository.save(oldSensorCalibrationEntity)
            }
        }
        sensorRepository.flush()
        sensorCalibrationRepository.flush()
    }

    private fun addSensor(
        obj: ObjectEntity?,
        deviceIndex: Int,
        sensorIndex: Int,
        groupName: String,
        descrPrefix: String,
        descrPostfix: String,
        sensorType: Int,
        portNum: Int,
        descr: String,
        minView: Double,
        maxView: Double,
        minLimit: Double = 0.0,
        maxLimit: Double = 0.0,
        indicatorDelimiterCount: Int = 4,
        indicatorMultiplicator: Double = 1.0,
        phase: Int = 0,
    ) {
        val sensorEntity = SensorEntity(
            id = getNextId { nextId -> sensorRepository.existsById(nextId) },
            obj = obj,
            name = "",
            group = groupName,
            descr = "$descrPrefix $descr $descrPostfix",
            portNum = deviceIndex * CoreTelematicFunction.MAX_PORT_PER_DEVICE + portNum + sensorIndex,
            sensorType = sensorType,
            begTime = getCurrentTimeInt(),
            endTime = null,
            serialNo = "",

            minMovingTime = 1,
            minParkingTime = 300,
            minOverSpeedTime = 60,
            isAbsoluteRun = true,

            minIgnore = 0.0,
            maxIgnore = 1_000_000_000.0,
            dim = null,

            isWorkAboveBorder = true,
            workOnBorder = null,
            workIdleBorder = null,
            workOverBorder = null,
            workMinOnTime = 1,
            workMinOffTime = 1,
            workMinIdleTime = 1,
            workMinOverTime = 1,

            minView = minView,
            maxView = maxView,
            minLimit = minLimit,
            maxLimit = maxLimit,
            smoothTime = 0,
            indicatorDelimiterCount = indicatorDelimiterCount,
            indicatorMultiplicator = indicatorMultiplicator,

            isAbsoluteCount = true,
            inOutType = SensorConfigCounter.CALC_TYPE_OUT,

            containerType = SensorConfigLiquidLevel.CONTAINER_TYPE_WORK,

            phase = phase,

            schemeX = null,
            schemeY = null,
        )
        sensorRepository.save(sensorEntity)
        SensorService.checkAndCreateSensorTables(entityManager, sensorEntity.id)
    }

}

private class SensorInfo(
    val sensorType: Int,
    val portNum: Int,
    val descr: Map<LanguageEnum, String>,
    val minView: Double = 0.0,
    val maxView: Double = 1000.0,
    val minLimit: Double = 0.0,
    val maxLimit: Double = 0.0,
    val indicatorDelimiterCount: Int = 5,
    val indicatorMultiplicator: Double = 1.0,
    val phase: Int = 0,
)
