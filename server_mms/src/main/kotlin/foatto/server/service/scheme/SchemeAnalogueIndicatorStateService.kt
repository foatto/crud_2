package foatto.server.service.scheme

import foatto.core.model.model.xy.XyElement
import foatto.core.model.response.xy.XyElementConfig
import foatto.core.model.response.xy.geom.XyPoint
import foatto.core.util.getRandomInt
import foatto.core.util.getSplittedDouble
import foatto.server.calc.DataValueStateEnum
import foatto.server.calc.getPrecision
import foatto.server.initXyElementConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import foatto.server.service.ApplicationService
import foatto.server.service.SensorService
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Service
class SchemeAnalogueIndicatorStateService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
) : AbstractSchemeIndicatorStateService(
    entityManager = entityManager,
    objectRepository = objectRepository,
) {

    companion object {
        private const val TYPE_SCHEME_AI_TITLE: String = "mms_scheme_ai_title"

        private const val TYPE_SCHEME_AI_ARC_LIMIT: String = "mms_scheme_ai_arc_limit"
        private const val TYPE_SCHEME_AI_ARC_BASE: String = "mms_scheme_ai_arc_base"
        private const val TYPE_SCHEME_AI_ARC_NOTCH: String = "mms_scheme_ai_arc_notch"
        private const val TYPE_SCHEME_AI_ARC_VALUE: String = "mms_scheme_ai_arc_value"

        private const val TYPE_SCHEME_AI_MULTIPLICATOR: String = "mms_scheme_ai_multiplicator"

        private const val TYPE_SCHEME_AI_ARROW_BASE: String = "mms_scheme_ai_arrow_base"
        private const val TYPE_SCHEME_AI_ARROW: String = "mms_scheme_ai_arrow"

        private const val TYPE_SCHEME_AI_CUR_VALUE: String = "mms_scheme_ai_cur_value"
        private const val TYPE_SCHEME_AI_TIME: String = "mms_scheme_ai_descr"

        private const val INDICATOR_BACK_COLOR_ERROR = 0xFF_FF_D0_D0.toInt()
        private const val INDICATOR_BACK_COLOR_OFF = 0xFF_FF_E0_E0.toInt()
        private const val INDICATOR_BACK_COLOR_NEUTRAL = 0xFF_F0_F0_F0.toInt()
        private const val INDICATOR_BACK_COLOR_NORMAL = 0xFF_E0_FF_E0.toInt()
        private const val INDICATOR_BACK_COLOR_CRITICAL = 0xFF_FF_E0_E0.toInt()

        private const val INDICATOR_ARC_COLOR_NEUTRAL = 0xFF_A0_A0_A0.toInt()
        private const val INDICATOR_ARC_COLOR_NORMAL = 0xFF_00_B0_00.toInt()
        private const val INDICATOR_ARC_COLOR_CRITICAL = 0xFF_FF_00_00.toInt()
        private const val INDICATOR_ARC_COLOR_BASE = 0xFF_00_00_00.toInt()

        private const val INDICATOR_MULTIPLICATOR_COLOR = 0xFF_A0_A0_A0.toInt()
    }

    override fun getElementConfigs(): Map<String, XyElementConfig> = initXyElementConfig(level = 10, minScale = MIN_SCALE, maxScale = MAX_SCALE).apply {
        this[TYPE_SCHEME_AI_TITLE] = getTextConfig(TYPE_SCHEME_AI_TITLE, 1)

        this[TYPE_SCHEME_AI_ARC_LIMIT] = getArcConfig(TYPE_SCHEME_AI_ARC_LIMIT, 1)
        this[TYPE_SCHEME_AI_ARC_BASE] = getArcConfig(TYPE_SCHEME_AI_ARC_BASE, 2)
        this[TYPE_SCHEME_AI_ARC_NOTCH] = getLineConfig(TYPE_SCHEME_AI_ARC_NOTCH, 2)
        this[TYPE_SCHEME_AI_ARC_VALUE] = getTextConfig(TYPE_SCHEME_AI_ARC_VALUE, 2)

        this[TYPE_SCHEME_AI_MULTIPLICATOR] = getTextConfig(TYPE_SCHEME_AI_MULTIPLICATOR, 3)

        this[TYPE_SCHEME_AI_ARROW_BASE] = getArcConfig(TYPE_SCHEME_AI_ARROW_BASE, 3)
        this[TYPE_SCHEME_AI_ARROW] = getLineConfig(TYPE_SCHEME_AI_ARROW, 3)

        this[TYPE_SCHEME_AI_CUR_VALUE] = getTextConfig(TYPE_SCHEME_AI_CUR_VALUE, 1)
        this[TYPE_SCHEME_AI_TIME] = getTextConfig(TYPE_SCHEME_AI_TIME, 1)
    }

    override fun getElements(userConfig: ServerUserConfig, sensorId: Int, scale: Float): List<XyElement> {
        val alResult = mutableListOf<XyElement>()

        val sensorEntity = sensorRepository.findByIdOrNull(sensorId) ?: return emptyList()

        val valueMultiplicator = sensorEntity.indicatorMultiplicator ?: 1.0

        SensorService.checkAndCreateSensorTables(entityManager, sensorEntity.id)

        var sensorTime: Int? = null
        var sensorValue: Double? = null
        ApplicationService.withConnection(entityManager) { conn ->
            val rs = conn.executeQuery(
                """
                    SELECT ontime_0, value_1
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

        val dataValueStateEnum = sensorValue?.let { value ->
            sensorEntity.minLimit?.let { minLimit ->
                sensorEntity.maxLimit?.let { maxLimit ->
                    if (maxLimit - minLimit > 0) {
                        if (value < minLimit || value > maxLimit) {
                            DataValueStateEnum.CRITICAL
                        } else {
                            DataValueStateEnum.NORMAL
                        }
                    } else {
                        DataValueStateEnum.NEUTRAL
                    }
                } ?: run {
                    if (value < minLimit) {
                        DataValueStateEnum.CRITICAL
                    } else {
                        DataValueStateEnum.NORMAL
                    }
                }
            } ?: run {
                sensorEntity.maxLimit?.let { maxLimit ->
                    if (value > maxLimit) {
                        DataValueStateEnum.CRITICAL
                    } else {
                        DataValueStateEnum.NORMAL
                    }
                } ?: DataValueStateEnum.NEUTRAL
            }
        } ?: DataValueStateEnum.OFF

        val limitArcs = mutableListOf<Triple<Double, Double, DataValueStateEnum>>()

        sensorEntity.minView?.let { minView ->
            sensorEntity.maxView?.let { maxView ->
                if (maxView - minView > 0) {
                    sensorEntity.minLimit?.let { minLimit ->
                        sensorEntity.maxLimit?.let { maxLimit ->
                            if (maxLimit - minLimit > 0) {
                                limitArcs += Triple(minView, minLimit, DataValueStateEnum.CRITICAL)
                                limitArcs += Triple(minLimit, maxLimit, DataValueStateEnum.NORMAL)
                                limitArcs += Triple(maxLimit, maxView, DataValueStateEnum.CRITICAL)
                            }
                        } ?: run {
                            limitArcs += Triple(minView, minLimit, DataValueStateEnum.CRITICAL)
                            limitArcs += Triple(minLimit, maxView, DataValueStateEnum.NORMAL)
                        }
                    } ?: run {
                        sensorEntity.maxLimit?.let { maxLimit ->
                            limitArcs += Triple(minView, maxLimit, DataValueStateEnum.NORMAL)
                            limitArcs += Triple(maxLimit, maxView, DataValueStateEnum.CRITICAL)
                        }
                    }
                }
            }
        }

        val x0 = 6 * GRID_STEP
        val y0 = 6 * GRID_STEP

        //--- заголовок

        addTitleElement(
            sensorEntity = sensorEntity,
            sensorTime = sensorTime,
            errorTime = errorTime,
            elementType = TYPE_SCHEME_AI_TITLE,
            x = x0,
            y = 1 * GRID_STEP,
            scale = scale,
            alResult = alResult,
        )

        //--- по краю шкалы - широкая цветная дуга лимитов

        sensorEntity.minView?.let { minView ->
            sensorEntity.maxView?.let { maxView ->
                if (maxView - minView > 0) {
                    limitArcs.forEach { (startValue, endValue, valueState) ->
                        XyElement(TYPE_SCHEME_AI_ARC_LIMIT, -getRandomInt(), sensorId).apply {
                            isReadOnly = true
                            alPoint = listOf(XyPoint(x0, y0))
                            radius = 4 * GRID_STEP
                            // отсчёт углов в compose - в обратную сторону (по часовой стрелке)
                            startAngle = (180 + (startValue - minView) / (maxView - minView) * 180).toInt()
                            sweepAngle = ((endValue - startValue) / (maxView - minView) * 180).toInt()
                            fillColor = null
                            drawColor = when (valueState) {
                                DataValueStateEnum.NORMAL -> INDICATOR_ARC_COLOR_NORMAL
                                DataValueStateEnum.CRITICAL -> INDICATOR_ARC_COLOR_CRITICAL
                                else -> INDICATOR_ARC_COLOR_NEUTRAL
                            }
                            lineWidth = 16
                        }.let { xyElement ->
                            alResult.add(xyElement)
                        }
                    }
                }
            }
        }

        //--- поверх неё базовая дуга + фон шкалы (чтобы фон шкалы красиво закрыл половину ширины шкалы лимитов)

        XyElement(TYPE_SCHEME_AI_ARC_BASE, -getRandomInt(), sensorId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(x0, y0))
            radius = 4 * GRID_STEP
            // отсчёт углов в compose - в обратную сторону (по часовой стрелке)
            startAngle = 180
            sweepAngle = 180
            fillColor = if (isErrorStatus) {
                INDICATOR_BACK_COLOR_ERROR
            } else {
                when (dataValueStateEnum) {
                    DataValueStateEnum.OFF -> INDICATOR_BACK_COLOR_OFF
                    DataValueStateEnum.NEUTRAL -> INDICATOR_BACK_COLOR_NEUTRAL
                    DataValueStateEnum.NORMAL -> INDICATOR_BACK_COLOR_NORMAL
                    DataValueStateEnum.CRITICAL -> INDICATOR_BACK_COLOR_CRITICAL
                }
            }
            drawColor = INDICATOR_ARC_COLOR_BASE
            lineWidth = 1
        }.let { xyElement ->
            alResult.add(xyElement)
        }

        //--- риски/насечки и значения на базовой дуге

        sensorEntity.minView?.let { minView ->
            sensorEntity.maxView?.let { maxView ->
                val deltaView = maxView - minView
                if (deltaView > 0) {
                    val sectorCount = sensorEntity.indicatorDelimiterCount ?: 4
                    val avgValue = minView + (maxView - minView) / 2
                    val prec = if (deltaView < 10) {
                        2
                    } else if (deltaView < 100) {
                        1
                    } else {
                        0
                    }

                    for (i in 0..sectorCount) {
                        val value = minView + (maxView - minView) / sectorCount * i

                        //val degree = 180 * sensorValue / dv
                        //val radian = degree * PI / 180
                        //--- итого:
                        val radian = (value - minView) / (maxView - minView) * PI
                        val dxt = -cos(radian) * (GRID_STEP * 4 + GRID_STEP / 4)
                        val dyt = -sin(radian) * (GRID_STEP * 4 + GRID_STEP / 4)
                        val dx1 = -cos(radian) * GRID_STEP * 4
                        val dy1 = -sin(radian) * GRID_STEP * 4
                        val dx2 = -cos(radian) * (GRID_STEP * 4 - GRID_STEP / 8)
                        val dy2 = -sin(radian) * (GRID_STEP * 4 - GRID_STEP / 8)

                        XyElement(TYPE_SCHEME_AI_ARC_VALUE, -getRandomInt(), sensorId).apply {
                            isReadOnly = true
                            alPoint = listOf(XyPoint(x0 + dxt, y0 + dyt))
                            anchorX = if (value < avgValue) {
                                XyElement.Anchor.RB
                            } else if (value > avgValue) {
                                XyElement.Anchor.LT
                            } else {
                                XyElement.Anchor.CC
                            }
                            anchorY = XyElement.Anchor.RB
                            text = getSplittedDouble(value / valueMultiplicator, prec)
                            textColor = TEXT_COLOR_NORMAL
                            fillColor = null
                            drawColor = null
                            lineWidth = null
                            fontSize = when {
                                scale <= 12_000 -> 24
                                scale <= 24_000 -> 18
                                scale <= 36_000 -> 12
                                scale <= 48_000 -> 11
                                scale <= 60_000 -> 10
                                else -> 9
                            }
                            isFontBold = true
                        }.let { xyElement ->
                            alResult.add(xyElement)
                        }
                        XyElement(TYPE_SCHEME_AI_ARC_NOTCH, -getRandomInt(), sensorId).apply {
                            isReadOnly = true
                            alPoint = listOf(
                                XyPoint(x0 + dx1, y0 + dy1),
                                XyPoint(x0 + dx2, y0 + dy2),
                            )
                            fillColor = null
                            drawColor = TEXT_COLOR_NORMAL
                            lineWidth = 4
                        }.let { xyElement ->
                            alResult.add(xyElement)
                        }
                    }
                }
            }
        }

        //--- отображение мультипликатора

        if (valueMultiplicator != 1.0) {
            val prec = if (valueMultiplicator < 1) {
                -1
            } else {
                0
            }
            XyElement(TYPE_SCHEME_AI_MULTIPLICATOR, -getRandomInt(), sensorId).apply {
                isReadOnly = true
                alPoint = listOf(XyPoint(x0, y0 - GRID_STEP))
                anchorX = XyElement.Anchor.CC
                anchorY = XyElement.Anchor.RB
                text = "x " + getSplittedDouble(valueMultiplicator, prec)
                textColor = INDICATOR_MULTIPLICATOR_COLOR
                fillColor = null
                drawColor = null
                lineWidth = null
                //--- на несколько размеров меньше, чем показатель текущего значения
                fontSize = when {
                    scale <= 12_000 -> 32
                    scale <= 24_000 -> 26
                    scale <= 36_000 -> 20
                    scale <= 48_000 -> 16
                    scale <= 60_000 -> 12
                    else -> 10
                }
                isFontBold = false
            }.let { xyElement ->
                alResult.add(xyElement)
            }
        }

        //--- стрелка с основанием

        sensorEntity.minView?.let { minView ->
            sensorEntity.maxView?.let { maxView ->
                sensorValue?.let { sv ->
                    if (maxView - minView > 0) {
                        XyElement(TYPE_SCHEME_AI_ARROW_BASE, -getRandomInt(), sensorId).apply {
                            isReadOnly = true
                            alPoint = listOf(XyPoint(x0, y0))
                            radius = GRID_STEP / 8
                            // отсчёт углов в compose - в обратную сторону (по часовой стрелке)
                            startAngle = 180
                            sweepAngle = 360
                            fillColor = TEXT_COLOR_NORMAL
                            drawColor = TEXT_COLOR_NORMAL
                            lineWidth = 1
                        }.let { xyElement ->
                            alResult.add(xyElement)
                        }

                        //val degree = 180 * sensorValue / dv
                        //val radian = degree * PI / 180
                        //--- итого:
                        val radian = (sv - minView) / (maxView - minView) * PI
                        val dx = -cos(radian) * (GRID_STEP * 4 - GRID_STEP / 3)
                        val dy = -sin(radian) * (GRID_STEP * 4 - GRID_STEP / 3)
                        XyElement(TYPE_SCHEME_AI_ARROW, -getRandomInt(), sensorId).apply {
                            isReadOnly = true
                            alPoint = listOf(
                                XyPoint(x0, y0),
                                XyPoint(x0 + dx, y0 + dy),
                            )
                            fillColor = null
                            drawColor = TEXT_COLOR_NORMAL
                            lineWidth = 4
                        }.let { xyElement ->
                            alResult.add(xyElement)
                        }
                    }
                }
            }
        }

        val valueText = sensorValue?.let { sv ->
            val dim = sensorEntity.dim?.trim() ?: ""
            getSplittedDouble(sv, getPrecision(sv)) + if (dim.isNotEmpty()) {
                " [$dim]"
            } else {
                ""
            }
        } ?: "-"

        XyElement(TYPE_SCHEME_AI_CUR_VALUE, -getRandomInt(), sensorId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(x0, y0 + GRID_STEP / 4))
            anchorX = XyElement.Anchor.CC
            anchorY = XyElement.Anchor.LT
            text = if (isErrorStatus) {
                errorMessage ?: "(неизвестная ошибка)"
            } else {
                valueText
            }
            textColor = if (isErrorStatus) {
                TEXT_COLOR_CRITICAL
            } else {
                TEXT_COLOR_NORMAL
            }
            fillColor = null
            drawColor = null
            lineWidth = null
            fontSize = when {
                isErrorStatus -> 12
                scale <= 12_000 -> 40
                scale <= 24_000 -> 32
                scale <= 36_000 -> 24
                scale <= 48_000 -> 20
                scale <= 60_000 -> 16
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
            elementType = TYPE_SCHEME_AI_TIME,
            x = x0,
            y = 7 * GRID_STEP + GRID_STEP / 4,
            scale = scale,
            alResult = alResult,
        )

        return alResult
    }

}
