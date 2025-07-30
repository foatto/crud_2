package foatto.server.service.composite

import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.request.CompositeActionRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.CompositeActionResponse
import foatto.core.model.response.HeaderData
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.TitleData
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
import foatto.server.model.sensor.SensorConfig
import foatto.server.repository.DeviceRepository
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import foatto.server.service.scheme.AbstractSchemeIndicatorStateService
import foatto.server.service.scheme.SchemeAnalogueIndicatorStateService
import foatto.server.service.scheme.SchemeCounterIndicatorStateService
import foatto.server.service.scheme.SchemeWorkIndicatorStateService
import kotlinx.serialization.json.Json
import org.springframework.data.repository.findByIdOrNull
import kotlin.math.ceil
import kotlin.math.round
import kotlin.math.sqrt

abstract class AbstractCompositeDashboardService(
    private val objectRepository: ObjectRepository,
    private val deviceRepository: DeviceRepository,
    private val sensorRepository: SensorRepository,
    private val schemeAnalogueIndicatorStateService: SchemeAnalogueIndicatorStateService,
    private val schemeCounterIndicatorStateService: SchemeCounterIndicatorStateService,
    private val schemeWorkIndicatorStateService: SchemeWorkIndicatorStateService,
) {

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
                tabCaption = moduleConfig.caption,
                action = getCompositeResponseAction(action),
                items = getCompositeItems(sessionId, action)
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

        val caption = moduleConfig.caption
        val rows = getHeaderRows(objectEntity, deviceEntity)

        val sensorEntities = mutableListOf<Pair<DashboardSensorTypeEnum, SensorEntity>>()
        //--- пока просто подряд, по порядку перечисления в SensorConfig
        //--- потом по просьбам заказчиком перетасуем
        listOf(
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
        ).forEach { sensorType ->
            sensorRepository
                .findByObjAndSensorTypeAndTime(objectEntity, sensorType, getCurrentTimeInt())
                .filter { sensorEntity ->
                    deviceEntity?.let {
                        deviceEntity.index == (sensorEntity.portNum ?: -1) / CoreTelematicFunction.MAX_PORT_PER_DEVICE
                    } ?: true
                }
                .forEach { sensorEntity ->
                    sensorEntities += DashboardSensorTypeEnum.ANALOGUE to sensorEntity
                }
        }
        listOf(
            SensorConfig.SENSOR_MASS_ACCUMULATED,
            SensorConfig.SENSOR_VOLUME_ACCUMULATED,
            SensorConfig.SENSOR_LIQUID_USING,
            SensorConfig.SENSOR_ENERGO_COUNT_AD,
            SensorConfig.SENSOR_ENERGO_COUNT_AR,
            SensorConfig.SENSOR_ENERGO_COUNT_RD,
            SensorConfig.SENSOR_ENERGO_COUNT_RR,
        ).forEach { sensorType ->
            sensorRepository
                .findByObjAndSensorTypeAndTime(objectEntity, sensorType, getCurrentTimeInt())
                .filter { sensorEntity ->
                    deviceEntity?.let {
                        deviceEntity.index == (sensorEntity.portNum ?: -1) / CoreTelematicFunction.MAX_PORT_PER_DEVICE
                    } ?: true
                }
                .forEach { sensorEntity ->
                    sensorEntities += DashboardSensorTypeEnum.COUNTER to sensorEntity
                }
        }
        listOf(
            SensorConfig.SENSOR_WORK,
        ).forEach { sensorType ->
            sensorRepository
                .findByObjAndSensorTypeAndTime(objectEntity, sensorType, getCurrentTimeInt())
                .filter { sensorEntity ->
                    deviceEntity?.let {
                        deviceEntity.index == (sensorEntity.portNum ?: -1) / CoreTelematicFunction.MAX_PORT_PER_DEVICE
                    } ?: true
                }
                .forEach { sensorEntity ->
                    sensorEntities += DashboardSensorTypeEnum.WORK to sensorEntity
                }
        }

        val viewSizeRelation = viewWidth / viewHeight * AbstractSchemeIndicatorStateService.SCHEME_HEIGHT / AbstractSchemeIndicatorStateService.SCHEME_WIDTH
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
                w = compositeLayoutDatas?.let { compositeLayoutData?.w ?: 1 } ?: 1,
                h = compositeLayoutDatas?.let { compositeLayoutData?.h ?: 1 } ?: 1,
                action = AppAction(
                    type = ActionType.MODULE_SCHEME,
                    module = when (sensorType) {
                        DashboardSensorTypeEnum.ANALOGUE -> AppModuleMMS.SCHEME_ANALOGUE_INDICATOR_STATE
                        DashboardSensorTypeEnum.COUNTER -> AppModuleMMS.SCHEME_COUNTER_INDICATOR_STATE
                        DashboardSensorTypeEnum.WORK -> AppModuleMMS.SCHEME_WORK_INDICATOR_STATE
                    },
                    id = sensorEntity.id,
                ),
                schemeResponse = SchemeResponse(
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
                ),
            )

            col++
            if (col >= blockCols) {
                row++
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

    protected abstract fun getCompositeResponseAction(action: AppAction): AppAction

    protected abstract fun getCompositeItems(
        sessionId: Long,
        action: AppAction,
    ): List<CompositeListItemData>?

    protected abstract fun getHeaderRows(
        objectEntity: ObjectEntity,
        deviceEntity: DeviceEntity?,
    ): List<Pair<String, String>>

}
