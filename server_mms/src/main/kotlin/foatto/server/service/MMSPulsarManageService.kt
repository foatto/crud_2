package foatto.server.service

import foatto.core.util.getCurrentTimeInt
import foatto.server.ds.PulsarCommandRequest
import foatto.server.ds.PulsarCommandResult
import foatto.server.repository.DeviceManageRepository
import foatto.server.repository.DeviceRepository
import org.springframework.stereotype.Service

@Service
class MMSPulsarManageService(
    private val deviceRepository: DeviceRepository,
    private val deviceManageRepository: DeviceManageRepository,
) {

    fun getPulsarCommand(pulsarCommandRequest: PulsarCommandRequest): PulsarCommandResult {
        val deviceEntity = deviceRepository.findBySerialNo(pulsarCommandRequest.serialNo).firstOrNull() ?: return PulsarCommandResult(errorCode = 1)

        return deviceManageRepository.findByDevice(deviceEntity)
            .filter { deviceManageEntity ->
                deviceManageEntity.sendTime == null
            }
            .sortedBy { deviceManageEntity ->
                deviceManageEntity.createTime
            }
            .firstOrNull()?.let { deviceManageEntity ->
                deviceManageEntity.sendTime = getCurrentTimeInt()
                deviceManageRepository.saveAndFlush(deviceManageEntity)

                PulsarCommandResult(errorCode = 0, command = deviceManageEntity.command)
            } ?: PulsarCommandResult(errorCode = 0, command = null)
    }
}