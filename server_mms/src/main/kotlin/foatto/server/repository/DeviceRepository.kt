package foatto.server.repository

import foatto.server.entity.DeviceEntity
import foatto.server.entity.ObjectEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface DeviceRepository : JpaRepository<DeviceEntity, Int> {

    fun findByUserId(userId: Int): List<DeviceEntity>
    fun findByUserIdIn(userIds: List<Int>): List<DeviceEntity>
    fun findBySerialNo(serialNo: String): List<DeviceEntity>

    @Query(
        """
            SELECT de
            FROM DeviceEntity de
            LEFT JOIN de.obj oe
            WHERE oe.id = ?1
        """
    )
    fun findByObjectId(objectId: Int): List<DeviceEntity>

    @Query(
        """
            SELECT de
            FROM DeviceEntity de
            LEFT JOIN de.obj oe
            WHERE oe = ?1
        """
    )
    fun findByObject(objectEntity: ObjectEntity): List<DeviceEntity>

    @Query(
        """
            SELECT de
            FROM DeviceEntity de
            LEFT JOIN de.obj oe
            WHERE de.id <> 0
                AND (
                       ?1 IS NULL
                    OR de.userId IN ?1
                )
                AND (
                       ?2 IS NULL
                    OR oe = ?2
                )
                AND de.userId IN ?3
                AND (
                        ?4 = ''
                     OR LOWER(de.serialNo) LIKE LOWER( CONCAT( '%', ?4, '%' ) )
                     OR LOWER(de.name) LIKE LOWER( CONCAT( '%', ?4, '%' ) )
                     OR LOWER(oe.name) LIKE LOWER( CONCAT( '%', ?4, '%' ) )
                     OR LOWER(oe.model) LIKE LOWER( CONCAT( '%', ?4, '%' ) )
                     OR LOWER(de.cellImei) LIKE LOWER( CONCAT( '%', ?4, '%' ) )
                     OR LOWER(de.cellNumber) LIKE LOWER( CONCAT( '%', ?4, '%' ) )
                     OR LOWER(de.cellIcc) LIKE LOWER( CONCAT( '%', ?4, '%' ) )
                     OR LOWER(de.cellOperator) LIKE LOWER( CONCAT( '%', ?4, '%' ) )
                     OR LOWER(de.cellImei2) LIKE LOWER( CONCAT( '%', ?4, '%' ) )
                     OR LOWER(de.cellNumber2) LIKE LOWER( CONCAT( '%', ?4, '%' ) )
                     OR LOWER(de.cellIcc2) LIKE LOWER( CONCAT( '%', ?4, '%' ) )
                     OR LOWER(de.cellOperator2) LIKE LOWER( CONCAT( '%', ?4, '%' ) )
                     OR LOWER(de.fwVersion) LIKE LOWER( CONCAT( '%', ?4, '%' ) )
                     OR LOWER(de.lastSessionStatus) LIKE LOWER( CONCAT( '%', ?4, '%' ) )
                     OR LOWER(de.lastSessionError) LIKE LOWER( CONCAT( '%', ?4, '%' ) )
                     OR CAST(de.index AS String) LIKE CONCAT( '%', ?4, '%' )
                     OR (
                            de.lastSessionTime IS NOT NULL
                        AND TO_CHAR(MAKE_TIMESTAMP(1970, 1, 1, 0, 0, 0) + de.lastSessionTime second + ?5 second, 'DD.MM.YYYY HH24:MI:SS') LIKE CONCAT( '%', ?4, '%' )
                     )
                     OR (
                            de.usingStartDate.ye IS NOT NULL
                        AND de.usingStartDate.mo IS NOT NULL
                        AND de.usingStartDate.da IS NOT NULL
                        AND TO_CHAR(MAKE_TIMESTAMP(de.usingStartDate.ye, de.usingStartDate.mo, de.usingStartDate.da, 0, 0, 0), 'DD.MM.YYYY HH24:MI:SS') LIKE CONCAT( '%', ?4, '%' )
                     )
                )
                AND (
                        ?6 = -1
                     OR (
                            de.lastSessionTime IS NOT NULL
                        AND de.lastSessionTime >= ?6
                     )
                     OR (
                            de.usingStartDate.ye IS NOT NULL
                        AND de.usingStartDate.mo IS NOT NULL
                        AND de.usingStartDate.da IS NOT NULL
                        AND MAKE_TIMESTAMP(de.usingStartDate.ye, de.usingStartDate.mo, de.usingStartDate.da, 0, 0, 0) >= ( MAKE_TIMESTAMP(1970, 1, 1, 0, 0, 0) + ?6 second )
                     )
                )
                AND (
                        ?7 = -1
                     OR (
                            de.lastSessionTime IS NOT NULL
                        AND de.lastSessionTime <= ?7
                     )
                     OR (
                            de.usingStartDate.ye IS NOT NULL
                        AND de.usingStartDate.mo IS NOT NULL
                        AND de.usingStartDate.da IS NOT NULL
                        AND MAKE_TIMESTAMP(de.usingStartDate.ye, de.usingStartDate.mo, de.usingStartDate.da, 0, 0, 0) < ( MAKE_TIMESTAMP(1970, 1, 1, 0, 0, 0) + ?7 second )
                     )
                )
        """
    )
    fun findByParentUserIdAndObjAndUserIdInAndFilter(
        parentUserIds: List<Int>?,
        obj: ObjectEntity?,
        userIds: List<Int>,
        findText: String,
        timeOffset: Int,
        begDateTime: Int,
        endDateTime: Int,
        pageRequest: Pageable,
    ): Page<DeviceEntity>
}
