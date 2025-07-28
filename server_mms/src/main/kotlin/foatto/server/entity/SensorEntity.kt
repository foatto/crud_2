package foatto.server.entity

import foatto.server.entity.converter.BooleanToIntConverter
import jakarta.persistence.*

@Entity
@Table(name = "MMS_sensor")
class SensorEntity(

    @Id
    var id: Int,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "object_id")
    var obj: ObjectEntity?,

    val name: String?,       // inner/system sensor name for programmatically sensors adding

    @Column(name = "group_name")
    val group: String?,      // sensor group name for sensors logical linking and/or grouping

    val descr: String?,      // sensor visible description

    @Column(name = "port_num")
    var portNum: Int?,

    @Column(name = "sensor_type")
    val sensorType: Int?,

    @Column(name = "beg_time")
    var begTime: Int?,
    @Column(name = "end_time")
    var endTime: Int?,

    @Column(name = "serial_no")
    val serialNo: String?,

    //--- geo-sensor data

    @Column(name = "min_moving_time")
    val minMovingTime: Int?,

    @Column(name = "min_parking_time")
    val minParkingTime: Int?,

    @Column(name = "min_over_speed_time")
    val minOverSpeedTime: Int?,

    @Convert(converter = BooleanToIntConverter::class)
    @Column(name = "is_absolute_run")
    val isAbsoluteRun: Boolean?,

    @Column(name = "speed_round_rule")
    val speedRoundRule: Int?,

    @Column(name = "run_koef")
    val runKoef: Double?,

    @Convert(converter = BooleanToIntConverter::class)
    @Column(name = "is_use_pos")
    val isUsePos: Boolean?,

    @Convert(converter = BooleanToIntConverter::class)
    @Column(name = "is_use_speed")
    val isUseSpeed: Boolean?,

    @Convert(converter = BooleanToIntConverter::class)
    @Column(name = "is_use_run")
    val isUseRun: Boolean?,

    //--- for all sensor tpes (exclude geo-sensor)

    @Column(name = "ignore_min_sensor")
    val minIgnore: Double?,

    @Column(name = "ignore_max_sensor")
    val maxIgnore: Double?,

    val dim: String?,

    //--- equipment working sensors

    @Convert(converter = BooleanToIntConverter::class)
    @Column(name = "active_value")
    val isAboveBorder: Boolean?,

    @Column(name = "bound_value")
    val onOffBorder: Double?,

    @Column(name = "idle_border")
    val idleBorder: Double?,

    @Column(name = "limit_border")
    val limitBorder: Double?,

    @Column(name = "min_on_time")
    val minOnTime: Int?,

    @Column(name = "min_off_time")
    val minOffTime: Int?,

    //--- analogue sensor attributes

    @Column(name = "analog_min_view")
    val minView: Double?,

    @Column(name = "analog_max_view")
    val maxView: Double?,

    @Column(name = "analog_min_limit")
    val minLimit: Double?,

    @Column(name = "analog_max_limit")
    val maxLimit: Double?,

    @Column(name = "smooth_time")
    val smoothTime: Int?,

    @Column(name = "analog_indicator_delimiter_count")
    val indicatorDelimiterCount: Int?,

    @Column(name = "analog_indicator_multiplicator")
    val indicatorMultiplicator: Double?,

    //--- (fuel) counter sensor attributes

    @Convert(converter = BooleanToIntConverter::class)
    @Column(name = "is_absolute_count")
    val isAbsoluteCount: Boolean?,

    @Column(name = "in_out_type")
    val inOutType: Int?,

    //--- liquid level sensors only

    @Column(name = "container_type")
    val containerType: Int?,

    //--- energo sensor attributes

    @Column(name = "energo_phase")
    val phase: Int?,

    //--- base sensor attributes

    @Column(name = "liquid_name")
    val liquidName: String?,

    @Column(name = "liquid_norm")
    val liquidNorm: Double?,

    @Column(name = "scheme_x")
    val schemeX: Int?,

    @Column(name = "scheme_y")
    val schemeY: Int?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SensorEntity) return false

        if (portNum != other.portNum) return false
        if (sensorType != other.sensorType) return false
        if (begTime != other.begTime) return false
        if (endTime != other.endTime) return false
        if (minMovingTime != other.minMovingTime) return false
        if (minParkingTime != other.minParkingTime) return false
        if (minOverSpeedTime != other.minOverSpeedTime) return false
        if (isAbsoluteRun != other.isAbsoluteRun) return false
        if (speedRoundRule != other.speedRoundRule) return false
        if (runKoef != other.runKoef) return false
        if (isUsePos != other.isUsePos) return false
        if (isUseSpeed != other.isUseSpeed) return false
        if (isUseRun != other.isUseRun) return false
        if (isAboveBorder != other.isAboveBorder) return false
        if (onOffBorder != other.onOffBorder) return false
        if (idleBorder != other.idleBorder) return false
        if (limitBorder != other.limitBorder) return false
        if (minOnTime != other.minOnTime) return false
        if (minOffTime != other.minOffTime) return false
        if (smoothTime != other.smoothTime) return false
        if (minIgnore != other.minIgnore) return false
        if (maxIgnore != other.maxIgnore) return false
        if (liquidNorm != other.liquidNorm) return false
        if (minView != other.minView) return false
        if (maxView != other.maxView) return false
        if (minLimit != other.minLimit) return false
        if (maxLimit != other.maxLimit) return false
        if (indicatorDelimiterCount != other.indicatorDelimiterCount) return false
        if (indicatorMultiplicator != other.indicatorMultiplicator) return false
        if (isAbsoluteCount != other.isAbsoluteCount) return false
        if (phase != other.phase) return false
        if (inOutType != other.inOutType) return false
        if (containerType != other.containerType) return false
        if (schemeX != other.schemeX) return false
        if (schemeY != other.schemeY) return false
        if (obj != other.obj) return false
        if (name != other.name) return false
        if (group != other.group) return false
        if (descr != other.descr) return false
        if (serialNo != other.serialNo) return false
        if (liquidName != other.liquidName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = portNum ?: 0
        result = 31 * result + (sensorType ?: 0)
        result = 31 * result + (begTime ?: 0)
        result = 31 * result + (endTime ?: 0)
        result = 31 * result + (minMovingTime ?: 0)
        result = 31 * result + (minParkingTime ?: 0)
        result = 31 * result + (minOverSpeedTime ?: 0)
        result = 31 * result + (isAbsoluteRun?.hashCode() ?: 0)
        result = 31 * result + (speedRoundRule ?: 0)
        result = 31 * result + (runKoef?.hashCode() ?: 0)
        result = 31 * result + (isUsePos?.hashCode() ?: 0)
        result = 31 * result + (isUseSpeed?.hashCode() ?: 0)
        result = 31 * result + (isUseRun?.hashCode() ?: 0)
        result = 31 * result + (isAboveBorder?.hashCode() ?: 0)
        result = 31 * result + (onOffBorder?.hashCode() ?: 0)
        result = 31 * result + (idleBorder?.hashCode() ?: 0)
        result = 31 * result + (limitBorder?.hashCode() ?: 0)
        result = 31 * result + (minOnTime ?: 0)
        result = 31 * result + (minOffTime ?: 0)
        result = 31 * result + (smoothTime ?: 0)
        result = 31 * result + (minIgnore?.hashCode() ?: 0)
        result = 31 * result + (maxIgnore?.hashCode() ?: 0)
        result = 31 * result + (liquidNorm?.hashCode() ?: 0)
        result = 31 * result + (minView?.hashCode() ?: 0)
        result = 31 * result + (maxView?.hashCode() ?: 0)
        result = 31 * result + (minLimit?.hashCode() ?: 0)
        result = 31 * result + (maxLimit?.hashCode() ?: 0)
        result = 31 * result + (indicatorDelimiterCount ?: 0)
        result = 31 * result + (indicatorMultiplicator?.hashCode() ?: 0)
        result = 31 * result + (isAbsoluteCount?.hashCode() ?: 0)
        result = 31 * result + (phase ?: 0)
        result = 31 * result + (inOutType ?: 0)
        result = 31 * result + (containerType ?: 0)
        result = 31 * result + (schemeX ?: 0)
        result = 31 * result + (schemeY ?: 0)
        result = 31 * result + (obj?.hashCode() ?: 0)
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (group?.hashCode() ?: 0)
        result = 31 * result + (descr?.hashCode() ?: 0)
        result = 31 * result + (serialNo?.hashCode() ?: 0)
        result = 31 * result + (liquidName?.hashCode() ?: 0)
        return result
    }
}
