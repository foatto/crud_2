package foatto.server.cli

import foatto.core.util.getCurrentTimeInt
import foatto.server.service.SensorService
import foatto.server.util.AdvancedLogger
import kotlin.system.exitProcess

class DataClean(aConfigFileName: String) : CoreServiceWorker(aConfigFileName) {

    companion object {

        private const val CONFIG_DB_EXPIRE_PERIOD = "db_expire_period"

        //----------------------------------------------------------------------------------------------------------------------------------------

        @JvmStatic
        fun main(args: Array<String>) {
            var exitCode = 0   // нормальный выход с прерыванием цикла запусков
            try {
                serviceWorkerName = "DataClean"
                if (args.size == 1) {
                    DataClean(args[0]).run()
                    exitCode = 1
                } else {
                    println("Usage: $serviceWorkerName <ini-file-name>")
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }

            exitProcess(exitCode)
        }
    }

    //----------------------------------------------------------------------------------------------------------------------------------------

    private var expirePeriod = 100 * 52

    override val isRunOnce: Boolean
        get() = true

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun loadConfig() {
        super.loadConfig()

        expirePeriod = hmConfig[CONFIG_DB_EXPIRE_PERIOD]!!.toInt() * 7 * 24 * 60 * 60
    }

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun cycle() {
        //--- загрузка списка обрабатываемых в этом цикле а/м
        val objects = mutableMapOf<Int, String>()
        val rs = conn.executeQuery(" SELECT id , name FROM MMS_object WHERE id <> 0 ")
        while (rs.next()) {
            objects[rs.getInt(1)] = rs.getString(2)
        }
        rs.close()

        //--- полный срок хранения: expire_period считается в неделях
        val expireTime = getCurrentTimeInt() - expirePeriod

        //--- теперь по каждому объекту
        var objectNo = 1
        objects.forEach { (objectId, objectName) ->
            AdvancedLogger.debug("-".repeat(60))
            AdvancedLogger.debug("${objectNo++}/${objects.size}: $objectName ")

            //--- стираем старые сырые данные
            val rawRows = conn.executeUpdate(" DELETE FROM MMS_data_$objectId WHERE ontime < $expireTime ")
            conn.executeUpdate(" REINDEX TABLE MMS_data_$objectId ")
            conn.commit()

            //--- стираем старые агрегированные данные и события
            val sensorIds = mutableListOf<Int>()
            val rs = conn.executeQuery(" SELECT id FROM MMS_sensor WHERE object_id = $objectId ")
            while (rs.next()) {
                sensorIds += rs.getInt(1)
            }
            rs.close()

            var aggRows = 0
            var textRows = 0
            sensorIds.forEach { sensorId ->
                if (SensorService.checkAggTableIsExists(conn, sensorId)) {
                    aggRows += conn.executeUpdate(" DELETE FROM MMS_agg_$sensorId WHERE ontime_0 < $expireTime ")
                    conn.executeUpdate(" REINDEX TABLE MMS_agg_$sensorId ")
                    conn.commit()
                }
                if (SensorService.checkTextTableIsExists(conn, sensorId)) {
                    textRows += conn.executeUpdate(" DELETE FROM MMS_text_$sensorId WHERE ontime_0 < $expireTime ")
                    conn.executeUpdate(" REINDEX TABLE MMS_text_$sensorId ")
                    conn.commit()
                }
            }

            AdvancedLogger.debug("MMS_data: $rawRows rows")
            AdvancedLogger.debug("MMS_agg: $aggRows rows")
            AdvancedLogger.debug("MMS_text: $textRows rows")
        }
    }
}
