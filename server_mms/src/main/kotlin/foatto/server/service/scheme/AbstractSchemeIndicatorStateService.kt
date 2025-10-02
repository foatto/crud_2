package foatto.server.service.scheme

import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.model.xy.XyElement
import foatto.core.model.request.SchemeActionRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.HeaderData
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.SchemeActionResponse
import foatto.core.model.response.TitleData
import foatto.core.model.response.xy.XyElementClientType
import foatto.core.model.response.xy.XyElementConfig
import foatto.core.model.response.xy.geom.XyPoint
import foatto.core.model.response.xy.scheme.SchemeResponse
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getDateTimeDMYHMSString
import foatto.core.util.getRandomInt
import foatto.server.SpringApp
import foatto.server.appModuleConfigs
import foatto.server.checkAccessPermission
import foatto.server.entity.SensorEntity
import foatto.server.model.ServerUserConfig
import foatto.server.model.sensor.SensorConfig
import foatto.server.repository.ObjectRepository
import foatto.server.service.ApplicationService
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import kotlin.math.max

abstract class AbstractSchemeIndicatorStateService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
) {

    companion object {

        const val MIN_SCALE: Int = 1
        const val MAX_SCALE: Int = 1024 * 1024 * 1024

        //--- it was empirically found that if the step size is too small, side effects from integer division begin
        //--- and the edges of the outline can flush against the edges of the window / screen.
        //--- A step of 1 million points allows you to fit up to 2000 circuit elements (2 billion MAX_INTEGER / 1 million = 2000) with sufficient accuracy.
        const val GRID_STEP: Int = 1024 * 1024

        const val SCHEME_WIDTH: Int = 12
        const val SCHEME_HEIGHT: Int = 8


        const val TEXT_COLOR_NORMAL = 0xFF_00_00_00.toInt()
        const val TEXT_COLOR_CRITICAL = 0xFF_FF_00_00.toInt()
    }

    fun scheme(
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
        val objectEntity = action.id?.let { id ->
            objectRepository.findByIdOrNull(id) ?: return AppResponse(ResponseCode.LOGON_NEED)
        } ?: return AppResponse(ResponseCode.LOGON_NEED)

        val caption = moduleConfig.caption
        val rows = listOf(
            "Наименование объекта" to (objectEntity.name ?: "-"),
            "Модель" to (objectEntity.model ?: "-"),
        )

        return AppResponse(
            responseCode = ResponseCode.MODULE_SCHEME,
            scheme = SchemeResponse(
                elementConfigs = getElementConfigs(),
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

    abstract fun getElementConfigs(): Map<String, XyElementConfig>

    fun schemeAction(schemeActionRequest: SchemeActionRequest): SchemeActionResponse {
        val actionModule = schemeActionRequest.action.module
        val (viewWidth, viewHeight) = schemeActionRequest.viewSize

        val sessionData = SpringApp.getSessionData(schemeActionRequest.sessionId) ?: return SchemeActionResponse(ResponseCode.LOGON_NEED)
        val userConfig = sessionData.serverUserConfig ?: return SchemeActionResponse(ResponseCode.LOGON_NEED)
        if (!checkAccessPermission(actionModule, userConfig.roles)) {
            return SchemeActionResponse(ResponseCode.LOGON_NEED)
        }

        return when (schemeActionRequest.action.type) {
            ActionType.GET_COORDS -> {
                SchemeActionResponse(
                    responseCode = ResponseCode.OK,
                    minCoord = XyPoint(0, 0),
                    maxCoord = XyPoint(SCHEME_WIDTH * GRID_STEP, SCHEME_HEIGHT * GRID_STEP),
                )
            }

            ActionType.GET_ELEMENTS -> {
                SchemeActionResponse(
                    responseCode = ResponseCode.OK,
                    elements = getElements(
                        userConfig = userConfig,
                        sensorId = schemeActionRequest.action.id!!,
                        scale = max(SCHEME_WIDTH * GRID_STEP / viewWidth, SCHEME_HEIGHT * GRID_STEP / viewHeight),
                    ),
                )
            }

            else -> {
                SchemeActionResponse(
                    responseCode = ResponseCode.ERROR,
                )
            }
        }
    }

    protected abstract fun getElements(userConfig: ServerUserConfig, sensorId: Int, scale: Float): List<XyElement>

    protected fun getErrorText(sensorId: Int): Pair<Int?, String?> {
        var errorTime: Int? = null
        var errorValue: String? = null
        ApplicationService.withConnection(entityManager) { conn ->
            val rs = conn.executeQuery(
                """
                    SELECT ontime_1, message_0
                    FROM MMS_text_${sensorId}
                    WHERE ontime_1 = (
                        SELECT MAX(ontime_1) FROM MMS_text_${sensorId} 
                    )
                """
            )
            if (rs.next()) {
                errorTime = rs.getInt(1)
                errorValue = rs.getString(2)
            }
            rs.close()
        }

        return errorTime to errorValue
    }

    protected fun addTitleElement(
        sensorEntity: SensorEntity,
        sensorTime: Int?,
        errorTime: Int?,
        elementType: String,
        x: Int,
        y: Int,
        scale: Float,
        alResult: MutableList<XyElement>,
    ) {
        val isErrorStatus = errorTime != null && errorTime > (sensorTime ?: 0)
        XyElement(elementType, -getRandomInt(), sensorEntity.id).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(x, y))
            anchorX = XyElement.Anchor.CC
            anchorY = XyElement.Anchor.RB
            text = sensorEntity.descr ?: "-"
            textColor = if (isErrorStatus) {
                TEXT_COLOR_CRITICAL
            } else {
                TEXT_COLOR_NORMAL
            }
            fillColor = null
            drawColor = null
            lineWidth = null
            fontSize = when {
                scale <= 12_000 -> 18
                scale <= 24_000 -> 14
                scale <= 36_000 -> 12
                scale <= 48_000 -> 11
                scale <= 60_000 -> 10
                else -> 9
            }
            isFontBold = true
        }.let { xyElement ->
            alResult.add(xyElement)
        }
    }

    protected fun addTimeElement(
        userConfig: ServerUserConfig,
        sensorId: Int,
        sensorTime: Int?,
        errorTime: Int?,
        elementType: String,
        x: Int,
        y: Int,
        scale: Float,
        alResult: MutableList<XyElement>,
    ) {
        val isErrorStatus = errorTime != null && errorTime > (sensorTime ?: 0)
        val lastDataTime = if (isErrorStatus) {
            errorTime
        } else {
            sensorTime
        }
        lastDataTime?.let {
            XyElement(elementType, -getRandomInt(), sensorId).apply {
                isReadOnly = true
                alPoint = listOf(XyPoint(x, y))
                anchorX = XyElement.Anchor.CC
                anchorY = XyElement.Anchor.LT
                text = getDateTimeDMYHMSString(userConfig.timeOffset, lastDataTime)
                textColor = if (isErrorStatus || getCurrentTimeInt() - lastDataTime > SensorConfig.CRITICAL_OFF_PERIOD) {
                    TEXT_COLOR_CRITICAL
                } else {
                    TEXT_COLOR_NORMAL
                }
                fillColor = null
                drawColor = null
                lineWidth = null
                fontSize = when {
                    scale <= 12_000 -> 18
                    scale <= 24_000 -> 14
                    scale <= 36_000 -> 12
                    scale <= 48_000 -> 11
                    scale <= 60_000 -> 10
                    else -> 9
                }
                isFontBold = isErrorStatus || getCurrentTimeInt() - lastDataTime > SensorConfig.CRITICAL_OFF_PERIOD
            }.let { xyElement ->
                alResult.add(xyElement)
            }
        }
    }

    protected fun getArcConfig(name: String, layer: Int) = XyElementConfig(
        name = name,
        clientType = XyElementClientType.ARC,
        layer = layer,
        scaleMin = MIN_SCALE,
        scaleMax = MAX_SCALE,
        descrForAction = "",
        isRotatable = false,
        isMoveable = false,
        isEditablePoint = false
    )

    protected fun getIconConfig(name: String, layer: Int) = XyElementConfig(
        name = name,
        clientType = XyElementClientType.ICON,
        layer = layer,
        scaleMin = MIN_SCALE,
        scaleMax = MAX_SCALE,
        descrForAction = "",
        isRotatable = false,
        isMoveable = false,
        isEditablePoint = false
    )

    protected fun getLineConfig(name: String, layer: Int) = XyElementConfig(
        name = name,
        clientType = XyElementClientType.POLY,
        layer = layer,
        scaleMin = MIN_SCALE,
        scaleMax = MAX_SCALE,
        descrForAction = "",
        isRotatable = false,
        isMoveable = false,
        isEditablePoint = false
    )

    protected fun getTextConfig(name: String, layer: Int) = XyElementConfig(
        name = name,
        clientType = XyElementClientType.TEXT,
        layer = layer,
        scaleMin = MIN_SCALE,
        scaleMax = MAX_SCALE,
        descrForAction = "",
        isRotatable = false,
        isMoveable = false,
        isEditablePoint = false
    )
}