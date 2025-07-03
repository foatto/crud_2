package foatto.server.service.composite

import foatto.core.model.AppAction
import foatto.core.model.response.composite.CompositeListItemData
import foatto.core_mms.AppModuleMMS
import foatto.server.entity.DeviceEntity
import foatto.server.entity.ObjectEntity
import foatto.server.repository.DeviceRepository
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import foatto.server.service.scheme.SchemeAnalogueIndicatorStateService
import foatto.server.service.scheme.SchemeCounterIndicatorStateService
import foatto.server.service.scheme.SchemeWorkIndicatorStateService
import org.springframework.stereotype.Service

@Service
class CompositeObjectDashboardService(
    private val objectRepository: ObjectRepository,
    private val deviceRepository: DeviceRepository,
    private val sensorRepository: SensorRepository,
    private val schemeAnalogueIndicatorStateService: SchemeAnalogueIndicatorStateService,
    private val schemeCounterIndicatorStateService: SchemeCounterIndicatorStateService,
    private val schemeWorkIndicatorStateService: SchemeWorkIndicatorStateService,
) : AbstractCompositeDashboardService(
    objectRepository = objectRepository,
    deviceRepository = deviceRepository,
    sensorRepository = sensorRepository,
    schemeAnalogueIndicatorStateService = schemeAnalogueIndicatorStateService,
    schemeCounterIndicatorStateService = schemeCounterIndicatorStateService,
    schemeWorkIndicatorStateService = schemeWorkIndicatorStateService,
) {

    override fun getCompositeResponseAction(action: AppAction): AppAction =
        action.copy(
            parentModule = AppModuleMMS.OBJECT,
        )

    override fun getCompositeItems(
        sessionId: Long,
        action: AppAction,
    ): List<CompositeListItemData>? = null

    override fun getHeaderRows(objectEntity: ObjectEntity, deviceEntity: DeviceEntity?): List<Pair<String, String>> =
        listOf(
            "Наименование объекта" to (objectEntity.name ?: "-"),
            "Модель объекта" to (objectEntity.model ?: "-"),
        )
}

