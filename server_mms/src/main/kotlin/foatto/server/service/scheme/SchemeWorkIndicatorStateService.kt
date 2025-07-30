package foatto.server.service.scheme

import foatto.core.model.model.xy.XyElement
import foatto.core.model.response.xy.XyElementConfig
import foatto.core.model.response.xy.geom.XyPoint
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getDateTimeDMYHMSString
import foatto.core.util.getRandomInt
import foatto.server.initXyElementConfig
import foatto.server.model.ServerUserConfig
import foatto.server.model.sensor.SensorConfig
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
    objectRepository = objectRepository,
) {

    companion object {
        private const val TYPE_SCHEME_WI_TITLE_TEXT: String = "type_scheme_wi_title_text"
        private const val TYPE_SCHEME_WI_VALUE: String = "type_scheme_wi_value"
        private const val TYPE_SCHEME_WI_ICON: String = "type_scheme_wi_icon"
        private const val TYPE_SCHEME_WI_DESCR_TEXT: String = "type_scheme_wi_descr_text"

        private val INDICATOR_BACK_COLOR_NO_DATA: Int? = null
        private val INDICATOR_BACK_COLOR_OFF: Int? = 0xFF_D0_D0_D0.toInt()
        private val INDICATOR_BACK_COLOR_IDLE: Int? = 0xFF_E0_E0_FF.toInt()
        private val INDICATOR_BACK_COLOR_WORK: Int? = 0xFF_E0_FF_E0.toInt()
        private val INDICATOR_BACK_COLOR_OVER: Int? = 0xFF_FF_E0_E0.toInt()

        private val INDICATOR_BORDER_COLOR_NO_DATA: Int? = 0xFF_00_00_00.toInt()
        private val INDICATOR_BORDER_COLOR_OFF: Int? = 0xFF_A0_A0_A0.toInt()
        private val INDICATOR_BORDER_COLOR_IDLE: Int? = 0xFF_00_00_B0.toInt()
        private val INDICATOR_BORDER_COLOR_WORK: Int? = 0xFF_00_B0_00.toInt()
        private val INDICATOR_BORDER_COLOR_OVER: Int? = 0xFF_FF_00_00.toInt()

        private const val TEXT_COLOR_NORMAL = 0xFF_00_00_00.toInt()
        private const val TEXT_COLOR_CRITICAL = 0xFF_FF_00_00.toInt()
    }

    override fun getElementConfigs(): Map<String, XyElementConfig> = initXyElementConfig(level = 10, minScale = MIN_SCALE, maxScale = MAX_SCALE).apply {
        this[TYPE_SCHEME_WI_TITLE_TEXT] = getTextConfig(TYPE_SCHEME_WI_TITLE_TEXT, 1)
        this[TYPE_SCHEME_WI_VALUE] = getArcConfig(TYPE_SCHEME_WI_VALUE, 1)
        this[TYPE_SCHEME_WI_ICON] = getIconConfig(TYPE_SCHEME_WI_ICON, 2)
        this[TYPE_SCHEME_WI_DESCR_TEXT] = getTextConfig(TYPE_SCHEME_WI_DESCR_TEXT, 1)
    }

    override fun getElements(userConfig: ServerUserConfig, sensorId: Int, scale: Float): List<XyElement> {
        val alResult = mutableListOf<XyElement>()

        val sensorEntity = sensorRepository.findByIdOrNull(sensorId) ?: return emptyList()

        var sensorTime: Int? = null
        var sensorValue: Int? = null

        SensorService.checkAndCreateSensorTables(entityManager, sensorEntity.id)

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

        val x0 = 6 * GRID_STEP
        val y0 = 4 * GRID_STEP

        //--- заголовок

        XyElement(TYPE_SCHEME_WI_TITLE_TEXT, -getRandomInt(), sensorId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(x0, 1 * GRID_STEP))
            anchorX = XyElement.Anchor.CC
            anchorY = XyElement.Anchor.RB
            text = sensorEntity.descr ?: "-"
            textColor = TEXT_COLOR_NORMAL
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

        XyElement(TYPE_SCHEME_WI_VALUE, -getRandomInt(), sensorId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(x0, y0))
            radius = 2 * GRID_STEP
            // отсчёт углов в compose - в обратную сторону (по часовой стрелке)
            startAngle = 180
            sweepAngle = 360
            fillColor = when (sensorValue) {
                SensorConfigWork.STATE_OFF -> INDICATOR_BACK_COLOR_OFF
                SensorConfigWork.STATE_IDLE -> INDICATOR_BACK_COLOR_IDLE
                SensorConfigWork.STATE_WORK -> INDICATOR_BACK_COLOR_WORK
                SensorConfigWork.STATE_OVER -> INDICATOR_BACK_COLOR_OVER
                else -> INDICATOR_BACK_COLOR_NO_DATA
            }
            drawColor = when (sensorValue) {
                SensorConfigWork.STATE_OFF -> INDICATOR_BORDER_COLOR_OFF
                SensorConfigWork.STATE_IDLE -> INDICATOR_BORDER_COLOR_IDLE
                SensorConfigWork.STATE_WORK -> INDICATOR_BORDER_COLOR_WORK
                SensorConfigWork.STATE_OVER -> INDICATOR_BORDER_COLOR_OVER
                else -> INDICATOR_BORDER_COLOR_NO_DATA
            }
            lineWidth = if (sensorValue == null) {
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

        sensorTime?.let { lastDataTime ->
            XyElement(TYPE_SCHEME_WI_DESCR_TEXT, -getRandomInt(), sensorId).apply {
                isReadOnly = true
                alPoint = listOf(XyPoint(x0, y0 + 3 * GRID_STEP))
                anchorX = XyElement.Anchor.CC
                anchorY = XyElement.Anchor.LT
                text = getDateTimeDMYHMSString(userConfig.timeOffset, lastDataTime)
                textColor = if (getCurrentTimeInt() - lastDataTime > SensorConfig.CRITICAL_OFF_PERIOD) {
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
                isFontBold = getCurrentTimeInt() - lastDataTime > SensorConfig.CRITICAL_OFF_PERIOD
            }.let { xyElement ->
                alResult.add(xyElement)
            }
        }

        return alResult
    }

}