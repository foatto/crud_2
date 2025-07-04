package foatto.server.repository

import foatto.server.entity.ObjectEntity
import foatto.server.entity.SensorEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SensorRepository : JpaRepository<SensorEntity, Int> {
    fun findByObjAndSensorType(obj: ObjectEntity, sensorType: Int): List<SensorEntity>

    @Query(
        """
            SELECT se
            FROM SensorEntity se
            LEFT JOIN se.obj oe
            WHERE se.id <> 0
                AND oe = ?1
                AND se.group = ?2
                AND se.sensorType IN ?3
        """
    )
    fun findByObjAndGroupAndSensorTypeIn(obj: ObjectEntity, group: String?, sensorTypes: Collection<Int>): List<SensorEntity>

    fun findByObjAndDescr(obj: ObjectEntity, descr: String): List<SensorEntity>

    fun findByObj(obj: ObjectEntity): List<SensorEntity>

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
                     OR LOWER(se.group) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(se.descr) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(se.serialNo) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                )
        """
    )
    fun findByObjAndFilter(obj: ObjectEntity, findText: String, pageRequest: Pageable): Page<SensorEntity>
}
