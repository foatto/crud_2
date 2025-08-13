//package foatto.server.service.chart
//
//import foatto.core.model.request.ChartActionRequest
//import foatto.core.model.response.ChartActionResponse
//import foatto.core.model.response.ResponseCode
//import foatto.core.model.response.chart.*
//import foatto.server.entity.SensorEntity
//import foatto.server.model.sensor.SensorConfig
//import foatto.server.repository.ObjectRepository
//import foatto.server.repository.SensorRepository
//import jakarta.persistence.EntityManager
//import org.springframework.data.repository.findByIdOrNull
//import org.springframework.stereotype.Service
//import java.util.*
//
//@Service
//class ChartLiquidLevelService(
//    private val entityManager: EntityManager,
//    private val objectRepository: ObjectRepository,
//    private val sensorRepository: SensorRepository,
//) : AbstractChartService(
//    entityManager = entityManager,
//    objectRepository = objectRepository,
//    sensorRepository = sensorRepository,
//) {
//
//    override fun getCharts(chartActionRequest: ChartActionRequest): ChartActionResponse {
////        isGeoSensorShowed = false
////        isCommonTroubleShowed = false
//
//        val (begTime, endTime) = chartActionRequest.times
//        val (viewWidth, viewHeight) = chartActionRequest.viewSize
//
//        val tmElement = sortedMapOf<String, ChartData>()
//
//        chartActionRequest.action.id?.let { objectId ->
//            objectRepository.findByIdOrNull(objectId)?.let { objectEntity ->
//                sensorRepository.findByObjAndSensorTypeAndPeriod(objectEntity, SensorConfig.SENSOR_LIQUID_LEVEL, begTime, endTime).forEach { levelSensorEntity ->
//                    val additiveSensorEntities = sensorRepository.findByObjAndGroupAndSensorTypeInAndPeriod(
//                        obj = objectEntity,
//                        group = levelSensorEntity.group,
//                        sensorTypes = setOf(
//                            SensorConfig.SENSOR_WEIGHT,
//                            SensorConfig.SENSOR_TEMPERATURE,
//                            SensorConfig.SENSOR_DENSITY,
//                        ),
//                        begTime = begTime,
//                        endTime = endTime,
//                    )
//                    val counterSensorEntities = sensorRepository.findByObjAndGroupAndSensorTypeInAndPeriod(
//                        obj = objectEntity,
//                        group = levelSensorEntity.group,
//                        sensorTypes = setOf(
//                            SensorConfig.SENSOR_MASS_ACCUMULATED,
//                            SensorConfig.SENSOR_VOLUME_ACCUMULATED,
//                            SensorConfig.SENSOR_LIQUID_USING,
//                        ),
//                        begTime = begTime,
//                        endTime = endTime,
//                    )
//
//                    getChart(
//                        levelSensorEntity = levelSensorEntity,
//                        additiveSensorEntities = additiveSensorEntities,
//                        counterSensorEntities = counterSensorEntities,
//                        begTime = begTime,
//                        endTime = endTime,
//                        viewWidth = viewWidth,
//                        viewHeight = viewHeight,
//                        tmElement = tmElement,
//                        legends = emptyList(),
//                    )
//                }
//            }
//        }
//
//        return ChartActionResponse(
//            responseCode = ResponseCode.OK,
//            charts = tmElement.toList(),
//            legends = emptyList(),
//        )
//    }
//
//    private fun getChart(
//        levelSensorEntity: SensorEntity,
//        additiveSensorEntities: List<SensorEntity>,
//        counterSensorEntities: List<SensorEntity>,
//        begTime: Int,
//        endTime: Int,
//        viewWidth: Float,
//        viewHeight: Float,
//        tmElement: SortedMap<String, ChartData>,
//        legends: List<ChartLegendData>,
//    ) {
//        val chartTitle = levelSensorEntity.descr ?: "-"
//        val objectId = levelSensorEntity.obj?.id ?: 0
//        val portNum = levelSensorEntity.portNum ?: 0
//        val levelMinView = levelSensorEntity.minView?.toFloat() ?: 0.0f
//        val levelMaxView = levelSensorEntity.maxView?.toFloat() ?: 100.0f
//
//        val xScale = (endTime - begTime) / viewWidth
//        val yScale = (levelMaxView - levelMinView) / viewHeight
//
//        val elements = mutableListOf<ChartElementData>()
//
////            objectConfig.scg?.let { scg ->
////                if (!isGeoSensorShowed && scg.isUseSpeed) {
////                    val aBack = ChartElementDTO(ChartElementTypeDTO.BACK, 0, 0, false)
////                    calcGeoSensor(alRawTime, alRawData, scg, begTime, endTime, aBack)
////                    alGDC.add(aBack)
////
////                    isGeoSensorShowed = true
////                }
////            }
//
//        val alAxisYData = mutableListOf<ChartAxisData>()
//
//        //--- первый график - уровень топлива
//
//        var axisIndex = 0
//        alAxisYData += ChartAxisData(
//            title = chartTitle,
//            min = levelMinView,
//            max = levelMaxView,
//            color = getChartAxisColor(axisIndex),
//            isReversedY = SensorConfig.isReversedChart(levelSensorEntity.sensorType)
//        )
//
//        val levelLines = ChartElementData(ChartElementType.LINE, axisIndex, 3, SensorConfig.isReversedChart(levelSensorEntity.sensorType))
//        getLines(
//            sensorEntity = levelSensorEntity,
//            valueIndex = 1,
//            begTime = begTime,
//            endTime = endTime,
//            xScale = xScale,
//            yScale = yScale,
//            colorFun = { value: Double -> getChartLineNormalColor(axisIndex) },
//            lines = levelLines,
//        )
//        elements += levelLines
//
//        //--- следующие графики - температуры/плотности/массы (если они заданы)
//        additiveSensorEntities.forEach { additiveSensorEntity ->
//            val additiveMinView = additiveSensorEntity.minView?.toFloat() ?: 0.0f
//            val additiveMaxView = additiveSensorEntity.maxView?.toFloat() ?: 100.0f
//
//            axisIndex++
//            alAxisYData += ChartAxisData(
//                title = additiveSensorEntity.descr ?: "-",
//                min = additiveMinView,
//                max = additiveMaxView,
//                color = getChartAxisColor(axisIndex),
//                isReversedY = SensorConfig.isReversedChart(additiveSensorEntity.sensorType)
//            )
//
//            val additiveSensorLines = ChartElementData(ChartElementType.LINE, axisIndex, 1, SensorConfig.isReversedChart(additiveSensorEntity.sensorType))
//            getLines(
//                sensorEntity = additiveSensorEntity,
//                valueIndex = 1,
//                begTime = begTime,
//                endTime = endTime,
//                xScale = xScale,
//                yScale = yScale,
//                colorFun = { value: Double ->
//                    when {
//                        additiveSensorEntity.minLimit != null && additiveSensorEntity.minLimit != additiveSensorEntity.maxLimit && value < additiveSensorEntity.minLimit -> {
//                            getChartLineBelowColor(axisIndex)
//                        }
//
//                        additiveSensorEntity.maxLimit != null && additiveSensorEntity.minLimit != additiveSensorEntity.maxLimit && value > additiveSensorEntity.maxLimit -> {
//                            getChartLineAboveColor(axisIndex)
//                        }
//
//                        else -> {
//                            getChartLineNormalColor(axisIndex)
//                        }
//                    }
//                },
//                lines = additiveSensorLines,
//            )
//            elements += additiveSensorLines
//        }
//
//        //--- следующие графики - скорость расхода топлива по счётчику (если они заданы)
//
//        counterSensorEntities.forEach { counterSensorEntity ->
//            val counterMinView = counterSensorEntity.minView?.toFloat() ?: 0.0f
//            val counterMaxView = counterSensorEntity.maxView?.toFloat() ?: (levelMaxView / 10)
//
//            axisIndex++
//            alAxisYData += ChartAxisData(
//                title = "Скорость расхода [л/час]",
//                min = counterMinView,
//                max = counterMaxView,
//                color = getChartAxisColor(axisIndex),
//                isReversedY = SensorConfig.isReversedChart(counterSensorEntity.sensorType)
//            )
//
//            val usingSpeedLines = ChartElementData(ChartElementType.LINE, axisIndex, 1, SensorConfig.isReversedChart(counterSensorEntity.sensorType))
//            getLines(
//                sensorEntity = counterSensorEntity,
//                valueIndex = 3,
//                begTime = begTime,
//                endTime = endTime,
//                xScale = xScale,
//                yScale = yScale,
//                colorFun = { value: Double ->
//                    when {
//                        counterSensorEntity.minLimit != null && counterSensorEntity.minLimit != counterSensorEntity.maxLimit && value < counterSensorEntity.minLimit -> {
//                            getChartLineBelowColor(axisIndex)
//                        }
//
//                        counterSensorEntity.maxLimit != null && counterSensorEntity.minLimit != counterSensorEntity.maxLimit && value > counterSensorEntity.maxLimit -> {
//                            getChartLineAboveColor(axisIndex)
//                        }
//
//                        else -> {
//                            getChartLineNormalColor(axisIndex)
//                        }
//                    }
//                },
//                lines = usingSpeedLines,
//            )
//            elements += usingSpeedLines
//        }
//
////            val texts = ChartElement(ChartElementType.TEXT, axisIndex, 0, isReversedY)
//        /*
//
//                        //--- если вывод текстов задан, сделаем вывод режимов работы оборудования
//                        objectConfig.hmSensorConfig[SensorConfig.SENSOR_WORK]?.let { hmSC ->
//                            val alGTD = aText.alGTD.toMutableList()
//                            hmSC.values.forEach { sc ->
//                                val scw = sc as SensorConfigWork
//                                //--- пропускаем датчики работы оборудования не из своей группы
//                                if (scw.group == sca.group) {
//                                    val alWork = ObjectCalc.calcWorkSensor(alRawTime, alRawData, scw, begTime, endTime).alWorkOnOff
//                                    for (apd in alWork) {
//                                        val workDescr = StringBuilder(scw.descr).append(" : ").append(if (apd.getState() != 0) "ВКЛ" else "выкл").toString()
//                                        alGTD += ChartElementTextDTO(
//                                            textX1 = apd.begTime,
//                                            textX2 = apd.endTime,
//                                            fillColorIndex = if (apd.getState() != 0) {
//                                                ChartColorIndex.FILL_NORMAL
//                                            } else {
//                                                ChartColorIndex.FILL_WARNING
//                                            },
//                                            borderColorIndex = if (apd.getState() != 0) {
//                                                ChartColorIndex.BORDER_NORMAL
//                                            } else {
//                                                ChartColorIndex.BORDER_WARNING
//                                            },
//                                            textColorIndex = if (apd.getState() != 0) {
//                                                ChartColorIndex.TEXT_NORMAL
//                                            } else {
//                                                ChartColorIndex.TEXT_WARNING
//                                            },
//                                            text = workDescr,
//                                            toolTip = workDescr
//                                        )
//                                    }
//                                }
//                            }
//                            aText.alGTD = alGTD
//                        }
//
//                        //--- общие нештатные ситуации показываем после работы оборудования,
//                        //--- отображаемого в виде сплошной полосы различного цвета и
//                        //--- после специфических (как правило - более критических) ошибок конкретных датчиков
//                        if (!isCommonTroubleShowed) {
//                            checkCommonTrouble(alRawTime, alRawData, objectConfig, begTime, endTime, aText)
//                            isCommonTroubleShowed = true
//                        }
//
//                        //--- пост-обработка графика
//                        graphicElementPostCalc(
//                            begTime = begTime,
//                            endTime = endTime,
//                            sca = sca,
//                            alRawTime = alRawTime,
//                            alRawData = alRawData,
//                            axisIndex = axisIndex,
//                            aLine = aLine,
//                            aText = aText,
//                        )
//                        alGDC.addAll(listOfNotNull(aText, aMinLimit, aMaxLimit, aLine).filter { it.isNotEmpty() })
//                        axisIndex++
//
//                        //--- добавление дополнительных графиков на то же поле
//                        axisIndex = addGraphicItem(
//                            begTime = begTime,
//                            endTime = endTime,
//                            viewWidth = viewWidth,
//                            viewHeight = viewHeight,
//                            alRawTime = alRawTime,
//                            alRawData = alRawData,
//                            objectConfig = objectConfig,
//                            sca = sca,
//                            aAxisIndex = axisIndex,
//                            alAxisYData = alAxisYData,
//                            aLine = aLine,
//                            aText = aText,
//                            alGDC = alGDC,
//                        )
//         */
////            }
//
//        tmElement[chartTitle] = ChartData(
//            title = chartTitle,
//            legends = legends,
//            height = -1.0f,
//            axises = alAxisYData,
//            elements = elements,
//        )
//    }
//
//}
