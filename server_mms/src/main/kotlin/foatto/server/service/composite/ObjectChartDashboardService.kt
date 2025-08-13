package foatto.server.service.composite

import foatto.server.repository.DeviceRepository
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import org.springframework.stereotype.Service

@Service
class ObjectChartDashboardService(
    objectRepository: ObjectRepository,
    deviceRepository: DeviceRepository,
    sensorRepository: SensorRepository,
//    chartAnalogueSensorService: ChartAnalogueSensorService,
) : AbstractChartDashboardService(
    objectRepository = objectRepository,
    deviceRepository = deviceRepository,
    sensorRepository = sensorRepository,
//    chartAnalogueSensorService = chartAnalogueSensorService,
) {

    override fun withObjectList(): Boolean = false

}

