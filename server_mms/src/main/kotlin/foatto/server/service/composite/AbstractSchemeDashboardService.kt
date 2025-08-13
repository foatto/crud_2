package foatto.server.service.composite

import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.response.HeaderData
import foatto.core.model.response.xy.scheme.SchemeResponse
import foatto.core_mms.AppModuleMMS
import foatto.server.DashboardSensorTypeEnum
import foatto.server.entity.DeviceEntity
import foatto.server.entity.ObjectEntity
import foatto.server.entity.SensorEntity
import foatto.server.repository.DeviceRepository
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import foatto.server.service.scheme.AbstractSchemeIndicatorStateService
import foatto.server.service.scheme.SchemeAnalogueIndicatorStateService
import foatto.server.service.scheme.SchemeCounterIndicatorStateService
import foatto.server.service.scheme.SchemeWorkIndicatorStateService

abstract class AbstractSchemeDashboardService(
    objectRepository: ObjectRepository,
    deviceRepository: DeviceRepository,
    sensorRepository: SensorRepository,
    private val schemeAnalogueIndicatorStateService: SchemeAnalogueIndicatorStateService,
    private val schemeCounterIndicatorStateService: SchemeCounterIndicatorStateService,
    private val schemeWorkIndicatorStateService: SchemeWorkIndicatorStateService,
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
        addCounterSensors(
            objectEntity = objectEntity,
            deviceEntity = deviceEntity,
            sensorEntities = sensorEntities,
        )
        addWorkSensors(
            objectEntity = objectEntity,
            deviceEntity = deviceEntity,
            sensorEntities = sensorEntities,
        )
    }

    override fun getViewSizeRelation(viewWidth: Float, viewHeight: Float): Float =
        viewWidth / viewHeight * AbstractSchemeIndicatorStateService.SCHEME_HEIGHT / AbstractSchemeIndicatorStateService.SCHEME_WIDTH

    override fun getBlockWidth(): Int = 1
    override fun getBlockHeight(): Int = 1

    override fun getCompositeBlockAction(
        sensorType: DashboardSensorTypeEnum,
        sensorEntity: SensorEntity
    ): AppAction = AppAction(
        type = ActionType.MODULE_SCHEME,
        module = when (sensorType) {
            DashboardSensorTypeEnum.ANALOGUE -> AppModuleMMS.SCHEME_ANALOGUE_INDICATOR_STATE
            DashboardSensorTypeEnum.COUNTER -> AppModuleMMS.SCHEME_COUNTER_INDICATOR_STATE
            DashboardSensorTypeEnum.WORK -> AppModuleMMS.SCHEME_WORK_INDICATOR_STATE
        },
        id = sensorEntity.id,
    )

    override fun getSchemeResponse(sensorType: DashboardSensorTypeEnum, sensorEntity: SensorEntity): SchemeResponse? = SchemeResponse(
        elementConfigs = when (sensorType) {
            DashboardSensorTypeEnum.ANALOGUE -> schemeAnalogueIndicatorStateService.getElementConfigs()
            DashboardSensorTypeEnum.COUNTER -> schemeCounterIndicatorStateService.getElementConfigs()
            DashboardSensorTypeEnum.WORK -> schemeWorkIndicatorStateService.getElementConfigs()
        },
        tabCaption = "",
        headerData = HeaderData(
            titles = emptyList(),
            rows = listOf("Описание датчика" to (sensorEntity.descr ?: "(датчик без описания)")),
        ),
    )
}