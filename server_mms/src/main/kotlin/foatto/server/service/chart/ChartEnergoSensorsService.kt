package foatto.server.service.chart

import foatto.core.model.response.chart.ChartData
import foatto.server.entity.ObjectEntity
import foatto.server.entity.SensorEntity
import foatto.server.model.ServerUserConfig
import foatto.server.model.sensor.SensorConfig
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service

@Service
class ChartEnergoSensorsService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
) : AbstractObjectChartService(
    entityManager = entityManager,
    objectRepository = objectRepository,
    sensorRepository = sensorRepository,
) {

    companion object {
        val phasedSensorTypes = listOf(
            SensorConfig.SENSOR_ENERGO_VOLTAGE,
            SensorConfig.SENSOR_ENERGO_CURRENT,
            SensorConfig.SENSOR_ENERGO_POWER_KOEF,
            SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
            SensorConfig.SENSOR_ENERGO_POWER_REACTIVE,
            SensorConfig.SENSOR_ENERGO_POWER_FULL,
            SensorConfig.SENSOR_ENERGO_TRANSFORM_KOEF_CURRENT,
            SensorConfig.SENSOR_ENERGO_TRANSFORM_KOEF_VOLTAGE,
        )
    }

    override fun getObjectCharts(
        userConfig: ServerUserConfig,
        objectEntity: ObjectEntity,
        begTime: Int,
        endTime: Int,
        viewWidth: Float,
        viewHeight: Float,
        charts: MutableList<ChartData>
    ) {
        sensorRepository
            .findByObj(objectEntity)
            .groupBy { sensorEntity -> sensorEntity.group }
            .forEach { (group, entities) ->
                phasedSensorTypes.forEach { sensorType ->
                    val phasedEnergoSensorEntities = entities.filter { sensorEntity -> sensorEntity.sensorType == sensorType }
                    if (phasedEnergoSensorEntities.isNotEmpty()) {
                        charts += getCombinedSensorChart(
                            userConfig = userConfig,
                            chartTitle = group ?: "-",
                            sensorEntities = phasedEnergoSensorEntities,
                            begTime = begTime,
                            endTime = endTime,
                            viewWidth = viewWidth,
                            viewHeight = viewHeight,
                        )
                    }
                }
            }

//--- как использовать sensorBeg/EndTime для списка датчиков?
//                val begTimeCheck = sensorEntity.endTime?.let { sensorEndTime ->
//                    begTime < sensorEndTime
//                } ?: true
//                val endTimeCheck = sensorEntity.begTime?.let { sensorBegTime ->
//                    endTime > sensorBegTime
//                } ?: true
//                if (begTimeCheck && endTimeCheck) {
//                    charts += getSensorChart(
//                        sensorEntity = sensorEntity,
//                        begTime = begTime,
//                        endTime = endTime,
//                        viewWidth = viewWidth,
//                        viewHeight = viewHeight,
//                    )
//                }
    }

    override fun getAxisTitle(sensorEntity: SensorEntity): String =
        "${sensorEntity.descr ?: "-"} (${SensorConfig.hmPhaseDescr[sensorEntity.phase] ?: "(фаза не указана)"}) [${sensorEntity.dim ?: "-"}]"
}