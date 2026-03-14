package foatto.server.repository

import foatto.server.entity.ActionLogEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ActionLogRepository : JpaRepository<ActionLogEntity, Int> {

    fun deleteByUserId(userId: Int)

    @Query(
        """
            SELECT ale
            FROM ActionLogEntity ale
            WHERE (
                       ?1 IS NULL
                    OR ale.userId = ?1
                )
                AND (
                        ?2 = ''
                     OR LOWER(ale.type) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(ale.module) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(ale.parentModule) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(ale.action) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR CAST(ale.recordId AS String) LIKE CONCAT( '%', ?2, '%' )
                     OR CAST(ale.parentId AS String) LIKE CONCAT( '%', ?2, '%' )
                     OR (
                            ale.onTime IS NOT NULL
                        AND TO_CHAR(MAKE_TIMESTAMP(1970, 1, 1, 0, 0, 0) + ale.onTime second + ?3 second, 'DD.MM.YYYY HH24:MI:SS') LIKE CONCAT( '%', ?2, '%' )
                     )
                )
                AND (
                        ?4 = -1
                     OR (
                            ale.onTime IS NOT NULL
                        AND ale.onTime >= ?4
                     )
                )
                AND (
                        ?5 = -1
                     OR (
                            ale.onTime IS NOT NULL
                        AND ale.onTime <= ?5
                     )
                )
        """
    )
    fun findByParentUserIdAndFilter(
        parentUserId: Int?,
        findText: String,
        timeOffset: Int,
        begDateTime: Int,
        endDateTime: Int,
        pageRequest: Pageable,
    ): Page<ActionLogEntity>

}