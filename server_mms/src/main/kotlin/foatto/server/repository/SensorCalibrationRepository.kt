package foatto.server.repository

import foatto.server.entity.SensorCalibrationEntity
import foatto.server.entity.SensorEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface SensorCalibrationRepository : JpaRepository<SensorCalibrationEntity, Int> {
    fun findBySensorIdOrderBySensorValue(sensorId: Int): List<SensorCalibrationEntity>
    fun findBySensorOrderBySensorValue(sensor: SensorEntity): List<SensorCalibrationEntity>

    fun deleteBySensor(sensor: SensorEntity): Int

    fun findBySensor(sensor: SensorEntity, pageRequest: Pageable): Page<SensorCalibrationEntity>
}