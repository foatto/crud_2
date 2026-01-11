package foatto.server.service.report

import foatto.core.ActionType
import foatto.core.i18n.getLocalizedMessage
import foatto.core.model.request.FormActionData
import foatto.core.util.getTimeZone
import foatto.core_mms.AppModuleMMS
import foatto.server.getEnabledUserIds
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.ActionLogRepository
import foatto.server.repository.DayWorkRepository
import foatto.server.repository.ObjectRepository
import foatto.server.service.CalcService
import foatto.server.service.FileStoreService
import jxl.write.Label
import jxl.write.WritableSheet
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import org.springframework.stereotype.Service
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Service
class DayWorkReportService(
    private val objectRepository: ObjectRepository,
    private val dayWorkRepository: DayWorkRepository,
    private val calcService: CalcService,
    private val fileStoreService: FileStoreService,
    private val actionLogRepository: ActionLogRepository,
) : AbstractPeriodSummaryService(
    objectRepository = objectRepository,
    calcService = calcService,
    fileStoreService = fileStoreService,
    actionLogRepository = actionLogRepository,
    isUseGroupField = true,
) {

    override fun getReport(
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        formActionData: Map<String, FormActionData>,
        sheet: WritableSheet,
    ) {
        val parentObjectId = formActionData[FIELD_OBJECT_ID]?.stringValue?.toIntOrNull()
        val begTime = formActionData[FIELD_BEGIN_DATE_TIME]?.dateTimeValue ?: return
        val endTime = formActionData[FIELD_END_DATE_TIME]?.dateTimeValue ?: return
        val isGroupByObject = formActionData[FIELD_GROUP_BY]?.stringValue == GROUP_BY_OBJECT

        val timeZone = getTimeZone(userConfig.timeOffset)
        val enabledDayWorkUserIds = getEnabledUserIds(AppModuleMMS.DAY_ALL_WORK, ActionType.MODULE_TABLE, userConfig.relatedUserIds, userConfig.roles)

        defineFormats(8, 2, 0)
        defineSummaryReportHeaders(sheet)

        var offsY = fillReportTitle(
            userConfig = userConfig,
            title = getLocalizedMessage(moduleConfig.captions, userConfig.lang),
            begTime = begTime,
            endTime = endTime,
            sheet = sheet,
            offsX = 1,
        )

        if (isGroupByObject) {
            var countNN = 1
            val objectEntities = loadObjectEntities(userConfig, parentObjectId)
            for (objectEntity in objectEntities) {
                sheet.addCell(Label(0, offsY, (countNN++).toString(), wcfNN))
                offsY = addGroupTitle(sheet, offsY, getObjectGroupTitle(userConfig, objectEntity))

                var countSubNN = 1
                val dayWorkEntities = dayWorkRepository.findByObjAndUserIdIn(objectEntity, enabledDayWorkUserIds, begTime, endTime)
                for (dayWorkEntity in dayWorkEntities) {
                    //val dayObjectEntity = dayWorkEntity.obj ?: continue
                    val dayEntity = dayWorkEntity.day ?: continue
                    val ye = dayEntity.ye ?: continue
                    val mo = dayEntity.mo ?: continue
                    val da = dayEntity.da ?: continue

                    sheet.addCell(Label(0, offsY, "${countNN - 1}.${countSubNN++}", wcfNN))
                    offsY = addSubGroupTitle(sheet, offsY, getDateEntityDMYString(dayEntity))

                    val dayBegTime = LocalDateTime(ye, mo, da, 0, 0).toInstant(timeZone).epochSeconds.toInt()
                    val dayEndTime = dayBegTime + 86_400
                    offsY = outWorkBlock(
                        objectEntity = objectEntity,
                        begTime = dayBegTime,
                        endTime = dayEndTime,
                        offsY = offsY,
                        sheet = sheet,
                    )
                }
            }
        } else {
            var lastDateTimeGroup = ""
            var countNN = 1
            var countSubNN = 1
            val dayWorkEntities = dayWorkRepository.findByObjAndUserIdIn(null, enabledDayWorkUserIds, begTime, endTime)
            for (dayWorkEntity in dayWorkEntities) {
                val dayObjectEntity = dayWorkEntity.obj ?: continue
                val dayEntity = dayWorkEntity.day ?: continue
                val ye = dayEntity.ye ?: continue
                val mo = dayEntity.mo ?: continue
                val da = dayEntity.da ?: continue

                val dateTimeGroup = getDateEntityDMYString(dayEntity)
                if (dateTimeGroup != lastDateTimeGroup) {
                    sheet.addCell(Label(0, offsY, (countNN++).toString(), wcfNN))
                    offsY = addGroupTitle(sheet, offsY, dateTimeGroup)

                    lastDateTimeGroup = dateTimeGroup
                    countSubNN = 1
                }

                sheet.addCell(Label(0, offsY, "${countNN - 1}.${countSubNN++}", wcfNN))
                offsY = addSubGroupTitle(sheet, offsY, getObjectGroupTitle(userConfig, dayObjectEntity))

                val dayBegTime = LocalDateTime(ye, mo, da, 0, 0).toInstant(timeZone).epochSeconds.toInt()
                val dayEndTime = dayBegTime + 86_400
                offsY = outWorkBlock(
                    objectEntity = dayObjectEntity,
                    begTime = dayBegTime,
                    endTime = dayEndTime,
                    offsY = offsY,
                    sheet = sheet,
                )
            }
        }
        outReportTrail(sheet, offsY, userConfig)
    }

}