package foatto.server.service

import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.form.FormButton
import foatto.core.model.response.form.FormCellVisibility
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.form.cells.FormBooleanCell
import foatto.core.model.response.form.cells.FormComboCell
import foatto.core.model.response.form.cells.FormFileCell
import foatto.core.model.response.form.cells.FormSimpleCell
import foatto.core.model.response.table.TableCaption
import foatto.core.model.response.table.TablePageButton
import foatto.core.model.response.table.TablePopup
import foatto.core.model.response.table.TableRow
import foatto.core.model.response.table.cell.TableBaseCell
import foatto.core.model.response.table.cell.TableBooleanCell
import foatto.core.model.response.table.cell.TableButtonCell
import foatto.core.model.response.table.cell.TableSimpleCell
import foatto.core.util.getCurrentTimeInt
import foatto.core_mms.AppModuleMMS
import foatto.server.ObjectType
import foatto.server.checkFormAddPermission
import foatto.server.checkRowPermission
import foatto.server.entity.ObjectEntity
import foatto.server.entity.SensorEntity
import foatto.server.getEnabledUserIds
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.model.sensor.SensorConfig
import foatto.server.model.sensor.SensorConfigCounter
import foatto.server.model.sensor.SensorConfigGeo
import foatto.server.model.sensor.SensorConfigLiquidLevel
import foatto.server.repository.ActionLogRepository
import foatto.server.repository.DepartmentRepository
import foatto.server.repository.DeviceRepository
import foatto.server.repository.GroupRepository
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorCalibrationRepository
import foatto.server.repository.SensorRepository
import foatto.server.service.SensorService.Companion.checkAndCreateSensorTables
import foatto.server.util.getNextId
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.io.File

@Service
class ObjectService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val departmentRepository: DepartmentRepository,
    private val groupRepository: GroupRepository,
    private val sensorRepository: SensorRepository,
    private val sensorCalibrationRepository: SensorCalibrationRepository,
    private val deviceRepository: DeviceRepository,
    private val fileStoreService: FileStoreService,
    private val actionLogRepository: ActionLogRepository,
) : MMSService(
    fileStoreService = fileStoreService,
    actionLogRepository = actionLogRepository,
) {

    companion object {
        const val FIELD_ID: String = "id"
        private const val FIELD_USER_ID = "userId"
        const val FIELD_TYPE: String = "type"
        private const val FIELD_IS_DISABLED = "isDisabled"
        private const val FIELD_DISABLE_REASON = "disableReason"
        const val FIELD_NAME: String = "name"
        const val FIELD_MODEL: String = "model"
        private const val FIELD_DEPARTMENT = "department"
        private const val FIELD_GROUP = "group"
        private const val FIELD_INFO = "info"
        private const val FIELD_EMAIL = "eMail"
        private const val FIELD_FILE = "fileId"

        //        private const val FIELD_LAST_ALERT_TIME = "lastAlertTime"
        private const val FIELD_IS_AUTO_WORK_SHIFT_ENABLED = "isAutoWorkShiftEnabled"

        private const val FIELD_OWNER_FULL_NAME = "_ownerFullName"   // псевдополе для селектора
        private const val FIELD_DEPARTMENT_NAME = "_departmentName"   // псевдополе для селектора
        private const val FIELD_GROUP_NAME = "_groupName"             // псевдополе для селектора
    }

    @Value("\${data_server_ini_file}")
    val dataServerIniFileName: String = ""

    override fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<TableCaption> {
        val alColumnInfo = mutableListOf<Pair<String?, String>>()

        if (action.isSelectorMode) {
            alColumnInfo += null to "" // selector button
        }
        alColumnInfo += null to "" // userId
        if (getObjectType(action) == null) {
            alColumnInfo += FIELD_TYPE to "Тип"
        }
        alColumnInfo += FIELD_IS_DISABLED to "Заблокирован"
        alColumnInfo += FIELD_NAME to "Наименование"
        alColumnInfo += FIELD_MODEL to "Модель"
        alColumnInfo += null to "Подразделение"
        alColumnInfo += null to "Группа"

        if (isAdminOnly(userConfig)) {
            alColumnInfo += null to "Файл схемы объекта"
        }

        return getTableColumnCaptionActions(
            action = action,
            alColumnInfo = alColumnInfo,
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

        var currentRowNo: Int? = null
        var row = 0

        val pageRequest = getTableSortedPageRequest(action, Sort.Order(Sort.Direction.ASC, FIELD_NAME))
        val findText = action.findText?.trim() ?: ""

        val objectType = getObjectType(action)

        val enabledUserIds = getEnabledUserIds(action.module, action.type, userConfig.relatedUserIds, userConfig.roles)

        val page: Page<ObjectEntity> = objectRepository.findByTypeAndUserIdInAndFilter(
            type = objectType,
            userIds = enabledUserIds,
            findText = findText,
            pageRequest = pageRequest,
        )
        fillTablePageButtons(action, page.totalPages, pageButtons)
        val objectEntities = page.content

        for (objectEntity in objectEntities) {
            var col = 0

            val rowOwnerShortName = userConfig.shortNames[objectEntity.userId]
            val rowOwnerFullName = userConfig.fullNames[objectEntity.userId]

            val isFormEnabled = checkRowPermission(
                module = action.module,
                actionType = ActionType.MODULE_FORM,
                rowUserRelation = userConfig.relatedUserIds[objectEntity.userId],
                userRoles = userConfig.roles
            )

            val selectorAction = AppAction(
                type = ActionType.FORM_SELECTOR,
                selectorData = mapOf(
                    FIELD_ID to objectEntity.id.toString(),
                    FIELD_NAME to (objectEntity.name ?: "(неизвестно)"),
                ),
            )

            if (action.isSelectorMode) {
                tableCells += getTableSelectorButtonCell(row = row, col = col++, selectorAction = selectorAction)
            }
            tableCells += getTableUserNameCell(
                row = row,
                col = col++,
                userId = userConfig.id,
                rowUserId = objectEntity.userId,
                rowOwnerShortName = rowOwnerShortName,
                rowOwnerFullName = rowOwnerFullName
            )
            if (objectType == null) {
                tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, minWidth = 100, name = objectEntity.type?.getDescr(userConfig.lang) ?: "-")
            }
            tableCells += TableBooleanCell(row = row, col = col++, dataRow = row, minWidth = 100, value = objectEntity.isDisabled ?: false)
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, minWidth = 100, name = objectEntity.name ?: "-")
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, minWidth = 100, name = objectEntity.model ?: "-")
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, minWidth = 100, name = objectEntity.department?.name ?: "-")
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = row, minWidth = 100, name = objectEntity.group?.name ?: "-")
            if (isAdminOnly(userConfig)) {
                tableCells += TableButtonCell(
                    row = row,
                    col = col++,
                    dataRow = row,
                    minWidth = 100,
                    values = getTableFileButtonCellData(objectEntity.fileId),
                )
            }

            val formOpenAction = action.copy(
                type = ActionType.MODULE_FORM,
                id = objectEntity.id,
            )

            val popupDatas = getTablePopupDatas(
                userConfig = userConfig,
                objectId = objectEntity.id,
                isFormEnabled = isFormEnabled,
                formOpenAction = formOpenAction,
            )

            tableRows += TableRow(
                rowAction = if (action.isSelectorMode) {
                    AppAction(
                        type = ActionType.FORM_SELECTOR,
                        selectorData = mapOf(
                            FIELD_ID to objectEntity.id.toString(),
                            FIELD_NAME to (objectEntity.name ?: "(неизвестно)"),
                        ),
                    )
                } else if (isFormEnabled) {
                    formOpenAction
                } else {
                    null
                },
                isRowUrlInNewTab = false,
                tablePopups = popupDatas,
            )

            if (objectEntity.id == action.id) {
                currentRowNo = row
            }

            row++
        }
        return currentRowNo
    }

    private fun getTablePopupDatas(
        userConfig: ServerUserConfig,
        objectId: Int,
        isFormEnabled: Boolean,
        formOpenAction: AppAction,
    ): List<TablePopup> {
        val alPopupData = mutableListOf<TablePopup>()

        if (isFormEnabled) {
            alPopupData += TablePopup(
                action = formOpenAction,
                text = "Открыть",
                inNewTab = false,
            )
        }

        val begTime = getCurrentTimeInt() / 86_400 * 86_400 - userConfig.timeOffset
        val endTime = begTime + 86_400

        getTableTablePopupData(userConfig, AppModuleMMS.DAY_WORK, AppModuleMMS.OBJECT, objectId, alPopupData)

        getTableReportPopupData(userConfig, AppModuleMMS.REPORT_SUMMARY, AppModuleMMS.OBJECT, objectId, begTime, endTime, alPopupData)

        getTableDashboardPopupData(userConfig, AppModuleMMS.OBJECT_SCHEME_DASHBOARD, AppModuleMMS.OBJECT, objectId, alPopupData)
        getTableDashboardPopupData(userConfig, AppModuleMMS.OBJECT_CHART_DASHBOARD, AppModuleMMS.OBJECT, objectId, alPopupData)

//        getTableChartPopupData(userConfig, AppModuleMMS.CHART_LIQUID_LEVEL, AppModuleMMS.OBJECT, id, begTime, endTime, alPopupData)

        getTableMapPopupData(userConfig, AppModuleMMS.MAP_TRACE, AppModuleMMS.OBJECT, objectId, begTime, endTime, alPopupData)

        getTableTablePopupData(userConfig, AppModuleMMS.SENSOR, AppModuleMMS.OBJECT, objectId, alPopupData)
        getTableTablePopupData(userConfig, AppModuleMMS.OBJECT_DATA, AppModuleMMS.OBJECT, objectId, alPopupData)
        getTableTablePopupData(userConfig, AppModuleMMS.DEVICE, AppModuleMMS.OBJECT, objectId, alPopupData)

        return alPopupData
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

        val objectEntity = id?.let {
            //--- TODO: ошибка об отсутствии такой записи
            objectRepository.findByIdOrNull(id) ?: return emptyList()
        }

        val userId = objectEntity?.let {
            objectEntity.userId
        } ?: userConfig.id

        val departmentId = objectEntity?.let {
            objectEntity.department?.id
        } ?: run {
            if (action.parentModule == AppModuleMMS.DEPARTMENT) {
                action.parentId ?: 0
            } else {
                0
            }
        }

        val groupId = objectEntity?.let {
            objectEntity.group?.id
        } ?: run {
            if (action.parentModule == AppModuleMMS.GROUP) {
                action.parentId ?: 0
            } else {
                0
            }
        }

        val changeEnabled = action.id?.let { editEnabled } ?: addEnabled

        formCells += FormComboCell(
            name = FIELD_TYPE,
            caption = "Тип объекта",
            isEditable = changeEnabled,
            value = (objectEntity?.type ?: getObjectType(action) ?: ObjectType.STATIONARY).name,
            values = ObjectType.entries.map { v -> v.name to v.getDescr(userConfig.lang) },
            asRadioButtons = true,
        )
        fillFormUserCells(
            fieldUserId = FIELD_USER_ID,
            fieldOwnerFullName = FIELD_OWNER_FULL_NAME,
            userId = userId,
            userConfig = userConfig,
            changeEnabled = changeEnabled,
            formCells = formCells,
        )
        formCells += FormBooleanCell(
            name = FIELD_IS_DISABLED,
            caption = "Заблокирован",
            isEditable = changeEnabled,
            value = objectEntity?.isDisabled ?: false,
        )
        formCells += FormSimpleCell(
            name = FIELD_DISABLE_REASON,
            caption = "Причина блокировки",
            isEditable = changeEnabled,
            value = objectEntity?.disableReason ?: "",
            visibility = FormCellVisibility(
                name = FIELD_IS_DISABLED,
                state = true,
                values = setOf(true.toString()),
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_NAME,
            caption = "Наименование",
            isEditable = changeEnabled,
            value = objectEntity?.name ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_MODEL,
            caption = "Модель",
            isEditable = changeEnabled,
            value = objectEntity?.model ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_DEPARTMENT,
            caption = "",
            isEditable = false,
            value = departmentId.toString(),
        )
        formCells += FormSimpleCell(
            name = FIELD_DEPARTMENT_NAME,
            caption = "Подразделение",
            isEditable = false,
            value = if (departmentId == 0) {
                "-"
            } else {
                objectEntity?.department?.name ?: "(неизвестно)"
            },
            selectorAction = if (changeEnabled) {
                AppAction(
                    type = ActionType.MODULE_TABLE,
                    module = AppModuleMMS.DEPARTMENT,
                    isSelectorMode = true,
                    selectorPath = mapOf(
                        DepartmentService.FIELD_ID to FIELD_DEPARTMENT,
                        DepartmentService.FIELD_NAME to FIELD_DEPARTMENT_NAME,
                    ),
                    selectorClear = mapOf(
                        FIELD_DEPARTMENT to "0",
                        FIELD_DEPARTMENT_NAME to "",
                    ),
                )
            } else {
                null
            },
        )
        formCells += FormSimpleCell(
            name = FIELD_GROUP,
            caption = "",
            isEditable = false,
            value = groupId.toString(),
        )
        formCells += FormSimpleCell(
            name = FIELD_GROUP_NAME,
            caption = "Группа",
            isEditable = false,
            value = if (groupId == 0) {
                "-"
            } else {
                objectEntity?.group?.name ?: "(неизвестно)"
            },
            selectorAction = if (changeEnabled) {
                AppAction(
                    type = ActionType.MODULE_TABLE,
                    module = AppModuleMMS.GROUP,
                    isSelectorMode = true,
                    selectorPath = mapOf(
                        GroupService.FIELD_ID to FIELD_GROUP,
                        GroupService.FIELD_NAME to FIELD_GROUP_NAME,
                    ),
                    selectorClear = mapOf(
                        FIELD_GROUP to "0",
                        FIELD_GROUP_NAME to "",
                    ),
                )
            } else {
                null
            },
        )
        formCells += FormSimpleCell(
            name = FIELD_INFO,
            caption = "Информация",
            isEditable = changeEnabled,
            value = objectEntity?.info ?: "",
            rows = 5,
        )
        formCells += FormSimpleCell(
            name = FIELD_EMAIL,
            caption = "E-mail",
            isEditable = changeEnabled,
            value = objectEntity?.eMail ?: "",
        )
        formCells += FormFileCell(
            name = FIELD_FILE,
            caption = "Файл схемы объекта",
            isEditable = changeEnabled,
            fileId = objectEntity?.fileId,
            files = getFormFileCellData(objectEntity?.fileId)
        )
        formCells += FormBooleanCell(
            name = FIELD_IS_AUTO_WORK_SHIFT_ENABLED,
            caption = "Автосоздание рабочих смен",
            isEditable = changeEnabled,
            value = objectEntity?.isAutoWorkShiftEnabled ?: false,
        )

        return formCells
    }

    override fun getFormButtons(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, addEnabled: Boolean, editEnabled: Boolean, deleteEnabled: Boolean): List<FormButton> {
        val id = action.id

        val isExistsDepencies: Boolean = if (deleteEnabled) {
            id?.let {
                deviceRepository.findByObjectId(id).isNotEmpty()
            } ?: false
        } else {
            false
        }

        return super.getFormButtons(action, userConfig, moduleConfig, addEnabled, editEnabled, deleteEnabled && !isExistsDepencies)
    }

    override fun formActionSave(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        formActionData: Map<String, FormActionData>
    ): FormActionResponse {
        val id = action.id

        val recordUserId = formActionData[FIELD_USER_ID]?.stringValue?.toIntOrNull() ?: 0

        val name = formActionData[FIELD_NAME]?.stringValue?.trim() ?: return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_NAME to "Не введёно наименование"))
        if (name.isEmpty()) {
            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_NAME to "Не введёно наименование"))
        }
        if (objectRepository.findByUserIdAndName(recordUserId, name).any { oe -> oe.id != id }) {
            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_NAME to "Такое наименование уже существует"))
        }

        val objectType = formActionData[FIELD_TYPE]?.stringValue?.let { s -> ObjectType.valueOf(s) } ?: ObjectType.STATIONARY

        // !!! getNextIntId(arrayOf("MMS_object", "MMS_zone"), arrayOf("id", "id"))
        val recordId = id ?: getNextId { nextId -> objectRepository.existsById(nextId) }
        val objectEntity = ObjectEntity(
            id = recordId,
            userId = recordUserId,
            type = objectType,
            isDisabled = formActionData[FIELD_IS_DISABLED]?.booleanValue == true,
            disableReason = formActionData[FIELD_DISABLE_REASON]?.stringValue,
            name = formActionData[FIELD_NAME]?.stringValue,
            model = formActionData[FIELD_MODEL]?.stringValue,
            department = departmentRepository.findByIdOrNull(formActionData[FIELD_DEPARTMENT]?.stringValue?.toIntOrNull() ?: 0),
            group = groupRepository.findByIdOrNull(formActionData[FIELD_GROUP]?.stringValue?.toIntOrNull() ?: 0),
            info = formActionData[FIELD_INFO]?.stringValue,
            eMail = formActionData[FIELD_EMAIL]?.stringValue,
            fileId = formActionData[FIELD_FILE]?.let { fad ->
                formActionSaveFiles(fad)
            },
            isAutoWorkShiftEnabled = formActionData[FIELD_IS_AUTO_WORK_SHIFT_ENABLED]?.booleanValue == true,
        )
        objectRepository.saveAndFlush(objectEntity)

        id?.let {
            //!!! executeNativeSql(" UPDATE MMS_day_work SET user_id = $recordUserId WHERE object_id = $recordId ")
        } ?: run {
            executeNativeSql(
                entityManager,
                " CREATE TABLE MMS_data_$recordId ( ontime INT NOT NULL, sensor_data BYTEA ) ",
                " CREATE INDEX MMS_data_${recordId}_ontime ON MMS_data_$recordId ( ontime ) ",
            )
        }

        val geoSensors = sensorRepository.findByObjAndPortNumAndSensorType(objectEntity, SensorConfigGeo.PORT_NUM, SensorConfig.SENSOR_GEO)
        when (objectType) {
            ObjectType.MOBILE -> {
                if (geoSensors.isEmpty()) {
                    val recordId = id ?: getNextId { nextId -> sensorRepository.existsById(nextId) }
                    val sensorEntity = SensorEntity(
                        id = recordId,
                        obj = objectEntity,
                        name = "",
                        group = "",
                        descr = SensorConfig.hmSensorDescr[SensorConfig.SENSOR_GEO],
                        portNum = SensorConfigGeo.PORT_NUM,
                        sensorType = SensorConfig.SENSOR_GEO,
                        begTime = getCurrentTimeInt(),
                        endTime = null,
                        serialNo = "",
                        minMovingTime = 1,
                        minParkingTime = 300,
                        minOverSpeedTime = 60,
                        isAbsoluteRun = true,
                        minIgnore = 0.0,
                        maxIgnore = 0.0,
                        dim = null,
                        isWorkAboveBorder = true,
                        workOnBorder = null,
                        workIdleBorder = null,
                        workOverBorder = null,
                        workMinOffTime = 1,
                        workMinOnTime = 1,
                        workMinIdleTime = 1,
                        workMinOverTime = 1,
                        minView = 0.0,
                        maxView = 100.0,
                        minLimit = 0.0,
                        maxLimit = 100.0,
                        smoothTime = 0,
                        indicatorDelimiterCount = 4,
                        indicatorMultiplicator = 1.0,
                        isAbsoluteCount = true,
                        inOutType = SensorConfigCounter.CALC_TYPE_OUT,
                        containerType = SensorConfigLiquidLevel.CONTAINER_TYPE_WORK,
                        phase = 0,
                        schemeX = null,
                        schemeY = null,
                    )
                    sensorRepository.saveAndFlush(sensorEntity)
                    checkAndCreateSensorTables(entityManager, recordId)

                    //--- (re)create DataServer restart flag file
                    File(dataServerIniFileName).copyTo(File(dataServerIniFileName + "_"), true)
                }
            }

            ObjectType.STATIONARY -> {
                if (geoSensors.isNotEmpty()) {
                    geoSensors.forEach { sensorEntity ->
                        SensorService.deleteSensor(entityManager, sensorRepository, sensorCalibrationRepository, sensorEntity.id)
                    }
                }
            }
        }

        return FormActionResponse(
            responseCode = ResponseCode.OK,
            nextAction = action.prevAction?.copy(id = recordId),
        )
    }

    override fun formActionDelete(userId: Int, id: Int): FormActionResponse {
        //--- remove all depended sensors & datas
        objectRepository.findByIdOrNull(id)?.let { objectEntity ->
            fileStoreService.deleteFile(objectEntity.fileId, null)
            sensorRepository.findByObj(objectEntity).forEach { sensorEntity ->
                SensorService.deleteSensor(
                    entityManager = entityManager,
                    sensorRepository = sensorRepository,
                    sensorCalibrationRepository = sensorCalibrationRepository,
                    sensorId = sensorEntity.id,
                )
            }
        }

        objectRepository.deleteById(id)
        executeNativeSql(entityManager, " DROP TABLE MMS_data_$id ")

        /*
            conn.executeUpdate(" DELETE FROM MMS_work_shift_data WHERE shift_id IN ( SELECT id FROM MMS_work_shift WHERE object_id = $id ) ")
            conn.executeUpdate(" DELETE FROM MMS_work_shift WHERE object_id = $id ")

            conn.executeUpdate(" DELETE FROM MMS_equip_service_shedule WHERE equip_id IN ( SELECT id FROM MMS_sensor WHERE object_id = $id ) ")
            conn.executeUpdate(" DELETE FROM MMS_equip_service_history WHERE equip_id IN ( SELECT id FROM MMS_sensor WHERE object_id = $id ) ")
         */
        return FormActionResponse(responseCode = ResponseCode.OK)
    }

    override fun getFormActionPermissions(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): Triple<Boolean, Boolean, Boolean> {
        val id = action.id

        val addEnabled = checkFormAddPermission(moduleConfig, userConfig.roles)

        val objectEntity = id?.let {
            objectRepository.findByIdOrNull(id) ?: return Triple(addEnabled, false, false)
        }

        val editEnabled = checkRowPermission(action.module, ActionType.FORM_EDIT, userConfig.relatedUserIds[objectEntity?.userId], userConfig.roles)
        val deleteEnabled = checkRowPermission(action.module, ActionType.FORM_DELETE, userConfig.relatedUserIds[objectEntity?.userId], userConfig.roles)

        return Triple(addEnabled, editEnabled, deleteEnabled)
    }

    private fun getObjectType(action: AppAction): ObjectType? =
        action.params[FIELD_TYPE]?.let { objectTypeStr ->
            try {
                ObjectType.valueOf(objectTypeStr)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
}
/*
    companion object {
        const val PERM_REMOTE_CONTROL: String = "remote_control"
        const val PERM_SCHEME_SENSOR_MOVE: String = "sensor_move"
    }

    override fun definePermission() {
        super.definePermission()
        //--- права доступа на (дистанционное) управление объектом
        alPermission.add(Pair(PERM_REMOTE_CONTROL, "20 Remote Control"))
        //--- права доступа на перемещение датчиков по схеме объекта
        alPermission.add(Pair(PERM_SCHEME_SENSOR_MOVE, "21 Scheme Sensor Move"))
    }

    override fun getTableColumnStyle(isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell) {
        super.getTableColumnStyle(isNewRow, hmColumnData, column, tci)

        val md = model as mObject
        if (column == md.columnObjectName) {
            tci.foreColorType = TableCellForeColorType.DEFINED

            if ((hmColumnData[md.columnDisabled] as DataBoolean).value) {
                tci.foreColor = TABLE_CELL_FORE_COLOR_DISABLED
            } else {
                val id = (hmColumnData[model.columnId] as DataInt).intValue

                val rs = conn.executeQuery(" SELECT MAX(ontime) FROM MMS_data_${id} ")
                val lastDataTime = if (rs.next()) {
                    rs.getInt(1)
                } else {
                    0
                }
                rs.close()

                //--- нет данных больше суток - критично
                if (getCurrentTimeInt() - lastDataTime > 1 * 24 * 60 * 60) {
                    tci.foreColor = TABLE_CELL_FORE_COLOR_CRITICAL
                }
                //--- нет данных больше часа - предупреждение + начинается оповещение по e-mail
                else if (getCurrentTimeInt() - lastDataTime > 1 * 60 * 60) {
                    tci.foreColor = TABLE_CELL_FORE_COLOR_WARNING
                }
                //--- всё нормально
                else {
                    tci.foreColor = TABLE_CELL_FORE_COLOR_NORMAL
                }
            }
        }
    }
 */