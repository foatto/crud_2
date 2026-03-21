package foatto.server.service.pulsar

import foatto.core.util.getCurrentTimeInt
import foatto.server.ds.request.PulsarCommandRequest
import foatto.server.ds.response.PulsarCommandResponse
import foatto.server.repository.DeviceManageRepository
import foatto.server.repository.DeviceRepository
import org.springframework.stereotype.Service

@Service
class MMSPulsarManageService(
    private val deviceRepository: DeviceRepository,
    private val deviceManageRepository: DeviceManageRepository,
) {

    fun getPulsarCommand(pulsarCommandRequest: PulsarCommandRequest): PulsarCommandResponse {
        val deviceEntity = deviceRepository.findBySerialNo(pulsarCommandRequest.serialNo).firstOrNull() ?: return PulsarCommandResponse(errorCode = 1)

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

                PulsarCommandResponse(errorCode = 0, command = deviceManageEntity.command)
            } ?: PulsarCommandResponse(errorCode = 0, command = null)
    }
}