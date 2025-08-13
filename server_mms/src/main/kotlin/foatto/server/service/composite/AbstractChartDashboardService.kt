package foatto.server.service.composite

import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.response.HeaderData
import foatto.core.model.response.chart.ChartResponse
import foatto.core_mms.AppModuleMMS
import foatto.server.DashboardSensorTypeEnum
import foatto.server.entity.DeviceEntity
import foatto.server.entity.ObjectEntity
import foatto.server.entity.SensorEntity
import foatto.server.repository.DeviceRepository
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository

abstract class AbstractChartDashboardService(
    objectRepository: ObjectRepository,
    deviceRepository: DeviceRepository,
    sensorRepository: SensorRepository,
//    private val chartAnalogueSensorService: ChartAnalogueSensorService,
) : AbstractDashboardService(
    objectRepository = objectRepository,
    deviceRepository = deviceRepository,
    sensorRepository = sensorRepository,
) {

    override fun addSensors(
        objectEntity: ObjectEntity,
        deviceEntity: DeviceEntity?,
        sensorEntities: MutableList<Pair<DashboardSensorTypeEnum, SensorEntity>>
    ) {
        addAnalogueSensors(
            objectEntity = objectEntity,
            deviceEntity = deviceEntity,
            sensorEntities = sensorEntities,
        )
    }

    override fun getViewSizeRelation(viewWidth: Float, viewHeight: Float): Float =
        viewWidth / viewHeight * 1 / 3

    override fun getBlockWidth(): Int = 3
    override fun getBlockHeight(): Int = 1

    override fun getCompositeBlockAction(
        sensorType: DashboardSensorTypeEnum,
        sensorEntity: SensorEntity
    ): AppAction = AppAction(
        type = ActionType.MODULE_CHART,
        module = when (sensorType) {
            DashboardSensorTypeEnum.ANALOGUE -> AppModuleMMS.CHART_SENSOR
//            DashboardSensorTypeEnum.COUNTER -> AppModuleMMS.SCHEME_COUNTER_INDICATOR_STATE
//            DashboardSensorTypeEnum.WORK -> AppModuleMMS.SCHEME_WORK_INDICATOR_STATE
            else -> AppModuleMMS.CHART_SENSOR
        },
        id = sensorEntity.id,
        timeRangeType = 24 * 60 * 60,   // графики за последние 24 часа
    )

    override fun getChartResponse(sensorEntity: SensorEntity): ChartResponse? = ChartResponse(
        tabCaption = "",
        headerData = HeaderData(
            titles = emptyList(),
            rows = listOf("Описание датчика" to (sensorEntity.descr ?: "(датчик без описания)")),
        ),
    )
}