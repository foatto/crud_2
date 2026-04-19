package foatto.server.service.map

import foatto.core.ActionType
import foatto.core.i18n.getLocalizedMessage
import foatto.core.model.AppAction
import foatto.core.model.model.xy.XyElement
import foatto.core.model.model.xy.XyProjection
import foatto.core.model.model.xy.XyViewCoord
import foatto.core.model.request.MapActionRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.HeaderData
import foatto.core.model.response.MapActionResponse
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.TitleData
import foatto.core.model.response.xy.XyBitmapType
import foatto.core.model.response.xy.XyElementConfig
import foatto.core.model.response.xy.XyElementType
import foatto.core.model.response.xy.geom.XyPoint
import foatto.core.model.response.xy.map.MapResponse
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getRandomInt
import foatto.core_mms.AppModuleMMS
import foatto.core_mms.i18n.LocalizedMMSMessages
import foatto.core_mms.i18n.getLocalizedMMSMessage
import foatto.server.SpringApp
import foatto.server.appModuleConfigs
import foatto.server.checkAccessPermission
import foatto.server.entity.ObjectEntity
import foatto.server.entity.SensorEntity
import foatto.server.initXyElementConfig
import foatto.server.model.ServerUserConfig
import foatto.server.model.sensor.SensorConfig
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import foatto.server.service.ApplicationService
import foatto.server.service.SensorService
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Service
class MapTraceService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
) {

    companion object {
        //--- пока минимально используемый масштаб 1:16, что соответствует старому 1:2000 при 4 пикселях на мм
        //--- ограничение исходит из-за отсутствия битмапов выше 18-го уровня ( т.е. для 1:1000 ),
        //--- а 18-й уровень битмапов сооветствует масштабу 1:16
        const val MAP_MIN_SCALE: Int = 16

        //--- пока максимально используемый масштаб 1:512*1024, что соответствует старому 1:32_768_000 при 4 пикселях на мм
        const val MAP_MAX_SCALE: Int = 512 * 1024

        const val ELEMENT_TYPE_ZONE: String = "mms_zone"

        //--- располагаем по возрастанию уровня расположения
        const val TYPE_OBJECT_TRACE: String = "mms_object_trace"
        const val TYPE_OBJECT_PARKING: String = "mms_object_parking"
        const val TYPE_OBJECT_OVER_SPEED: String = "mms_object_over_speed"
        const val TYPE_OBJECT_TRACE_INFO: String = "mms_object_trace_info"
        const val TYPE_OBJECT_INFO: String = "mms_object_info"

        /*
                private const val INDICATOR_BACK_COLOR_NO_DATA = 0xFF_D0_D0_D0.toInt()
                private const val INDICATOR_BACK_COLOR_OFF = 0xFF_FF_E0_E0.toInt()
                private const val INDICATOR_BACK_COLOR_ON = 0xFF_E0_FF_E0.toInt()

                private const val INDICATOR_BORDER_COLOR_NO_DATA = 0xFF_A0_A0_A0.toInt()
                private const val INDICATOR_BORDER_COLOR_OFF = 0xFF_FF_00_00.toInt()
                private const val INDICATOR_BORDER_COLOR_ON = 0xFF_00_B0_00.toInt()

                private const val INDICATOR_BACK_COLOR_OFF = 0xFF_D0_D0_D0.toInt()
                private const val INDICATOR_BACK_COLOR_NEUTRAL = 0xFF_F0_F0_F0.toInt()
                private const val INDICATOR_BACK_COLOR_NORMAL = 0xFF_E0_FF_E0.toInt()
                private const val INDICATOR_BACK_COLOR_CRITICAL = 0xFF_FF_E0_E0.toInt()

                private const val INDICATOR_ARC_COLOR_NEUTRAL = 0xFF_A0_A0_A0.toInt()
                private const val INDICATOR_ARC_COLOR_NORMAL = 0xFF_00_B0_00.toInt()
                private const val INDICATOR_ARC_COLOR_CRITICAL = 0xFF_FF_00_00.toInt()
                private const val INDICATOR_ARC_COLOR_BASE = 0xFF_00_00_00.toInt()

                private const val INDICATOR_MULTIPLICATOR_COLOR = 0xFF_A0_A0_A0.toInt()

                private const val TEXT_COLOR = 0xFF_00_00_00.toInt()
         */
    }

    private fun getElementConfigs(userConfig: ServerUserConfig): Map<String, XyElementConfig> =
        initXyElementConfig(level = 10, minScale = MAP_MIN_SCALE, maxScale = MAP_MAX_SCALE).apply {
            this[TYPE_OBJECT_TRACE] = XyElementConfig(
                name = TYPE_OBJECT_TRACE,
                type = XyElementType.TRACE,
                layer = 11,
                scaleMin = MAP_MIN_SCALE,
                scaleMax = MAP_MAX_SCALE,
//                descrForAction = "",
//                isRotatable = false,
//                isMoveable = false,
//                isEditablePoint = false
            )

            this[TYPE_OBJECT_PARKING] = XyElementConfig(
                name = TYPE_OBJECT_PARKING,
                type = XyElementType.TEXT,
                layer = 12,
                scaleMin = MAP_MIN_SCALE,
                scaleMax = MAP_MAX_SCALE,
//                descrForAction = "",
//                isRotatable = false,
//                isMoveable = false,
//                isEditablePoint = false
            )

            this[TYPE_OBJECT_OVER_SPEED] = XyElementConfig(
                name = TYPE_OBJECT_OVER_SPEED,
                type = XyElementType.TEXT,
                layer = 13,
                scaleMin = MAP_MIN_SCALE,
                scaleMax = MAP_MAX_SCALE,
//                descrForAction = "",
//                isRotatable = false,
//                isMoveable = false,
//                isEditablePoint = false
            )

            this[TYPE_OBJECT_TRACE_INFO] = XyElementConfig(
                name = TYPE_OBJECT_TRACE_INFO,
                type = XyElementType.TEXT,
                layer = 14,
                scaleMin = MAP_MIN_SCALE,
                scaleMax = MAP_MAX_SCALE,
//                descrForAction = "",
//                isRotatable = false,
//                isMoveable = false,
//                isEditablePoint = false
            )

            this[TYPE_OBJECT_INFO] = XyElementConfig(
                name = TYPE_OBJECT_INFO,
                type = XyElementType.MARKER,
                layer = 15,
                scaleMin = MAP_MIN_SCALE,
                scaleMax = MAP_MAX_SCALE,
//                descrForAction = "",
//                isRotatable = false,
//                isMoveable = false,
//                isEditablePoint = false
            )

            //--- прикладные топо-объекты, добавляемые пользователем вручную на карте

            this[ELEMENT_TYPE_ZONE] = XyElementConfig(
                name = ELEMENT_TYPE_ZONE,
                type = XyElementType.ZONE,
                layer = 10,
                scaleMin = MAP_MIN_SCALE,
                scaleMax = MAP_MAX_SCALE,
//                descrForAction = getLocalizedMMSMessage(LocalizedMMSMessages.GEOFENCE, userConfig.lang),
//                isRotatable = false,
//                isMoveable = true,
//                isEditablePoint = true
            )
        }

    fun map(
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

        val objectEntity = getObjectEntity(action) ?: return AppResponse(ResponseCode.LOGON_NEED)

        val caption = getLocalizedMessage(moduleConfig.captions, userConfig.lang)
        val rows = mutableListOf(
            getLocalizedMMSMessage(LocalizedMMSMessages.OBJECT_NAME, userConfig.lang) to (objectEntity.name ?: "-"),
            getLocalizedMMSMessage(LocalizedMMSMessages.MODEL, userConfig.lang) to (objectEntity.model ?: "-"),
        )

        if (action.timeRangeType != 0) {
            rows += getLocalizedMMSMessage(LocalizedMMSMessages.PERIOD, userConfig.lang) to
                    "${getLocalizedMMSMessage(LocalizedMMSMessages.FOR_THE_LAST, userConfig.lang)} " +
                    if (action.timeRangeType % 3600 == 0) {
                        "${action.timeRangeType / 3600} ${getLocalizedMMSMessage(LocalizedMMSMessages.HOUR_S, userConfig.lang)}"
                    } else if (action.timeRangeType % 60 == 0) {
                        "${action.timeRangeType / 60} ${getLocalizedMMSMessage(LocalizedMMSMessages.MINUTES, userConfig.lang)}"
                    } else {
                        "${action.timeRangeType} ${getLocalizedMMSMessage(LocalizedMMSMessages.SECONDS, userConfig.lang)}"
                    }
        }
        return AppResponse(
            responseCode = ResponseCode.MODULE_MAP,
            map = MapResponse(
                elementConfigs = getElementConfigs(userConfig),
                tabCaption = caption,
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
            )
        )
    }

    fun mapAction(mapActionRequest: MapActionRequest): MapActionResponse {
        val actionModule = mapActionRequest.action.module

        val sessionData = SpringApp.getSessionData(mapActionRequest.sessionId) ?: return MapActionResponse(ResponseCode.LOGON_NEED)
        val userConfig = sessionData.serverUserConfig ?: return MapActionResponse(ResponseCode.LOGON_NEED)
        if (!checkAccessPermission(actionModule, userConfig.roles)) {
            return MapActionResponse(ResponseCode.LOGON_NEED)
        }

        return when (mapActionRequest.action.type) {
            ActionType.GET_COORDS -> getCoords(mapActionRequest)

            ActionType.GET_ELEMENTS -> {
                MapActionResponse(
                    responseCode = ResponseCode.OK,
                    elements = getElements(mapActionRequest),
                )
            }

            else -> {
                MapActionResponse(
                    responseCode = ResponseCode.ERROR,
                )
            }
        }
    }

    private fun getCoords(mapActionRequest: MapActionRequest): MapActionResponse {
        var minPoint: XyPoint? = null
        var maxPoint: XyPoint? = null

        val (begTime, endTime) = getBegEndTime(mapActionRequest.action)

        getGeoSensorEntity(mapActionRequest.action, begTime, endTime)?.let { sensorEntity ->
            SensorService.checkAndCreateSensorTables(entityManager, sensorEntity.id)

            ApplicationService.withConnection(entityManager) { conn ->
                val rs = conn.executeQuery(
                    """
                            SELECT COUNT(ontime_0) , MIN(value_0) , MAX(value_0) , MIN(value_1) , MAX(value_1)
                            FROM MMS_agg_${sensorEntity.id}
                            WHERE ontime_0 BETWEEN $begTime AND $endTime
                        """
                )
                if (rs.next() && rs.getInt(1) > 0) {
                    val minWgsX = rs.getDouble(2)
                    val maxWgsX = rs.getDouble(3)
                    val minWgsY = rs.getDouble(4)
                    val maxWgsY = rs.getDouble(5)
                    minPoint = XyProjection.wgs_pix(minWgsX, maxWgsY)
                    maxPoint = XyProjection.wgs_pix(maxWgsX, minWgsY)
                }
                rs.close()
            }
        }

        //--- если элементы, соответствующие стартовым объектам, не нашлись, то возвращаем границы РФ
        return MapActionResponse(
            responseCode = ResponseCode.OK,
            minCoord = minPoint ?: XyProjection.wgs_pix(20.0, 70.0),
            maxCoord = maxPoint ?: XyProjection.wgs_pix(180.0, 40.0),
        )
    }

    private fun getElements(mapActionRequest: MapActionRequest): List<XyElement> {
        val appAction = mapActionRequest.action

        val (begTime, endTime) = getBegEndTime(appAction)

        val points = mutableListOf<XyPoint>()
        val drawColors = mutableListOf<Int>()
        val fillColors = mutableListOf<Int>()

        getGeoSensorEntity(mapActionRequest.action, begTime, endTime)?.let { sensorEntity ->
            SensorService.checkAndCreateSensorTables(entityManager, sensorEntity.id)

            ApplicationService.withConnection(entityManager) { conn ->
                val rs = conn.executeQuery(
                    """
                            SELECT value_0 , value_1
                            FROM MMS_agg_${sensorEntity.id}
                            WHERE ontime_0 BETWEEN $begTime AND $endTime
                            ORDER BY ontime_0 
                        """
                )
                while (rs.next()) {
                    val wgsX = rs.getDouble(1)
                    val wgsY = rs.getDouble(2)

                    points += XyProjection.wgs_pix(wgsX, wgsY)
                    drawColors += 0xFF_00_00_FF.toInt()
                    fillColors += 0xFF_00_FF_FF.toInt()
                }
                rs.close()
            }
        }

        val elements = mutableListOf(
            XyElement(TYPE_OBJECT_TRACE, -getRandomInt(), appAction.parentId ?: 0).apply {
                this.isReadOnly = true
                this.points = points
                this.fillColor = null
                //drawColor = 0xFF_00_00_FF.toInt()
                this.lineWidth = 2
                this.drawColors = drawColors
                this.fillColors = fillColors
            }
        )

        outBitmapElements(mapActionRequest.bitmapTypeName!!, mapActionRequest.viewCoord!!, elements)

        return elements
    }

    private fun getObjectEntity(action: AppAction): ObjectEntity? =
        when (action.parentModule) {
            AppModuleMMS.ALL_OBJECT, AppModuleMMS.MOBILE_OBJECT -> {
                action.parentId?.let { parentId ->
                    objectRepository.findByIdOrNull(parentId)
                }
            }

            else -> null
        }

    private fun getGeoSensorEntity(action: AppAction, begTime: Int, endTime: Int): SensorEntity? =
        getObjectEntity(action)?.let { objectEntity ->
            sensorRepository.findByObjAndSensorTypeAndPeriod(objectEntity, SensorConfig.SENSOR_GEO, begTime, endTime).firstOrNull()
        }

    private fun getBegEndTime(action: AppAction): Pair<Int, Int> {
        val endTime = if (action.timeRangeType == 0) {
            action.endTime ?: getCurrentTimeInt()
        } else {
            getCurrentTimeInt()
        }

        val begTime = if (action.timeRangeType == 0) {
            action.begTime ?: (getCurrentTimeInt() - 86_400)
        } else {
            getCurrentTimeInt() - action.timeRangeType
        }

        return begTime to endTime
    }

    private fun outBitmapElements(bmTypeName: String, viewCoord: XyViewCoord, elements: MutableList<XyElement>) {
        val zoomLevel = XyBitmapType.hmTypeScaleZ[bmTypeName]?.get(viewCoord.scale) ?: return
        if (zoomLevel == -1) {
            return
        }

        val arrPrefixOSM = charArrayOf('a', 'b', 'c')

        //--- мировой размер битмапа для текущего масштаба в метрах
        val bmRealSize = XyBitmapType.BLOCK_SIZE * viewCoord.scale
        //--- выравнивание запрашиваемой области по размеру битмапа в большую сторону
        val x1 = (viewCoord.x1 / bmRealSize - 1) * bmRealSize
        val y1 = (viewCoord.y1 / bmRealSize - 1) * bmRealSize
        val x2 = (viewCoord.x2 / bmRealSize + 1) * bmRealSize
        val y2 = (viewCoord.y2 / bmRealSize + 1) * bmRealSize

        var y = y1
        while (y <= y2) {
            var x = x1
            while (x <= x2) {
                val blockX = x / XyBitmapType.BLOCK_SIZE / viewCoord.scale
                val blockY = y / XyBitmapType.BLOCK_SIZE / viewCoord.scale
                //--- при высоких масштабах можем залезть за край земли
                if (blockX < 0 || blockY < 0) {
                    x += bmRealSize
                    continue
                }

                //val serverURL = "${XyBitmapType.BITMAP_DIR}$bmTypeName/$zoomLevel/$blockY/$blockX.${XyBitmapType.BITMAP_EXT}"

                //--- специфично для MAPNIK
                val serverURL = "https://${arrPrefixOSM[getRandomInt() % 3]}.tile.openstreetmap.org/$zoomLevel/$blockX/$blockY.png"

                //--- если выходной поток задан, пишем в него
                //--- (null может быть при запуске загрузки других типов карт)
                elements += XyElement(XyElementType.BITMAP.name, -getRandomInt(), 0).apply {
                    isReadOnly = true
                    points = listOf(XyPoint(x, y))
                    imageWidth = bmRealSize
                    imageHeight = bmRealSize
                    imageName = serverURL
                }

                x += bmRealSize
            }
            y += bmRealSize
        }
    }

}