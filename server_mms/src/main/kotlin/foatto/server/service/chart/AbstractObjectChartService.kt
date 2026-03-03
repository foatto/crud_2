package foatto.server.service.chart

import foatto.core.i18n.LocalizedMessages
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
import foatto.core.model.response.chart.ChartLegendData
import foatto.core_mms.AppModuleMMS
import foatto.core_mms.i18n.LocalizedMMSMessages
import foatto.core_mms.i18n.getLocalizedMMSMessage
import foatto.server.appModuleConfigs
import foatto.server.entity.ObjectEntity
import foatto.server.entity.SensorEntity
import foatto.server.model.ServerUserConfig
import foatto.server.model.sensor.SensorConfig
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull

abstract class AbstractObjectChartService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
) : MMSChartService(
    entityManager = entityManager,
    objectRepository = objectRepository,
    sensorRepository = sensorRepository,
) {

    override fun getChartHeader(userConfig: ServerUserConfig, action: AppAction): HeaderData {
        val objectEntity = if (action.parentModule in setOf(AppModuleMMS.ALL_OBJECT, AppModuleMMS.MOBILE_OBJECT, AppModuleMMS.STATIONARY_OBJECT)) {
            action.parentId?.let { parentId -> objectRepository.findByIdOrNull(parentId) }
        } else {
            null
        }

        val caption = appModuleConfigs[action.module]?.captions?.let { captions ->
            getLocalizedMessage(captions, userConfig.lang)
        } ?: "(${getLocalizedMessage(LocalizedMessages.UNKNOWN_MODULE, userConfig.lang)}: ${action.module})"

        val rows = listOf(
            getLocalizedMMSMessage(LocalizedMMSMessages.OBJECT_NAME, userConfig.lang) to (objectEntity?.name ?: "-"),
            getLocalizedMMSMessage(LocalizedMMSMessages.MODEL, userConfig.lang) to (objectEntity?.model ?: "-"),
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

    override fun getCharts(userConfig: ServerUserConfig, chartActionRequest: ChartActionRequest): ChartActionResponse {
        val (begTime, endTime) = chartActionRequest.times
        val (viewWidth, viewHeight) = chartActionRequest.viewSize

        val charts = mutableListOf<ChartData>()

        if (chartActionRequest.action.parentModule in setOf(AppModuleMMS.ALL_OBJECT, AppModuleMMS.MOBILE_OBJECT, AppModuleMMS.STATIONARY_OBJECT)) {
            chartActionRequest.action.parentId?.let { objectId ->
                objectRepository.findByIdOrNull(objectId)?.let { objectEntity ->
                    getObjectCharts(
                        userConfig = userConfig,
                        objectEntity = objectEntity,
                        begTime = begTime,
                        endTime = endTime,
                        viewWidth = viewWidth,
                        viewHeight = viewHeight,
                        charts = charts,
                    )
                }
            }
        }

        return ChartActionResponse(
            responseCode = ResponseCode.OK,
            charts = charts,
        )
    }

    protected abstract fun getObjectCharts(
        userConfig: ServerUserConfig,
        objectEntity: ObjectEntity,
        begTime: Int,
        endTime: Int,
        viewWidth: Float,
        viewHeight: Float,
        charts: MutableList<ChartData>,
    )

    fun getCombinedSensorChart(
        userConfig: ServerUserConfig,
        chartTitle: String,
        sensorEntities: List<SensorEntity>,
        begTime: Int,
        endTime: Int,
        viewWidth: Float,
        viewHeight: Float,
    ): ChartData {
        val axises = mutableListOf<ChartAxisData>()
        val elements = mutableListOf<ChartElementData>()
        val legends = mutableListOf<ChartLegendData>()

        val isReversedY = false // SensorConfig.isReversedChart(sensorEntity.sensorType)

//--- на совмещённых графиках датчиков не будем показывать лимиты, там и так много графиков
//        if (sensorEntity.minLimit != sensorEntity.maxLimit) {
//            sensorEntity.minLimit?.let { minLimit ->
//                elements += ChartElementData(
//                    isReversedY = isReversedY,
//                    axisIndex = axisIndex,
//                    lineWidth = 1,
//                    lines = listOf(
//                        ChartElementLineData(x = begTime, y = minLimit.toFloat(), color = LINE_LIMIT),
//                        ChartElementLineData(x = endTime, y = minLimit.toFloat(), color = LINE_LIMIT),
//                    ),
//                )
//            }
//            sensorEntity.maxLimit?.let { maxLimit ->
//                elements += ChartElementData(
//                    isReversedY = isReversedY,
//                    axisIndex = axisIndex,
//                    lineWidth = 1,
//                    lines = listOf(
//                        ChartElementLineData(x = begTime, y = maxLimit.toFloat(), color = LINE_LIMIT),
//                        ChartElementLineData(x = endTime, y = maxLimit.toFloat(), color = LINE_LIMIT),
//                    ),
//                )
//            }
//        }

        sensorEntities.forEachIndexed { index, phasedEnergoSensorEntity ->
            val minView = phasedEnergoSensorEntity.minView?.toFloat() ?: 0.0f
            val maxView = phasedEnergoSensorEntity.maxView?.toFloat() ?: 1_000_000_000.0f

            val xScale = (endTime - begTime) / viewWidth
            val yScale = (maxView - minView) / viewHeight

            axises += ChartAxisData(
                title = getAxisTitle(phasedEnergoSensorEntity),
                min = minView,
                max = maxView,
                color = getChartAxisColor(index),
                isReversedY = isReversedY
            )

            elements += getChartLineElement(
                sensorEntity = phasedEnergoSensorEntity,
                valueFieldName = when (phasedEnergoSensorEntity.sensorType) {
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
                axisIndex = index,
                lineWidth = 3,
                colorFun = { prevTime: Int?, curTime: Int, value: Double ->
//                    when {
//                        sensorEntity.minLimit != null && sensorEntity.minLimit != sensorEntity.maxLimit && value < sensorEntity.minLimit!! -> {
//                            getChartLineBelowColor(axisIndex)
//                        }
//
//                        sensorEntity.maxLimit != null && sensorEntity.minLimit != sensorEntity.maxLimit && value > sensorEntity.maxLimit!! -> {
//                            getChartLineAboveColor(axisIndex)
//                        }
//
//                        else -> {
                    prevTime?.let {
                        if (curTime - prevTime <= MIN_NO_DATA_TIME) {
                            getChartLineNormalColor(index)
                        } else {
                            getChartLineNoneColor(index)
                        }

                    } ?: getChartLineNormalColor(index)
//                        }
//                    }
                },
            )

            elements += getChartTextElement(
                userConfig = userConfig,
                sensorEntity = phasedEnergoSensorEntity,
                begTime = begTime,
                endTime = endTime,
                xScale = xScale,
                axisIndex = index,
            )
        }

        return ChartData(
            title = chartTitle,
            axises = axises,
            elements = elements,
            legends = legends,
        )
    }

}