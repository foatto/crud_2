package foatto.server.ds

object PortNumbers {

    const val GALILEO_POWER_VOLTAGE_8: Int = 8
    const val GALILEO_ACCUM_VOLTAGE_9: Int = 9
    const val GALILEO_UNIVERSAL_10: Int = 10
    const val GALILEO_CONTROLLER_TEMPERATURE_18: Int = 18
    const val GALILEO_RS_485_FUEL_20: Int = 20
    const val GALILEO_CAN_FUEL_LEVEL_36: Int = 36
    const val GALILEO_CAN_LIQUID_TEMPERATURE_37: Int = 37
    const val GALILEO_CAN_ENGINE_TURN_38: Int = 38
    // port #39 skipped yet
    const val GALILEO_RS_485_TEMPERATURE_40: Int = 40
    const val GALILEO_EXT_TEMPERATURE_60: Int = 60
    // port #70..99 is free yet
    const val GALILEO_USER_DATA_100: Int = 100
    const val GALILEO_COUNT_110: Int = 110
    const val LLS_LEVEL_120: Int = 120
    const val GALILEO_VOLTAGE_140: Int = 140

    const val MERCURY_COUNT_ACTIVE_DIRECT_160: Int = 160
    const val MERCURY_COUNT_ACTIVE_REVERSE_164: Int = 164
    const val MERCURY_COUNT_REACTIVE_DIRECT_168: Int = 168
    const val MERCURY_COUNT_REACTIVE_REVERSE_172: Int = 172
    const val MERCURY_VOLTAGE_A_180: Int = 180
    const val MERCURY_VOLTAGE_B_184: Int = 184
    const val MERCURY_VOLTAGE_C_188: Int = 188
    const val MERCURY_CURRENT_A_200: Int = 200
    const val MERCURY_CURRENT_B_204: Int = 204
    const val MERCURY_CURRENT_C_208: Int = 208
    const val MERCURY_POWER_KOEF_A_220: Int = 220
    const val MERCURY_POWER_KOEF_B_224: Int = 224
    const val MERCURY_POWER_KOEF_C_228: Int = 228
    const val MERCURY_POWER_ACTIVE_A_232: Int = 232
    const val MERCURY_POWER_ACTIVE_B_236: Int = 236
    const val MERCURY_POWER_ACTIVE_C_240: Int = 240
    const val MERCURY_POWER_REACTIVE_A_244: Int = 244
    const val MERCURY_POWER_REACTIVE_B_248: Int = 248
    const val MERCURY_POWER_REACTIVE_C_252: Int = 252
    const val MERCURY_POWER_FULL_A_256: Int = 256
    const val MERCURY_POWER_FULL_B_260: Int = 260
    const val MERCURY_POWER_FULL_C_264: Int = 264

    const val EMIS_MASS_FLOW_270: Int = 270
    const val EMIS_DENSITY_280: Int = 280
    const val EMIS_TEMPERATURE_290: Int = 290
    const val EMIS_VOLUME_FLOW_300: Int = 300
    const val EMIS_ACCUMULATED_MASS_310: Int = 310
    const val EMIS_ACCUMULATED_VOLUME_320: Int = 320

    const val MERCURY_POWER_ACTIVE_ABC_330: Int = 330
    const val MERCURY_POWER_REACTIVE_ABC_340: Int = 340
    const val MERCURY_POWER_FULL_ABC_350: Int = 350
    const val MERCURY_TRANSFORM_KOEF_CURRENT_360: Int = 360
    const val MERCURY_TRANSFORM_KOEF_VOLTAGE_370: Int = 370

    const val GALILEO_EXT_MODBUS_400: Int = 400
    // port #470..499 is free yet

    //--- в MMSPulsarDataController приходят как EMIS_xxx
//    const val ESD_STATUS_500: Int = 500 - не используется
    const val ESD_VOLUME_504: Int = 504
    const val ESD_FLOW_508: Int = 508
    const val ESD_CAMERA_VOLUME_512: Int = 512
    const val ESD_CAMERA_FLOW_516: Int = 516
    const val ESD_CAMERA_TEMPERATURE_520: Int = 520
    const val ESD_REVERSE_CAMERA_VOLUME_524: Int = 524
    const val ESD_REVERSE_CAMERA_FLOW_528: Int = 528
    const val ESD_REVERSE_CAMERA_TEMPERATURE_532: Int = 532

    const val PMP_LEVEL_540: Int = 540
    const val PMP_TEMPERATURE_560: Int = 560
    const val PMP_VOLUME_580: Int = 580
    const val PMP_MASS_600: Int = 600
    const val PMP_DENSITY_620: Int = 620

    const val PRESSURE_640: Int = 640
    const val TURN_660: Int = 660
    const val CONSTANT_VOLTAGE_680: Int = 680

    const val GALILEO_EXT_BLUETOOTH_700: Int = 700
    // port #770..799 is free yet
}