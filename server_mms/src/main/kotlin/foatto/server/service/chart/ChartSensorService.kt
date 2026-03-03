package foatto.server.service.chart

import foatto.core.i18n.LocalizedMessages
import foatto.core.i18n.getLocalizedMessage
import foatto.core.model.AppAction
import foatto.core.model.request.ChartActionRequest
import foatto.core.model.response.ChartActionResponse
import foatto.core.model.response.HeaderData
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.TitleData
import foatto.core.model.response.chart.ChartData
import foatto.core_mms.AppModuleMMS
import foatto.core_mms.i18n.LocalizedMMSMessages
import foatto.core_mms.i18n.getLocalizedMMSMessage
import foatto.server.appModuleConfigs
import foatto.server.model.ServerUserConfig
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class ChartSensorService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
) : MMSChartService(
    entityManager = entityManager,
    objectRepository = objectRepository,
    sensorRepository = sensorRepository,
) {

    override fun getChartHeader(userConfig: ServerUserConfig, action: AppAction): HeaderData {
        val sensorEntity = if (action.parentModule == AppModuleMMS.SENSOR) {
            action.parentId?.let { parentId -> sensorRepository.findByIdOrNull(parentId) }
        } else {
            null
        }

        val caption = appModuleConfigs[action.module]?.captions?.let { captions ->
            getLocalizedMessage(captions, userConfig.lang)
        } ?: "(${getLocalizedMessage(LocalizedMessages.UNKNOWN_MODULE, userConfig.lang)}: ${action.module})"

        val rows = listOf(
            getLocalizedMMSMessage(LocalizedMMSMessages.OBJECT_NAME, userConfig.lang) to (sensorEntity?.obj?.name ?: "-"),
            getLocalizedMMSMessage(LocalizedMMSMessages.MODEL, userConfig.lang) to (sensorEntity?.obj?.model ?: "-"),
            getLocalizedMMSMessage(LocalizedMMSMessages.SENSOR_NAME, userConfig.lang) to (sensorEntity?.descr ?: "-"),
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

        if (chartActionRequest.action.parentModule == AppModuleMMS.SENSOR) {
            chartActionRequest.action.parentId?.let { sensorId ->
                sensorRepository.findByIdOrNull(sensorId)?.let { sensorEntity ->
                    val begTimeCheck = sensorEntity.endTime?.let { sensorEndTime ->
                        begTime < sensorEndTime
                    } ?: true
                    val endTimeCheck = sensorEntity.begTime?.let { sensorBegTime ->
                        endTime > sensorBegTime
                    } ?: true
                    if (begTimeCheck && endTimeCheck) {
                        charts += getSensorChart(
                            userConfig = userConfig,
                            sensorEntity = sensorEntity,
                            begTime = begTime,
                            endTime = endTime,
                            viewWidth = viewWidth,
                            viewHeight = viewHeight,
                        )
                    }
                }
            }
        }

        return ChartActionResponse(
            responseCode = ResponseCode.OK,
            charts = charts,
        )
    }
}