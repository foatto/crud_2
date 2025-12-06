package foatto.server.service.report

import foatto.core.util.getDateTimeDMYHMSString
import foatto.server.model.ServerUserConfig
import foatto.server.repository.ActionLogRepository
import foatto.server.service.FileStoreService
import foatto.server.service.ReportService
import jxl.write.Label
import jxl.write.WritableSheet

abstract class MMSReportService(
    private val fileStoreService: FileStoreService,
    private val actionLogRepository: ActionLogRepository,
) : ReportService(
    fileStoreService = fileStoreService,
    actionLogRepository = actionLogRepository,
) {

    //--- стандартные ширины столбцов
    // NNN                  = "N п/п"               = 5
    // dd.mm.yyyy hh:mm:ss  = "начало/окончание"    = 16    - в одну строку
    // dd.mm.yyyy hh:mm:ss  = "начало/окончание"    = 9     - в две строки
    // hhhh:mm:ss           = "длитель-ность"       = 9
    // A999AA116RUS         = "объект/скважина"    >= 20 ( нельзя уменьшить менее 20? )
    // A999AA116RUS         = "датчик/оборуд."     <= 20 ( можно уменьшить до 15? )
    // 9999.9               = "пробег"              = 7
    // 9999.9               = "время работы"        = 7
    // dd.mm.yyyy           = "дата"                = 9
    // АИ-95 ( осн. )( изм. )   = "наим. жидкости"  = 15
    // 9999.9               = "расход жидкости"     = 7

    protected fun fillReportTitle(
        userConfig: ServerUserConfig,
        title: String,
        begDateTime: Int,
        endDateTime: Int,
        sheet: WritableSheet,
        offsX: Int,
    ): Int {
        var offsY = 0

        sheet.addCell(Label(offsX, offsY++, title, wcfTitleL))
        sheet.addCell(
            Label(
                offsX,
                offsY++,
                "за период с ${getDateTimeDMYHMSString(userConfig.timeOffset, begDateTime)}" +
                        " по ${getDateTimeDMYHMSString(userConfig.timeOffset, endDateTime)}",
                wcfTitleL,
            )
        )
        offsY++    //??? еще одна пустая строчка снизу

        return offsY
    }
}