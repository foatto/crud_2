package foatto.server.repository

import foatto.server.entity.UserEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UserRepository : JpaRepository<UserEntity, Int> {
    fun findByLogin(login: String): List<UserEntity>
    fun findByLoginAndPassword(login: String, password: String): List<UserEntity>
    fun findByFullName(fullName: String): List<UserEntity>
    fun findByParentId(parentId: Int): List<UserEntity>
    fun findByParentIdAndOrgType(parentId: Int, orgType: Int): List<UserEntity>

    @Query(
        """
            SELECT ue
            FROM UserEntity ue
            WHERE ue.id <> 0
                AND ue.parentId = ?1
                AND ue.userId IN ?2
                AND (
                        ?3 = ''
                     OR LOWER(ue.login) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(ue.shortName) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(ue.fullName) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(ue.eMail) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(ue.contactInfo) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(ue.lastIP) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR CAST(ue.timeOffset AS String) LIKE CONCAT( '%', ?3, '%' )
                     OR TO_CHAR(MAKE_TIMESTAMP(
                        ue.lastLoginDateTime.ye, ue.lastLoginDateTime.mo, ue.lastLoginDateTime.da, 
                        ue.lastLoginDateTime.ho, ue.lastLoginDateTime.mi, ue.lastLoginDateTime.se
                     ), 'DD.MM.YYYY HH24:MI:SS') LIKE CONCAT( '%', ?3, '%' )
                )
        """
    )
    fun findByParentIdAndUserIdInAndFilter(parentId: Int, userIds: List<Int>, findText: String, timeOffset: Int, pageRequest: Pageable): Page<UserEntity>
}