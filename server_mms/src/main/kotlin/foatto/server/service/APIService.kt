package foatto.server.service

import foatto.core.ActionType
import foatto.core.util.getDateTimeDMYHMSString
import foatto.core_mms.AppModuleMMS
import foatto.server.OrgType
import foatto.server.entity.UserEntity
import foatto.server.getEnabledUserIds
import foatto.server.model.DevicesStatusResponse
import foatto.server.model.DevicesStatusResponseDeviceInfo
import foatto.server.model.ObjectDataResponse
import foatto.server.model.ObjectDataResponseSensorInfo
import foatto.server.model.sensor.SensorConfig
import foatto.server.repository.DeviceRepository
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import foatto.server.repository.UserRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import kotlin.math.max
import kotlin.math.min
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Service
class APIService(
    private val entityManager: EntityManager,
    private val logonService: LogonService,
    private val userRepository: UserRepository,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
    private val deviceRepository: DeviceRepository,
) {

    fun getObjectData(
        token: String,
        objectName: String,
        start: String?,
        duration: Int?,
    ): ObjectDataResponse {
        val userEntity = getUser(token) ?: return ObjectDataResponse(errorMessage = "A user with this login and password was not found")
        if (userEntity.isDisabled == true) {
            return ObjectDataResponse(errorMessage = "User has been blocked")
        }

        duration?.let {
            if (duration > 86_400) {
                return ObjectDataResponse(errorMessage = "The period cannot be longer than 1 day")
            }
        }

        // 2020-08-30T18:43:00Z
        // 2023-01-02T23:40:57Z
        val begTime = start?.let { Instant.parse(start).epochSeconds.toInt() }
        val endTime = begTime?.let { begTime + (duration ?: 86_400) }

        val enabledUserIds = getEnabledUserIds(
            module = AppModuleMMS.ALL_OBJECT,
            actionType = ActionType.MODULE_TABLE,
            relatedUserIds = logonService.loadRelatedUserIds(
                userId = userEntity.id,
                parentId = userEntity.parentId ?: 0,
                orgType = userEntity.orgType ?: OrgType.ORG_TYPE_WORKER,
            ),
            roles = userEntity.roles,
        )

        val objectEntities = objectRepository.findByUserIdInAndName(enabledUserIds, objectName)
        if (objectEntities.size > 1) {
            return ObjectDataResponse(errorMessage = "${objectEntities.size} objects with this name were found")
        }
        val objectEntity = objectEntities.firstOrNull() ?: return ObjectDataResponse(errorMessage = "An object with this name was not found.")

        val sensors = mutableListOf<ObjectDataResponseSensorInfo>()
        sensorRepository.findByObjAndPeriod(objectEntity, begTime ?: -1, endTime ?: -1).forEach { sensorEntity ->
            val data = mutableListOf<Map<String, String>>()

            SensorService.checkAndCreateSensorTables(entityManager, sensorEntity.id)

            ApplicationService.queryNativeSql(
                entityManager,
                """
                    SELECT ontime_0 , ontime_1 , type_0 , value_0 , value_1 , value_2 , value_3
                    FROM MMS_agg_${sensorEntity.id}
                """ + (
                        begTime?.let {
                            if (sensorEntity.sensorType == SensorConfig.SENSOR_WORK) {
                                """ 
                                    WHERE ontime_0 <= $endTime
                                      AND ontime_1 >= $begTime
                                    ORDER BY ontime_0 ASC
                                """
                            } else {
                                """ 
                                    WHERE ontime_0 BETWEEN $begTime AND $endTime
                                    ORDER BY ontime_0 ASC
                                """
                            }
                        } ?: """
                                WHERE ontime_0 = ( SELECT MAX(ontime_0) FROM MMS_agg_${sensorEntity.id} )
                             """
                        )
            ) { rs ->
                while (rs.next()) {
                    var pos = 1

                    val ontime0 = rs.getInt(pos++)
                    val ontime1 = rs.getInt(pos++)
                    val type0 = rs.getInt(pos++)
                    val value0 = rs.getDouble(pos++)
                    val value1 = rs.getDouble(pos++)
                    val value2 = rs.getDouble(pos++)
                    val value3 = rs.getDouble(pos++)

                    when (sensorEntity.sensorType) {
                        SensorConfig.SENSOR_GEO -> {
                            data += mapOf(
                                "time" to Instant.fromEpochSeconds(ontime0.toLong()).toString(),
                                "speed" to type0.toString(),
                                "wgsX" to value0.toString(),
                                "wgsY" to value1.toString(),
                                "absoluteRun" to value2.toString(),
                                "relativeRun" to value3.toString(),
                            )
                        }

                        SensorConfig.SENSOR_WORK -> {
                            data += mapOf(
                                "begTime" to Instant.fromEpochSeconds(
                                    (
                                            begTime?.let {
                                                max(begTime, ontime0)
                                            } ?: ontime0
                                            ).toLong()).toString(),
                                "endTime" to Instant.fromEpochSeconds(
                                    (
                                            endTime?.let {
                                                min(endTime, ontime1)
                                            } ?: ontime1
                                            ).toLong()).toString(),
                                "state" to type0.toString(),
                            )
                        }

                        in SensorConfig.analogueSensorTypes -> {
                            data += mapOf(
                                "time" to Instant.fromEpochSeconds(ontime0.toLong()).toString(),
                                //--- сразу выводим значение после сглаживания
                                "value" to value1.toString(),
                            )
                        }

                        in SensorConfig.counterSensorTypes -> {
                            data += mapOf(
                                "time" to Instant.fromEpochSeconds(ontime0.toLong()).toString(),
                                "value" to value0.toString(),
                                "delta" to value1.toString(),
                                "avgHour" to value2.toString(),
                                "avgMinute" to (value2 / 60).toString(), // оставлено для совместимости с ИНКами
                            )
                        }
                    }
                }
            }
            sensors += ObjectDataResponseSensorInfo(
                id = sensorEntity.name,
                name = sensorEntity.descr,
                type = sensorEntity.sensorType,
                data = data,
            )
        }

        return ObjectDataResponse(sensors = sensors)
    }

    fun getDevicesStatus(
        token: String,
    ): DevicesStatusResponse {
        val userEntity = getUser(token) ?: return DevicesStatusResponse(errorMessage = "A user with this login and password was not found")
        if (userEntity.isDisabled == true) {
            return DevicesStatusResponse(errorMessage = "User has been blocked")
        }

        val enabledUserIds = getEnabledUserIds(
            module = AppModuleMMS.DEVICE,
            actionType = ActionType.MODULE_TABLE,
            relatedUserIds = logonService.loadRelatedUserIds(
                userId = userEntity.id,
                parentId = userEntity.parentId ?: 0,
                orgType = userEntity.orgType ?: OrgType.ORG_TYPE_WORKER,
            ),
            roles = userEntity.roles,
        )

        val devices = mutableListOf<DevicesStatusResponseDeviceInfo>()
        deviceRepository.findByUserIdIn(enabledUserIds).forEach { deviceEntity ->
            devices += DevicesStatusResponseDeviceInfo(
                serialNo = deviceEntity.serialNo,
                objectName = deviceEntity.obj?.name,
                lastSessionTime = getDateTimeDMYHMSString(userEntity.timeOffset ?: 0, deviceEntity.lastSessionTime ?: 0),
                lastSessionStatus = deviceEntity.lastSessionStatus,
                lastSessionError = deviceEntity.lastSessionError,
            )
        }

        return DevicesStatusResponse(devices = devices)
    }

    private fun getUser(token: String): UserEntity? {
        val tokens = token.split(" ")
        if (tokens.size != 2) {
            return null
        }
        tokens.firstOrNull()?.let { login ->
            tokens.lastOrNull()?.let { password ->
                return userRepository.findByLoginAndPassword(login, password).firstOrNull()
            }
        }
        return null
    }
}