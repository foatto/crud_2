package foatto.server.repository

import foatto.server.entity.ObjectEntity
import foatto.server.entity.SensorEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SensorRepository : JpaRepository<SensorEntity, Int> {

    fun findByObj(obj: ObjectEntity): List<SensorEntity>

    fun findByObjAndPortNumAndSensorType(obj: ObjectEntity, portNum: Int, sensorType: Int): List<SensorEntity>

    @Query(
        """
            SELECT se
            FROM SensorEntity se
            LEFT JOIN se.obj oe
            WHERE se.id <> 0
                AND oe = ?1
                AND (
                       ?2 = -1
                    OR se.endTime IS NULL 
                    OR se.endTime >= ?2
                )
                AND (
                       ?2 = -1
                    OR se.begTime IS NULL  
                    OR se.begTime <= ?2
                )
        """
    )
    fun findByObjAndTime(obj: ObjectEntity, time: Int): List<SensorEntity>

    @Query(
        """
            SELECT se
            FROM SensorEntity se
            LEFT JOIN se.obj oe
            WHERE se.id <> 0
                AND oe = ?1
                AND (
                       ?2 = -1
                    OR se.endTime IS NULL 
                    OR se.endTime >= ?2
                )
                AND (
                       ?3 = -1
                    OR se.begTime IS NULL  
                    OR se.begTime <= ?3
                )
        """
    )
    fun findByObjAndPeriod(obj: ObjectEntity, begTime: Int, endTime: Int): List<SensorEntity>

    @Query(
        """
            SELECT se
            FROM SensorEntity se
            LEFT JOIN se.obj oe
            WHERE se.id <> 0
                AND oe = ?1
                AND se.sensorType = ?2
        """
    )
    fun findByObjAndSensorType(obj: ObjectEntity, sensorType: Int): List<SensorEntity>

    @Query(
        """
            SELECT se
            FROM SensorEntity se
            LEFT JOIN se.obj oe
            WHERE se.id <> 0
                AND oe = ?1
                AND se.sensorType = ?2
                AND (
                       ?3 = -1
                    OR se.endTime IS NULL 
                    OR se.endTime >= ?3
                )
                AND (
                       ?3 = -1
                    OR se.begTime IS NULL  
                    OR se.begTime <= ?3
                )
        """
    )
    fun findByObjAndSensorTypeAndTime(obj: ObjectEntity, sensorType: Int, time: Int): List<SensorEntity>

    @Query(
        """
            SELECT se
            FROM SensorEntity se
            LEFT JOIN se.obj oe
            WHERE se.id <> 0
                AND oe = ?1
                AND se.sensorType = ?2
                AND (
                       ?3 = -1
                    OR se.endTime IS NULL 
                    OR se.endTime >= ?3
                )
                AND (
                       ?4 = -1
                    OR se.begTime IS NULL  
                    OR se.begTime <= ?4
                )
        """
    )
    fun findByObjAndSensorTypeAndPeriod(obj: ObjectEntity, sensorType: Int, begTime: Int, endTime: Int): List<SensorEntity>

    fun findByObjAndPortNumBetween(obj: ObjectEntity, startPort: Int, endPort: Int): List<SensorEntity>

    @Query(
        """
            SELECT se
            FROM SensorEntity se
            LEFT JOIN se.obj oe
            WHERE se.id <> 0
                AND oe = ?1
                AND (
                        ?2 = ''
                     OR LOWER(se.name) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(se.group) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(se.descr) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(se.serialNo) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR CAST(se.portNum AS String) LIKE CONCAT( '%', ?2, '%' )
                     OR (
                            se.begTime IS NOT NULL
                        AND TO_CHAR(MAKE_TIMESTAMP(1970, 1, 1, 0, 0, 0) + se.begTime second + ?3 second, 'DD.MM.YYYY HH24:MI:SS') LIKE CONCAT( '%', ?2, '%' )
                     )
                     OR (
                            se.endTime IS NOT NULL
                        AND TO_CHAR(MAKE_TIMESTAMP(1970, 1, 1, 0, 0, 0) + se.endTime second + ?3 second, 'DD.MM.YYYY HH24:MI:SS') LIKE CONCAT( '%', ?2, '%' )
                     )
                )
                AND (
                        ?4 = -1
                     OR se.endTime IS NULL
                     OR se.endTime >= ?4
                )
                AND (
                        ?5 = -1
                     OR se.begTime IS NULL
                     OR se.begTime <= ?5
                )
        """
    )
    fun findByObjAndFilter(
        obj: ObjectEntity,
        findText: String,
        timeOffset: Int,
        begDateTime: Int,
        endDateTime: Int,
        pageRequest: Pageable,
    ): Page<SensorEntity>
}
