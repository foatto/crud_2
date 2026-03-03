package foatto.server.service.chart

import foatto.core.model.response.chart.ChartAxisData
import foatto.core.model.response.chart.ChartData
import foatto.core.model.response.chart.ChartElementData
import foatto.core.model.response.chart.ChartElementLineData
import foatto.core.model.response.chart.ChartElementTextData
import foatto.core.model.response.chart.ChartLegendData
import foatto.server.ds.MMSTelematicFunction
import foatto.server.entity.SensorEntity
import foatto.server.model.ServerUserConfig
import foatto.server.model.sensor.SensorConfig
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import foatto.server.service.AbstractChartService
import foatto.server.service.ApplicationService
import foatto.server.service.SensorService
import jakarta.persistence.EntityManager
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

abstract class MMSChartService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
) : AbstractChartService(
    entityManager = entityManager,
) {

    companion object {
        protected const val MIN_NO_DATA_TIME: Int = 15 * 60
        private const val TEXT_DATA_MIN_VISIBLE_WIDTH = 4    // минимальная ширина видимого текстового блока
    }

    protected fun getSensorChart(
        userConfig: ServerUserConfig,
        sensorEntity: SensorEntity,
        begTime: Int,
        endTime: Int,
        viewWidth: Float,
        viewHeight: Float,
    ): ChartData {

        val chartTitle = "${sensorEntity.descr ?: "-"} [${sensorEntity.dim ?: "-"}]"
        val minView = sensorEntity.minView?.toFloat() ?: 0.0f
        val maxView = sensorEntity.maxView?.toFloat() ?: 1_000_000_000.0f

        val xScale = (endTime - begTime) / viewWidth
        val yScale = (maxView - minView) / viewHeight

        val axises = mutableListOf<ChartAxisData>()
        val elements = mutableListOf<ChartElementData>()
        val legends = mutableListOf<ChartLegendData>()

        val isReversedY = SensorConfig.isReversedChart(sensorEntity.sensorType)

        val axisIndex = 0
        axises += ChartAxisData(
            title = chartTitle,
            min = minView,
            max = maxView,
            color = getChartAxisColor(axisIndex),
            isReversedY = isReversedY
        )

        if (sensorEntity.minLimit != sensorEntity.maxLimit) {
            sensorEntity.minLimit?.let { minLimit ->
                elements += ChartElementData(
                    isReversedY = isReversedY,
                    axisIndex = axisIndex,
                    lineWidth = 1,
                    lines = listOf(
                        ChartElementLineData(x = begTime, y = minLimit.toFloat(), color = LINE_LIMIT),
                        ChartElementLineData(x = endTime, y = minLimit.toFloat(), color = LINE_LIMIT),
                    ),
                )
            }
            sensorEntity.maxLimit?.let { maxLimit ->
                elements += ChartElementData(
                    isReversedY = isReversedY,
                    axisIndex = axisIndex,
                    lineWidth = 1,
                    lines = listOf(
                        ChartElementLineData(x = begTime, y = maxLimit.toFloat(), color = LINE_LIMIT),
                        ChartElementLineData(x = endTime, y = maxLimit.toFloat(), color = LINE_LIMIT),
                    ),
                )
            }
        }

        elements += getChartLineElement(
            sensorEntity = sensorEntity,
            valueFieldName = when (sensorEntity.sensorType) {
                SensorConfig.SENSOR_GEO -> "type_0"
                in SensorConfig.analogueSensorTypes -> "value_1"
                in SensorConfig.counterSensorTypes -> "value_0" // not need yet: value_1 - delta, value_2 - average/hour
                else -> "value_0"
            },
            begTime = begTime,
            endTime = endTime,
            xScale = xScale,
            yScale = yScale,
            isReversedY = isReversedY,
            axisIndex = axisIndex,
            lineWidth = 3,
            colorFun = { prevTime: Int?, curTime: Int, value: Double ->
                when {
                    sensorEntity.minLimit != null && sensorEntity.minLimit != sensorEntity.maxLimit && value < sensorEntity.minLimit!! -> {
                        getChartLineBelowColor(axisIndex)
                    }

                    sensorEntity.maxLimit != null && sensorEntity.minLimit != sensorEntity.maxLimit && value > sensorEntity.maxLimit!! -> {
                        getChartLineAboveColor(axisIndex)
                    }

                    else -> {
                        prevTime?.let {
                            if (curTime - prevTime <= MIN_NO_DATA_TIME) {
                                getChartLineNormalColor(axisIndex)
                            } else {
                                getChartLineNoneColor(axisIndex)
                            }

                        } ?: getChartLineNormalColor(axisIndex)
                    }
                }
            },
        )

        elements += getChartTextElement(
            userConfig = userConfig,
            sensorEntity = sensorEntity,
            begTime = begTime,
            endTime = endTime,
            xScale = xScale,
            axisIndex = axisIndex,
        )

        return ChartData(
            title = chartTitle,
            axises = axises,
            elements = elements,
            legends = legends,
        )
    }

    protected fun getChartLineElement(
        sensorEntity: SensorEntity,
        valueFieldName: String,
        begTime: Int,
        endTime: Int,
        xScale: Float,
        yScale: Float,
        isReversedY: Boolean,
        axisIndex: Int,
        lineWidth: Int,
        colorFun: (prevTime: Int?, curTime: Int, value: Double) -> Int,
    ): ChartElementData {
        val lines = mutableListOf<ChartElementLineData>()

        SensorService.checkAndCreateSensorTables(entityManager, sensorEntity.id)

        ApplicationService.withConnection(entityManager) { conn ->
            val rs = conn.executeQuery(
                """
                    SELECT ontime_0, $valueFieldName
                    FROM MMS_agg_${sensorEntity.id}
                    WHERE ontime_0 BETWEEN $begTime AND $endTime
                    ORDER BY ontime_0
                """
            )
            while (rs.next()) {
                val time = rs.getInt(1)
                val value = rs.getDouble(2)

                val lastPoint = lines.lastOrNull()

                val color = colorFun(lastPoint?.x, time, value)

                if (lastPoint == null || time - lastPoint.x > xScale || abs(value - lastPoint.y) > yScale || color != lastPoint.color) {
                    lines += ChartElementLineData(time, value.toFloat(), color)
                }
            }
            rs.close()
        }

        return ChartElementData(
            isReversedY = isReversedY,
            axisIndex = axisIndex,
            lineWidth = lineWidth,
            lines = lines,
        )
    }

    protected fun getChartTextElement(
        userConfig: ServerUserConfig,
        sensorEntity: SensorEntity,
        begTime: Int,
        endTime: Int,
        xScale: Float,
        axisIndex: Int,
    ): ChartElementData {
        val texts = mutableListOf<ChartElementTextData>()

        SensorService.checkAndCreateSensorTables(entityManager, sensorEntity.id)

        ApplicationService.withConnection(entityManager) { conn ->
            val rs = conn.executeQuery(
                """
                    SELECT ontime_0 , ontime_1 , type_0 , code_0
                    FROM MMS_text_${sensorEntity.id}
                    WHERE ontime_0 < $endTime
                      AND ontime_1 > $begTime  
                    ORDER BY ontime_0
                """
            )
            while (rs.next()) {
                var pos = 1
                val time0 = rs.getInt(pos++)
                val time1 = rs.getInt(pos++)
                val type = rs.getInt(pos++)
                val code = rs.getInt(pos++)

                //val color = colorFun(lastPoint?.x, time, value) - цвет (фона?) в зависимости от type

                if (time1 - time0 > xScale * TEXT_DATA_MIN_VISIBLE_WIDTH) {
                    texts += ChartElementTextData(
                        x1 = max(time0, begTime),
                        x2 = min(time1, endTime),
                        fillColor = FILL_CRITICAL,
                        borderColor = BORDER_CRITICAL,
                        textColor = getChartAxisColor(axisIndex),   //TEXT_CRITICAL,
                        text = MMSTelematicFunction.getText(code, userConfig.lang),
                    )
                }
            }
            rs.close()
        }

        return ChartElementData(
            isReversedY = false,
            axisIndex = axisIndex,
            texts = texts,
        )
    }

    protected open fun getAxisTitle(sensorEntity: SensorEntity): String = "${sensorEntity.descr ?: "-"} [${sensorEntity.dim ?: "-"}]"

}

/*
    companion object {
        //const val MIN_CONNECT_OFF_TIME = 15 * 60
        private const val MIN_POWER_OFF_TIME = 5 * 60
        private const val MIN_LIQUID_COUNTER_STATE_TIME = 5 * 60

        //--- ловля основных/системных нештатных ситуаций, показываемых только на первом/верхнем графике:
        //--- нет связи, нет данных и резервное питание
        fun checkCommonTrouble(
            alRawTime: List<Int>,
            alRawData: List<AdvancedByteBuffer>,
            oc: ObjectConfig,
            begTime: Int,
            endTime: Int,
            aText: ChartElementDTO
        ) {
            val alGTD = aText.alGTD.toMutableList()

            //--- поиск значительных промежутков отсутствия данных ---

            var lastDataTime = begTime
            for (rawTime in alRawTime) {
                //--- сразу пропускаем запредельные точки, загруженные для бесшовного сглаживания между соседними диапазонами
                if (rawTime < begTime) continue
                if (rawTime > endTime) break

                if (rawTime - lastDataTime > MIN_NO_DATA_TIME) {
                    alGTD += ChartElementTextDTO(
                        textX1 = lastDataTime,
                        textX2 = rawTime,
                        fillColorIndex = ChartColorIndex.FILL_CRITICAL,
                        borderColorIndex = ChartColorIndex.BORDER_CRITICAL,
                        textColorIndex = ChartColorIndex.TEXT_CRITICAL,
                        text = "Нет данных от контроллера",
                        toolTip = "Нет данных от контроллера"
                    )
                }
                lastDataTime = rawTime
            }
            if (min(lastDataTime, endTime) - lastDataTime > MIN_NO_DATA_TIME) {
                alGTD += ChartElementTextDTO(
                    textX1 = lastDataTime,
                    textX2 = min(lastDataTime, endTime),
                    fillColorIndex = ChartColorIndex.FILL_CRITICAL,
                    borderColorIndex = ChartColorIndex.BORDER_CRITICAL,
                    textColorIndex = ChartColorIndex.TEXT_CRITICAL,
                    text = "Нет данных от контроллера",
                    toolTip = "Нет данных от контроллера"
                )
            }

            //--- поиск значительных промежутков отсутствия основного питания ( перехода на резервное питание )
            oc.hmSensorConfig[SensorConfig.SENSOR_VOLTAGE]?.values?.forEach { sc ->
                val sca = sc as SensorConfigAnalogue
                //--- чтобы не смешивались разные ошибки по одному датчику и одинаковые ошибки по разным датчикам,
                //--- добавляем в описание ошибки не только само описание ошибки, но и описание датчика
                checkSensorError(
                    alRawTime = alRawTime,
                    alRawData = alRawData,
                    portNum = sca.portNum,
                    sensorDescr = sca.descr,
                    begTime = begTime,
                    endTime = endTime,
                    aFillColorIndex = ChartColorIndex.FILL_WARNING,
                    aBorderColorIndex = ChartColorIndex.BORDER_WARNING,
                    aTextColorIndex = ChartColorIndex.TEXT_WARNING,
                    troubleCode = 0,
                    troubleDescr = "Нет питания",
                    minTime = MIN_POWER_OFF_TIME,
                    alGTD = alGTD
                )
            }

//!!! временно отключим - больше мешают, чем помогают
            //--- поиск критических режимов работы счётчика топлива EuroSens Delta
//            oc.hmSensorConfig[SensorConfig.SENSOR_LIQUID_USING_COUNTER_STATE]?.values?.forEach { sc ->
//                listOf(
//                    SensorConfigCounter.STATUS_OVERLOAD,
//                    SensorConfigCounter.STATUS_CHEAT,
//                    SensorConfigCounter.STATUS_REVERSE,
//                    SensorConfigCounter.STATUS_INTERVENTION,
//                ).forEach { stateCode ->
//                    checkSensorError(
//                        alRawTime = alRawTime,
//                        alRawData = alRawData,
//                        portNum = sc.portNum,
//                        sensorDescr = sc.descr,
//                        begTime = begTime,
//                        endTime = endTime,
//                        aFillColorIndex = GraphicColorIndex.FILL_CRITICAL,
//                        aBorderColorIndex = GraphicColorIndex.BORDER_CRITICAL,
//                        aTextColorIndex = GraphicColorIndex.TEXT_CRITICAL,
//                        troubleCode = stateCode,
//                        troubleDescr = SensorConfigCounter.hmStatusDescr[stateCode] ?: "(неизвестный код состояния)",
//                        minTime = MIN_LIQUID_COUNTER_STATE_TIME,
//                        alGTD = alGTD
//                    )
//                }
//                listOf(
//                    SensorConfigCounter.STATUS_UNKNOWN,
//                    SensorConfigCounter.STATUS_IDLE,
//                    //SensorConfigCounter.STATUS_NORMAL,
//                ).forEach { stateCode ->
//                    checkSensorError(
//                        alRawTime = alRawTime,
//                        alRawData = alRawData,
//                        portNum = sc.portNum,
//                        sensorDescr = sc.descr,
//                        begTime = begTime,
//                        endTime = endTime,
//                        aFillColorIndex = GraphicColorIndex.FILL_WARNING,
//                        aBorderColorIndex = GraphicColorIndex.BORDER_WARNING,
//                        aTextColorIndex = GraphicColorIndex.TEXT_WARNING,
//                        troubleCode = stateCode,
//                        troubleDescr = SensorConfigCounter.hmStatusDescr[stateCode] ?: "(неизвестный код состояния)",
//                        minTime = MIN_LIQUID_COUNTER_STATE_TIME,
//                        alGTD = alGTD
//                    )
//                }
//            }

            aText.alGTD = alGTD
        }

        fun checkSensorError(
            alRawTime: List<Int>,
            alRawData: List<AdvancedByteBuffer>,
            portNum: Int,
            sensorDescr: String,
            begTime: Int,
            endTime: Int,
            aFillColorIndex: ChartColorIndex,
            aBorderColorIndex: ChartColorIndex,
            aTextColorIndex: ChartColorIndex,
            troubleCode: Int,
            troubleDescr: String,
            minTime: Int,
            alGTD: MutableList<ChartElementTextDTO>
        ) {

            //--- в основном тексте пишем только текст ошибки, а в tooltips'e напишем вместе с описанием датчика
            val fullTroubleDescr = StringBuilder(sensorDescr).append(": ").append(troubleDescr).toString()
            var troubleBegTime = 0
            var sensorData: Int

            for (pos in alRawTime.indices) {
                val rawTime = alRawTime[pos]
                //--- сразу пропускаем запредельные точки, загруженные для бесшовного сглаживания между соседними диапазонами
                if (rawTime < begTime) continue
                if (rawTime > endTime) break

                sensorData = AbstractObjectStateCalc.getSensorData(portNum, alRawData[pos])?.toInt() ?: continue
                if (sensorData == troubleCode) {
                    if (troubleBegTime == 0) {
                        troubleBegTime = rawTime
                    }
                } else if (troubleBegTime != 0) {
                    if (rawTime - troubleBegTime > minTime) {
                        alGTD += ChartElementTextDTO(
                            textX1 = troubleBegTime,
                            textX2 = rawTime,
                            fillColorIndex = aFillColorIndex,
                            borderColorIndex = aBorderColorIndex,
                            textColorIndex = aTextColorIndex,
                            text = troubleDescr,
                            toolTip = fullTroubleDescr
                        )
                    }
                    troubleBegTime = 0
                }
            }
            //--- запись последней незакрытой проблемы
            if (troubleBegTime != 0 && min(getCurrentTimeInt(), endTime) - troubleBegTime > minTime) {
                alGTD += ChartElementTextDTO(
                    textX1 = troubleBegTime,
                    textX2 = min(getCurrentTimeInt(), endTime),
                    fillColorIndex = aFillColorIndex,
                    borderColorIndex = aBorderColorIndex,
                    textColorIndex = aTextColorIndex,
                    text = troubleDescr,
                    toolTip = fullTroubleDescr
                )
            }
        }
    }

 */
//            val texts = ChartElement(ChartElementType.TEXT, axisIndex, 0, isReversedY)
/*

                //--- если вывод текстов задан, сделаем вывод режимов работы оборудования
                objectConfig.hmSensorConfig[SensorConfig.SENSOR_WORK]?.let { hmSC ->
                    val alGTD = aText.alGTD.toMutableList()
                    hmSC.values.forEach { sc ->
                        val scw = sc as SensorConfigWork
                        //--- пропускаем датчики работы оборудования не из своей группы
                        if (scw.group == sca.group) {
                            val alWork = ObjectCalc.calcWorkSensor(alRawTime, alRawData, scw, begTime, endTime).alWorkOnOff
                            for (apd in alWork) {
                                val workDescr = StringBuilder(scw.descr).append(" : ").append(if (apd.getState() != 0) "ВКЛ" else "выкл").toString()
                                alGTD += ChartElementTextDTO(
                                    textX1 = apd.begTime,
                                    textX2 = apd.endTime,
                                    fillColorIndex = if (apd.getState() != 0) {
                                        ChartColorIndex.FILL_NORMAL
                                    } else {
                                        ChartColorIndex.FILL_WARNING
                                    },
                                    borderColorIndex = if (apd.getState() != 0) {
                                        ChartColorIndex.BORDER_NORMAL
                                    } else {
                                        ChartColorIndex.BORDER_WARNING
                                    },
                                    textColorIndex = if (apd.getState() != 0) {
                                        ChartColorIndex.TEXT_NORMAL
                                    } else {
                                        ChartColorIndex.TEXT_WARNING
                                    },
                                    text = workDescr,
                                    toolTip = workDescr
                                )
                            }
                        }
                    }
                    aText.alGTD = alGTD
                }

                //--- общие нештатные ситуации показываем после работы оборудования,
                //--- отображаемого в виде сплошной полосы различного цвета и
                //--- после специфических (как правило - более критических) ошибок конкретных датчиков
                if (!isCommonTroubleShowed) {
                    checkCommonTrouble(alRawTime, alRawData, objectConfig, begTime, endTime, aText)
                    isCommonTroubleShowed = true
                }

                //--- пост-обработка графика
                graphicElementPostCalc(
                    begTime = begTime,
                    endTime = endTime,
                    sca = sca,
                    alRawTime = alRawTime,
                    alRawData = alRawData,
                    axisIndex = axisIndex,
                    aLine = aLine,
                    aText = aText,
                )
                alGDC.addAll(listOfNotNull(aText, aMinLimit, aMaxLimit, aLine).filter { it.isNotEmpty() })
                axisIndex++

                //--- добавление дополнительных графиков на то же поле
                axisIndex = addGraphicItem(
                    begTime = begTime,
                    endTime = endTime,
                    viewWidth = viewWidth,
                    viewHeight = viewHeight,
                    alRawTime = alRawTime,
                    alRawData = alRawData,
                    objectConfig = objectConfig,
                    sca = sca,
                    aAxisIndex = axisIndex,
                    alAxisYData = alAxisYData,
                    aLine = aLine,
                    aText = aText,
                    alGDC = alGDC,
                )
 */

/*
    //--- упрощённый вывод данных по гео-датчику (движение/стоянка/нет данных АКА ошибка)
    //--- (без учёта минимального времени стоянки)
    private fun calcGeoSensor(alRawTime: List<Int>, alRawData: List<AdvancedByteBuffer>, scg: SensorConfigGeo, begTime: Int, endTime: Int, aBack: ChartElementDTO) {
        var lastStatus = -2 // -1 = нет гео-данных, 0 = стоянка, 1 - движение
        var lastTime = 0
        val alGBD = aBack.alGBD.toMutableList()
        for (pos in alRawTime.indices) {
            val rawTime = alRawTime[pos]
            //--- данные до запрашиваемого диапазона ( расширенные для сглаживания )
            //--- в данном случае не интересны и их можно пропустить
            if (rawTime < begTime) {
                continue
            }
            //--- данные после запрашиваемого диапазона ( расширенные для сглаживания )
            //--- в данном случае не интересны и можно прекращать обработку
            if (rawTime > endTime) {
                break
            }

            val gd = AbstractObjectStateCalc.getGeoData(scg, alRawData[pos])
            //--- самих геоданных может и не оказаться (нет датчика или нет или ошибка GPS-данных)
            if (gd == null) {
                //--- если до этого было другое состояние (движение или стоянка)
                if (lastStatus != -1) {
                    //--- если это не первое состояние
                    if (lastTime != 0) {
                        alGBD += ChartElementBackDTO(
                            x1 = lastTime,
                            x2 = rawTime,
                            color = hmIndexColor[if (lastStatus == 0) {
                                ChartColorIndex.FILL_WARNING
                            } else {
                                ChartColorIndex.FILL_NORMAL
                            }] ?: 0
                        )
                    }
                    lastStatus = -1
                    lastTime = rawTime
                }
            } else if (gd.speed <= AbstractObjectStateCalc.MAX_SPEED_AS_PARKING) {
                //--- если до этого было другое состояние (движение или ошибка)
                if (lastStatus != 0) {
                    //--- если это не первое состояние
                    if (lastTime != 0) {
                        alGBD += ChartElementBackDTO(
                            x1 = lastTime,
                            x2 = rawTime,
                            color = hmIndexColor[if (lastStatus == -1) {
                                ChartColorIndex.FILL_CRITICAL
                            } else {
                                ChartColorIndex.FILL_NORMAL
                            }] ?: 0
                        )
                    }
                    lastStatus = 0
                    lastTime = rawTime
                }
            } else {
                //--- если до этого было другое состояние (стоянка или ошибка)
                if (lastStatus != 1) {
                    //--- если это не первое состояние
                    if (lastTime != 0) {
                        alGBD += ChartElementBackDTO(
                            x1 = lastTime,
                            x2 = rawTime,
                            color = hmIndexColor[if (lastStatus == -1) {
                                ChartColorIndex.FILL_CRITICAL
                            } else {
                                ChartColorIndex.FILL_WARNING
                            }] ?: 0
                        )
                    }
                    lastStatus = 1
                    lastTime = rawTime
                }
            }//--- движение
            //--- стоянка
        }
        //--- если это не первое состояние
        if (lastTime != 0) {
            alGBD += ChartElementBackDTO(
                x1 = lastTime,
                x2 = min(getCurrentTimeInt(), endTime),
                color = hmIndexColor[if (lastStatus == -1) {
                    ChartColorIndex.FILL_CRITICAL
                } else if (lastStatus == 0) {
                    ChartColorIndex.FILL_WARNING
                } else {
                    ChartColorIndex.FILL_NORMAL
                }] ?: 0
            )
        }
        aBack.alGBD = alGBD
    }

 */