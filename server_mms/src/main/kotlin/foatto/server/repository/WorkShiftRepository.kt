package foatto.server.repository

import foatto.server.entity.WorkShiftEntity
import foatto.server.entity.ObjectEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface WorkShiftRepository : JpaRepository<WorkShiftEntity, Int> {

    @Query(
        """
            SELECT wse
            FROM WorkShiftEntity wse
            LEFT JOIN wse.obj oe
            WHERE wse.id <> 0
                AND (
                       ?1 IS NULL
                    OR oe = ?1
                )
                AND wse.userId IN ?2
                AND (
                        ?3 = -1
                     OR (
                            wse.begTime IS NOT NULL
                        AND wse.begTime >= ?3
                     )
                     OR (
                            wse.endTime IS NOT NULL
                        AND wse.endTime >= ?3
                     )
                )
                AND (
                        ?4 = -1
                     OR (
                            wse.begTime IS NOT NULL
                        AND wse.begTime <= ?4
                     )
                     OR (
                            wse.endTime IS NOT NULL
                        AND wse.endTime <= ?4
                     )
                )
            ORDER BY wse.begTime, wse.endTime
        """
    )
    fun findByObjAndUserIdIn(
        obj: ObjectEntity?,
        userIds: List<Int>,
        begDateTime: Int,
        endDateTime: Int,
    ): List<WorkShiftEntity>

    @Query(
        """
            SELECT wse
            FROM WorkShiftEntity wse
            LEFT JOIN wse.obj oe
            LEFT JOIN oe.group ge
            LEFT JOIN oe.department de
            WHERE wse.id <> 0
                AND oe.type = 'STATIONARY'
                AND wse.userId IN ?1
                AND (
                        ?2 = ''
                     OR LOWER(oe.name) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(oe.model) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(ge.name) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(de.name) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR (
                            wse.begTime IS NOT NULL
                        AND TO_CHAR(MAKE_TIMESTAMP(1970, 1, 1, 0, 0, 0) + wse.begTime second + ?3 second, 'DD.MM.YYYY HH24:MI:SS') LIKE CONCAT( '%', ?2, '%' )
                     )
                     OR (
                            wse.endTime IS NOT NULL
                        AND TO_CHAR(MAKE_TIMESTAMP(1970, 1, 1, 0, 0, 0) + wse.endTime second + ?3 second, 'DD.MM.YYYY HH24:MI:SS') LIKE CONCAT( '%', ?2, '%' )
                     )
                )
                AND (
                        ?4 = -1
                     OR (
                            wse.begTime IS NOT NULL
                        AND wse.begTime >= ?4
                     )
                     OR (
                            wse.endTime IS NOT NULL
                        AND wse.endTime >= ?4
                     )
                )
                AND (
                        ?5 = -1
                     OR (
                            wse.begTime IS NOT NULL
                        AND wse.begTime <= ?5
                     )
                     OR (
                            wse.endTime IS NOT NULL
                        AND wse.endTime <= ?5
                     )
                )
        """
    )
    fun findByUserIdInAndFilter(
        userIds: List<Int>,
        findText: String,
        timeOffset: Int,
        begDateTime: Int,
        endDateTime: Int,
        pageRequest: Pageable,
    ): Page<WorkShiftEntity>

    @Query(
        """
            SELECT wse
            FROM WorkShiftEntity wse
            LEFT JOIN wse.obj oe
            LEFT JOIN oe.group ge
            LEFT JOIN oe.department de
            WHERE wse.id <> 0
                AND oe = ?1
                AND wse.userId IN ?2
                AND (
                        ?3 = ''
                     OR LOWER(oe.name) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(oe.model) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(ge.name) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(de.name) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR (
                            wse.begTime IS NOT NULL
                        AND TO_CHAR(MAKE_TIMESTAMP(1970, 1, 1, 0, 0, 0) + wse.begTime second + ?4 second, 'DD.MM.YYYY HH24:MI:SS') LIKE CONCAT( '%', ?3, '%' )
                     )
                     OR (
                            wse.endTime IS NOT NULL
                        AND TO_CHAR(MAKE_TIMESTAMP(1970, 1, 1, 0, 0, 0) + wse.endTime second + ?4 second, 'DD.MM.YYYY HH24:MI:SS') LIKE CONCAT( '%', ?3, '%' )
                     )
                )
                AND (
                        ?5 = -1
                     OR (
                            wse.begTime IS NOT NULL
                        AND wse.begTime >= ?5
                     )
                     OR (
                            wse.endTime IS NOT NULL
                        AND wse.endTime >= ?5
                     )
                )
                AND (
                        ?6 = -1
                     OR (
                            wse.begTime IS NOT NULL
                        AND wse.begTime <= ?6
                     )
                     OR (
                            wse.endTime IS NOT NULL
                        AND wse.endTime <= ?6
                     )
                )
        """
    )
    fun findByObjAndUserIdInAndFilter(
        obj: ObjectEntity,
        userIds: List<Int>,
        findText: String,
        timeOffset: Int,
        begDateTime: Int,
        endDateTime: Int,
        pageRequest: Pageable,
    ): Page<WorkShiftEntity>
}