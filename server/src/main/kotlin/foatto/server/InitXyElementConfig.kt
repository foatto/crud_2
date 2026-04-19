package foatto.server

import foatto.core.model.response.xy.XyElementType
import foatto.core.model.response.xy.XyElementConfig

fun initXyElementConfig(level: Int, minScale: Int, maxScale: Int): MutableMap<String, XyElementConfig> {
    val hmElementConfig = mutableMapOf<String, XyElementConfig>()

    hmElementConfig[XyElementType.BITMAP.name] = XyElementConfig(
        name = XyElementType.BITMAP.name,
        type = XyElementType.BITMAP,
        layer = 0,
        scaleMin = minScale,
        scaleMax = maxScale,
        descrForAction = "",
        isRotatable = false,
        isMoveable = true,
        isEditablePoint = false
    )

    hmElementConfig[XyElementType.ICON.name] = XyElementConfig(
        name = XyElementType.ICON.name,
        type = XyElementType.ICON,
        layer = level,
        scaleMin = minScale,
        scaleMax = maxScale,
        descrForAction = "",
        isRotatable = true,
        isMoveable = true,
        isEditablePoint = false
    )

    hmElementConfig[XyElementType.MARKER.name] = XyElementConfig(
        name = XyElementType.MARKER.name,
        type = XyElementType.MARKER,
        layer = level,
        scaleMin = minScale,
        scaleMax = maxScale,
        descrForAction = "",
        isRotatable = true,
        isMoveable = true,
        isEditablePoint = false
    )

    hmElementConfig[XyElementType.POLY.name] = XyElementConfig(
        name = XyElementType.POLY.name,
        type = XyElementType.POLY,
        layer = level,
        scaleMin = minScale,
        scaleMax = maxScale,
        descrForAction = "",
        isRotatable = false,
        isMoveable = true,
        isEditablePoint = true
    )

    hmElementConfig[XyElementType.TEXT.name] = XyElementConfig(
        name = XyElementType.TEXT.name,
        type = XyElementType.TEXT,
        layer = level,
        scaleMin = minScale,
        scaleMax = maxScale,
        descrForAction = "",
        isRotatable = true,
        isMoveable = true,
        isEditablePoint = false
    )

    hmElementConfig[XyElementType.TRACE.name] = XyElementConfig(
        name = XyElementType.TRACE.name,
        type = XyElementType.TRACE,
        layer = level,
        scaleMin = minScale,
        scaleMax = maxScale,
        descrForAction = "",
        isRotatable = false,
        isMoveable = true,
        isEditablePoint = true
    )

    return hmElementConfig
}
