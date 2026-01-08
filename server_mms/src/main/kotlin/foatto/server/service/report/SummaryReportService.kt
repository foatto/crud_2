package foatto.server.service.report

import foatto.core.ActionType
import foatto.core.i18n.getLocalizedMessage
import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.form.FormDateTimeCellMode
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.form.cells.FormDateTimeCell
import foatto.core.model.response.form.cells.FormSimpleCell
import foatto.core_mms.AppModuleMMS
import foatto.server.entity.ObjectEntity
import foatto.server.getEnabledUserIds
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.ActionLogRepository
import foatto.server.repository.ObjectRepository
import foatto.server.service.AbstractObjectService
import foatto.server.service.CalcService
import foatto.server.service.FileStoreService
import jxl.write.Label
import jxl.write.WritableSheet
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Service
class SummaryReportService(
    private val objectRepository: ObjectRepository,
    private val calcService: CalcService,
    private val fileStoreService: FileStoreService,
    private val actionLogRepository: ActionLogRepository,
) : AbstractPeriodSummaryService(
    fileStoreService = fileStoreService,
    actionLogRepository = actionLogRepository,
) {

    companion object {
        private const val FIELD_OBJECT_ID = "obj.id"
        private const val FIELD_OBJECT_NAME = "obj.name"
        private const val FIELD_OBJECT_MODEL = "obj.model"

        private const val FIELD_BEGIN_DATE_TIME = "_begin_date_time"
        private const val FIELD_END_DATE_TIME = "_end_date_time"

//        private const val FIELD_OUT_TROUBLES = "_out_troubles"
//
//        private const val FIELD_OUT_GROUP_SUM = "_out_group_sum"
//        private const val FIELD_SUM_ONLY = "_sum_only"
//        private const val FIELD_SUM_USER = "_sum_user"
//        private const val FIELD_SUM_OBJECT = "_sum_object"
    }

    override fun getFormCells(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        addEnabled: Boolean,
        editEnabled: Boolean
    ): List<FormBaseCell> {

        val formCells = mutableListOf<FormBaseCell>()

        val parentObjectId = getParentObjectId(action)
        val parentObjectEntity = parentObjectId?.let {
            objectRepository.findByIdOrNull(parentObjectId)
        }

        formCells += FormSimpleCell(
            name = FIELD_OBJECT_ID,
            caption = "",
            isEditable = false,
            value = parentObjectEntity?.id?.toString() ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_OBJECT_NAME,
            caption = "Наименование",
            isEditable = false,
            value = parentObjectEntity?.name ?: "",
            selectorAction = AppAction(
                type = ActionType.MODULE_TABLE,
                module = AppModuleMMS.ALL_OBJECT,
                isSelectorMode = true,
                selectorPath = mapOf(
                    AbstractObjectService.FIELD_ID to FIELD_OBJECT_ID,
                    AbstractObjectService.FIELD_NAME to FIELD_OBJECT_NAME,
                    AbstractObjectService.FIELD_MODEL to FIELD_OBJECT_MODEL,
                ),
                selectorClear = mapOf(
                    FIELD_OBJECT_ID to "",
                    FIELD_OBJECT_NAME to "",
                    FIELD_OBJECT_MODEL to "",
                ),
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_OBJECT_MODEL,
            caption = "Модель",
            isEditable = false,
            value = parentObjectEntity?.model ?: "",
        )

        formCells += FormDateTimeCell(
            name = FIELD_BEGIN_DATE_TIME,
            caption = "Дата/время начала периода",
            isEditable = true,
            mode = FormDateTimeCellMode.DMYHMS,
            value = action.begTime,
        )
        formCells += FormDateTimeCell(
            name = FIELD_END_DATE_TIME,
            caption = "Дата/время окончания периода",
            isEditable = true,
            mode = FormDateTimeCellMode.DMYHMS,
            value = action.endTime,
        )

//        formCells += FormBooleanCell(
//            name = FIELD_OUT_TROUBLES,
//            caption = "Выводить неисправности",
//            isEditable = true,
//            value = false,
//        )
//
//        formCells += FormBooleanCell(
//            name = FIELD_OUT_GROUP_SUM,
//            caption = "Выводить суммы по группам",
//            isEditable = true,
//            value = false,
//        )
//        formCells += FormBooleanCell(
//            name = FIELD_SUM_ONLY,
//            caption = "Выводить только суммы",
//            isEditable = true,
//            value = false,
//        )
//        formCells += FormBooleanCell(
//            name = FIELD_SUM_USER,
//            caption = "Выводить суммы по владельцам",
//            isEditable = true,
//            value = false,
//        )
//        formCells += FormBooleanCell(
//            name = FIELD_SUM_OBJECT,
//            caption = "Выводить суммы по объектам",
//            isEditable = true,
//            value = false,
//        )

        return formCells
    }

    override fun getReport(
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        formActionData: Map<String, FormActionData>,
        sheet: WritableSheet,
    ) {
        val parentObjectId = formActionData[FIELD_OBJECT_ID]?.stringValue?.toIntOrNull()

        val begDateTime = formActionData[FIELD_BEGIN_DATE_TIME]?.dateTimeValue ?: return
        val endDateTime = formActionData[FIELD_END_DATE_TIME]?.dateTimeValue ?: return

        /*
        private const val FIELD_OUT_TROUBLES = "_out_troubles"

        private const val FIELD_OUT_GROUP_SUM = "_out_group_sum"
        private const val FIELD_SUM_ONLY = "_sum_only"
        private const val FIELD_SUM_USER = "_sum_user"
        private const val FIELD_SUM_OBJECT = "_sum_object"
         */

        defineFormats(8, 2, 0)

        defineSummaryReportHeaders(sheet)

        var offsY = fillReportTitle(
            userConfig = userConfig,
            title = getLocalizedMessage(moduleConfig.captions, userConfig.lang),
            begDateTime = begDateTime,
            endDateTime = endDateTime,
            sheet = sheet,
            offsX = 1,
        )

        val objectEntities = mutableListOf<ObjectEntity>()
        parentObjectId?.let {
            objectRepository.findByIdOrNull(parentObjectId)?.let { parentObjectEntity ->
                objectEntities += parentObjectEntity
            }
        } ?: run {
            val enabledUserIds = getEnabledUserIds(AppModuleMMS.ALL_OBJECT, ActionType.MODULE_TABLE, userConfig.relatedUserIds, userConfig.roles)
            objectEntities += objectRepository.findByUserIdIn(enabledUserIds)
        }

//        val allSumCollector = ReportSumCollector()
        var countNN = 1
        for (objectEntity in objectEntities) {
            val rowOwnerShortName = userConfig.shortNames[objectEntity.userId]
            val rowOwnerFullName = userConfig.fullNames[objectEntity.userId]
            val userName = if (objectEntity.userId == null) {
                null
            } else if (objectEntity.userId == 0) {
                null
            } else if (objectEntity.userId == userConfig.id) {
                null
            } else if (!rowOwnerShortName.isNullOrEmpty()) {
                rowOwnerShortName
            } else {
                rowOwnerFullName ?: "(неизвестный пользователь)"
            }
            var groupTitle = ""
            if (!userName.isNullOrBlank()) {
                groupTitle += userName + '\n'
            }
            if (!objectEntity.name.isNullOrBlank()) {
                groupTitle += objectEntity.name + '\n'
            }
            if (!objectEntity.model.isNullOrBlank()) {
                groupTitle += objectEntity.model + ", "
            }
            if (!objectEntity.department?.name.isNullOrBlank()) {
                groupTitle += objectEntity.department.name + ", "
            }
            if (!objectEntity.group?.name.isNullOrBlank()) {
                groupTitle += objectEntity.group.name + ", "
            }

            val works = calcService.calcWorks(objectEntity, begDateTime, endDateTime).sortedBy { wcd -> wcd.sensorEntity.descr ?: "-" }
            val usings = calcService.calcUsings(objectEntity, begDateTime, endDateTime).sortedBy { ccd -> ccd.sensorEntity.descr ?: "-" }
            val energos = calcService.calcEnergos(objectEntity, begDateTime, endDateTime).sortedBy { ccd -> ccd.sensorEntity.descr ?: "-" }
            val liquidLevels = calcService.calcLiquidLevels(objectEntity, begDateTime, endDateTime).sortedBy { acd -> acd.sensorEntity.descr ?: "-" }
            val temperatures = calcService.calcTemperatures(objectEntity, begDateTime, endDateTime).sortedBy { acd -> acd.sensorEntity.descr ?: "-" }
            val densities = calcService.calcDensities(objectEntity, begDateTime, endDateTime).sortedBy { acd -> acd.sensorEntity.descr ?: "-" }

//            allSumCollector.add(null, objectCalc)

            //--- первая строка: порядковый номер и наименование объекта
            sheet.addCell(Label(0, offsY, (countNN++).toString(), wcfNN))
            offsY = addGroupTitle(sheet, offsY, groupTitle)

            offsY = outBlock(
                sheet = sheet,
                aOffsY = offsY,
                works = works,
                usings = usings,
                energos = energos,
                liquidLevels = liquidLevels,
                temperatures = temperatures,
                densities = densities,
//                troubles = troubles,
//                isOutGroupSum = reportOutGroupSum,
            )
        }
        outReportTrail(sheet, offsY, userConfig)
    }
    /*
                val troubles = if (reportOutTroubles) {
                    val (alRawTime, alRawData) = ObjectCalc.loadAllSensorData(conn, objectConfig, begTime, endTime)
                    val t = ChartElementDTO(ChartElementTypeDTO.TEXT, 0, 0, false)
                    sdcAbstractAnalog.checkCommonTrouble(alRawTime, alRawData, objectConfig, begTime, endTime, t)
                    //--- ловим ошибки с датчиков уровня топлива
                    objectConfig.hmSensorConfig[SensorConfig.SENSOR_LIQUID_LEVEL]?.values?.forEach { sc ->
                        sdcLiquid.checkLiquidLevelSensorTrouble(
                            alRawTime = alRawTime,
                            alRawData = alRawData,
                            sca = sc as SensorConfigAnalogue,
                            begTime = begTime,
                            endTime = endTime,
                            aText = t,
                        )
                    }
                    t
                } else {
                    null
                }

            }

            sheet.addCell(Label(0, offsY, "ИТОГО общее", wcfCellCBStdYellow))
            sheet.mergeCells(0, offsY, getColumnCount(1), offsY + 2)
            offsY += 4

            offsY = outSumData(sheet, offsY, allSumCollector.sumUser, true, null)
     */

}