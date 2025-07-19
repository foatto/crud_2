package foatto.compose.control.model.xy

enum class XyElementDataType {
    ARC,
    CIRCLE,
    ELLIPSE,
    ICON,   //!!! временно псевдоотличаем ICON от IMAGE, пока не научимся грузить и рисовать image на канвасе
    IMAGE,
    LINE,
    POLY,
    RECT,
    TEXT,
}
