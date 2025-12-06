package foatto.server.service.report

import foatto.core.util.getPrecision
import foatto.server.calc.AnalogueCalcData
import foatto.server.calc.CounterCalcData
import foatto.server.calc.WorkCalcData
import foatto.server.entity.SensorEntity
import foatto.server.model.ServerUserConfig
import foatto.server.model.sensor.SensorConfigCounter
import foatto.server.model.sensor.SensorConfigLiquidLevel
import foatto.server.repository.ActionLogRepository
import foatto.server.service.FileStoreService
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet

abstract class AbstractPeriodSummaryService(
    private val fileStoreService: FileStoreService,
    private val actionLogRepository: ActionLogRepository,
) : MMSReportService(
    fileStoreService = fileStoreService,
    actionLogRepository = actionLogRepository,
) {

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
            alDim.add(11)   // 11 вместо 9, т.е. на 9-ке плохо умещаются длинные заголовки столбцов ("Температура", например)
        }

        for (i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }
    }

    protected fun addGroupTitle(sheet: WritableSheet, aOffsY: Int, title: String): Int {
        var offsY = aOffsY
        sheet.addCell(Label(1, offsY, title, wcfCellCB))
        sheet.mergeCells(1, offsY, 4, offsY + 2)
        offsY += 4
        return offsY
    }

    protected fun outBlock(
        sheet: WritableSheet,
        aOffsY: Int,
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

        //--- geo-sensor report
//        objectConfig.scg?.let { scg ->
//            if (scg.isUseSpeed || scg.isUseRun) {
//                var offsX = 1
//                sheet.addCell(Label(offsX, offsY, "Датчик ГЛОНАСС/GPS", wcfCaptionHC))
//                sheet.mergeCells(offsX, offsY, offsX, offsY + 1)
//                offsX++
//                if (scg.isUseRun) {
//                    sheet.addCell(Label(offsX, offsY, "Пробег [км]", wcfCaptionHC))
//                    sheet.mergeCells(offsX, offsY, offsX, offsY + 1)
//                    offsX++
//                }
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
//                offsX = 1
//                sheet.addCell(Label(offsX++, offsY, objectCalc.sGeoName, wcfCellC))
//                if (scg.isUseRun) {
//                    sheet.addCell(Label(offsX++, offsY, objectCalc.sGeoRun, wcfCellC))
//                }
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
//                if (isKeepPlaceForComment) {
//                    sheet.addCell(Label(1, offsY, "", wcfComment))
//                    sheet.mergeCells(1, offsY, getColumnCount(1), offsY)
//                    offsY += 2
//                }
//            }
//        }

        //--- report on sensors of equipment operation
        if (works.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Оборудование", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Время работы [час]", wcfCaptionHC))
//            sheet.addCell(Label(3, offsY, "Сред. расх. [на 1 час]", wcfCaptionHC))
            offsY++
            for (wcd in works) {
                sheet.addCell(Label(1, offsY, wcd.sensorEntity.descr ?: "-", wcfCellC))
                sheet.addCell(getNumberCell(2, offsY, wcd.onTime.toDouble() / 3600.0, 1, wcfCellC))
//                val tmWork = objectCalc.tmGroupSum[wcd.group]?.tmWork
//                val tmLiquidUsing = objectCalc.tmGroupSum[wcd.group]?.tmLiquidUsing
//                if (tmWork?.size == 1 && tmLiquidUsing?.size == 1 && wcd.onTime > 0) {
//                    sheet.addCell(getNumberCell(3, offsY, tmLiquidUsing[tmLiquidUsing.firstKey()]!! / (wcd.onTime.toDouble() / 60.0 / 60.0), 1, wcfCellC))
//                } else {
//                    sheet.addCell(Label(3, offsY, "-", wcfCellC))
//                }
                offsY++
            }
            offsY++
        }

        val inCounters = usings.filter { ccd ->
            ccd.sensorEntity.inOutType == SensorConfigCounter.CALC_TYPE_IN
        }
        if (inCounters.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Входящие счётчики [ед.изм.]", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Приход", wcfCaptionHC))
            offsY++
            offsY = outCounterRows(sheet, offsY, inCounters)
            offsY++
        }
        val outCounters = usings.filter { ccd ->
            ccd.sensorEntity.inOutType == SensorConfigCounter.CALC_TYPE_OUT
        }
        if (outCounters.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Исходящие счётчики [ед.изм.]", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Расход", wcfCaptionHC))
            offsY++
            offsY = outCounterRows(sheet, offsY, outCounters)
            offsY++
        }

        //--- report on energo sensors
        if (energos.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Наименование [ед.изм.]", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Расход/Генерация", wcfCaptionHC))
            offsY++
            offsY = outCounterRows(sheet, offsY, energos)
            offsY++
        }

        val mainLiquidLevels = liquidLevels.filter { acd ->
            acd.sensorEntity.containerType == SensorConfigLiquidLevel.CONTAINER_TYPE_MAIN
        }
        if (mainLiquidLevels.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Наименование основной ёмкости [ед.изм.]", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Уровень начальный", wcfCaptionHC))
            sheet.addCell(Label(3, offsY, "Уровень конечный", wcfCaptionHC))
            offsY++
            offsY = outAnalogueRows(sheet, offsY, mainLiquidLevels)
            offsY++
        }
        val workLiquidLevels = liquidLevels.filter { acd ->
            acd.sensorEntity.containerType == SensorConfigLiquidLevel.CONTAINER_TYPE_WORK
        }
        if (workLiquidLevels.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Наименование рабочей/расходной ёмкости [ед.изм.]", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Уровень начальный", wcfCaptionHC))
            sheet.addCell(Label(3, offsY, "Уровень конечный", wcfCaptionHC))
            offsY++
            offsY = outAnalogueRows(sheet, offsY, workLiquidLevels)
            offsY++
        }

//            var allBegLevelSum = 0.0
//            var allEndLevelSum = 0.0
//            var diffIncDec = 0.0

//            listOf(SensorConfigLiquidLevel.CONTAINER_TYPE_MAIN, SensorConfigLiquidLevel.CONTAINER_TYPE_WORK).forEach { containerType ->
//                if (objectCalc.tmLiquidLevel.any { (_, llcd) ->
//                        llcd.containerType == containerType
//                    }) {
//                    val containerTypeDescr = if (containerType == SensorConfigLiquidLevel.CONTAINER_TYPE_MAIN) {
//                        "основной"
//                    } else {
//                        "рабочей/расходной"
//                    }

//                    //--- дополнительная группировка по ед.изм. из наименования датчика
//                    val tmLiquidLevelByDim = sortedMapOf<String, SortedMap<String, LiquidLevelCalcData>>()
//                    objectCalc.tmLiquidLevel
//                        .filter { (_, llcd) ->
//                            llcd.containerType == containerType
//                        }
//                        .forEach { (liquidName, llcd) ->
//                            val dim = liquidName.substringAfterLast(' ')
//                            val llByDim = tmLiquidLevelByDim.getOrPut(dim) { sortedMapOf() }
//                            llByDim[liquidName] = llcd
//                        }

//                        var begLevelSum = 0.0
//                        var endLevelSum = 0.0
//                        var incTotalSum = 0.0
//                        var decTotalSum = 0.0
//                        var usingTotalSum = 0.0

//                        tmLiquidLevel.forEach { (liquidName, llcd) ->
//                            sheet.addCell(Label(1, offsY, liquidName, wcfCellC))
//                            sheet.addCell(getNumberCell(2, offsY, llcd.begLevel, ObjectCalc.getPrecision(llcd.begLevel), wcfCellC))
//                            sheet.addCell(getNumberCell(3, offsY, llcd.endLevel, ObjectCalc.getPrecision(llcd.endLevel), wcfCellC))
//                            sheet.addCell(getNumberCell(4, offsY, llcd.incTotal, ObjectCalc.getPrecision(llcd.incTotal), wcfCellC))
//                            sheet.addCell(getNumberCell(5, offsY, llcd.decTotal, ObjectCalc.getPrecision(llcd.decTotal), wcfCellC))
//                            if (containerType == SensorConfigLiquidLevel.CONTAINER_TYPE_WORK ||
//                                containerType == SensorConfigLiquidLevel.CONTAINER_TYPE_MAIN && isOutLiquidLevelMainContainerUsing
//                            ) {
//                                sheet.addCell(getNumberCell(6, offsY, llcd.usingTotal, ObjectCalc.getPrecision(llcd.usingTotal), wcfCellC))
//
//                                if (isUsingCalc) sheet.addCell(
//                                    Label(
//                                        7, offsY, if (llcd.usingCalc <= 0) "-"
//                                        else getSplittedDouble(llcd.usingCalc, ObjectCalc.getPrecision(llcd.usingCalc), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellC
//                                    )
//                                )
//                            }
//                            offsY++

//                            begLevelSum += llcd.begLevel
//                            endLevelSum += llcd.endLevel
//                            incTotalSum += llcd.incTotal
//                            decTotalSum += llcd.decTotal
//                            usingTotalSum += llcd.usingTotal
//
//                            allBegLevelSum += llcd.begLevel
//                            allEndLevelSum += llcd.endLevel

//                        sheet.addCell(Label(1, offsY, "ИТОГО по $containerTypeDescr ёмкости:", wcfCaptionHC))
//                        sheet.addCell(getNumberCell(2, offsY, begLevelSum, ObjectCalc.getPrecision(begLevelSum), wcfCellC))
//                        sheet.addCell(getNumberCell(3, offsY, endLevelSum, ObjectCalc.getPrecision(endLevelSum), wcfCellC))
//                        sheet.addCell(getNumberCell(4, offsY, incTotalSum, ObjectCalc.getPrecision(incTotalSum), wcfCellC))
//                        sheet.addCell(getNumberCell(5, offsY, decTotalSum, ObjectCalc.getPrecision(decTotalSum), wcfCellC))
//                        if (containerType == SensorConfigLiquidLevel.CONTAINER_TYPE_WORK ||
//                            containerType == SensorConfigLiquidLevel.CONTAINER_TYPE_MAIN && isOutLiquidLevelMainContainerUsing
//                        ) {
//                            sheet.addCell(getNumberCell(6, offsY, usingTotalSum, ObjectCalc.getPrecision(usingTotalSum), wcfCellC))
//                        }
//                        offsY += 2

//            sheet.addCell(Label(1, offsY, "ИТОГО суммарно по всем ёмкостям:", wcfCaptionHC))
//            sheet.addCell(Label(2, offsY, "Остаток на начало периода", wcfCaptionHC))
//            sheet.addCell(Label(3, offsY, "Остаток на конец периода", wcfCaptionHC))
//            if (isShowDiffIncDec) {
//                sheet.addCell(Label(4, offsY, "Разница показаний сливов и заправок", wcfCaptionHC))
//            }
//            offsY++
//
//            sheet.addCell(getNumberCell(2, offsY, allBegLevelSum, ObjectCalc.getPrecision(allBegLevelSum), wcfCellC))
//            sheet.addCell(getNumberCell(3, offsY, allEndLevelSum, ObjectCalc.getPrecision(allEndLevelSum), wcfCellC))
//            if (isShowDiffIncDec) {
//                sheet.addCell(getNumberCell(4, offsY, diffIncDec, ObjectCalc.getPrecision(diffIncDec), wcfCellC))
//            }
//            offsY++

        if (temperatures.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Наименование [ед.изм.]", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Температура начальная", wcfCaptionHC))
            sheet.addCell(Label(3, offsY, "Температура конечная", wcfCaptionHC))
            offsY++
            offsY = outAnalogueRows(sheet, offsY, temperatures)
            offsY++
        }

        if (densities.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Наименование [ед.изм.]", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Плотность начальная", wcfCaptionHC))
            sheet.addCell(Label(3, offsY, "Плотность конечная", wcfCaptionHC))
            offsY++
            offsY = outAnalogueRows(sheet, offsY, densities)
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
//        //--- withdrawal of the amount for each amount group
//        if (isOutGroupSum && objectCalc.tmGroupSum.size > 1) {
//            objectCalc.tmGroupSum.forEach { (sumName, sumData) ->
//                sheet.addCell(Label(1, offsY, "ИТОГО по '$sumName':", wcfCellRBStdYellow))
//                offsY++
//
//                offsY = outGroupSum(sheet, offsY, sumData)
//                offsY++
//            }
//        }
//
//        sheet.addCell(Label(1, offsY, "ИТОГО:", wcfCellRBStdYellow))
//        offsY++
//
//        offsY = outGroupSum(sheet, offsY, objectCalc.allSumData)
//        offsY++

        return offsY
    }

//    private fun outGroupSum(sheet: WritableSheet, aOffsY: Int, sumData: CalcSumData): Int {
//        var offsY = aOffsY
//
//        if (sumData.tmEnergo.isNotEmpty()) {
//            sheet.addCell(Label(1, offsY++, "Расход/генерация э/энергии", wcfCellRBStdYellow))
//
//            sheet.addCell(Label(1, offsY, "Наименование", wcfCellRBStdYellow))
//            sheet.addCell(Label(2, offsY, "Расход/генерация", wcfCellRBStdYellow))
//            sheet.addCell(Label(3, offsY, "Средний расход топлива", wcfCellRBStdYellow))
//            offsY++
//
//            sumData.tmEnergo.forEach { (sensorType, dataByPhase) ->
//                dataByPhase.forEach { (phase, value) ->
//                    sheet.addCell(Label(1, offsY, (SensorConfig.hmSensorDescr[sensorType] ?: "(неизв. тип датчика)") + getPhaseDescr(phase), wcfCellRBStdYellow))
//                    sheet.addCell(getNumberCell(2, offsY, value, ObjectCalc.getPrecision(value), wcfCellC))
//                    val tmLiquidUsing = sumData.tmLiquidUsing
//                    if (tmLiquidUsing.size == 1 && value > 0) {
//                        sheet.addCell(getNumberCell(3, offsY, tmLiquidUsing[tmLiquidUsing.firstKey()]!! / value, 1, wcfCellC))
//                    } else {
//                        sheet.addCell(Label(3, offsY, "-", wcfCellC))
//                    }
//                    offsY++
//                }
//            }
//        }
//
//        if (sumData.tmLiquidUsing.isNotEmpty()) {
//            sheet.addCell(Label(1, offsY++, "Расход жидкостей/топлива", wcfCellRBStdYellow))
//
//            sheet.addCell(Label(1, offsY, "Наименование", wcfCellRBStdYellow))
//            sheet.addCell(Label(2, offsY, "Расход", wcfCellRBStdYellow))
//            offsY++
//
//            sumData.tmLiquidUsing.forEach { (name, using) ->
//                sheet.addCell(Label(1, offsY, name, wcfCellRBStdYellow))
//                sheet.addCell(getNumberCell(2, offsY, using, ObjectCalc.getPrecision(using), wcfCellC))
//                offsY++
//            }
//        }
//
//        return offsY
//    }

//    protected fun outObjectAndUserSum(
//        sheet: WritableSheet,
//        aOffsY: Int,
//        reportSumUser: Boolean,
//        reportSumObject: Boolean,
//        tmUserSumCollector: TreeMap<String, ReportSumCollector>,
//        allSumCollector: ReportSumCollector,
//    ): Int {
//        var offsY = aOffsY
//
//        if (reportSumUser) {
//            sheet.addCell(Label(0, offsY, "ИТОГО по объектам и их владельцам", wcfCellCBStdYellow))
//            sheet.mergeCells(0, offsY, getColumnCount(1), offsY + 2)
//            offsY += 4
//
//            for ((userName, sumUser) in tmUserSumCollector) {
//                sheet.addCell(Label(0, offsY, userName, wcfCellLBStdYellow))
//                sheet.mergeCells(0, offsY, getColumnCount(1), offsY)
//                offsY += 2
//                if (reportSumObject) {
//                    val tmObjectSum = sumUser.tmSumObject
//                    for ((objectInfo, objectSum) in tmObjectSum) {
//                        sheet.addCell(Label(1, offsY, objectInfo, wcfCellLB))
//                        offsY++
//
//                        offsY = outSumData(sheet, offsY, objectSum, false, objectSum.scg)
//                    }
//                }
//
//                sheet.addCell(Label(0, offsY, "ИТОГО по владельцу:", wcfCellLBStdYellow))
//                sheet.mergeCells(0, offsY, 1, offsY)
//                offsY++
//
//                offsY = outSumData(sheet, offsY, sumUser.sumUser, true, null)
//            }
//        }
//
//        sheet.addCell(Label(0, offsY, "ИТОГО общее", wcfCellCBStdYellow))
//        sheet.mergeCells(0, offsY, getColumnCount(1), offsY + 2)
//        offsY += 4
//
//        offsY = outSumData(sheet, offsY, allSumCollector.sumUser, true, null)
//
//        return offsY
//    }
//
//    protected fun outSumData(sheet: WritableSheet, aOffsY: Int, sumData: ReportSumData, isManyObjects: Boolean, scg: SensorConfigGeo?): Int {
//        var offsY = aOffsY
//
//        //--- сумма пробегов, времени и стоянок имеет смысл только в разрезе конкретной единицы оборудования
//        scg?.let {
//            val sGeoRun = if (sumData.run < 0) "-" else getSplittedDouble(sumData.run, 1, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
//            val sGeoMovingTime = if (sumData.movingTime < 0) "-" else secondIntervalToString(sumData.movingTime)
//            val sGeoParkingTime = if (sumData.parkingTime < 0) "-" else secondIntervalToString(sumData.parkingTime)
//            val sGeoParkingCount = if (sumData.parkingCount < 0) {
//                "-"
//            } else if (userConfig.upIsUseThousandsDivider) {
//                getSplittedLong(sumData.parkingCount.toLong())
//            } else {
//                sumData.parkingCount.toString()
//            }
//
//            var offsX = 1
//            sheet.addCell(Label(offsX, offsY, "Датчик ГЛОНАСС/GPS", wcfCaptionHC))
//            sheet.mergeCells(offsX, offsY, offsX, offsY + 1)
//            offsX++
//            if (scg.isUseRun) {
//                sheet.addCell(Label(offsX, offsY, "Пробег [км]", wcfCaptionHC))
//                sheet.mergeCells(offsX, offsY, offsX, offsY + 1)
//                offsX++
//            }
//            if (scg.isUseSpeed) {
//                sheet.addCell(Label(offsX, offsY, "Время", wcfCaptionHC))
//                sheet.mergeCells(offsX, offsY, offsX + 1, offsY)
//                sheet.addCell(Label(offsX, offsY + 1, "в движении", wcfCaptionHC))
//                sheet.addCell(Label(offsX + 1, offsY + 1, "на стоянках", wcfCaptionHC))
//                sheet.addCell(Label(offsX + 2, offsY, "Кол-во стоянок", wcfCaptionHC))
//                sheet.mergeCells(offsX + 2, offsY, offsX + 2, offsY + 1)
//            }
//            offsY += 2
//
//            offsX = 1
//            sheet.addCell(Label(offsX++, offsY, scg.descr, wcfCellC))
//            if (scg.isUseRun) {
//                sheet.addCell(Label(offsX++, offsY, sGeoRun, wcfCellC))
//            }
//            if (scg.isUseSpeed) {
//                sheet.addCell(Label(offsX++, offsY, sGeoMovingTime, wcfCellC))
//                sheet.addCell(Label(offsX++, offsY, sGeoParkingTime, wcfCellC))
//                sheet.addCell(Label(offsX++, offsY, sGeoParkingCount, wcfCellC))
//            }
//            offsY++
//        }
//
//        //--- сумма моточасов имеет смысл только в разрезе конкретной единицы оборудования
//        if (!isManyObjects && sumData.tmWork.isNotEmpty()) {
//            sheet.addCell(Label(1, offsY, "Оборудование", wcfCaptionHC))
//            sheet.addCell(Label(2, offsY, "Время работы [час]", wcfCaptionHC))
//            offsY++
//            sumData.tmWork.forEach { (workDescr, onTime) ->
//                sheet.addCell(Label(1, offsY, workDescr, wcfCellC))
//                sheet.addCell(getNumberCell(2, offsY, onTime.toDouble() / 60.0 / 60.0, 1, wcfCellC))
//                offsY++
//            }
//        }
//
//        if (sumData.tmEnergo.isNotEmpty()) {
//            sheet.addCell(Label(1, offsY, "Наименование", wcfCaptionHC))
//            sheet.addCell(Label(2, offsY, "Расход/генерация", wcfCaptionHC))
//            offsY++
//            sumData.tmEnergo.forEach { (sensorType, dataByPhase) ->
//                dataByPhase.forEach { (phase, value) ->
//                    sheet.addCell(
//                        Label(
//                            1,
//                            offsY,
//                            (SensorConfig.hmSensorDescr[sensorType] ?: "(неизв. тип датчика)") + getPhaseDescr(phase),
//                            if (isManyObjects) wcfCellCBStdYellow else wcfCellC
//                        )
//                    )
//                    sheet.addCell(
//                        Label(
//                            2,
//                            offsY,
//                            getSplittedDouble(value, ObjectCalc.getPrecision(value), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider),
//                            if (isManyObjects) wcfCellCBStdYellow else wcfCellC
//                        )
//                    )
//                    offsY++
//                }
//            }
//        }
//
//        if (sumData.tmLiquidUsing.isNotEmpty()) {
//            sheet.addCell(Label(1, offsY, "Наименование топлива", wcfCaptionHC))
//            sheet.addCell(Label(2, offsY, "Расход", wcfCaptionHC))
//            offsY++
//            sumData.tmLiquidUsing.forEach { (liquidDescr, total) ->
//                sheet.addCell(Label(1, offsY, liquidDescr, if (isManyObjects) wcfCellCBStdYellow else wcfCellC))
//                sheet.addCell(getNumberCell(2, offsY, total, ObjectCalc.getPrecision(total), if (isManyObjects) wcfCellCBStdYellow else wcfCellC))
//                offsY++
//            }
//            offsY++
//        }
//
////        if (sumData.tmLiquidIncDec.isNotEmpty()) {
////            sheet.addCell(Label(1, offsY, "Наименование топлива", wcfCaptionHC))
////            sheet.addCell(Label(2, offsY, "Заправка", wcfCaptionHC))
////            sheet.addCell(Label(3, offsY, "Слив", wcfCaptionHC))
////            offsY++
////            sumData.tmLiquidIncDec.forEach { (liquidDescr, pairIncDec) ->
////                sheet.addCell(Label(1, offsY, liquidDescr, if (isManyObjects) wcfCellCBStdYellow else wcfCellC))
////                sheet.addCell(getNumberCell(2, offsY, pairIncDec.first, ObjectCalc.getPrecision(pairIncDec.first), if (isManyObjects) wcfCellCBStdYellow else wcfCellC))
////                sheet.addCell(getNumberCell(3, offsY, pairIncDec.second, ObjectCalc.getPrecision(pairIncDec.second), if (isManyObjects) wcfCellCBStdYellow else wcfCellC))
////                offsY++
////            }
////            offsY++
////        }
//
//        return offsY
//    }

    private fun outCounterRows(sheet: WritableSheet, aOffsY: Int, counters: List<CounterCalcData>): Int {
        var offsY = aOffsY
        for (ccd in counters) {
            sheet.addCell(Label(1, offsY, getFullSensorDescr(ccd.sensorEntity), wcfCellC))
            sheet.addCell(getNumberCell(2, offsY, ccd.value, getPrecision(ccd.value), wcfCellC))
            offsY++
        }
        counters.groupBy { ccd -> ccd.sensorEntity.dim ?: "-" }.forEach { dim, ccds ->
            val sum = ccds.sumOf { ccd -> ccd.value }

            sheet.addCell(Label(1, offsY, "ИТОГО [$dim]", wcfCellRStdYellow))
            sheet.addCell(getNumberCell(2, offsY, sum, getPrecision(sum), wcfCellCBStdYellow))
            offsY++
        }
        return offsY
    }

    private fun outAnalogueRows(sheet: WritableSheet, aOffsY: Int, analogues: List<AnalogueCalcData>): Int {
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

            sheet.addCell(Label(1, offsY, "ИТОГО [$dim]", wcfCellRStdYellow))
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