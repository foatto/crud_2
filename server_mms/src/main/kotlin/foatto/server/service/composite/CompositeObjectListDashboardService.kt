package foatto.server.service.composite

import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.response.composite.CompositeListItemData
import foatto.core_mms.AppModuleMMS
import foatto.server.SpringApp
import foatto.server.entity.DeviceEntity
import foatto.server.entity.ObjectEntity
import foatto.server.getEnabledUserIds
import foatto.server.repository.DeviceRepository
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import foatto.server.service.scheme.SchemeAnalogueIndicatorStateService
import foatto.server.service.scheme.SchemeCounterIndicatorStateService
import foatto.server.service.scheme.SchemeWorkIndicatorStateService
import org.springframework.stereotype.Service

@Service
class CompositeObjectListDashboardService(
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
            id = null,
            parentModule = null,
        )

    override fun getCompositeItems(
        sessionId: Long,
        action: AppAction,
    ): List<CompositeListItemData>? {
        val result = mutableListOf<CompositeListItemData>()

        val sessionData = SpringApp.getSessionData(sessionId) ?: return emptyList()
        val userConfig = sessionData.serverUserConfig ?: return emptyList()

        val enabledUserIds = getEnabledUserIds(AppModuleMMS.OBJECT, ActionType.MODULE_TABLE, userConfig.relatedUserIds, userConfig.roles)

        val objectEntities = objectRepository.findByUserIdIn(enabledUserIds)
        val deviceEntities = deviceRepository.findAll()

        for (objectEntity in objectEntities) {
            if (objectEntity.isDisabled == true) {
                continue
            }

            val deviceList = mutableListOf<CompositeListItemData>()

            val filteredDeviceEntities = deviceEntities.filter { deviceEntity ->
                deviceEntity.obj?.id == objectEntity.id
            }
            for (deviceEntity in filteredDeviceEntities) {
                deviceList += CompositeListItemData(
                    text = deviceEntity.name ?: deviceEntity.serialNo ?: deviceEntity.index?.toString() ?: "(без наименования, серийного номера и индекса)",
                    itemId = deviceEntity.id,
                    itemModule = AppModuleMMS.DEVICE,
                )
            }

//            val rowOwnerShortName = userConfig.shortNames[objectEntity.userId]
//            val rowOwnerFullName = userConfig.fullNames[objectEntity.userId]

            result += CompositeListItemData(
                text = objectEntity.name ?: "(без наименования)",
                itemId = objectEntity.id,
                itemModule = AppModuleMMS.OBJECT,
                subListDatas = if (deviceList.isEmpty()) {
                    null
                } else {
                    deviceList
                },
            )
        }

        return result
    }

    override fun getHeaderRows(objectEntity: ObjectEntity, deviceEntity: DeviceEntity?): List<Pair<String, String>> {
        val rows = mutableListOf(
            "Наименование объекта" to (objectEntity.name ?: "-"),
            "Модель объекта" to (objectEntity.model ?: "-"),
        )
        deviceEntity?.let {
            rows += "Серийный номер контроллера" to (deviceEntity.serialNo ?: "-")
            rows += "Наименование контроллера" to (deviceEntity.name ?: "-")
        }

        return rows
    }

}

