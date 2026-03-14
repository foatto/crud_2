package foatto.server.repository

import foatto.server.ObjectType
import foatto.server.entity.DayWorkEntity
import foatto.server.entity.ObjectEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface DayWorkRepository : JpaRepository<DayWorkEntity, Int> {

    fun deleteByUserId(userId: Int)
    fun deleteByObj(obj: ObjectEntity)

    @Query(
        """
            SELECT dwe
            FROM DayWorkEntity dwe
            LEFT JOIN dwe.obj oe
            WHERE dwe.id <> 0
                AND (
                       ?1 IS NULL 
                    OR oe = ?1
                )
                AND dwe.userId IN ?2
                AND (
                        ?3 = -1
                     OR (
                            dwe.day.ye IS NOT NULL
                        AND dwe.day.mo IS NOT NULL
                        AND dwe.day.da IS NOT NULL
                        AND MAKE_TIMESTAMP(dwe.day.ye, dwe.day.mo, dwe.day.da, 0, 0, 0) >= ( MAKE_TIMESTAMP(1970, 1, 1, 0, 0, 0) + ?3 second )
                     )
                )
                AND (
                        ?4 = -1
                     OR (
                            dwe.day.ye IS NOT NULL
                        AND dwe.day.mo IS NOT NULL
                        AND dwe.day.da IS NOT NULL
                        AND MAKE_TIMESTAMP(dwe.day.ye, dwe.day.mo, dwe.day.da, 0, 0, 0) <= ( MAKE_TIMESTAMP(1970, 1, 1, 0, 0, 0) + ?4 second )
                     )
                )
            ORDER BY dwe.day.ye , dwe.day.mo , dwe.day.da  
        """
    )
    fun findByObjAndUserIdIn(
        obj: ObjectEntity?,
        userIds: List<Int>,
        begDateTime: Int,
        endDateTime: Int,
    ): List<DayWorkEntity>

    @Query(
        """
            SELECT dwe
            FROM DayWorkEntity dwe
            LEFT JOIN dwe.obj oe
            LEFT JOIN oe.group ge
            LEFT JOIN oe.department de
            WHERE dwe.id <> 0
                AND (
                       ?1 IS NULL
                    OR dwe.userId IN ?1
                )
                AND (
                       ?2 IS NULL
                    OR oe = ?2
                )
                AND (
                       ?3 IS NULL
                    OR oe.type = ?3
                )
                AND dwe.userId IN ?4
                AND (
                        ?5 = ''
                     OR LOWER(oe.name) LIKE LOWER( CONCAT( '%', ?5, '%' ) )
                     OR LOWER(oe.model) LIKE LOWER( CONCAT( '%', ?5, '%' ) )
                     OR LOWER(ge.name) LIKE LOWER( CONCAT( '%', ?5, '%' ) )
                     OR LOWER(de.name) LIKE LOWER( CONCAT( '%', ?5, '%' ) )
                     OR (
                            dwe.day.ye IS NOT NULL
                        AND dwe.day.mo IS NOT NULL
                        AND dwe.day.da IS NOT NULL
                        AND TO_CHAR(MAKE_TIMESTAMP(dwe.day.ye, dwe.day.mo, dwe.day.da, 0, 0, 0), 'DD.MM.YYYY HH24:MI:SS') LIKE CONCAT( '%', ?5, '%' )
                    )
                )
                AND (
                        ?6 = -1
                     OR (
                            dwe.day.ye IS NOT NULL
                        AND dwe.day.mo IS NOT NULL
                        AND dwe.day.da IS NOT NULL
                        AND MAKE_TIMESTAMP(dwe.day.ye, dwe.day.mo, dwe.day.da, 0, 0, 0) >= ( MAKE_TIMESTAMP(1970, 1, 1, 0, 0, 0) + ?6 second )
                     )
                )
                AND (
                        ?7 = -1
                     OR (
                            dwe.day.ye IS NOT NULL
                        AND dwe.day.mo IS NOT NULL
                        AND dwe.day.da IS NOT NULL
                        AND MAKE_TIMESTAMP(dwe.day.ye, dwe.day.mo, dwe.day.da, 0, 0, 0) <= ( MAKE_TIMESTAMP(1970, 1, 1, 0, 0, 0) + ?7 second )
                     )
                )
        """
    )
    fun findByParentUserIdAndObjAndUserIdInAndFilter(
        parentUserIds: List<Int>?,
        obj: ObjectEntity?,
        objectType: ObjectType?,
        userIds: List<Int>,
        findText: String,
        begDateTime: Int,
        endDateTime: Int,
        pageRequest: Pageable,
    ): Page<DayWorkEntity>
}