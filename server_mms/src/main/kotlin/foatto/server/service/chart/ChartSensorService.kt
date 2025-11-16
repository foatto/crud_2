package foatto.server.service.chart

import foatto.core.i18n.getLocalizedMessage
import foatto.core.model.AppAction
import foatto.core.model.request.ChartActionRequest
import foatto.core.model.response.ChartActionResponse
import foatto.core.model.response.HeaderData
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.TitleData
import foatto.core.model.response.chart.ChartAxisData
import foatto.core.model.response.chart.ChartData
import foatto.core.model.response.chart.ChartElementData
import foatto.core.model.response.chart.ChartElementLineData
import foatto.core.model.response.chart.ChartLegendData
import foatto.server.appModuleConfigs
import foatto.server.entity.SensorEntity
import foatto.server.model.ServerUserConfig
import foatto.server.model.sensor.SensorConfig
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import foatto.server.service.ApplicationService
import foatto.server.service.SensorService
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import kotlin.math.abs

@Service
class ChartSensorService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
) : AbstractChartService(
    entityManager = entityManager,
    objectRepository = objectRepository,
    sensorRepository = sensorRepository,
) {

    override fun getChartHeader(userConfig: ServerUserConfig, action: AppAction): HeaderData {
        val sensorEntity = action.id?.let { id -> sensorRepository.findByIdOrNull(id) }

        val caption = appModuleConfigs[action.module]?.captions?.let { captions ->
            getLocalizedMessage(captions, userConfig.lang)
        } ?: "(неизвестный модуль: ${action.module})"
        val rows = listOf(
            "Наименование объекта" to (sensorEntity?.obj?.name ?: "-"),
            "Модель" to (sensorEntity?.obj?.model ?: "-"),
            "Наименование датчика" to (sensorEntity?.descr ?: "-"),
        )

        return HeaderData(
            titles = listOf(
                TitleData(
                    action = null,
                    text = caption,
                    isBold = true,
                )
            ),
            rows = rows,
        )
    }

    override fun getCharts(chartActionRequest: ChartActionRequest): ChartActionResponse {
        val (begTime, endTime) = chartActionRequest.times
        val (viewWidth, viewHeight) = chartActionRequest.viewSize

        var charts: List<ChartData> = emptyList()

        chartActionRequest.action.id?.let { sensorId ->
            sensorRepository.findByIdOrNull(sensorId)?.let { sensorEntity ->
                val begTimeCheck = sensorEntity.endTime?.let { sensorEndTime ->
                    begTime < sensorEndTime
                } ?: true
                val endTimeCheck = sensorEntity.begTime?.let { sensorBegTime ->
                    endTime > sensorBegTime
                } ?: true
                if (begTimeCheck && endTimeCheck) {
                    charts = getChart(
                        sensorEntity = sensorEntity,
                        begTime = begTime,
                        endTime = endTime,
                        viewWidth = viewWidth,
                        viewHeight = viewHeight,
                    )
                }
            }
        }

        return ChartActionResponse(
            responseCode = ResponseCode.OK,
            charts = charts,
        )
    }

    private fun getChart(
        sensorEntity: SensorEntity,
        begTime: Int,
        endTime: Int,
        viewWidth: Float,
        viewHeight: Float,
    ): List<ChartData> {

        val chartTitle = sensorEntity.descr ?: "-"
        val minView = sensorEntity.minView?.toFloat() ?: 0.0f
        val maxView = sensorEntity.maxView?.toFloat() ?: 1_000_000_000.0f

        val xScale = (endTime - begTime) / viewWidth
        val yScale = (maxView - minView) / viewHeight

        val elements = mutableListOf<ChartElementData>()
        val axises = mutableListOf<ChartAxisData>()
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

        elements += getChartElement(
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
            colorFun = { value: Double ->
                when {
                    sensorEntity.minLimit != null && sensorEntity.minLimit != sensorEntity.maxLimit && value < sensorEntity.minLimit!! -> {
                        getChartLineBelowColor(axisIndex)
                    }

                    sensorEntity.maxLimit != null && sensorEntity.minLimit != sensorEntity.maxLimit && value > sensorEntity.maxLimit!! -> {
                        getChartLineAboveColor(axisIndex)
                    }

                    else -> {
                        getChartLineNormalColor(axisIndex)
                    }
                }
            },
        )

        return listOf(
            ChartData(
                title = chartTitle,
                axises = axises,
                elements = elements,
                legends = legends,
            )
        )
    }

    private fun getChartElement(
        sensorEntity: SensorEntity,
        valueFieldName: String,
        begTime: Int,
        endTime: Int,
        xScale: Float,
        yScale: Float,
        isReversedY: Boolean,
        axisIndex: Int,
        lineWidth: Int,
        colorFun: (value: Double) -> Int,
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

                val color = colorFun(value)

                val lastPoint = lines.lastOrNull()
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

}
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