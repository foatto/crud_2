package foatto.server.service.composite

import foatto.server.repository.DeviceRepository
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import foatto.server.service.scheme.SchemeAnalogueIndicatorStateService
import foatto.server.service.scheme.SchemeCounterIndicatorStateService
import foatto.server.service.scheme.SchemeWorkIndicatorStateService
import org.springframework.stereotype.Service

@Service
class ObjectListSchemeDashboardService(
    private val objectRepository: ObjectRepository,
    private val deviceRepository: DeviceRepository,
    private val sensorRepository: SensorRepository,
    private val schemeAnalogueIndicatorStateService: SchemeAnalogueIndicatorStateService,
    private val schemeCounterIndicatorStateService: SchemeCounterIndicatorStateService,
    private val schemeWorkIndicatorStateService: SchemeWorkIndicatorStateService,
) : AbstractSchemeDashboardService(
    objectRepository = objectRepository,
    deviceRepository = deviceRepository,
    sensorRepository = sensorRepository,
    schemeAnalogueIndicatorStateService = schemeAnalogueIndicatorStateService,
    schemeCounterIndicatorStateService = schemeCounterIndicatorStateService,
    schemeWorkIndicatorStateService = schemeWorkIndicatorStateService,
) {

    override fun withObjectList(): Boolean = true

}

