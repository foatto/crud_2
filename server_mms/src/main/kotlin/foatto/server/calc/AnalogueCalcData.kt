package foatto.server.calc

import foatto.server.entity.SensorEntity

class AnalogueCalcData(
    val sensorEntity: SensorEntity,
    val begValue: Double?,
    val endValue: Double?,
)