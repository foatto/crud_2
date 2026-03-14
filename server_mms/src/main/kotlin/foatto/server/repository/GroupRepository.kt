package foatto.server.repository

import foatto.server.entity.GroupEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface GroupRepository : JpaRepository<GroupEntity, Int> {

    fun findByUserId(userId: Int): List<GroupEntity>
    fun findByUserIdAndName(userId: Int, name: String): List<GroupEntity>

    @Query(
        """
            SELECT ge
            FROM GroupEntity ge
            WHERE ge.id <> 0
                AND (
                       ?1 IS NULL
                    OR ge.userId IN ?1
                )
                AND ge.userId IN ?2
                AND (
                       ?3 = ''
                    OR LOWER(ge.name) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                )
        """
    )
    fun findByParentUserIdAndUserIdInAndFilter(
        parentUserIds: List<Int>?,
        userIds: List<Int>,
        findText: String,
        pageRequest: Pageable
    ): Page<GroupEntity>
}