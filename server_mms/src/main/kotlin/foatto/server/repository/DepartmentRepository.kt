package foatto.server.repository

import foatto.server.entity.DepartmentEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface DepartmentRepository : JpaRepository<DepartmentEntity, Int> {

    fun findByUserId(userId: Int): List<DepartmentEntity>
    fun findByUserIdAndName(userId: Int, name: String): List<DepartmentEntity>

    @Query(
        """
            SELECT de
            FROM DepartmentEntity de
            WHERE de.id <> 0
                AND (
                       ?1 IS NULL
                    OR de.userId IN ?1
                )
                AND de.userId IN ?2
                AND (
                       ?3 = ''
                    OR LOWER(de.name) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                )
        """
    )
    fun findByParentUserIdAndUserIdInAndFilter(
        parentUserIds: List<Int>?,
        userIds: List<Int>,
        findText: String,
        pageRequest: Pageable
    ): Page<DepartmentEntity>
}