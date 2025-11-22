package foatto.server.service.scheme

import foatto.core.model.model.xy.XyElement
import foatto.core.model.response.xy.XyElementConfig
import foatto.core.model.response.xy.geom.XyPoint
import foatto.core.util.getPrecision
import foatto.core.util.getRandomInt
import foatto.core.util.getSplittedDouble
import foatto.server.initXyElementConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import foatto.server.service.ApplicationService
import foatto.server.service.SensorService
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class SchemeCounterIndicatorStateService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
) : AbstractSchemeIndicatorStateService(
    entityManager = entityManager,
    objectRepository = objectRepository,
) {

    companion object {
        private const val TYPE_SCHEME_CI_TITLE: String = "mms_scheme_ci_title"

        private const val TYPE_SCHEME_CI_CUR_VALUE: String = "mms_scheme_ci_cur_value"
        private const val TYPE_SCHEME_CI_DESCR: String = "mms_scheme_ci_descr"

        private const val ERROR_BACK_COLOR = 0xFF_FF_D0_D0.toInt()
        private const val BACK_COLOR = 0xFF_E0_E0_E0.toInt()
        private const val NO_DATA_BACK_COLOR = 0xFF_FF_E0_E0.toInt()

        private const val ERROR_BORDER_COLOR = 0xFF_FF_C0_C0.toInt()
        private const val BORDER_COLOR = 0xFF_D0_D0_D0.toInt()
        private const val NO_DATA_BORDER_COLOR = 0xFF_FF_D0_D0.toInt()
    }

    override fun getElementConfigs(): Map<String, XyElementConfig> = initXyElementConfig(level = 10, minScale = MIN_SCALE, maxScale = MAX_SCALE).apply {
        this[TYPE_SCHEME_CI_TITLE] = getTextConfig(TYPE_SCHEME_CI_TITLE, 1)

        this[TYPE_SCHEME_CI_CUR_VALUE] = getTextConfig(TYPE_SCHEME_CI_CUR_VALUE, 1)
        this[TYPE_SCHEME_CI_DESCR] = getTextConfig(TYPE_SCHEME_CI_DESCR, 1)
    }

    override fun getElements(userConfig: ServerUserConfig, sensorId: Int, scale: Float): List<XyElement> {
        val alResult = mutableListOf<XyElement>()

        val sensorEntity = sensorRepository.findByIdOrNull(sensorId) ?: return emptyList()

        SensorService.checkAndCreateSensorTables(entityManager, sensorEntity.id)

        var sensorTime: Int? = null
        var sensorValue: Double? = null
        ApplicationService.withConnection(entityManager) { conn ->
            val rs = conn.executeQuery(
                """
                    SELECT ontime_0, value_0
                    FROM MMS_agg_${sensorEntity.id}
                    WHERE ontime_0 = (
                        SELECT MAX(ontime_0) FROM MMS_agg_${sensorEntity.id} 
                    )
                """
            )
            if (rs.next()) {
                sensorTime = rs.getInt(1)
                sensorValue = rs.getDouble(2)
            }
            rs.close()
        }

        val (errorTime, errorMessage) = getErrorText(sensorEntity.id)
        val isErrorStatus = errorTime != null && errorTime > (sensorTime ?: 0)

        val x0 = 6 * GRID_STEP

        //--- заголовок

        addTitleElement(
            sensorEntity = sensorEntity,
            sensorTime = sensorTime,
            errorTime = errorTime,
            elementType = TYPE_SCHEME_CI_TITLE,
            x = x0,
            y = 1 * GRID_STEP,
            scale = scale,
            alResult = alResult,
        )

        val valueText = sensorValue?.let { sv ->
            val dim = sensorEntity.dim?.trim() ?: ""
            getSplittedDouble(sv, getPrecision(sv)) + if (dim.isNotEmpty()) {
                " [$dim]"
            } else {
                ""
            }
        } ?: "-"

        XyElement(TYPE_SCHEME_CI_CUR_VALUE, -getRandomInt(), sensorId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(x0, 5 * GRID_STEP))
            anchorX = XyElement.Anchor.CC
            anchorY = XyElement.Anchor.RB
            text = if (isErrorStatus) {
                " $errorMessage "
            } else {
                " $valueText "
            }
            textColor = if (isErrorStatus) {
                TEXT_COLOR_CRITICAL
            } else {
                sensorValue?.let { TEXT_COLOR_NORMAL } ?: TEXT_COLOR_CRITICAL
            }
            fillColor = if (isErrorStatus) {
                ERROR_BACK_COLOR
            } else {
                sensorValue?.let { BACK_COLOR } ?: NO_DATA_BACK_COLOR
            }
            drawColor = if (isErrorStatus) {
                ERROR_BORDER_COLOR
            } else {
                sensorValue?.let { BORDER_COLOR } ?: NO_DATA_BORDER_COLOR
            }
            lineWidth = 1
            fontSize = when {
                isErrorStatus -> 12
                scale <= 12_000 -> 60
                scale <= 24_000 -> 40
                scale <= 36_000 -> 28
                scale <= 48_000 -> 22
                scale <= 60_000 -> 18
                else -> 12
            }
            isFontBold = true
        }.let { xyElement ->
            alResult.add(xyElement)
        }

        addTimeElement(
            userConfig = userConfig,
            sensorId = sensorId,
            sensorTime = sensorTime,
            errorTime = errorTime,
            elementType = TYPE_SCHEME_CI_DESCR,
            x = x0,
            y = 7 * GRID_STEP,
            scale = scale,
            alResult = alResult,
        )

        return alResult
    }

}