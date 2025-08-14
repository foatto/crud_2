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
                       se.endTime IS NULL
                    OR ?2 IS NULL
                    OR ?2 <= se.endTime
                )
                AND (
                       se.begTime IS NULL
                    OR ?2 IS NULL 
                    OR ?2 >= se.begTime
                )
        """
    )
    fun findByObjAndTime(obj: ObjectEntity, time: Int?): List<SensorEntity>

    @Query(
        """
            SELECT se
            FROM SensorEntity se
            LEFT JOIN se.obj oe
            WHERE se.id <> 0
                AND oe = ?1
                AND (
                       se.endTime IS NULL
                    OR ?2 IS NULL
                    OR ?2 <= se.endTime
                )
                AND (
                       se.begTime IS NULL
                    OR ?3 IS NULL 
                    OR ?3 >= se.begTime
                )
        """
    )
    fun findByObjAndPeriod(obj: ObjectEntity, begTime: Int?, endTime: Int?): List<SensorEntity>

//    fun findByObjAndSensorType(obj: ObjectEntity, sensorType: Int): List<SensorEntity>

    @Query(
        """
            SELECT se
            FROM SensorEntity se
            LEFT JOIN se.obj oe
            WHERE se.id <> 0
                AND oe = ?1
                AND se.sensorType = ?2
                AND (
                       se.endTime IS NULL
                    OR ?3 IS NULL
                    OR ?3 <= se.endTime
                )
                AND (
                       se.begTime IS NULL
                    OR ?3 IS NULL 
                    OR ?3 >= se.begTime
                )
        """
    )
    fun findByObjAndSensorTypeAndTime(obj: ObjectEntity, sensorType: Int, time: Int?): List<SensorEntity>

    @Query(
        """
            SELECT se
            FROM SensorEntity se
            LEFT JOIN se.obj oe
            WHERE se.id <> 0
                AND oe = ?1
                AND se.sensorType = ?2
                AND (
                       se.endTime IS NULL
                    OR ?3 IS NULL
                    OR ?3 <= se.endTime
                )
                AND (
                       se.begTime IS NULL
                    OR ?4 IS NULL 
                    OR ?4 >= se.begTime
                )
        """
    )
    fun findByObjAndSensorTypeAndPeriod(obj: ObjectEntity, sensorType: Int, begTime: Int?, endTime: Int?): List<SensorEntity>

    //    @Query(
//        """
//            SELECT se
//            FROM SensorEntity se
//            LEFT JOIN se.obj oe
//            WHERE se.id <> 0
//                AND oe = ?1
//                AND se.group = ?2
//                AND se.sensorType IN ?3
//        """
//    )
//    fun findByObjAndGroupAndSensorTypeIn(obj: ObjectEntity, group: String?, sensorTypes: Collection<Int>): List<SensorEntity>

//    @Query(
//        """
//            SELECT se
//            FROM SensorEntity se
//            LEFT JOIN se.obj oe
//            WHERE se.id <> 0
//                AND oe = ?1
//                AND se.group = ?2
//                AND se.sensorType IN ?3
//                AND (
//                       se.endTime IS NULL
//                    OR ?4 IS NULL
//                    OR ?4 <= se.endTime
//                )
//                AND (
//                       se.begTime IS NULL
//                    OR ?5 IS NULL
//                    OR ?5 >= se.begTime
//                )
//        """
//    )
//    fun findByObjAndGroupAndSensorTypeInAndPeriod(obj: ObjectEntity, group: String?, sensorTypes: Collection<Int>, begTime: Int?, endTime: Int?): List<SensorEntity>

//    fun findByObjAndDescr(obj: ObjectEntity, descr: String): List<SensorEntity>

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
