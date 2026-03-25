package foatto.server.service

import foatto.core.ActionType
import foatto.core.i18n.LocalizedMessages
import foatto.core.i18n.getLocalizedMessage
import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.HeaderData
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.TitleData
import foatto.core.model.response.form.FormCellCaption
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
import foatto.core.model.response.table.cell.TableCellBackColorType
import foatto.core.model.response.table.cell.TableSimpleCell
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getDateTimeDMYHMSString
import foatto.core.util.getRandomInt
import foatto.core.util.getTimeZone
import foatto.core_mms.AppModuleMMS
import foatto.core_mms.i18n.LocalizedMMSMessages
import foatto.core_mms.i18n.getLocalizedMMSMessage
import foatto.server.UserRelationEnum
import foatto.server.appModuleConfigs
import foatto.server.checkAccessPermission
import foatto.server.checkFormAddPermission
import foatto.server.checkRowPermission
import foatto.server.entity.SensorCalibrationEntity
import foatto.server.entity.SensorEntity
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.model.sensor.SensorConfig
import foatto.server.model.sensor.SensorConfigCounter
import foatto.server.model.sensor.SensorConfigLiquidLevel
import foatto.server.repository.ActionLogRepository
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorCalibrationRepository
import foatto.server.repository.SensorRepository
import foatto.server.repository.UserRepository
import foatto.server.sql.CoreAdvancedConnection
import foatto.server.util.getNextId
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.io.File
import java.sql.ResultSet

@Service
class SensorService(
    private val entityManager: EntityManager,
    private val sensorRepository: SensorRepository,
    private val objectRepository: ObjectRepository,
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
        private const val FIELD_OBJECT_ID = "obj_id"

        private const val FIELD_NAME = "name"
        private const val FIELD_GROUP = "group"
        private const val FIELD_DESCR = "descr"
        private const val FIELD_PORT_NUM = "portNum"
        private const val FIELD_SENSOR_TYPE = "sensorType"
        private const val FIELD_BEG_TIME = "begTime"
        private const val FIELD_END_TIME = "endTime"
        private const val FIELD_SERIAL_NO = "serialNo"

        private const val FIELD_MIN_MOVING_TIME = "minMovingTime"
        private const val FIELD_MIN_PARKING_TIME = "minParkingTime"
        private const val FIELD_MIN_OVER_SPEED_TIME = "minOverSpeedTime"
        private const val FIELD_IS_ABSOLUTE_RUN = "isAbsoluteRun"

        private const val FIELD_MIN_IGNORE = "minIgnore"
        private const val FIELD_MAX_IGNORE = "maxIgnore"
        private const val FIELD_DIM = "dim"

        private const val FIELD_IS_ABOVE_BORDER = "isAboveBorder"
        private const val FIELD_ON_BORDER = "workOnBorder"
        private const val FIELD_IDLE_BORDER = "workIdleBorder"
        private const val FIELD_OVER_BORDER = "workOverBorder"
        private const val FIELD_MIN_OFF_TIME = "workMinOffTime"
        private const val FIELD_MIN_ON_TIME = "workMinOnTime"
        private const val FIELD_MIN_IDLE_TIME = "workMinIdleTime"
        private const val FIELD_MIN_OVER_TIME = "workMinOverTime"

        private const val FIELD_MIN_VIEW = "minView"
        private const val FIELD_MAX_VIEW = "maxView"
        private const val FIELD_MIN_LIMIT = "minLimit"
        private const val FIELD_MAX_LIMIT = "maxLimit"
        private const val FIELD_SMOOTH_TIME = "smoothTime"
        private const val FIELD_INDICATOR_DELIMITER_COUNT = "indicatorDelimiterCount"
        private const val FIELD_INDICATOR_MILTIPLICATOR = "indicatorMultiplicator"

        private const val FIELD_IS_ABSOLUTE_COUNT = "isAbsoluteCount"
        private const val FIELD_IN_OUT_TYPE = "inOutType"

        private const val FIELD_CONTAINER_TYPE = "containerType"
        private const val FIELD_PHASE = "phase"

        private const val FIELD_SCHEME_X = "schemeX"
        private const val FIELD_SCHEME_Y = "schemeY"

        private const val FIELD_CALIBRATION = "_calibration" // псевдополе для калибровки/тарировки

        fun checkAndCreateSensorTables(conn: CoreAdvancedConnection, sensorId: Int) {
            if (!checkAggTableIsExists(conn, sensorId)) {
                createAggTable(conn, sensorId)
            }
            if (!checkTextTableIsExists(conn, sensorId)) {
                createTextTable(conn, sensorId)
            }
        }

        fun checkAndCreateSensorTables(entityManager: EntityManager, sensorId: Int) {
            if (!checkAggTableIsExists(entityManager, sensorId)) {
                createAggTable(entityManager, sensorId)
            }
            if (!checkTextTableIsExists(entityManager, sensorId)) {
                createTextTable(entityManager, sensorId)
            }
        }

        private fun checkAggTableIsExists(conn: CoreAdvancedConnection, sensorId: Int): Boolean {
            val rs = conn.executeQuery(getAggTableIsExistsSql(sensorId))
            val result = rs.next()
            rs.close()

            return result
        }

        private fun checkAggTableIsExists(entityManager: EntityManager, sensorId: Int): Boolean {
            var result = false
            queryNativeSql(entityManager, getAggTableIsExistsSql(sensorId)) { rs: ResultSet ->
                result = rs.next()
            }

            return result
        }

        private fun getAggTableIsExistsSql(sensorId: Int): String =
            """
                SELECT tablename FROM pg_tables
                WHERE schemaname = 'public' 
                AND tablename = 'mms_agg_$sensorId'
            """

        private fun checkTextTableIsExists(conn: CoreAdvancedConnection, sensorId: Int): Boolean {
            val rs = conn.executeQuery(getTextTableIsExistsSql(sensorId))
            val result = rs.next()
            rs.close()

            return result
        }

        private fun checkTextTableIsExists(entityManager: EntityManager, sensorId: Int): Boolean {
            var result = false
            queryNativeSql(entityManager, getTextTableIsExistsSql(sensorId)) { rs: ResultSet ->
                result = rs.next()
            }

            return result
        }

        private fun getTextTableIsExistsSql(sensorId: Int): String =
            """
                SELECT tablename FROM pg_tables
                WHERE schemaname = 'public' 
                AND tablename = 'mms_text_$sensorId'
            """

        private fun createAggTable(conn: CoreAdvancedConnection, sensorId: Int) {
            getCreateAggTableSqls(sensorId).forEach { sql ->
                conn.executeUpdate(sql)
            }
        }

        private fun createAggTable(entityManager: EntityManager, sensorId: Int) {
            executeNativeSql(entityManager, *getCreateAggTableSqls(sensorId))
        }

        private fun getCreateAggTableSqls(sensorId: Int): Array<String> = arrayOf(
            """
                CREATE TABLE MMS_agg_$sensorId (
                    ontime_0        INT NOT NULL,
                    ontime_1        INT,         
                    type_0          INT,
                    value_0         FLOAT8,
                    value_1         FLOAT8,
                    value_2         FLOAT8,
                    value_3         FLOAT8
                )
            """,
            " CREATE INDEX MMS_agg_${sensorId}_ontime_0 ON MMS_agg_$sensorId ( ontime_0 ) ",
            " CREATE INDEX MMS_agg_${sensorId}_ontime_1 ON MMS_agg_$sensorId ( ontime_1 ) ",
        )

        private fun createTextTable(conn: CoreAdvancedConnection, sensorId: Int) {
            getCreateTextTableSqls(sensorId).forEach { sql ->
                conn.executeUpdate(sql)
            }
        }

        private fun createTextTable(entityManager: EntityManager, sensorId: Int) {
            executeNativeSql(entityManager, *getCreateTextTableSqls(sensorId))
        }

        private fun getCreateTextTableSqls(sensorId: Int): Array<String> = arrayOf(
            """
                CREATE TABLE MMS_text_$sensorId (
                    ontime_0        INT NOT NULL,
                    ontime_1        INT,         
                    type_0          INT,
                    code_0          INT,
                    message_0       VARCHAR(125),
                    text_0          TEXT
                )
            """,
            " CREATE INDEX MMS_text_${sensorId}_ontime_0 ON MMS_text_$sensorId ( ontime_0 ) ",
            " CREATE INDEX MMS_text_${sensorId}_ontime_1 ON MMS_text_$sensorId ( ontime_1 ) ",
        )

        fun deleteSensor(
            entityManager: EntityManager,
            sensorRepository: SensorRepository,
            sensorCalibrationRepository: SensorCalibrationRepository,
            sensorId: Int
        ) {
            sensorRepository.findByIdOrNull(sensorId)?.let { sensorEntity ->
                sensorCalibrationRepository.deleteBySensor(sensorEntity)
            }
            sensorRepository.deleteById(sensorId)
            executeNativeSql(entityManager, " DROP TABLE MMS_agg_$sensorId ")
            executeNativeSql(entityManager, " DROP TABLE MMS_text_$sensorId ")
        }
    }

    @Value("\${data_server_ini_file}")
    val dataServerIniFileName: String = ""

    override fun getTableHeaderData(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig
    ): HeaderData {
        val rows = mutableListOf<Pair<String, String>>()

        getParentObjectId(action)?.let { parentObjectId ->
            objectRepository.findByIdOrNull(parentObjectId)?.let { parentObjectEntity ->
                rows += getLocalizedMMSMessage(LocalizedMMSMessages.OBJECT_NAME, userConfig.lang) to (parentObjectEntity.name ?: "-")
                rows += getLocalizedMMSMessage(LocalizedMMSMessages.OBJECT_MODEL, userConfig.lang) to (parentObjectEntity.model ?: "-")
            }
        }

        return HeaderData(
            titles = listOf(
                TitleData(
                    action = null,
                    text = getLocalizedMessage(moduleConfig.captions, userConfig.lang),
                    isBold = true,
                )
            ),
            rows = rows,
        )
    }

    override fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<TableCaption> {
        val alColumnInfo = mutableListOf<Pair<String?, String>>()

        alColumnInfo += null to "" // sensorGroup

        alColumnInfo += FIELD_NAME to getLocalizedMMSMessage(LocalizedMMSMessages.INTEGRATION_ID, userConfig.lang)
        alColumnInfo += FIELD_DESCR to getLocalizedMMSMessage(LocalizedMMSMessages.DESCRIPTION, userConfig.lang)
        alColumnInfo += FIELD_PORT_NUM to getLocalizedMMSMessage(LocalizedMMSMessages.INPUT_NUMBER, userConfig.lang)
        alColumnInfo += FIELD_SENSOR_TYPE to getLocalizedMMSMessage(LocalizedMMSMessages.SENSOR_TYPE, userConfig.lang)
        alColumnInfo += FIELD_BEG_TIME to getLocalizedMMSMessage(LocalizedMMSMessages.USING_START, userConfig.lang)
        alColumnInfo += FIELD_END_TIME to getLocalizedMMSMessage(LocalizedMMSMessages.USING_END, userConfig.lang)
        alColumnInfo += FIELD_SERIAL_NO to getLocalizedMMSMessage(LocalizedMMSMessages.SERIAL_NUMBER, userConfig.lang)

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

        val chartableSensorTypes = setOf(SensorConfig.SENSOR_GEO) + SensorConfig.analogueSensorTypes + SensorConfig.counterSensorTypes

        val zoneLocal = getTimeZone(userConfig.timeOffset)

        var currentRowNo: Int? = null
        var row = 0

        val pageRequest = getTableSortedPageRequest(action, Sort.Order(Sort.Direction.ASC, FIELD_GROUP), Sort.Order(Sort.Direction.ASC, FIELD_PORT_NUM))
        val findText = action.findText?.trim() ?: ""

        val parentObjectId = getParentObjectId(action) ?: return null
        val parentObjectEntity = objectRepository.findByIdOrNull(parentObjectId) ?: return null

        val page: Page<SensorEntity> = sensorRepository.findByObjAndFilter(
            obj = parentObjectEntity,
            findText = findText,
            timeOffset = userConfig.timeOffset,
            begDateTime = action.begDateTimeValue ?: -1,
            endDateTime = action.endDateTimeValue ?: -1,
            pageRequest = pageRequest,
        )

        fillTablePageButtons(userConfig, action, page.totalPages, pageButtons)
        val sensorEntities = page.content

        var prevGroupName: String? = null
        for (sensorEntity in sensorEntities) {
            var col = 0

            val isFormEnabled = checkRowPermission(
                module = action.module,
                actionType = ActionType.MODULE_FORM,
                rowUserRelation = UserRelationEnum.NOBODY,
                userRoles = userConfig.roles
            )

            val groupName = sensorEntity.group ?: "-"
            if (prevGroupName != groupName) {
                tableCells += TableSimpleCell(
                    row = row,
                    col = col,
                    colSpan = 8,
                    dataRow = row,
                    name = groupName,
                    backColorType = TableCellBackColorType.GROUP_0,
                    isBoldText = true,
                )
                prevGroupName = groupName
                tableRows += TableRow()
                row++
            }
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, name = "", backColorType = TableCellBackColorType.GROUP_0)

            tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, name = sensorEntity.name ?: "")
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, name = sensorEntity.descr ?: "-")
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, name = sensorEntity.portNum?.toString() ?: "-")
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = sensorEntity.sensorType?.let { sensorType ->
                    SensorConfig.hmSensorDescr[sensorType]?.get(userConfig.lang) ?: getLocalizedMMSMessage(LocalizedMMSMessages.UNKNOWN_SENSOR_TYPE, userConfig.lang)
                } ?: "-",
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = sensorEntity.begTime?.let { begTime -> getDateTimeDMYHMSString(zoneLocal, begTime) } ?: "-",
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = sensorEntity.endTime?.let { endTime -> getDateTimeDMYHMSString(zoneLocal, endTime) } ?: "-",
            )
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, name = sensorEntity.serialNo ?: "-")

            val formOpenAction = action.copy(
                type = ActionType.MODULE_FORM,
                id = sensorEntity.id,
            )

            val popupDatas = mutableListOf<TablePopup>()

            if (isFormEnabled) {
                popupDatas += TablePopup(
                    action = formOpenAction,
                    text = getLocalizedMessage(LocalizedMessages.OPEN, userConfig.lang),
                    inNewTab = false,
                )
            }

//        if (isEquip) {
//            alChildData.add(ChildData("mms_equip_service_shedule", columnId, true))
//            alChildData.add(ChildData("mms_equip_service_history", columnId))
//        } else {
//            alChildData.add(ChildData("mms_sensor_calibration", columnId, true))
//        }
            if (checkAccessPermission(AppModuleMMS.SENSOR_CALIBRATION, userConfig.roles)) {
                popupDatas += TablePopup(
                    action = AppAction(
                        type = ActionType.MODULE_TABLE,
                        module = AppModuleMMS.SENSOR_CALIBRATION,
                        parentModule = action.module,
                        parentId = sensorEntity.id,
                    ),
                    text = appModuleConfigs[AppModuleMMS.SENSOR_CALIBRATION]?.captions?.let { captions ->
                        getLocalizedMessage(captions, userConfig.lang)
                    } ?: "(${getLocalizedMessage(LocalizedMessages.UNKNOWN_MODULE_TYPE, userConfig.lang)}: ${AppModuleMMS.SENSOR_CALIBRATION})",
                    inNewTab = true,
                )
            }
            if (checkAccessPermission(AppModuleMMS.SENSOR_DATA, userConfig.roles)) {
                popupDatas += TablePopup(
                    action = AppAction(
                        type = ActionType.MODULE_TABLE,
                        module = AppModuleMMS.SENSOR_DATA,
                        parentModule = action.module,
                        parentId = sensorEntity.id,
                    ),
                    text = appModuleConfigs[AppModuleMMS.SENSOR_DATA]?.captions?.let { captions ->
                        getLocalizedMessage(captions, userConfig.lang)
                    } ?: "(${getLocalizedMessage(LocalizedMessages.UNKNOWN_MODULE_TYPE, userConfig.lang)}: ${AppModuleMMS.SENSOR_DATA})",
                    inNewTab = true,
                )
            }
            if (sensorEntity.sensorType in chartableSensorTypes && checkAccessPermission(AppModuleMMS.CHART_SENSOR, userConfig.roles)) {
                popupDatas += TablePopup(
                    action = AppAction(
                        type = ActionType.MODULE_CHART,
                        module = AppModuleMMS.CHART_SENSOR,
                        parentModule = action.module,
                        parentId = sensorEntity.id,
                        timeRangeType = 24 * 60 * 60,   // графики за последние 24 часа
                    ),
                    text = appModuleConfigs[AppModuleMMS.CHART_SENSOR]?.captions?.let { captions ->
                        getLocalizedMessage(captions, userConfig.lang)
                    } ?: "(${getLocalizedMessage(LocalizedMessages.UNKNOWN_MODULE_TYPE, userConfig.lang)}: '${AppModuleMMS.CHART_SENSOR}')",
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

            if (sensorEntity.id == action.id) {
                currentRowNo = row
            }

            row++
        }
        return currentRowNo
    }

    override fun getFormHeaderData(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig
    ): HeaderData {
        val rows = mutableListOf<Pair<String, String>>()

        getParentObjectId(action)?.let { parentObjectId ->
            objectRepository.findByIdOrNull(parentObjectId)?.let { parentObjectEntity ->
                rows += getLocalizedMMSMessage(LocalizedMMSMessages.OBJECT_NAME, userConfig.lang) to (parentObjectEntity.name ?: "-")
                rows += getLocalizedMMSMessage(LocalizedMMSMessages.OBJECT_MODEL, userConfig.lang) to (parentObjectEntity.model ?: "-")
            }
        }

        return HeaderData(
            titles = listOf(
                TitleData(
                    action = null,
                    text = getLocalizedMessage(moduleConfig.captions, userConfig.lang),
                    isBold = true,
                )
            ),
            rows = rows,
        )
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

//        //--- this is "equipment" (for users) or full "sensors" (for installers)?
//        val isEquip = aliasConfig.name == "mms_equip"

        val sensorEntity = id?.let {
            sensorRepository.findByIdOrNull(id) ?: return emptyList()
        }

        val parentObjectId = getParentObjectId(action) ?: 0

        val sensorCalibrationInfo = id?.let {
            sensorCalibrationRepository.findBySensorIdOrderBySensorValue(id).joinToString("\n") { sensorCalibrationEntity ->
                "${sensorCalibrationEntity.sensorValue} = ${sensorCalibrationEntity.dataValue}"
            }
        } ?: ""

        val geoSensorType = setOf(SensorConfig.SENSOR_GEO).map { it.toString() }.toSet()
        val workSensorType = setOf(SensorConfig.SENSOR_WORK).map { it.toString() }.toSet()
        val liquidLevelSensorType = setOf(SensorConfig.SENSOR_LIQUID_LEVEL).map { it.toString() }.toSet()

        val counterAndSummarySensorTypes = setOf(
            SensorConfig.SENSOR_LIQUID_USING,
            SensorConfig.SENSOR_MASS_ACCUMULATED,
            SensorConfig.SENSOR_VOLUME_ACCUMULATED,
        ).map { it.toString() }.toSet()

//        val energoSummarySensorTypes = setOf(
//            SensorConfig.SENSOR_ENERGO_COUNT_AD,
//            SensorConfig.SENSOR_ENERGO_COUNT_AR,
//            SensorConfig.SENSOR_ENERGO_COUNT_RD,
//            SensorConfig.SENSOR_ENERGO_COUNT_RR,
//        ).map { it.toString() }.toSet()

        val phasedEnergoSensorTypes = setOf(
            SensorConfig.SENSOR_ENERGO_VOLTAGE,
            SensorConfig.SENSOR_ENERGO_CURRENT,
            SensorConfig.SENSOR_ENERGO_POWER_KOEF,
            SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
            SensorConfig.SENSOR_ENERGO_POWER_REACTIVE,
            SensorConfig.SENSOR_ENERGO_POWER_FULL,
        ).map { it.toString() }.toSet()

        val analogSensorTypes = (setOf(
            SensorConfig.SENSOR_WEIGHT,
            SensorConfig.SENSOR_TURN,
            SensorConfig.SENSOR_PRESSURE,
            SensorConfig.SENSOR_TEMPERATURE,
            SensorConfig.SENSOR_VOLTAGE,
            SensorConfig.SENSOR_POWER,
            SensorConfig.SENSOR_DENSITY,
            SensorConfig.SENSOR_MASS_FLOW,
            SensorConfig.SENSOR_VOLUME_FLOW,
            SensorConfig.SENSOR_ENERGO_TRANSFORM_KOEF_CURRENT,
            SensorConfig.SENSOR_ENERGO_TRANSFORM_KOEF_VOLTAGE,
        ) + liquidLevelSensorType + phasedEnergoSensorTypes).map { it.toString() }.toSet()

        //--- common data

        formCells += FormSimpleCell(
            name = FIELD_OBJECT_ID,
            caption = "",
            isEditable = false,
            value = parentObjectId.toString(),
        )

        //!!! combo с возможностью ручного ввода
//" SELECT DISTINCT group_name FROM $columnTableName WHERE object_id = $parentObjectId AND group_name IS NOT NULL AND group_name <> '' ORDER BY group_name "
        formCells += FormSimpleCell(
            name = FIELD_GROUP,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.GROUP, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.group ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_NAME,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.INTEGRATION_ID, userConfig.lang),
            isEditable = changeEnabled,
//            isEditable = !isEquip
            value = sensorEntity?.name ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_DESCR,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.DESCRIPTION, userConfig.lang),
            isEditable = changeEnabled,
//            isEditable = !isEquip
            value = sensorEntity?.descr ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_PORT_NUM,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.INPUT_NUMBER, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.portNum?.toString() ?: "",
        )
        formCells += FormComboCell(
            name = FIELD_SENSOR_TYPE,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.SENSOR_TYPE, userConfig.lang),
            //--- нельзя менять тип уже созданному датчику, т.к. в его таблицу уже записаны данные,
            //--- могущими быть некорректными для нового типа датчика
            isEditable = changeEnabled && id == null,
//            isEditable = !isEquip
            value = sensorEntity?.sensorType?.toString() ?: "",
            //!!! заполнять с сортировкой по популярности
            values = SensorConfig.hmSensorDescr.map { (key, values) ->
                key.toString() to (values[userConfig.lang] ?: getLocalizedMMSMessage(LocalizedMMSMessages.UNKNOWN_SENSOR_TYPE, userConfig.lang))
            },
        )
        /*
                    //--- arrange the types of sensors depending on their "popularity" (ie frequency of use)
                    val hmSensorDescr = mutableMapOf<Int, String>()
                    hmSensorDescr.putAll(SensorConfig.hmSensorDescr)
                    val rs = conn.executeQuery(" SELECT sensor_type, COUNT( sensor_type ) AS aaa FROM MMS_sensor GROUP BY sensor_type ORDER BY aaa DESC ")
                    while (rs.next()) {
                        val sensorType = rs.getInt(1)
                        //--- theoretically, incorrect / non-existent / obsolete (including zero) types of sensors are possible
                        val sensorDescr = hmSensorDescr[sensorType] ?: continue

                        addChoice(sensorType, sensorDescr)
                        hmSensorDescr.remove(sensorType)
                        //--- the most popular sensor type is set as the default type
                        if (defaultValue == null) defaultValue = sensorType
                    }
                    rs.close()
                    //--- add leftovers from unpopular sensors
                    hmSensorDescr.forEach { (sensorType, descr) ->
                        addChoice(sensorType, descr)
                    }
        */
        formCells += FormDateTimeCell(
            name = FIELD_BEG_TIME,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.USING_START, userConfig.lang),
            isEditable = true,
            mode = FormDateTimeCellMode.DMYHMS,
            value = if (id == 0) {
                getCurrentTimeInt()
            } else {
                sensorEntity?.begTime
            },
        )
        formCells += FormDateTimeCell(
            name = FIELD_END_TIME,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.USING_END, userConfig.lang),
            isEditable = true,
            mode = FormDateTimeCellMode.DMYHMS,
            value = sensorEntity?.endTime,
        )
        formCells += FormSimpleCell(
            name = FIELD_SERIAL_NO,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.SERIAL_NUMBER, userConfig.lang),
            isEditable = changeEnabled,
//            formPinMode = FormPinMode.OFF
//            isEditable = !isEquip
            value = sensorEntity?.serialNo ?: "",
        )

        //--- geo-sensors (coordinates, speed and mileage) ----------------------------------------------------------------------------------------

        formCells += FormSimpleCell(
            name = FIELD_MIN_MOVING_TIME,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.MINIMUM_DRIVING_TIME, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.minMovingTime?.toString() ?: "1",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = geoSensorType,
            ),
        )

        formCells += FormSimpleCell(
            name = FIELD_MIN_PARKING_TIME,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.MINIMUM_PARKING_TIME, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.minParkingTime?.toString() ?: "300",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = geoSensorType,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_MIN_OVER_SPEED_TIME,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.MINIMUM_SPEEDING_TIME, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.minOverSpeedTime?.toString() ?: "60",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = geoSensorType,
            ),
        )
        formCells += FormBooleanCell(
            name = FIELD_IS_ABSOLUTE_RUN,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.ABSOLUTE_MILEAGE, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.isAbsoluteRun ?: true,
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = geoSensorType,
            ),
        )

        //--- all sensors (exclude geo-sensor) --------------------------------------------------------------

        formCells += FormSimpleCell(
            name = FIELD_MIN_IGNORE,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.IGNORE_SENSOR_READINGS_LESS_THAN, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.minIgnore?.toString() ?: "0.0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = false,
                values = geoSensorType,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_MAX_IGNORE,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.IGNORE_SENSOR_READINGS_GREATER_THAN, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.maxIgnore?.toString() ?: "1000000.0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = false,
                values = geoSensorType,
            ),
        )

        formCells += FormSimpleCell(
            name = FIELD_DIM,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.UNIT_OF_MEASUREMENT, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.dim ?: "",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = false,
                values = geoSensorType + workSensorType,
            ),
        )

        //--- discrete sensors: equipment operating time; ---------------------------------------------------

        formCells += FormBooleanCell(
            name = FIELD_IS_ABOVE_BORDER,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.OPERATING_STATE_ABOVE_LIMIT, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.isWorkAboveBorder ?: true,
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = workSensorType,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_ON_BORDER,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.ON_LIMIT, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.workOnBorder?.toString() ?: "",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = workSensorType,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_IDLE_BORDER,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.IDLE_LIMIT, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.workIdleBorder?.toString() ?: "",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = workSensorType,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_OVER_BORDER,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.OVERLOAD_LIMIT, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.workOverBorder?.toString() ?: "",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = workSensorType,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_MIN_OFF_TIME,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.MINIMUM_IDLE_TIME_1, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.workMinOffTime?.toString() ?: "1",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = workSensorType,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_MIN_ON_TIME,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.MINIMUM_OPERATING_TIME, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.workMinOnTime?.toString() ?: "1",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = workSensorType,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_MIN_IDLE_TIME,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.MINIMUM_IDLE_TIME_2, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.workMinIdleTime?.toString() ?: "1",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = workSensorType,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_MIN_OVER_TIME,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.MINIMUM_OVERLOAD_TIME, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.workMinOverTime?.toString() ?: "1",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = workSensorType,
            ),
        )

        //--- analog / measuring sensors ---------------------------------------------------------------------------------

        formCells += FormSimpleCell(
            name = FIELD_MIN_VIEW,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.MINIMUM_DISPLAY_VALUE, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.minView?.toString() ?: "0.0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                //--- geoSensorType - для графика скорости
                //--- counterAndSummarySensorTypes - в отладочных целях (для диагностики вылетов значений счётчиков)
                values = geoSensorType + analogSensorTypes + counterAndSummarySensorTypes,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_MAX_VIEW,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.MAXIMUM_DISPLAY_VALUE, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.maxView?.toString() ?: "100.0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                //--- geoSensorType - для графика скорости
                //--- counterAndSummarySensorTypes - в отладочных целях (для диагностики вылетов значений счётчиков)
                values = geoSensorType + analogSensorTypes + counterAndSummarySensorTypes,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_MIN_LIMIT,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.MINIMUM_OPERATING_VALUE, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.minLimit?.toString() ?: "0.0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = analogSensorTypes,
            ),
        )

        formCells += FormSimpleCell(
            name = FIELD_MAX_LIMIT,
            caption = "-",
            isEditable = changeEnabled,
            value = sensorEntity?.maxLimit?.toString() ?: "0.0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = geoSensorType + analogSensorTypes,     //--- geo + analog / measuring sensors
            ),
            captions = FormCellCaption(
                name = FIELD_SENSOR_TYPE,
                captions = mapOf(
                    getLocalizedMMSMessage(LocalizedMMSMessages.SPEED_LIMIT, userConfig.lang) to geoSensorType,
                    getLocalizedMMSMessage(LocalizedMMSMessages.MAXIMUM_OPERATING_VALUE, userConfig.lang) to analogSensorTypes,
                ),
            ),
        )

        formCells += FormSimpleCell(
            name = FIELD_SMOOTH_TIME,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.SMOOTHING_PERIOD, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.smoothTime?.toString() ?: "0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = analogSensorTypes,
            ),
        )

        formCells += FormSimpleCell(
            name = FIELD_INDICATOR_DELIMITER_COUNT,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.SCALE_NUMBER_DIVISIONS, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.indicatorDelimiterCount?.toString() ?: "4",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = analogSensorTypes,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_INDICATOR_MILTIPLICATOR,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.SCALE_MULTIPLIER, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.indicatorMultiplicator?.toString() ?: "1",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = analogSensorTypes,
            ),
        )

        //--- applies only to (fuel) counter sensors

        formCells += FormBooleanCell(
            name = FIELD_IS_ABSOLUTE_COUNT,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.ACCUMULATIVE_METER, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.isAbsoluteCount ?: true,
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = counterAndSummarySensorTypes,
            ),
        )

        formCells += FormComboCell(
            name = FIELD_IN_OUT_TYPE,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.METERING_TYPE, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.inOutType?.toString() ?: SensorConfigCounter.CALC_TYPE_OUT.toString(),
            values = listOf(
                SensorConfigCounter.CALC_TYPE_IN.toString() to getLocalizedMMSMessage(LocalizedMMSMessages.INCOMING_FILLING_METER, userConfig.lang),
                SensorConfigCounter.CALC_TYPE_OUT.toString() to getLocalizedMMSMessage(LocalizedMMSMessages.OUTGOING_FLOW_METER, userConfig.lang),
            ),
            asRadioButtons = true,
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = counterAndSummarySensorTypes,
            ),
        )

        //--- while they are only used for liquid (fuel) level sensors

        formCells += FormComboCell(
            name = FIELD_CONTAINER_TYPE,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.TANK_TYPE, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.containerType?.toString() ?: SensorConfigLiquidLevel.CONTAINER_TYPE_WORK.toString(),
            values = listOf(
                SensorConfigLiquidLevel.CONTAINER_TYPE_MAIN.toString() to getLocalizedMMSMessage(LocalizedMMSMessages.MAIN_TANK, userConfig.lang),
                SensorConfigLiquidLevel.CONTAINER_TYPE_WORK.toString() to getLocalizedMMSMessage(LocalizedMMSMessages.WORKING_FLOW_TANK, userConfig.lang),
            ),
            asRadioButtons = true,
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = liquidLevelSensorType,
            ),
        )

        //--- applies only to readings of electricity meters

        formCells += FormComboCell(
            name = FIELD_PHASE,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.PHASE, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorEntity?.phase?.toString() ?: "0",
            values = listOf(
                0.toString() to getLocalizedMMSMessage(LocalizedMMSMessages.PHASE_SUM, userConfig.lang),
                1.toString() to "A",
                2.toString() to "B",
                3.toString() to "C",
            ),
            asRadioButtons = true,
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = phasedEnergoSensorTypes,
            ),
        )

        //--- common for geo and discrete sensors ------------------------------------------------------------------------------------------------

        formCells += FormSimpleCell(
            name = FIELD_SCHEME_X,
            caption = "",
            isEditable = false,
            value = sensorEntity?.schemeX?.toString() ?: "0",
        )
        formCells += FormSimpleCell(
            name = FIELD_SCHEME_Y,
            caption = "",
            isEditable = false,
            value = sensorEntity?.schemeY?.toString() ?: "0",
        )

        formCells += FormSimpleCell(
            name = FIELD_CALIBRATION,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.SENSOR_CALIBRATION, userConfig.lang),
            isEditable = changeEnabled,
            value = sensorCalibrationInfo,
            rows = 20,
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = false,
                values = geoSensorType + workSensorType,
            ),
        )

        return formCells
    }

    override fun formActionSave(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        formActionData: Map<String, FormActionData>
    ): FormActionResponse {
        val id = action.id

        val parentObjectId = formActionData[FIELD_OBJECT_ID]?.stringValue?.toIntOrNull() ?: 0
        val parentObjectEntity = objectRepository.findByIdOrNull(parentObjectId)!!

        val descr = formActionData[FIELD_DESCR]?.stringValue?.trim() ?: return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_DESCR to getLocalizedMMSMessage(LocalizedMMSMessages.NO_DESCRIPTION_ENTERED, userConfig.lang)))
        if (descr.isEmpty()) {
            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_DESCR to getLocalizedMMSMessage(LocalizedMMSMessages.NO_DESCRIPTION_ENTERED, userConfig.lang)))
        }
        //--- могу быть одинаковые описания датчиков из разных периодов
        //if (sensorRepository.findByObjAndDescr(parentObjectEntity, descr).any { oe -> oe.id != id }) {
        //    return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_DESCR to "Такое описание уже существует"))
        //}

        val portNum = formActionData[FIELD_PORT_NUM]?.stringValue?.toIntOrNull() ?: return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_PORT_NUM to getLocalizedMMSMessage(LocalizedMMSMessages.NO_ENTRY_NUMBER_ENTERED, userConfig.lang)))
        if (portNum !in 0..65535) {
            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_PORT_NUM to getLocalizedMMSMessage(LocalizedMMSMessages.THE_ENTRY_NUMBER_MUST_BE_BETWEEN_0_AND_65535, userConfig.lang)))
        }

        val recordId = id ?: getNextId { nextId -> sensorRepository.existsById(nextId) }
        val sensorEntity = SensorEntity(
            id = recordId,
            obj = parentObjectEntity,
            name = formActionData[FIELD_NAME]?.stringValue ?: "",
            group = formActionData[FIELD_GROUP]?.stringValue ?: "",
            descr = descr,
            portNum = portNum,
            sensorType = formActionData[FIELD_SENSOR_TYPE]?.stringValue?.toIntOrNull() ?: SensorConfig.SENSOR_GEO,
            begTime = formActionData[FIELD_BEG_TIME]?.dateTimeValue,
            endTime = formActionData[FIELD_END_TIME]?.dateTimeValue,
            serialNo = formActionData[FIELD_SERIAL_NO]?.stringValue ?: "",
            minMovingTime = formActionData[FIELD_MIN_MOVING_TIME]?.stringValue?.toIntOrNull() ?: 1,
            minParkingTime = formActionData[FIELD_MIN_PARKING_TIME]?.stringValue?.toIntOrNull() ?: 300,
            minOverSpeedTime = formActionData[FIELD_MIN_OVER_SPEED_TIME]?.stringValue?.toIntOrNull() ?: 60,
            isAbsoluteRun = formActionData[FIELD_IS_ABSOLUTE_RUN]?.booleanValue ?: true,
            minIgnore = formActionData[FIELD_MIN_IGNORE]?.stringValue?.toDoubleOrNull() ?: 0.0,
            maxIgnore = formActionData[FIELD_MAX_IGNORE]?.stringValue?.toDoubleOrNull() ?: 0.0,
            dim = formActionData[FIELD_DIM]?.stringValue,
            isWorkAboveBorder = formActionData[FIELD_IS_ABOVE_BORDER]?.booleanValue ?: true,
            workOnBorder = formActionData[FIELD_ON_BORDER]?.stringValue?.toDoubleOrNull(),
            workIdleBorder = formActionData[FIELD_IDLE_BORDER]?.stringValue?.toDoubleOrNull(),
            workOverBorder = formActionData[FIELD_OVER_BORDER]?.stringValue?.toDoubleOrNull(),
            workMinOffTime = formActionData[FIELD_MIN_OFF_TIME]?.stringValue?.toIntOrNull() ?: 1,
            workMinOnTime = formActionData[FIELD_MIN_ON_TIME]?.stringValue?.toIntOrNull() ?: 1,
            workMinIdleTime = formActionData[FIELD_MIN_IDLE_TIME]?.stringValue?.toIntOrNull() ?: 1,
            workMinOverTime = formActionData[FIELD_MIN_OVER_TIME]?.stringValue?.toIntOrNull() ?: 1,
            minView = formActionData[FIELD_MIN_VIEW]?.stringValue?.toDoubleOrNull() ?: 0.0,
            maxView = formActionData[FIELD_MAX_VIEW]?.stringValue?.toDoubleOrNull() ?: 100.0,
            minLimit = formActionData[FIELD_MIN_LIMIT]?.stringValue?.toDoubleOrNull() ?: 0.0,
            maxLimit = formActionData[FIELD_MAX_LIMIT]?.stringValue?.toDoubleOrNull() ?: 0.0,
            smoothTime = formActionData[FIELD_SMOOTH_TIME]?.stringValue?.toIntOrNull() ?: 0,
            indicatorDelimiterCount = formActionData[FIELD_INDICATOR_DELIMITER_COUNT]?.stringValue?.toIntOrNull() ?: 4,
            indicatorMultiplicator = formActionData[FIELD_INDICATOR_MILTIPLICATOR]?.stringValue?.toDoubleOrNull() ?: 1.0,
            isAbsoluteCount = formActionData[FIELD_IS_ABSOLUTE_COUNT]?.booleanValue ?: true,
            inOutType = formActionData[FIELD_IN_OUT_TYPE]?.stringValue?.toIntOrNull() ?: SensorConfigCounter.CALC_TYPE_OUT,
            containerType = formActionData[FIELD_CONTAINER_TYPE]?.stringValue?.toIntOrNull() ?: SensorConfigLiquidLevel.CONTAINER_TYPE_WORK,
            phase = formActionData[FIELD_PHASE]?.stringValue?.toIntOrNull() ?: 0,
            schemeX = formActionData[FIELD_SCHEME_X]?.stringValue?.toIntOrNull(),
            schemeY = formActionData[FIELD_SCHEME_Y]?.stringValue?.toIntOrNull(),
        )
        sensorRepository.saveAndFlush(sensorEntity)

        sensorCalibrationRepository.deleteBySensor(sensorEntity)
        val calibrationText = formActionData[FIELD_CALIBRATION]?.stringValue ?: ""
        calibrationText.split('\n').forEach { calibrationPair ->
            val alSensorData = calibrationPair.split('=')
            if (alSensorData.size == 2) {
                val sensorValue = alSensorData[0].trim().toDoubleOrNull()
                val dataValue = alSensorData[1].trim().toDoubleOrNull()

                if (sensorValue != null && dataValue != null) {
                    val sensorCalibrationEntity = SensorCalibrationEntity(
                        id = getNextSensorCalibrationId(),
                        sensor = sensorEntity,
                        sensorValue = sensorValue,
                        dataValue = dataValue,
                    )
                    sensorCalibrationRepository.save(sensorCalibrationEntity)
                }
            }
        }
        sensorCalibrationRepository.flush()

        checkAndCreateSensorTables(entityManager, recordId)

        //--- (re)create DataServer restart flag file
        File(dataServerIniFileName).copyTo(File(dataServerIniFileName + "_"), true)

        return FormActionResponse(
            responseCode = ResponseCode.OK,
            nextAction = action.prevAction?.copy(id = recordId),
        )
    }

    private fun getNextSensorCalibrationId(): Int {
        var nextId: Int
        while (true) {
            nextId = getRandomInt()
            if (nextId == 0) {
                continue
            }
            if (sensorCalibrationRepository.existsById(nextId)) {
                continue
            }
            return nextId
        }
    }

    override fun getFormActionPermissions(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): Triple<Boolean, Boolean, Boolean> {
        val addEnabled = checkFormAddPermission(moduleConfig, userConfig.roles)
        val editEnabled = checkRowPermission(action.module, ActionType.FORM_EDIT, UserRelationEnum.NOBODY, userConfig.roles)
        val deleteEnabled = checkRowPermission(action.module, ActionType.FORM_DELETE, UserRelationEnum.NOBODY, userConfig.roles)

        return Triple(addEnabled, editEnabled, deleteEnabled)
    }

    override fun formActionDelete(userId: Int, id: Int): FormActionResponse {
        deleteSensor(
            entityManager = entityManager,
            sensorRepository = sensorRepository,
            sensorCalibrationRepository = sensorCalibrationRepository,
            sensorId = id,
        )
//        alDependData.add(DependData("MMS_equip_service_shedule", "equip_id", DependData.DELETE))
//        alDependData.add(DependData("MMS_equip_service_history", "equip_id", DependData.DELETE))

        return FormActionResponse(responseCode = ResponseCode.OK)
    }

}

/*
    //--- "оборудование" нельзя добавить, оно является другим отображением "датчика работы оборудования"
    override fun isAddEnabled(): Boolean = aliasConfig.name != "mms_equip"

    override fun addSQLWhere(hsTableRenameList: Set<String>): String {
        var s = super.addSQLWhere(hsTableRenameList)
        if (aliasConfig.name == "mms_equip") {
            s += " AND ${renameTableName(hsTableRenameList, model.modelTableName)}." +
                "${(model as mSensor).columnSensorType.getFieldName()} = ${SensorConfig.SENSOR_WORK} "
        }
        return s
    }
 */