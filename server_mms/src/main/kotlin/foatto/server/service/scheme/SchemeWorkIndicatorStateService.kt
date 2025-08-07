package foatto.server.service.scheme

import foatto.core.model.model.xy.XyElement
import foatto.core.model.response.xy.XyElementConfig
import foatto.core.model.response.xy.geom.XyPoint
import foatto.core.util.getRandomInt
import foatto.server.initXyElementConfig
import foatto.server.model.ServerUserConfig
import foatto.server.model.sensor.SensorConfigWork
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import foatto.server.service.ApplicationService
import foatto.server.service.SensorService
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class SchemeWorkIndicatorStateService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
) : AbstractSchemeIndicatorStateService(
    entityManager = entityManager,
    objectRepository = objectRepository,
) {

    companion object {
        private const val TYPE_SCHEME_WI_TITLE: String = "type_scheme_wi_title_text"
        private const val TYPE_SCHEME_WI_VALUE: String = "type_scheme_wi_value"
        private const val TYPE_SCHEME_WI_ICON: String = "type_scheme_wi_icon"
        private const val TYPE_SCHEME_WI_TIME: String = "type_scheme_wi_time"

        private val INDICATOR_BACK_COLOR_ERROR: Int? = 0xFF_FF_D0_D0.toInt()
        private val INDICATOR_BACK_COLOR_NO_DATA: Int? = null
        private val INDICATOR_BACK_COLOR_OFF: Int? = 0xFF_D0_D0_D0.toInt()
        private val INDICATOR_BACK_COLOR_IDLE: Int? = 0xFF_E0_E0_FF.toInt()
        private val INDICATOR_BACK_COLOR_WORK: Int? = 0xFF_E0_FF_E0.toInt()
        private val INDICATOR_BACK_COLOR_OVER: Int? = 0xFF_FF_E0_E0.toInt()

        private val INDICATOR_BORDER_COLOR_ERROR: Int? = 0xFF_FF_00_00.toInt()
        private val INDICATOR_BORDER_COLOR_NO_DATA: Int? = 0xFF_00_00_00.toInt()
        private val INDICATOR_BORDER_COLOR_OFF: Int? = 0xFF_A0_A0_A0.toInt()
        private val INDICATOR_BORDER_COLOR_IDLE: Int? = 0xFF_00_00_B0.toInt()
        private val INDICATOR_BORDER_COLOR_WORK: Int? = 0xFF_00_B0_00.toInt()
        private val INDICATOR_BORDER_COLOR_OVER: Int? = 0xFF_FF_00_00.toInt()
    }

    override fun getElementConfigs(): Map<String, XyElementConfig> = initXyElementConfig(level = 10, minScale = MIN_SCALE, maxScale = MAX_SCALE).apply {
        this[TYPE_SCHEME_WI_TITLE] = getTextConfig(TYPE_SCHEME_WI_TITLE, 1)
        this[TYPE_SCHEME_WI_VALUE] = getArcConfig(TYPE_SCHEME_WI_VALUE, 1)
        this[TYPE_SCHEME_WI_ICON] = getIconConfig(TYPE_SCHEME_WI_ICON, 2)
        this[TYPE_SCHEME_WI_TIME] = getTextConfig(TYPE_SCHEME_WI_TIME, 1)
    }

    override fun getElements(userConfig: ServerUserConfig, sensorId: Int, scale: Float): List<XyElement> {
        val alResult = mutableListOf<XyElement>()

        val sensorEntity = sensorRepository.findByIdOrNull(sensorId) ?: return emptyList()

        SensorService.checkAndCreateSensorTables(entityManager, sensorEntity.id)

        var sensorTime: Int? = null
        var sensorValue: Int? = null
        ApplicationService.withConnection(entityManager) { conn ->
            val rs = conn.executeQuery(
                """
                    SELECT ontime_1, type_0
                    FROM MMS_agg_${sensorEntity.id}
                    WHERE ontime_1 = (
                        SELECT MAX(ontime_1) FROM MMS_agg_${sensorEntity.id} 
                    )
                """
            )
            if (rs.next()) {
                sensorTime = rs.getInt(1)
                sensorValue = rs.getInt(2)
            }
            rs.close()
        }

        val (errorTime, errorMessage) = getErrorText(sensorEntity.id)
        val isErrorStatus = errorTime != null && errorTime > (sensorTime ?: 0)

        val x0 = 6 * GRID_STEP
        val y0 = 4 * GRID_STEP

        //--- заголовок

        addTitleElement(
            sensorEntity = sensorEntity,
            sensorTime = sensorTime,
            errorTime = errorTime,
            elementType = TYPE_SCHEME_WI_TITLE,
            x = x0,
            y = 1 * GRID_STEP,
            scale = scale,
            alResult = alResult,
        )

        XyElement(TYPE_SCHEME_WI_VALUE, -getRandomInt(), sensorId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(x0, y0))
            radius = 2 * GRID_STEP
            // отсчёт углов в compose - в обратную сторону (по часовой стрелке)
            startAngle = 180
            sweepAngle = 360
            fillColor = if (isErrorStatus) {
                INDICATOR_BACK_COLOR_ERROR
            } else {
                when (sensorValue) {
                    SensorConfigWork.STATE_OFF -> INDICATOR_BACK_COLOR_OFF
                    SensorConfigWork.STATE_IDLE -> INDICATOR_BACK_COLOR_IDLE
                    SensorConfigWork.STATE_WORK -> INDICATOR_BACK_COLOR_WORK
                    SensorConfigWork.STATE_OVER -> INDICATOR_BACK_COLOR_OVER
                    else -> INDICATOR_BACK_COLOR_NO_DATA
                }
            }
            drawColor = if (isErrorStatus) {
                INDICATOR_BORDER_COLOR_ERROR
            } else {
                when (sensorValue) {
                    SensorConfigWork.STATE_OFF -> INDICATOR_BORDER_COLOR_OFF
                    SensorConfigWork.STATE_IDLE -> INDICATOR_BORDER_COLOR_IDLE
                    SensorConfigWork.STATE_WORK -> INDICATOR_BORDER_COLOR_WORK
                    SensorConfigWork.STATE_OVER -> INDICATOR_BORDER_COLOR_OVER
                    else -> INDICATOR_BORDER_COLOR_NO_DATA
                }
            }
            lineWidth = if (isErrorStatus) {
                4
            } else if (sensorValue == null) {
                1
            } else {
                4
            }
        }.let { xyElement ->
            alResult.add(xyElement)
        }

        val iconSize = when {
            scale <= 12_000 -> 96
            scale <= 24_000 -> 64
            scale <= 36_000 -> 64
            scale <= 48_000 -> 32
            scale <= 60_000 -> 32
            else -> 32
        }
        XyElement(TYPE_SCHEME_WI_ICON, -getRandomInt(), sensorId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(x0, y0))
            anchorX = XyElement.Anchor.CC
            anchorY = XyElement.Anchor.CC
            imageName = "/images/icons8-engine-$iconSize.png"
            imageWidth = iconSize
            imageHeight = iconSize
        }.let { xyElement ->
            alResult.add(xyElement)
        }

        addTimeElement(
            userConfig = userConfig,
            sensorId = sensorId,
            sensorTime = sensorTime,
            errorTime = errorTime,
            elementType = TYPE_SCHEME_WI_TIME,
            x = x0,
            y = y0 + 3 * GRID_STEP,
            scale = scale,
            alResult = alResult,
        )

        return alResult
    }

}