package foatto.server.service.composite

import foatto.core.ActionType
import foatto.core.i18n.getLocalizedMessage
import foatto.core.model.AppAction
import foatto.core.model.request.CompositeActionRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.CompositeActionResponse
import foatto.core.model.response.HeaderData
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.TitleData
import foatto.core.model.response.chart.ChartResponse
import foatto.core.model.response.composite.CompositeBlock
import foatto.core.model.response.composite.CompositeLayoutData
import foatto.core.model.response.composite.CompositeListItemData
import foatto.core.model.response.composite.CompositeResponse
import foatto.core.model.response.xy.scheme.SchemeResponse
import foatto.core.util.getCurrentTimeInt
import foatto.core_mms.AppModuleMMS
import foatto.server.DashboardSensorTypeEnum
import foatto.server.SpringApp
import foatto.server.appModuleConfigs
import foatto.server.checkAccessPermission
import foatto.server.ds.CoreTelematicFunction
import foatto.server.entity.DeviceEntity
import foatto.server.entity.ObjectEntity
import foatto.server.entity.SensorEntity
import foatto.server.getEnabledUserIds
import foatto.server.model.sensor.SensorConfig
import foatto.server.repository.DeviceRepository
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import kotlinx.serialization.json.Json
import org.springframework.data.repository.findByIdOrNull
import kotlin.math.ceil
import kotlin.math.round
import kotlin.math.sqrt

abstract class AbstractDashboardService(
    private val objectRepository: ObjectRepository,
    private val deviceRepository: DeviceRepository,
    private val sensorRepository: SensorRepository,
) {

    protected abstract fun withObjectList(): Boolean

    fun composite(
        sessionId: Long,
        action: AppAction,
    ): AppResponse {
        val actionModule = action.module

        val sessionData = SpringApp.getSessionData(sessionId) ?: return AppResponse(ResponseCode.LOGON_NEED)
        val userConfig = sessionData.serverUserConfig ?: return AppResponse(ResponseCode.LOGON_NEED)
        if (!checkAccessPermission(actionModule, userConfig.roles)) {
            return AppResponse(ResponseCode.LOGON_NEED)
        }

        val moduleConfig = appModuleConfigs[actionModule] ?: return AppResponse(ResponseCode.LOGON_NEED)

        return AppResponse(
            responseCode = ResponseCode.MODULE_COMPOSITE,
            composite = CompositeResponse(
                tabCaption = getLocalizedMessage(moduleConfig.captions, userConfig.lang),
                action = getCompositeResponseAction(action),
                items = getCompositeItems(sessionId)
            )
        )
    }

    fun compositeAction(
        sessionId: Long,
        compositeActionRequest: CompositeActionRequest,
    ): CompositeActionResponse {
        val action = compositeActionRequest.action
        val actionModule = action.module

        val (viewWidth, viewHeight) = compositeActionRequest.viewSize

        val sessionData = SpringApp.getSessionData(sessionId) ?: return getErrorCompositeActionResponse()
        val userConfig = sessionData.serverUserConfig ?: return getErrorCompositeActionResponse()
        if (!checkAccessPermission(actionModule, userConfig.roles)) {
            return getErrorCompositeActionResponse()
        }

        val moduleConfig = appModuleConfigs[actionModule] ?: return getErrorCompositeActionResponse()
        val (objectEntity, deviceEntity) = when (action.parentModule) {
            AppModuleMMS.OBJECT -> {
                val oe = action.id?.let { objectId ->
                    objectRepository.findByIdOrNull(objectId)
                } ?: return getErrorCompositeActionResponse()

                oe to null
            }

            AppModuleMMS.DEVICE -> {
                val de = action.id?.let { deviceId ->
                    deviceRepository.findByIdOrNull(deviceId)
                } ?: return getErrorCompositeActionResponse()

                val oe = de.obj ?: return getErrorCompositeActionResponse()

                oe to de
            }

            else -> return getErrorCompositeActionResponse()
        }

        val layoutSaveKey = action.parentModule + action.id
        val compositeLayoutDatas = userConfig.userProperties[layoutSaveKey]?.let { propertyValue ->
            try {
                Json.decodeFromString<Map<Int, CompositeLayoutData>>(propertyValue)
            } catch (iae: IllegalArgumentException) {
                null
            }
        }

        val caption = getLocalizedMessage(moduleConfig.captions, userConfig.lang)
        val rows = getHeaderRows(objectEntity, deviceEntity)

        val sensorEntities = mutableListOf<Pair<DashboardSensorTypeEnum, SensorEntity>>()
        addSensors(
            objectEntity = objectEntity,
            deviceEntity = deviceEntity,
            sensorEntities = sensorEntities,
        )

        val viewSizeRelation = getViewSizeRelation(viewWidth, viewHeight)
        val blockRows = round(sqrt(sensorEntities.size / viewSizeRelation)).toInt()
        val blockCols = ceil(sensorEntities.size.toFloat() / blockRows).toInt()

        val blocks = mutableListOf<CompositeBlock>()

        var row = 0
        var col = 0

        sensorEntities.forEach { (sensorType, sensorEntity) ->
            //--- логика обработки:
            //--- если compositeLayoutDatas == null, то сохранённого состояния вообще нет и раскладываем сами на серверной стороне,
            //--- иначе ищем там раскладку. Если раскладки нет - значит датчик свежедобавленный и будет isHidden с нулевыми координатами
            val compositeLayoutData = compositeLayoutDatas?.let {
                compositeLayoutDatas[sensorEntity.id]
            }

            blocks += CompositeBlock(
                id = sensorEntity.id,
                isHidden = compositeLayoutDatas?.let { compositeLayoutData?.isHidden ?: true } ?: false,
                x = compositeLayoutDatas?.let { compositeLayoutData?.x ?: 0 } ?: col,
                y = compositeLayoutDatas?.let { compositeLayoutData?.y ?: 0 } ?: row,
                w = compositeLayoutDatas?.let { compositeLayoutData?.w ?: getBlockWidth() } ?: getBlockWidth(),
                h = compositeLayoutDatas?.let { compositeLayoutData?.h ?: getBlockHeight() } ?: getBlockHeight(),
                action = getCompositeBlockAction(
                    sensorType = sensorType,
                    sensorEntity = sensorEntity,
                ),
                chartResponse = getChartResponse(
                    sensorEntity = sensorEntity,
                ),
                schemeResponse = getSchemeResponse(
                    sensorType = sensorType,
                    sensorEntity = sensorEntity,
                ),
            )

            col += getBlockWidth()
            if (col >= blockCols) {
                row += getBlockHeight()
                col = 0
            }
        }

        return CompositeActionResponse(
            responseCode = ResponseCode.OK,
            headerData = HeaderData(
                titles = listOf(
                    TitleData(
                        action = null,
                        text = caption,
                        isBold = true,
                    )
                ),
                rows = rows,
            ),
            blocks = blocks,
            layoutSaveKey = layoutSaveKey,
        )
    }

    private fun getErrorCompositeActionResponse() = CompositeActionResponse(
        responseCode = ResponseCode.ERROR,
        headerData = HeaderData(
            titles = emptyList(),
            rows = emptyList(),
        ),
        blocks = emptyList(),
        layoutSaveKey = "",
    )

    private fun getCompositeResponseAction(action: AppAction): AppAction = if (withObjectList()) {
        action.copy(
            id = null,
            parentModule = null,
        )
    } else {
        action.copy(
            parentModule = AppModuleMMS.OBJECT,
        )
    }

    private fun getCompositeItems(
        sessionId: Long,
    ): List<CompositeListItemData>? = if (withObjectList()) {
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
                    itemStatus = (getCurrentTimeInt() - (deviceEntity.lastSessionTime ?: 0)) < SensorConfig.CRITICAL_OFF_PERIOD,
                )
            }

            result += CompositeListItemData(
                text = objectEntity.name ?: "(без наименования)",
                itemId = objectEntity.id,
                itemModule = AppModuleMMS.OBJECT,
                itemStatus = true,
                subListDatas = if (deviceList.isEmpty()) {
                    null
                } else {
                    deviceList
                },
            )
        }

        result
    } else {
        null
    }

    private fun getHeaderRows(
        objectEntity: ObjectEntity,
        deviceEntity: DeviceEntity?,
    ): List<Pair<String, String>> = if (withObjectList()) {
        val rows = mutableListOf(
            "Наименование объекта" to (objectEntity.name ?: "-"),
            "Модель объекта" to (objectEntity.model ?: "-"),
        )
        deviceEntity?.let {
            rows += "Серийный номер контроллера" to (deviceEntity.serialNo ?: "-")
            rows += "Наименование контроллера" to (deviceEntity.name ?: "-")
        }

        rows
    } else {
        listOf(
            "Наименование объекта" to (objectEntity.name ?: "-"),
            "Модель объекта" to (objectEntity.model ?: "-"),
        )
    }

    protected abstract fun addSensors(
        objectEntity: ObjectEntity,
        deviceEntity: DeviceEntity?,
        sensorEntities: MutableList<Pair<DashboardSensorTypeEnum, SensorEntity>>,
    )

    protected fun addAnalogueSensors(
        objectEntity: ObjectEntity,
        deviceEntity: DeviceEntity?,
        sensorEntities: MutableList<Pair<DashboardSensorTypeEnum, SensorEntity>>,
    ) {
        addSensorsByType(
            sensorTypeEnum = DashboardSensorTypeEnum.ANALOGUE,
            sensorList = listOf(
                SensorConfig.SENSOR_LIQUID_LEVEL,
                SensorConfig.SENSOR_WEIGHT,
                SensorConfig.SENSOR_TURN,
                SensorConfig.SENSOR_PRESSURE,
                SensorConfig.SENSOR_TEMPERATURE,
                SensorConfig.SENSOR_VOLTAGE,
                SensorConfig.SENSOR_POWER,
                SensorConfig.SENSOR_DENSITY,
                SensorConfig.SENSOR_MASS_FLOW,
                SensorConfig.SENSOR_VOLUME_FLOW,
                SensorConfig.SENSOR_ENERGO_VOLTAGE,
                SensorConfig.SENSOR_ENERGO_CURRENT,
                SensorConfig.SENSOR_ENERGO_POWER_KOEF,
                SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
                SensorConfig.SENSOR_ENERGO_POWER_REACTIVE,
                SensorConfig.SENSOR_ENERGO_POWER_FULL,
                SensorConfig.SENSOR_ENERGO_TRANSFORM_KOEF_CURRENT,
                SensorConfig.SENSOR_ENERGO_TRANSFORM_KOEF_VOLTAGE,
            ),
            objectEntity = objectEntity,
            deviceEntity = deviceEntity,
            sensorEntities = sensorEntities,
        )
    }

    protected fun addCounterSensors(
        objectEntity: ObjectEntity,
        deviceEntity: DeviceEntity?,
        sensorEntities: MutableList<Pair<DashboardSensorTypeEnum, SensorEntity>>,
    ) {
        addSensorsByType(
            sensorTypeEnum = DashboardSensorTypeEnum.COUNTER,
            sensorList = listOf(
                SensorConfig.SENSOR_MASS_ACCUMULATED,
                SensorConfig.SENSOR_VOLUME_ACCUMULATED,
                SensorConfig.SENSOR_LIQUID_USING,
                SensorConfig.SENSOR_ENERGO_COUNT_AD,
                SensorConfig.SENSOR_ENERGO_COUNT_AR,
                SensorConfig.SENSOR_ENERGO_COUNT_RD,
                SensorConfig.SENSOR_ENERGO_COUNT_RR,
            ),
            objectEntity = objectEntity,
            deviceEntity = deviceEntity,
            sensorEntities = sensorEntities,
        )
    }

    protected fun addWorkSensors(
        objectEntity: ObjectEntity,
        deviceEntity: DeviceEntity?,
        sensorEntities: MutableList<Pair<DashboardSensorTypeEnum, SensorEntity>>,
    ) {
        addSensorsByType(
            sensorTypeEnum = DashboardSensorTypeEnum.WORK,
            sensorList = listOf(
                SensorConfig.SENSOR_WORK,
            ),
            objectEntity = objectEntity,
            deviceEntity = deviceEntity,
            sensorEntities = sensorEntities,
        )
    }

    private fun addSensorsByType(
        sensorTypeEnum: DashboardSensorTypeEnum,
        sensorList: List<Int>,
        objectEntity: ObjectEntity,
        deviceEntity: DeviceEntity?,
        sensorEntities: MutableList<Pair<DashboardSensorTypeEnum, SensorEntity>>,
    ) {
        sensorList.forEach { sensorType ->
            sensorRepository
                .findByObjAndSensorTypeAndTime(objectEntity, sensorType, getCurrentTimeInt())
                .filter { sensorEntity ->
                    deviceEntity?.let {
                        deviceEntity.index == (sensorEntity.portNum ?: -1) / CoreTelematicFunction.MAX_PORT_PER_DEVICE
                    } ?: true
                }
                .forEach { sensorEntity ->
                    sensorEntities += sensorTypeEnum to sensorEntity
                }
        }
    }

    protected abstract fun getViewSizeRelation(viewWidth: Float, viewHeight: Float): Float

    protected abstract fun getBlockWidth(): Int
    protected abstract fun getBlockHeight(): Int

    protected abstract fun getCompositeBlockAction(sensorType: DashboardSensorTypeEnum, sensorEntity: SensorEntity): AppAction

    protected open fun getChartResponse(sensorEntity: SensorEntity): ChartResponse? = null
    protected open fun getSchemeResponse(sensorType: DashboardSensorTypeEnum, sensorEntity: SensorEntity): SchemeResponse? = null
}
