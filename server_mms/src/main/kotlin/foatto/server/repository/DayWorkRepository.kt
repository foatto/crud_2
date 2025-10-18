package foatto.server.repository

import foatto.server.entity.DayWorkEntity
import foatto.server.entity.ObjectEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface DayWorkRepository : JpaRepository<DayWorkEntity, Int> {

    @Query(
        """
            SELECT dwe
            FROM DayWorkEntity dwe
            LEFT JOIN dwe.obj oe
            LEFT JOIN oe.group ge
            LEFT JOIN oe.department de
            WHERE dwe.id <> 0
                AND dwe.userId IN ?1
                AND (
                        ?2 = ''
                     OR LOWER(oe.name) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(oe.model) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(ge.name) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(de.name) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR TO_CHAR(MAKE_TIMESTAMP(dwe.day.ye, dwe.day.mo, dwe.day.da, 0, 0, 0), 'DD.MM.YYYY HH24:MI:SS') LIKE CONCAT( '%', ?2, '%' )
                )
        """
    )
    fun findByUserIdInAndFilter(userIds: List<Int>, findText: String, pageRequest: Pageable): Page<DayWorkEntity>

    @Query(
        """
            SELECT dwe
            FROM DayWorkEntity dwe
            LEFT JOIN dwe.obj oe
            LEFT JOIN oe.group ge
            LEFT JOIN oe.department de
            WHERE dwe.id <> 0
                AND oe = ?1
                AND dwe.userId IN ?2
                AND (
                        ?3 = ''
                     OR LOWER(oe.name) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(oe.model) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(ge.name) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(de.name) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR TO_CHAR(MAKE_TIMESTAMP(dwe.day.ye, dwe.day.mo, dwe.day.da, 0, 0, 0), 'DD.MM.YYYY HH24:MI:SS') LIKE CONCAT( '%', ?3, '%' )
                )
        """
    )
    fun findByObjAndUserIdInAndFilter(obj: ObjectEntity, userIds: List<Int>, findText: String, pageRequest: Pageable): Page<DayWorkEntity>
}