package foatto.server.repository

import foatto.server.ObjectType
import foatto.server.entity.ObjectEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ObjectRepository : JpaRepository<ObjectEntity, Int> {

    fun findByUserIdAndName(userId: Int, name: String): List<ObjectEntity>
    fun findByUserIdInAndName(userIds: List<Int>, name: String): List<ObjectEntity>

    @Query(
        """
            SELECT oe
            FROM ObjectEntity oe
            WHERE oe.id <> 0
                AND oe.userId IN ?1
            ORDER BY oe.name ASC
        """
    )
    fun findByUserIdIn(userIds: List<Int>): List<ObjectEntity>

    @Query(
        """
            SELECT oe
            FROM ObjectEntity oe
            LEFT JOIN oe.department de
            WHERE de.id = ?1
        """
    )
    fun findByDepartmentId(departmentId: Int): List<ObjectEntity>

    @Query(
        """
            SELECT oe
            FROM ObjectEntity oe
            LEFT JOIN oe.group ge
            WHERE ge.id = ?1
        """
    )
    fun findByGroupId(groupId: Int): List<ObjectEntity>

    @Query(
        """
            SELECT oe
            FROM ObjectEntity oe
            LEFT JOIN oe.department de
            LEFT JOIN oe.group ge
            WHERE oe.id <> 0
                AND (
                       ?1 IS NULL
                    OR oe.type = ?1
                )
                AND oe.userId IN ?2
                AND (
                        ?3 = ''
                     OR LOWER(oe.name) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(oe.model) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(de.name) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(ge.name) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                )
        """
    )
    fun findByTypeAndUserIdInAndFilter(type: ObjectType?, userIds: List<Int>, findText: String, pageRequest: Pageable): Page<ObjectEntity>

}
