package foatto.server.service.report

import foatto.core.i18n.getLocalizedMessage
import foatto.core.model.request.FormActionData
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.ActionLogRepository
import foatto.server.repository.ObjectRepository
import foatto.server.service.CalcService
import foatto.server.service.FileStoreService
import jxl.write.Label
import jxl.write.WritableSheet
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
    objectRepository = objectRepository,
    calcService = calcService,
    fileStoreService = fileStoreService,
    actionLogRepository = actionLogRepository,
    isUseGroupField = false,
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

        val objectEntities = loadObjectEntities(userConfig, parentObjectId)

        var countNN = 1
        for (objectEntity in objectEntities) {
            //--- первая строка: порядковый номер и наименование объекта
            sheet.addCell(Label(0, offsY, (countNN++).toString(), wcfNN))
            offsY = addGroupTitle(sheet, offsY, getObjectGroupTitle(userConfig, objectEntity))

            offsY = outWorkBlock(
                userConfig = userConfig,
                objectEntity = objectEntity,
                begTime = begTime,
                endTime = endTime,
                offsY = offsY,
                sheet = sheet,
            )
        }
        outReportTrail(sheet, offsY, userConfig)
    }

}