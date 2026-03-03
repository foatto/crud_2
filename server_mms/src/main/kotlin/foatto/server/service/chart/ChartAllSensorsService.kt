package foatto.server.service.chart

import foatto.core.model.response.chart.ChartData
import foatto.server.entity.ObjectEntity
import foatto.server.model.ServerUserConfig
import foatto.server.model.sensor.SensorConfig
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service

@Service
class ChartAllSensorsService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
) : AbstractObjectChartService(
    entityManager = entityManager,
    objectRepository = objectRepository,
    sensorRepository = sensorRepository,
) {

    override fun getObjectCharts(
        userConfig: ServerUserConfig,
        objectEntity: ObjectEntity,
        begTime: Int,
        endTime: Int,
        viewWidth: Float,
        viewHeight: Float,
        charts: MutableList<ChartData>
    ) {
        (setOf(SensorConfig.SENSOR_GEO) + SensorConfig.analogueSensorTypes + SensorConfig.counterSensorTypes).forEach { sensorType ->
            sensorRepository.findByObjAndSensorType(objectEntity, sensorType).forEach { sensorEntity ->
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
}