package foatto.server.repository

import foatto.server.entity.SensorCalibrationEntity
import foatto.server.entity.SensorEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SensorCalibrationRepository : JpaRepository<SensorCalibrationEntity, Int> {
    fun findBySensorIdOrderBySensorValue(sensorId: Int): List<SensorCalibrationEntity>
    fun findBySensorOrderBySensorValue(sensor: SensorEntity): List<SensorCalibrationEntity>

    fun deleteBySensor(sensor: SensorEntity): Int

    @Query(
        """
            SELECT sce
            FROM SensorCalibrationEntity sce
            LEFT JOIN sce.sensor se
            WHERE sce.id <> 0
                AND se = ?1
                AND (
                        ?2 = ''
                     OR CAST(sce.sensorValue AS String) LIKE CONCAT( '%', ?2, '%' )
                     OR CAST(sce.dataValue AS String) LIKE CONCAT( '%', ?2, '%' )
                )
        """
    )
    fun findBySensorAndFilter(sensor: SensorEntity, findText: String, pageRequest: Pageable): Page<SensorCalibrationEntity>
}