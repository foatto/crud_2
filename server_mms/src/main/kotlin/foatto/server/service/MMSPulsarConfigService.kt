package foatto.server.service

import foatto.core.util.getCurrentTimeInt
import foatto.server.ds.CoreTelematicFunction
import foatto.server.ds.PortNumbers
import foatto.server.ds.PulsarConfig
import foatto.server.ds.PulsarConfigResult
import foatto.server.entity.SensorCalibrationEntity
import foatto.server.entity.SensorEntity
import foatto.server.model.SensorConfig
import foatto.server.model.SensorConfigGeo
import foatto.server.repository.DeviceRepository
import foatto.server.repository.SensorCalibrationRepository
import foatto.server.repository.SensorRepository
import foatto.server.util.getNextId
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service

@Service
class MMSPulsarConfigService(
    private val entityManager: EntityManager,
    private val deviceRepository: DeviceRepository,
    private val sensorRepository: SensorRepository,
    private val sensorCalibrationRepository: SensorCalibrationRepository,
) {

    fun storePulsarConfig(pulsarConfig: PulsarConfig): PulsarConfigResult {

        val deviceEntity = deviceRepository.findBySerialNo(pulsarConfig.serialNo).firstOrNull() ?: return PulsarConfigResult(1)
        val objectEntity = deviceEntity.obj ?: return PulsarConfigResult(2)

        deviceEntity.name = pulsarConfig.name
        deviceRepository.saveAndFlush(deviceEntity)

        pulsarConfig.sensors.forEach { sensor ->
            val (sensorType, portNum, sensorIndex) = when (sensor.id) {
                in 0x0100..0x010F -> Triple(SensorConfig.SENSOR_LIQUID_LEVEL, PortNumbers.LLS_LEVEL_120, sensor.id - 0x0100)
                in 0x0140..0x014F -> Triple(SensorConfig.SENSOR_VOLTAGE, PortNumbers.GALILEO_VOLTAGE_140, sensor.id - 0x0140)
                in 0x0180..0x018F -> Triple(SensorConfig.SENSOR_LIQUID_USING, PortNumbers.GALILEO_COUNT_110, sensor.id - 0x0180)

                in 0x01C1..0x01C4 -> Triple(SensorConfig.SENSOR_ENERGO_COUNT_AD, PortNumbers.MERCURY_COUNT_ACTIVE_DIRECT_160, sensor.id - 0x01C1)
                in 0x0201..0x0204 -> Triple(SensorConfig.SENSOR_ENERGO_COUNT_AR, PortNumbers.MERCURY_COUNT_ACTIVE_REVERSE_164, sensor.id - 0x0201)
                in 0x0241..0x0244 -> Triple(SensorConfig.SENSOR_ENERGO_COUNT_RD, PortNumbers.MERCURY_COUNT_REACTIVE_DIRECT_168, sensor.id - 0x0241)
                in 0x0281..0x0284 -> Triple(SensorConfig.SENSOR_ENERGO_COUNT_RR, PortNumbers.MERCURY_COUNT_REACTIVE_REVERSE_172, sensor.id - 0x0281)

                in 0x02C1..0x02C4 -> Triple(SensorConfig.SENSOR_ENERGO_VOLTAGE, PortNumbers.MERCURY_VOLTAGE_A_180, sensor.id - 0x02C1)
                in 0x0301..0x0304 -> Triple(SensorConfig.SENSOR_ENERGO_VOLTAGE, PortNumbers.MERCURY_VOLTAGE_B_184, sensor.id - 0x0301)
                in 0x0341..0x0344 -> Triple(SensorConfig.SENSOR_ENERGO_VOLTAGE, PortNumbers.MERCURY_VOLTAGE_C_188, sensor.id - 0x0341)

                in 0x0381..0x0384 -> Triple(SensorConfig.SENSOR_ENERGO_CURRENT, PortNumbers.MERCURY_CURRENT_A_200, sensor.id - 0x0381)
                in 0x03C1..0x03C4 -> Triple(SensorConfig.SENSOR_ENERGO_CURRENT, PortNumbers.MERCURY_CURRENT_B_204, sensor.id - 0x03C1)
                in 0x0401..0x0404 -> Triple(SensorConfig.SENSOR_ENERGO_CURRENT, PortNumbers.MERCURY_CURRENT_C_208, sensor.id - 0x0401)

                in 0x0441..0x0444 -> Triple(SensorConfig.SENSOR_ENERGO_POWER_KOEF, PortNumbers.MERCURY_POWER_KOEF_A_220, sensor.id - 0x0441)
                in 0x0481..0x0484 -> Triple(SensorConfig.SENSOR_ENERGO_POWER_KOEF, PortNumbers.MERCURY_POWER_KOEF_B_224, sensor.id - 0x0481)
                in 0x0501..0x0504 -> Triple(SensorConfig.SENSOR_ENERGO_POWER_KOEF, PortNumbers.MERCURY_POWER_KOEF_C_228, sensor.id - 0x0501)

                in 0x0510..0x0513 -> Triple(SensorConfig.SENSOR_ENERGO_POWER_ACTIVE, PortNumbers.MERCURY_POWER_ACTIVE_A_232, sensor.id - 0x0510)
                in 0x0514..0x0517 -> Triple(SensorConfig.SENSOR_ENERGO_POWER_ACTIVE, PortNumbers.MERCURY_POWER_ACTIVE_B_236, sensor.id - 0x0514)
                in 0x0518..0x051B -> Triple(SensorConfig.SENSOR_ENERGO_POWER_ACTIVE, PortNumbers.MERCURY_POWER_ACTIVE_C_240, sensor.id - 0x0518)

                in 0x051C..0x051F -> Triple(SensorConfig.SENSOR_ENERGO_POWER_REACTIVE, PortNumbers.MERCURY_POWER_REACTIVE_A_244, sensor.id - 0x051C)
                in 0x0520..0x0523 -> Triple(SensorConfig.SENSOR_ENERGO_POWER_REACTIVE, PortNumbers.MERCURY_POWER_REACTIVE_B_248, sensor.id - 0x0520)
                in 0x0524..0x0527 -> Triple(SensorConfig.SENSOR_ENERGO_POWER_REACTIVE, PortNumbers.MERCURY_POWER_REACTIVE_C_252, sensor.id - 0x0524)

                in 0x0528..0x052B -> Triple(SensorConfig.SENSOR_ENERGO_POWER_FULL, PortNumbers.MERCURY_POWER_FULL_A_256, sensor.id - 0x0528)
                in 0x052C..0x052F -> Triple(SensorConfig.SENSOR_ENERGO_POWER_FULL, PortNumbers.MERCURY_POWER_FULL_B_260, sensor.id - 0x052C)
                in 0x0530..0x0533 -> Triple(SensorConfig.SENSOR_ENERGO_POWER_FULL, PortNumbers.MERCURY_POWER_FULL_C_264, sensor.id - 0x0530)

                in 0x0541..0x0544 -> Triple(SensorConfig.SENSOR_MASS_FLOW, PortNumbers.EMIS_MASS_FLOW_270, sensor.id - 0x0541)

                in 0x0550..0x0553 -> Triple(SensorConfig.SENSOR_ENERGO_TRANSFORM_KOEF_CURRENT, PortNumbers.MERCURY_TRANSFORM_KOEF_CURRENT_360, sensor.id - 0x0550)
                in 0x0560..0x0563 -> Triple(SensorConfig.SENSOR_ENERGO_TRANSFORM_KOEF_VOLTAGE, PortNumbers.MERCURY_TRANSFORM_KOEF_VOLTAGE_370, sensor.id - 0x0560)

                in 0x0581..0x0584 -> Triple(SensorConfig.SENSOR_DENSITY, PortNumbers.EMIS_DENSITY_280, sensor.id - 0x0581)
                in 0x05C1..0x05C4 -> Triple(SensorConfig.SENSOR_TEMPERATURE, PortNumbers.EMIS_TEMPERATURE_290, sensor.id - 0x05C1)
                in 0x0601..0x0604 -> Triple(SensorConfig.SENSOR_VOLUME_FLOW, PortNumbers.EMIS_VOLUME_FLOW_300, sensor.id - 0x0601)
                in 0x0641..0x0644 -> Triple(SensorConfig.SENSOR_MASS_ACCUMULATED, PortNumbers.EMIS_ACCUMULATED_MASS_310, sensor.id - 0x0641)
                in 0x0681..0x0684 -> Triple(SensorConfig.SENSOR_VOLUME_ACCUMULATED, PortNumbers.EMIS_ACCUMULATED_VOLUME_320, sensor.id - 0x0681)

                in 0x0700..0x0703 -> Triple(SensorConfig.SENSOR_ENERGO_POWER_ACTIVE, PortNumbers.MERCURY_POWER_ACTIVE_ABC_330, sensor.id - 0x0700)
                in 0x0710..0x0713 -> Triple(SensorConfig.SENSOR_ENERGO_POWER_REACTIVE, PortNumbers.MERCURY_POWER_REACTIVE_ABC_340, sensor.id - 0x0710)
                in 0x0720..0x0723 -> Triple(SensorConfig.SENSOR_ENERGO_POWER_FULL, PortNumbers.MERCURY_POWER_FULL_ABC_350, sensor.id - 0x0720)

                in 0x0800..0x080F -> Triple(SensorConfig.SENSOR_LIQUID_LEVEL, PortNumbers.PMP_LEVEL_540, sensor.id - 0x0800)
                in 0x0810..0x081F -> Triple(SensorConfig.SENSOR_TEMPERATURE, PortNumbers.PMP_TEMPERATURE_560, sensor.id - 0x0810)
                in 0x0820..0x082F -> Triple(SensorConfig.SENSOR_LIQUID_LEVEL, PortNumbers.PMP_VOLUME_580, sensor.id - 0x0820)
                in 0x0830..0x083F -> Triple(SensorConfig.SENSOR_WEIGHT, PortNumbers.PMP_MASS_600, sensor.id - 0x0830)
                in 0x0840..0x084F -> Triple(SensorConfig.SENSOR_DENSITY, PortNumbers.PMP_DENSITY_620, sensor.id - 0x0840)

                else -> return PulsarConfigResult(3)
            }

            val lastDeviceIndex = deviceRepository
                .findByObject(objectEntity)
                .sortedBy { deviceEntity ->
                    deviceEntity.index
                }.lastOrNull()?.index ?: -1

            val recordId = getNextId { nextId -> sensorRepository.existsById(nextId) }

            val sensorEntity = SensorEntity(
                id = recordId,
                obj = objectEntity,
                name = "",
                group = sensor.group,
                descr = sensor.descr,
                portNum = (lastDeviceIndex + 1) * CoreTelematicFunction.MAX_PORT_PER_DEVICE + portNum + sensorIndex,
                sensorType = sensorType,
                begTime = getCurrentTimeInt(),
                endTime = null,
                serialNo = null,

                minMovingTime = 1,
                minParkingTime = 300,
                minOverSpeedTime = 60,
                isAbsoluteRun = true,
                speedRoundRule = SensorConfigGeo.SPEED_ROUND_RULE_STANDART,
                runKoef = 1.0,
                isUsePos = true,
                isUseSpeed = true,
                isUseRun = true,

                minIgnore = sensor.minIgnore,
                maxIgnore = sensor.maxIgnore,
                dim = sensor.dim,

                isAboveBorder = sensor.isAboveBorder,
                onOffBorder = sensor.onOffBorder,
                idleBorder = sensor.idleBorder,
                limitBorder = sensor.limitBorder,
                minOnTime = sensor.minOnTime,
                minOffTime = sensor.minOffTime,

                minView = sensor.minView,
                maxView = sensor.maxView,
                minLimit = sensor.minLimit,
                maxLimit = sensor.maxLimit,
                smoothTime = sensor.smoothTime,
                indicatorDelimiterCount = 4,
                indicatorMultiplicator = 1.0,

                isAbsoluteCount = sensor.isAbsoluteCount,
                inOutType = sensor.inOutType,

                containerType = sensor.containerType,

                phase = sensor.phase,

                liquidName = sensor.liquidName,
                liquidNorm = sensor.liquidNorm,

                schemeX = null,
                schemeY = null,
            )
            sensorRepository.save(sensorEntity)

            sensorCalibrationRepository.deleteBySensorId(recordId)
            sensor.sensorValues.forEachIndexed { index, sensorValue ->
                val dataValue = sensor.dataValues[index]

                val sensorCalibrationEntity = SensorCalibrationEntity(
                    id = getNextId { nextId -> sensorCalibrationRepository.existsById(nextId) },
                    sensor = sensorEntity,
                    sensorValue = sensorValue,
                    dataValue = dataValue,
                )
                sensorCalibrationRepository.save(sensorCalibrationEntity)
            }
            sensorCalibrationRepository.flush()

            SensorService.checkAndCreateSensorTables(entityManager, recordId)
        }
        sensorRepository.flush()

        return PulsarConfigResult(0)
    }
}