package foatto.server.repository

import foatto.server.entity.WorkShiftEntity
import foatto.server.entity.ObjectEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface WorkShiftRepository : JpaRepository<WorkShiftEntity, Int> {

    fun deleteByUserId(userId: Int)
    fun deleteByObj(obj: ObjectEntity)

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
                AND (
                       ?1 IS NULL
                    OR wse.userId IN ?1
                )
                AND (
                       ?2 IS NULL
                    OR oe = ?2
                )
                AND wse.userId IN ?3
                AND (
                        ?4 = ''
                     OR LOWER(oe.name) LIKE LOWER( CONCAT( '%', ?4, '%' ) )
                     OR LOWER(oe.model) LIKE LOWER( CONCAT( '%', ?4, '%' ) )
                     OR LOWER(ge.name) LIKE LOWER( CONCAT( '%', ?4, '%' ) )
                     OR LOWER(de.name) LIKE LOWER( CONCAT( '%', ?4, '%' ) )
                     OR (
                            wse.begTime IS NOT NULL
                        AND TO_CHAR(MAKE_TIMESTAMP(1970, 1, 1, 0, 0, 0) + wse.begTime second + ?5 second, 'DD.MM.YYYY HH24:MI:SS') LIKE CONCAT( '%', ?4, '%' )
                     )
                     OR (
                            wse.endTime IS NOT NULL
                        AND TO_CHAR(MAKE_TIMESTAMP(1970, 1, 1, 0, 0, 0) + wse.endTime second + ?5 second, 'DD.MM.YYYY HH24:MI:SS') LIKE CONCAT( '%', ?4, '%' )
                     )
                )
                AND (
                        ?6 = -1
                     OR (
                            wse.begTime IS NOT NULL
                        AND wse.begTime >= ?6
                     )
                     OR (
                            wse.endTime IS NOT NULL
                        AND wse.endTime >= ?6
                     )
                )
                AND (
                        ?7 = -1
                     OR (
                            wse.begTime IS NOT NULL
                        AND wse.begTime <= ?7
                     )
                     OR (
                            wse.endTime IS NOT NULL
                        AND wse.endTime <= ?7
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
    ): Page<WorkShiftEntity>
}