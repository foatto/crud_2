package foatto.server.service.report

import foatto.core.ActionType
import foatto.core.i18n.LocalizedMessages
import foatto.core.i18n.getLocalizedMessage
import foatto.core.model.AppAction
import foatto.core.model.response.form.FormDateTimeCellMode
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.form.cells.FormComboCell
import foatto.core.model.response.form.cells.FormDateTimeCell
import foatto.core.model.response.form.cells.FormSimpleCell
import foatto.core.util.getPrecision
import foatto.core_mms.AppModuleMMS
import foatto.core_mms.i18n.LocalizedMMSMessages
import foatto.core_mms.i18n.getLocalizedMMSMessage
import foatto.server.ObjectType
import foatto.server.calc.AnalogueCalcData
import foatto.server.calc.CounterCalcData
import foatto.server.calc.WorkCalcData
import foatto.server.entity.ObjectEntity
import foatto.server.entity.SensorEntity
import foatto.server.getEnabledUserIds
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.model.sensor.SensorConfigCounter
import foatto.server.model.sensor.SensorConfigLiquidLevel
import foatto.server.repository.ActionLogRepository
import foatto.server.repository.ObjectRepository
import foatto.server.service.AbstractObjectService
import foatto.server.service.CalcService
import foatto.server.service.FileStoreService
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import org.springframework.data.repository.findByIdOrNull

abstract class AbstractPeriodSummaryService(
    private val objectRepository: ObjectRepository,
    private val calcService: CalcService,
    private val fileStoreService: FileStoreService,
    private val actionLogRepository: ActionLogRepository,
    private val isUseGroupField: Boolean,
) : MMSReportService(
    fileStoreService = fileStoreService,
    actionLogRepository = actionLogRepository,
) {

    companion object {
        protected const val FIELD_OBJECT_ID = "obj.id"
        protected const val FIELD_OBJECT_NAME = "obj.name"
        protected const val FIELD_OBJECT_MODEL = "obj.model"

        protected const val FIELD_BEGIN_DATE_TIME = "_begin_date_time"
        protected const val FIELD_END_DATE_TIME = "_end_date_time"

        protected const val FIELD_GROUP_BY = "_out_group_by"

//        private const val FIELD_OUT_TROUBLES = "_out_troubles"

        protected const val GROUP_BY_OBJECT = "group_by_object"
        protected const val GROUP_BY_DATE = "group_by_date"
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
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.NAME, userConfig.lang),
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
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.MODEL, userConfig.lang),
            isEditable = false,
            value = parentObjectEntity?.model ?: "",
        )

        formCells += FormDateTimeCell(
            name = FIELD_BEGIN_DATE_TIME,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.START_OF_PERIOD, userConfig.lang),
            isEditable = true,
            mode = FormDateTimeCellMode.DMYHMS,
            value = action.begTime,
        )
        formCells += FormDateTimeCell(
            name = FIELD_END_DATE_TIME,
            caption = getLocalizedMMSMessage(LocalizedMMSMessages.END_OF_PERIOD, userConfig.lang),
            isEditable = true,
            mode = FormDateTimeCellMode.DMYHMS,
            value = action.endTime,
        )

        if (isUseGroupField) {
            formCells += FormComboCell(
                name = FIELD_GROUP_BY,
                caption = getLocalizedMMSMessage(LocalizedMMSMessages.GROUPING, userConfig.lang),
                isEditable = getParentObjectId(action) == null,
                asRadioButtons = true,
                value = GROUP_BY_OBJECT,
                values = listOf(
                    GROUP_BY_OBJECT to getLocalizedMMSMessage(LocalizedMMSMessages.BY_OBJECTS, userConfig.lang),
                    GROUP_BY_DATE to getLocalizedMMSMessage(LocalizedMMSMessages.BY_DATES, userConfig.lang),
                ),
            )
        }

//        formCells += FormBooleanCell(
//            name = FIELD_OUT_TROUBLES,
//            caption = "Выводить неисправности",
//            isEditable = true,
//            value = false,
//        )

        return formCells
    }

    protected fun loadObjectEntities(userConfig: ServerUserConfig, parentObjectId: Int?): List<ObjectEntity> {
        val objectEntities = mutableListOf<ObjectEntity>()
        parentObjectId?.let {
            objectRepository.findByIdOrNull(parentObjectId)?.let { parentObjectEntity ->
                objectEntities += parentObjectEntity
            }
        } ?: run {
            val enabledUserIds = getEnabledUserIds(AppModuleMMS.ALL_OBJECT, ActionType.MODULE_TABLE, userConfig.relatedUserIds, userConfig.roles)
            objectEntities += objectRepository.findByUserIdIn(enabledUserIds)
        }
        return objectEntities
    }

    protected fun getObjectGroupTitle(userConfig: ServerUserConfig, objectEntity: ObjectEntity): String {
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
            rowOwnerFullName ?: getLocalizedMessage(LocalizedMessages.UNKNOWN_USER, userConfig.lang)
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

        return groupTitle
    }

    override fun setPrintOptions() {
        printPaperSize = PaperSize.A4

        printPageOrientation = PageOrientation.PORTRAIT

        printMarginLeft = 20
        printMarginRight = 10
        printMarginTop = 10
        printMarginBottom = 10

        printKeyX = 0.0
        printKeyY = 0.0
        printKeyW = 1.0
        printKeyH = 2.0
    }

    protected fun defineSummaryReportHeaders(sheet: WritableSheet) {
        val alDim = ArrayList<Int>()

        //--- setting the sizes of headers (total width = 90 for A4-portrait margins of 10 mm)
        //--- setting the sizes of headers (total width = 140 for A4-landscape margins of 10 mm)
        alDim.add(5)    // row no
        alDim.add(52)    // name
        //--- further, depending on options and the presence of a geo-sensor, data can be displayed
        //--- in 5 to 9 columns of equal width
        //--- 3 = 2 рабочих столбца + 1 столбец под комментарии
        repeat(3) {
            alDim.add(11)   // 11 вместо 9, т.к. на 9-ке плохо умещаются длинные заголовки столбцов ("Температура", например)
        }

        for (i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }
    }

    protected fun addGroupTitle(sheet: WritableSheet, aOffsY: Int, title: String): Int {
        var offsY = aOffsY
        sheet.addCell(Label(1, offsY, title, wcfCellCBStdYellow))
        sheet.mergeCells(1, offsY, 4, offsY + 2)
        offsY += 4
        return offsY
    }

    protected fun addSubGroupTitle(sheet: WritableSheet, aOffsY: Int, title: String): Int {
        var offsY = aOffsY
        sheet.addCell(Label(1, offsY, title, wcfCellCB))
        sheet.mergeCells(1, offsY, 4, offsY)
        offsY += 2
        return offsY
    }

    protected fun outWorkBlock(
        userConfig: ServerUserConfig,
        objectEntity: ObjectEntity,
        begTime: Int,
        endTime: Int,
        offsY: Int,
        sheet: WritableSheet,
    ): Int {
        val run = if (objectEntity.type == ObjectType.MOBILE) {
            calcService.calcRun(objectEntity, begTime, endTime)
        } else {
            null
        }
        val works = calcService.calcWorks(objectEntity, begTime, endTime).sortedBy { wcd -> wcd.sensorEntity.descr ?: "-" }
        val usings = calcService.calcUsings(objectEntity, begTime, endTime).sortedBy { ccd -> ccd.sensorEntity.descr ?: "-" }
        val energos = calcService.calcEnergos(objectEntity, begTime, endTime).sortedBy { ccd -> ccd.sensorEntity.descr ?: "-" }
        val liquidLevels = calcService.calcLiquidLevels(objectEntity, begTime, endTime).sortedBy { acd -> acd.sensorEntity.descr ?: "-" }
        val temperatures = calcService.calcTemperatures(objectEntity, begTime, endTime).sortedBy { acd -> acd.sensorEntity.descr ?: "-" }
        val densities = calcService.calcDensities(objectEntity, begTime, endTime).sortedBy { acd -> acd.sensorEntity.descr ?: "-" }

        return outBlock(
            userConfig = userConfig,
            sheet = sheet,
            aOffsY = offsY,
            run = run,
            works = works,
            usings = usings,
            energos = energos,
            liquidLevels = liquidLevels,
            temperatures = temperatures,
            densities = densities,
            //                troubles = troubles,
        )
    }

    private fun outBlock(
        userConfig: ServerUserConfig,
        sheet: WritableSheet,
        aOffsY: Int,
        run: Double?,
        works: List<WorkCalcData>,
        usings: List<CounterCalcData>,
        energos: List<CounterCalcData>,
        liquidLevels: List<AnalogueCalcData>,
        temperatures: List<AnalogueCalcData>,
        densities: List<AnalogueCalcData>,
//        troubles: ChartElementDTO?,
//        isOutGroupSum: Boolean,
    ): Int {
        var offsY = aOffsY

//                if (scg.isUseSpeed) {
//                    sheet.addCell(Label(offsX, offsY, "Время", wcfCaptionHC))
//                    sheet.mergeCells(offsX, offsY, offsX + 4, offsY)
//                    sheet.addCell(Label(offsX, offsY + 1, "выезда", wcfCaptionHC))
//                    sheet.addCell(Label(offsX + 1, offsY + 1, "заезда", wcfCaptionHC))
//                    sheet.addCell(Label(offsX + 2, offsY + 1, "в пути", wcfCaptionHC))
//                    sheet.addCell(Label(offsX + 3, offsY + 1, "в движении", wcfCaptionHC))
//                    sheet.addCell(Label(offsX + 4, offsY + 1, "на стоянках", wcfCaptionHC))
//                    sheet.addCell(Label(offsX + 5, offsY, "Кол-во стоянок", wcfCaptionHC))
//                    sheet.mergeCells(offsX + 5, offsY, offsX + 5, offsY + 1)
//                    offsX += 6
//                }
//                if (scg.isUseRun) {
//                    sheet.addCell(Label(offsX, offsY, "Сред. расх. [на 100 км]", wcfCaptionHC))
//                    sheet.mergeCells(offsX, offsY, offsX, offsY + 1)
//                    offsX++
//                }
//                offsY += 2
//
//                if (scg.isUseSpeed) {
//                    sheet.addCell(Label(offsX++, offsY, objectCalc.sGeoOutTime, wcfCellC))
//                    sheet.addCell(Label(offsX++, offsY, objectCalc.sGeoInTime, wcfCellC))
//                    sheet.addCell(Label(offsX++, offsY, objectCalc.sGeoWayTime, wcfCellC))
//                    sheet.addCell(Label(offsX++, offsY, objectCalc.sGeoMovingTime, wcfCellC))
//                    sheet.addCell(Label(offsX++, offsY, objectCalc.sGeoParkingTime, wcfCellC))
//                    sheet.addCell(Label(offsX++, offsY, objectCalc.sGeoParkingCount, wcfCellC))
//                }
//                if (scg.isUseRun) {
//                    val tmLiquidUsing = objectCalc.tmGroupSum[scg.group]?.tmLiquidUsing
//                    if (tmLiquidUsing?.size == 1 && objectCalc.gcd!!.run > 0.0) {
//                        sheet.addCell(getNumberCell(offsX++, offsY, 100.0 * tmLiquidUsing[tmLiquidUsing.firstKey()]!! / objectCalc.gcd!!.run, 1, wcfCellC))
//                    } else {
//                        sheet.addCell(Label(offsX++, offsY, "-", wcfCellC))
//                    }
//                }
//                offsY++

        run?.let {
            sheet.addCell(Label(1, offsY, getLocalizedMMSMessage(LocalizedMMSMessages.MILEAGE_UNITS, userConfig.lang), wcfCaptionHC))
            offsY++
            sheet.addCell(getNumberCell(1, offsY, run / 1000.0, 1, wcfCellC))
            offsY += 2
        }

        //--- report on sensors of equipment operation
        if (works.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, getLocalizedMMSMessage(LocalizedMMSMessages.EQUIPMENT, userConfig.lang), wcfCaptionHC))
            sheet.addCell(Label(2, offsY, getLocalizedMMSMessage(LocalizedMMSMessages.OPERATING_TIME, userConfig.lang), wcfCaptionHC))
            offsY++
            for (wcd in works) {
                sheet.addCell(Label(1, offsY, wcd.sensorEntity.descr ?: "-", wcfCellC))
                sheet.addCell(getNumberCell(2, offsY, wcd.onTime.toDouble() / 3600.0, 1, wcfCellC))
                offsY++
            }
            offsY++
        }

        val inCounters = usings.filter { ccd ->
            ccd.sensorEntity.inOutType == SensorConfigCounter.CALC_TYPE_IN
        }
        if (inCounters.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, getLocalizedMMSMessage(LocalizedMMSMessages.INCOMING_METERS, userConfig.lang), wcfCaptionHC))
            sheet.addCell(Label(2, offsY, getLocalizedMMSMessage(LocalizedMMSMessages.INCOME, userConfig.lang), wcfCaptionHC))
            offsY++
            offsY = outCounterRows(userConfig, sheet, offsY, inCounters)
            offsY++
        }
        val outCounters = usings.filter { ccd ->
            ccd.sensorEntity.inOutType == SensorConfigCounter.CALC_TYPE_OUT
        }
        if (outCounters.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, getLocalizedMMSMessage(LocalizedMMSMessages.OUTGOING_METERS, userConfig.lang), wcfCaptionHC))
            sheet.addCell(Label(2, offsY, getLocalizedMMSMessage(LocalizedMMSMessages.FLOW, userConfig.lang), wcfCaptionHC))
            offsY++
            offsY = outCounterRows(userConfig, sheet, offsY, outCounters)
            offsY++
        }

        //--- report on energo sensors
        if (energos.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, getLocalizedMMSMessage(LocalizedMMSMessages.NAME_UNITS, userConfig.lang), wcfCaptionHC))
            sheet.addCell(Label(2, offsY, getLocalizedMMSMessage(LocalizedMMSMessages.FLOW_GENERATION, userConfig.lang), wcfCaptionHC))
            offsY++
            offsY = outCounterRows(userConfig, sheet, offsY, energos)
            offsY++
        }

        val mainLiquidLevels = liquidLevels.filter { acd ->
            acd.sensorEntity.containerType == SensorConfigLiquidLevel.CONTAINER_TYPE_MAIN
        }
        if (mainLiquidLevels.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, getLocalizedMMSMessage(LocalizedMMSMessages.NAME_OF_MAIN_TANK, userConfig.lang), wcfCaptionHC))
            sheet.addCell(Label(2, offsY, getLocalizedMMSMessage(LocalizedMMSMessages.INITIAL_LEVEL, userConfig.lang), wcfCaptionHC))
            sheet.addCell(Label(3, offsY, getLocalizedMMSMessage(LocalizedMMSMessages.FINAL_LEVEL, userConfig.lang), wcfCaptionHC))
            offsY++
            offsY = outAnalogueRows(userConfig, sheet, offsY, mainLiquidLevels)
            offsY++
        }
        val workLiquidLevels = liquidLevels.filter { acd ->
            acd.sensorEntity.containerType == SensorConfigLiquidLevel.CONTAINER_TYPE_WORK
        }
        if (workLiquidLevels.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, getLocalizedMMSMessage(LocalizedMMSMessages.NAME_OF_WORKING_FLOW_TANK, userConfig.lang), wcfCaptionHC))
            sheet.addCell(Label(2, offsY, getLocalizedMMSMessage(LocalizedMMSMessages.INITIAL_LEVEL, userConfig.lang), wcfCaptionHC))
            sheet.addCell(Label(3, offsY, getLocalizedMMSMessage(LocalizedMMSMessages.FINAL_LEVEL, userConfig.lang), wcfCaptionHC))
            offsY++
            offsY = outAnalogueRows(userConfig, sheet, offsY, workLiquidLevels)
            offsY++
        }

        if (temperatures.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, getLocalizedMMSMessage(LocalizedMMSMessages.NAME_UNITS, userConfig.lang), wcfCaptionHC))
            sheet.addCell(Label(2, offsY, getLocalizedMMSMessage(LocalizedMMSMessages.INITIAL_TEMPERATURE, userConfig.lang), wcfCaptionHC))
            sheet.addCell(Label(3, offsY, getLocalizedMMSMessage(LocalizedMMSMessages.FINAL_TEMPERATURE, userConfig.lang), wcfCaptionHC))
            offsY++
            offsY = outAnalogueRows(userConfig, sheet, offsY, temperatures)
            offsY++
        }

        if (densities.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, getLocalizedMMSMessage(LocalizedMMSMessages.NAME_UNITS, userConfig.lang), wcfCaptionHC))
            sheet.addCell(Label(2, offsY, getLocalizedMMSMessage(LocalizedMMSMessages.INITIAL_DENSITY, userConfig.lang), wcfCaptionHC))
            sheet.addCell(Label(3, offsY, getLocalizedMMSMessage(LocalizedMMSMessages.FINAL_DENSITY, userConfig.lang), wcfCaptionHC))
            offsY++
            offsY = outAnalogueRows(userConfig, sheet, offsY, densities)
            offsY++
        }

//        troubles?.alGTD?.let { alGTD ->
//            if (alGTD.isNotEmpty()) {
//                sheet.addCell(Label(1, offsY, "Неисправность", wcfCaptionHC))
//                sheet.addCell(Label(2, offsY, "Время начала", wcfCaptionHC))
//                sheet.addCell(Label(3, offsY, "Время окончания", wcfCaptionHC))
//                offsY++
//                alGTD.forEach { gtd ->
//                    if (gtd.fillColorIndex == ChartColorIndex.FILL_CRITICAL ||
//                        gtd.borderColorIndex == ChartColorIndex.BORDER_CRITICAL ||
//                        gtd.textColorIndex == ChartColorIndex.TEXT_CRITICAL
//                    ) {
//                        sheet.addCell(Label(1, offsY, gtd.text, wcfCellR))
//                        sheet.addCell(Label(2, offsY, DateTime_DMYHMS(zoneId, gtd.textX1), wcfCellC))
//                        sheet.addCell(Label(3, offsY, DateTime_DMYHMS(zoneId, gtd.textX2), wcfCellC))
//                        if (isKeepPlaceForComment) {
//                            sheet.addCell(Label(4, offsY, "", wcfComment))
//                            sheet.mergeCells(4, offsY, getColumnCount(1), offsY)
//                        }
//                        offsY++
//                    }
//                }
//                offsY++
//            }
//        }
//

        return offsY
    }

    private fun outCounterRows(
        userConfig: ServerUserConfig,
        sheet: WritableSheet,
        aOffsY: Int,
        counters: List<CounterCalcData>
    ): Int {
        var offsY = aOffsY
        for (ccd in counters) {
            sheet.addCell(Label(1, offsY, getFullSensorDescr(ccd.sensorEntity), wcfCellC))
            sheet.addCell(getNumberCell(2, offsY, ccd.value, getPrecision(ccd.value), wcfCellC))
            offsY++
        }
        counters.groupBy { ccd -> ccd.sensorEntity.dim ?: "-" }.forEach { dim, ccds ->
            val sum = ccds.sumOf { ccd -> ccd.value }

            sheet.addCell(Label(1, offsY, "${getLocalizedMMSMessage(LocalizedMMSMessages.TOTAL, userConfig.lang)} [$dim]", wcfCellRStdYellow))
            sheet.addCell(getNumberCell(2, offsY, sum, getPrecision(sum), wcfCellCBStdYellow))
            offsY++
        }
        return offsY
    }

    private fun outAnalogueRows(
        userConfig: ServerUserConfig,
        sheet: WritableSheet,
        aOffsY: Int,
        analogues: List<AnalogueCalcData>
    ): Int {
        var offsY = aOffsY
        for (acd in analogues) {
            sheet.addCell(Label(1, offsY, getFullSensorDescr(acd.sensorEntity), wcfCellC))
            if (acd.begValue != null) {
                sheet.addCell(getNumberCell(2, offsY, acd.begValue, getPrecision(acd.begValue), wcfCellC))
            } else {
                sheet.addCell(Label(2, offsY, "-", wcfCellC))
            }
            if (acd.endValue != null) {
                sheet.addCell(getNumberCell(3, offsY, acd.endValue, getPrecision(acd.endValue), wcfCellC))
            } else {
                sheet.addCell(Label(3, offsY, "-", wcfCellC))
            }
            offsY++
        }
        analogues.groupBy { acd -> acd.sensorEntity.dim ?: "-" }.forEach { dim, acds ->
            val begSum = acds.sumOf { acd -> acd.begValue ?: 0.0 }
            val endSum = acds.sumOf { acd -> acd.endValue ?: 0.0 }

            sheet.addCell(Label(1, offsY, "${getLocalizedMMSMessage(LocalizedMMSMessages.TOTAL, userConfig.lang)} [$dim]", wcfCellRStdYellow))
            sheet.addCell(getNumberCell(2, offsY, begSum, getPrecision(begSum), wcfCellCBStdYellow))
            sheet.addCell(getNumberCell(3, offsY, endSum, getPrecision(endSum), wcfCellCBStdYellow))
            offsY++
        }
        return offsY
    }

    private fun getFullSensorDescr(sensorEntity: SensorEntity): String = "${sensorEntity.descr ?: "-"} [${sensorEntity.dim ?: "-"}]"

    protected fun outReportTrail(sheet: WritableSheet, offsY: Int, userConfig: ServerUserConfig): Int {
        sheet.addCell(Label(1, offsY, getPreparedAt(userConfig), wcfCellL))
//        sheet.mergeCells(1, offsY, 1, offsY)

        return offsY
    }

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
