package foatto.server.cli

import foatto.server.service.SensorService
import foatto.server.util.AdvancedLogger
import kotlin.system.exitProcess

class ClientDBPrepare(aConfigFileName: String) : CoreServiceWorker(aConfigFileName) {

    companion object {

        private const val CONFIG_ADMIN_ID = "admin_id"
        private const val CONFIG_USER_ID = "user_id"

        //----------------------------------------------------------------------------------------------------------------------------------------

        @JvmStatic
        fun main(args: Array<String>) {
            var exitCode = 0   // нормальный выход с прерыванием цикла запусков
            try {
                serviceWorkerName = "ClientDBPrepare"
                if (args.size == 1) {
                    ClientDBPrepare(args[0]).run()
                    exitCode = 1
                } else println("Usage: $serviceWorkerName <ini-file-name>")
            } catch (t: Throwable) {
                t.printStackTrace()
            }

            exitProcess(exitCode)
        }
    }

    //----------------------------------------------------------------------------------------------------------------------------------------

    //--- список оставляемых админов и пользователей
    private var configAdminIdList = ""
    private var configUserIdList = ""

    override val isRunOnce: Boolean
        get() = true

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun loadConfig() {
        super.loadConfig()

        configAdminIdList = hmConfig[CONFIG_ADMIN_ID]!!
        configUserIdList = hmConfig[CONFIG_USER_ID]!!
    }

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun cycle() {
        val destAdminIds = configAdminIdList.split(' ', ',', ';').filter { it.isNotEmpty() }.map { it.toInt() }.toMutableList()
        val destUserIds = configUserIdList.split(' ', ',', ';').filter { it.isNotEmpty() }.map { it.toInt() }.toMutableList()

        //--- рекурсивно заполняем/расширяем список оставляемых пользователей (на случай, если там заданы id подразделений/групп пользователей)
        // alUserIDDest.forEach { - даёт ConcurrentModifException, т.к. дополняется на ходу, только через индексы
        for (i in 0 until destAdminIds.size) {
            val rs = conn.executeQuery(" SELECT id FROM SYSTEM_users WHERE id <> 0 AND parent_id = ${destAdminIds[i]} ")
            while (rs.next()) {
                destAdminIds += rs.getInt(1)
            }
            rs.close()
        }
        val adminIdsString = destAdminIds.distinct().joinToString()

        //--- рекурсивно заполняем/расширяем список оставляемых пользователей (на случай, если там заданы id подразделений/групп пользователей)
        // alUserIDDest.forEach { - даёт ConcurrentModifException, т.к. дополняется на ходу, только через индексы
        for (i in 0 until destUserIds.size) {
            val rs = conn.executeQuery(" SELECT id FROM SYSTEM_users WHERE id <> 0 AND parent_id = ${destUserIds[i]} ")
            while (rs.next()) {
                destUserIds += rs.getInt(1)
            }
            rs.close()
        }
        val userIdsString = destUserIds.distinct().joinToString()

        //--- составляем список ID удаляемых объектов

        val objectIds = mutableListOf<Int>()
        var rs = conn.executeQuery(" SELECT id FROM MMS_object WHERE id <> 0 AND user_id NOT IN ( $userIdsString ) ")
        while (rs.next()) {
            objectIds += rs.getInt(1)
        }
        rs.close()
        val objectIdsString = objectIds.distinct().joinToString()

        AdvancedLogger.info("load object_id")

        //--- составляем список ID удаляемых датчиков

        val sensorIds = mutableListOf<Int>()
        rs = conn.executeQuery(" SELECT id FROM MMS_sensor WHERE id <> 0 AND object_id IN ( $objectIdsString ) ")
        while (rs.next()) {
            sensorIds += rs.getInt(1)
        }
        rs.close()

//--------------------------------------------------------------------------        

        //--- удаляем object-data-таблицы согласно списка

        for (objectId in objectIds) {
            conn.executeUpdate(" DROP TABLE MMS_data_$objectId ")
            conn.commit()
        }
        AdvancedLogger.info("MMS_data_NNN")

        //--- удаляем sensor-agg/text-таблицы согласно списка

        for (sensorId in sensorIds) {
            if (SensorService.checkAggTableIsExists(conn, sensorId)) {
                conn.executeUpdate(" DROP TABLE MMS_agg_$sensorId ")
            }
            if (SensorService.checkTextTableIsExists(conn, sensorId)) {
                conn.executeUpdate(" DROP TABLE MMS_text_$sensorId ")
            }
            conn.commit()
        }
        AdvancedLogger.info("MMS_agg/text_NNN")

        //--- чистим объекты

        conn.executeUpdate(" DELETE FROM MMS_object WHERE id <> 0 AND user_id NOT IN ( $userIdsString ) ")
        conn.executeUpdate(" REINDEX TABLE MMS_object ")
        conn.commit()
        AdvancedLogger.info("MMS_object")

        //--- чистим зоны (!!! только в старом пульсаре)

        conn.executeUpdate(" DELETE FROM MMS_zone WHERE id <> 0 AND user_id NOT IN ( $userIdsString ) ")
        conn.executeUpdate(" REINDEX TABLE MMS_zone ")
        conn.commit()
        AdvancedLogger.info("MMS_zone")

        //--- чистим зависимости

        val depends = mutableListOf(
            //--- справочники для MMS_object
            DependInfo("MMS_department", "id", "MMS_object", "department_id"),
            DependInfo("MMS_group", "id", "MMS_object", "group_id"),
            //--- зависимости первого порядка от MMS_object
            DependInfo("MMS_sensor", "object_id", "MMS_object", "id"),
            DependInfo("MMS_day_work", "object_id", "MMS_object", "id"),
            DependInfo("MMS_work_shift", "object_id", "MMS_object", "id"),
            DependInfo("MMS_device", "object_id", "MMS_object", "id"),
            DependInfo("MMS_device_command_history", "object_id", "MMS_object", "id"),  //!!! только в старом пульсаре
            //--- зависимости от MMS_sensor
            DependInfo("MMS_sensor_calibration", "sensor_id", "MMS_sensor", "id"),
            DependInfo("MMS_equip_service_shedule", "equip_id", "MMS_sensor", "id"),    //!!! только в старом пульсаре
            DependInfo("MMS_equip_service_history", "equip_id", "MMS_sensor", "id"),    //!!! только в старом пульсаре
            //--- справочники для MMS_work_shift
            DependInfo("MMS_worker", "id", "MMS_work_shift", "worker_id"),              //!!! только в старом пульсаре
            //--- зависимости от MMS_work_shift
            DependInfo("MMS_work_shift_data", "shift_id", "MMS_work_shift", "id"),      //!!! только в старом пульсаре
            //--- зависимости от MMS_zone
            DependInfo("MMS_user_zone", "zone_id", "MMS_zone", "id"),                   //!!! только в старом пульсаре
            DependInfo("MMS_object_zone", "zone_id", "MMS_zone", "id"),                 //!!! только в старом пульсаре
            DependInfo("XY_element", "object_id", "MMS_zone", "id"),                    //!!! только в старом пульсаре
            //--- зависимости от MMS_device
            DependInfo("MMS_device_command_history", "device_id", "MMS_device", "id"),
            DependInfo("MMS_device_manage", "device_id", "MMS_device", "id"),           // только в новом пульсаре
        )

        for (di in depends) {
            conn.executeUpdate(
                """
                    DELETE FROM ${di.destTable} 
                    WHERE id <> 0 
                        AND ${di.destField} <> 0 
                        AND ${di.destField} NOT IN ( 
                            SELECT ${di.sourField} FROM ${di.sourTable} 
                        )
                """
            )
            conn.executeUpdate(" REINDEX TABLE ${di.destTable} ")
            conn.commit()
            AdvancedLogger.info(di.destTable)
        }

        //--- отдельные зависимости в таблицах, не имеющих id-поля
        val dependsWithoutId = mutableListOf(
            //--- зависимости от XY_element
            DependInfo("XY_point", "element_id", "XY_element", "id"),
            DependInfo("XY_property", "element_id", "XY_element", "id"),
        )
        for (di in dependsWithoutId) {
            conn.executeUpdate(
                """ 
                    DELETE FROM ${di.destTable} 
                        WHERE ${di.destField} <> 0 
                            AND ${di.destField} NOT IN ( 
                                SELECT ${di.sourField} FROM ${di.sourTable} 
                            )                    
                """
            )
            conn.executeUpdate(" REINDEX TABLE ${di.destTable}")
            conn.commit()
            AdvancedLogger.info(di.destTable)
        }

        //--- очистка независимых таблиц

        conn.executeUpdate(" DELETE FROM MMS_service_order ")
        conn.executeUpdate(" REINDEX TABLE MMS_service_order ")
        conn.commit()
        AdvancedLogger.info("MMS_service_order")

        //--- дополнительные зачистки

        conn.executeUpdate(" DELETE FROM MMS_device WHERE object_id = 0 ")
        conn.executeUpdate(" REINDEX TABLE MMS_device ")
        conn.commit()
        AdvancedLogger.info("MMS_device")

        //--- зачистка ненужных пользователей
        conn.executeUpdate(" DELETE FROM SYSTEM_users WHERE id <> 0 AND id NOT IN ( $adminIdsString , $userIdsString ) ")
        conn.executeUpdate(" DELETE FROM SYSTEM_user_role WHERE id <> 0 AND user_id <> 0 AND user_id NOT IN ( SELECT id FROM SYSTEM_users ) ")
        conn.executeUpdate(" DELETE FROM SYSTEM_user_property WHERE user_id <> 0 AND user_id NOT IN ( SELECT id FROM SYSTEM_users ) ")
        conn.executeUpdate(" DELETE FROM SYSTEM_new WHERE user_id <> 0 AND user_id NOT IN ( SELECT id FROM SYSTEM_users ) ")
        conn.commit()
        AdvancedLogger.info("SYSTEM_users")

    }

    private class DependInfo(val destTable: String, val destField: String, val sourTable: String, val sourField: String)
}
