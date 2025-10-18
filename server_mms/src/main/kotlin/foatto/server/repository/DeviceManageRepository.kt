package foatto.server.repository

import foatto.server.entity.DeviceEntity
import foatto.server.entity.DeviceManageEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface DeviceManageRepository : JpaRepository<DeviceManageEntity, Int> {

    @Query(
        """
            SELECT dme
            FROM DeviceManageEntity dme
            LEFT JOIN dme.device de
            WHERE dme.id <> 0
                AND de = ?1
                AND (
                        ?2 = ''
                     OR LOWER(dme.descr) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(dme.command) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(de.serialNo) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(de.name) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR TO_CHAR(MAKE_TIMESTAMP(1970, 1, 1, 0, 0, 0) + dme.createTime second + ?3 second, 'DD.MM.YYYY HH24:MI:SS') LIKE CONCAT( '%', ?2, '%' )
                     OR TO_CHAR(MAKE_TIMESTAMP(1970, 1, 1, 0, 0, 0) + dme.editTime second + ?3 second, 'DD.MM.YYYY HH24:MI:SS') LIKE CONCAT( '%', ?2, '%' )
                     OR TO_CHAR(MAKE_TIMESTAMP(1970, 1, 1, 0, 0, 0) + dme.sendTime second + ?3 second, 'DD.MM.YYYY HH24:MI:SS') LIKE CONCAT( '%', ?2, '%' )
                )
        """
    )
    fun findByDeviceAndFilter(device: DeviceEntity, findText: String, timeOffset: Int, pageRequest: Pageable): Page<DeviceManageEntity>

    fun findByDevice(device: DeviceEntity): List<DeviceManageEntity>

    fun deleteByDevice(device: DeviceEntity): Int
}