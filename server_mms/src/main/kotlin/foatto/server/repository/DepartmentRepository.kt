package foatto.server.repository

import foatto.server.entity.DepartmentEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface DepartmentRepository : JpaRepository<DepartmentEntity, Int> {

    fun findByUserIdAndName(userId: Int, name: String): List<DepartmentEntity>

    @Query(
        """
            SELECT de
            FROM DepartmentEntity de
            WHERE de.id <> 0
                AND de.userId IN ?1
                AND (
                       ?2 = ''
                    OR LOWER(de.name) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                )
        """
    )
    fun findByUserIdInAndFilter(userIds: List<Int>, findText: String, pageRequest: Pageable): Page<DepartmentEntity>
}