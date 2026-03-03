package foatto.server.service

import foatto.core.ActionType
import foatto.core.i18n.getLocalizedMessage
import foatto.core.model.AppAction
import foatto.core.model.request.ChartActionRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.ChartActionResponse
import foatto.core.model.response.HeaderData
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.chart.ChartResponse
import foatto.server.SpringApp
import foatto.server.appModuleConfigs
import foatto.server.checkAccessPermission
import foatto.server.model.ServerUserConfig
import jakarta.persistence.EntityManager
import kotlin.collections.get

abstract class AbstractChartService(
    private val entityManager: EntityManager,
) {

    companion object {
        val FILL_NEUTRAL: Int = 0xFF_E0_FF_FF.toInt()
        val FILL_NORMAL: Int = 0xFF_E0_FF_E0.toInt()
        val FILL_WARNING: Int = 0xFF_FF_FF_E0.toInt()
        val FILL_CRITICAL: Int = 0xFF_FF_E0_E0.toInt()

        val BORDER_NEUTRAL: Int = 0xFF_C0_FF_FF.toInt()
        val BORDER_NORMAL: Int = 0xFF_B0_F0_B0.toInt()
        val BORDER_WARNING: Int = 0xFF_E0_E0_C0.toInt()
        val BORDER_CRITICAL: Int = 0xFF_FF_C0_C0.toInt()

        val TEXT_NEUTRAL: Int = 0xFF_00_00_80.toInt()
        val TEXT_NORMAL: Int = 0xFF_00_80_00.toInt()
        val TEXT_WARNING: Int = 0xFF_80_80_00.toInt()
        val TEXT_CRITICAL: Int = 0xFF_80_00_00.toInt()

        val POINT_NEUTRAL: Int = 0xFF_C0_C0_FF.toInt()
        val POINT_NORMAL: Int = 0xFF_C0_FF_C0.toInt()
        val POINT_BELOW: Int = 0xFF_E0_E0_A0.toInt()
        val POINT_ABOVE: Int = 0xFF_FF_C0_C0.toInt()

        val LINE_LIMIT: Int = 0xFF_FF_A0_A0.toInt()

        val LINE_NONE_0: Int = 0x00_80_80_80.toInt()
        val LINE_NORMAL_0: Int = 0xFF_00_D0_00.toInt()
        val LINE_BELOW_0: Int = 0xFF_00_60_D0.toInt()
        val LINE_ABOVE_0: Int = 0xFF_D0_60_00.toInt()

        val LINE_NONE_1: Int = 0x00_90_90_90.toInt()
        val LINE_NORMAL_1: Int = 0xFF_00_00_D0.toInt()

        val LINE_NONE_2: Int = 0x00_A0_A0_A0.toInt()
        val LINE_NORMAL_2: Int = 0xFF_D0_00_00.toInt()

        val LINE_NONE_3: Int = 0x00_B0_B0_B0.toInt()
        val LINE_NORMAL_3: Int = 0xFF_D0_00_D0.toInt()

        val LINE_NONE_4: Int = 0x00_C0_C0_C0.toInt()
        val LINE_NORMAL_4: Int = 0xFF_00_D0_D0.toInt()

        val LINE_NONE_5: Int = 0x00_D0_D0_D0.toInt()
        val LINE_NORMAL_5: Int = 0xFF_D0_D0_00.toInt()

        val AXIS_0: Int = LINE_NORMAL_0  //0xFF_80_C0_80.toInt()
        val AXIS_1: Int = LINE_NORMAL_1  //0xFF_80_80_C0.toInt()
        val AXIS_2: Int = LINE_NORMAL_2  //0xFF_C0_80_80.toInt()
        val AXIS_3: Int = LINE_NORMAL_3  //0xFF_C0_80_C0.toInt()
        val AXIS_4: Int = LINE_NORMAL_4
        val AXIS_5: Int = LINE_NORMAL_5

        private val chartAxisColor: List<Int> = listOf(
            AXIS_0,
            AXIS_1,
            AXIS_2,
            AXIS_3,
            AXIS_4,
            AXIS_5,
        )

        private val chartLineNoneColor: List<Int> = listOf(
            LINE_NONE_0,
            LINE_NONE_1,
            LINE_NONE_2,
            LINE_NONE_3,
            LINE_NONE_4,
            LINE_NONE_5,
        )

        private val chartLineNormalColor: List<Int> = listOf(
            LINE_NORMAL_0,
            LINE_NORMAL_1,
            LINE_NORMAL_2,
            LINE_NORMAL_3,
            LINE_NORMAL_4,
            LINE_NORMAL_5,
        )

        private val chartLineBelowColor: List<Int> = listOf(
            LINE_BELOW_0,
            LINE_BELOW_0,
            LINE_BELOW_0,
            LINE_BELOW_0,
            LINE_BELOW_0,
            LINE_BELOW_0,
        )

        private val chartLineAboveColor: List<Int> = listOf(
            LINE_ABOVE_0,
            LINE_ABOVE_0,
            LINE_ABOVE_0,
            LINE_ABOVE_0,
            LINE_ABOVE_0,
            LINE_ABOVE_0,
        )

        fun getChartAxisColor(index: Int): Int = chartAxisColor[index % chartAxisColor.size]
        fun getChartLineNoneColor(index: Int): Int = chartLineNoneColor[index % chartLineNoneColor.size]
        fun getChartLineNormalColor(index: Int): Int = chartLineNormalColor[index % chartLineNormalColor.size]
        fun getChartLineBelowColor(index: Int): Int = chartLineBelowColor[index % chartLineBelowColor.size]
        fun getChartLineAboveColor(index: Int): Int = chartLineAboveColor[index % chartLineAboveColor.size]
    }

    /*
        //--- данные по гео-датчику (движение/стоянка/ошибка) показываем только на первом/верхнем графике
        protected var isGeoSensorShowed = false

        //--- общие нештатные ситуации показываем только на первом/верхнем графике
        protected var isCommonTroubleShowed = false
    */

    fun chart(
        sessionId: Long,
        action: AppAction,
    ): AppResponse {
        val actionModule = action.module

        val sessionData = SpringApp.getSessionData(sessionId) ?: return AppResponse(ResponseCode.LOGON_NEED)
        val userConfig = sessionData.serverUserConfig ?: return AppResponse(ResponseCode.LOGON_NEED)
        if (!checkAccessPermission(actionModule, userConfig.roles)) {
            return AppResponse(ResponseCode.LOGON_NEED)
        }
        val moduleConfig = appModuleConfigs[actionModule] ?: return AppResponse(ResponseCode.LOGON_NEED)

        return AppResponse(
            responseCode = ResponseCode.MODULE_CHART,
            chart = ChartResponse(
                tabCaption = getLocalizedMessage(moduleConfig.captions, userConfig.lang),
                headerData = getChartHeader(userConfig, action),
            )
        )
    }

    fun chartAction(chartActionRequest: ChartActionRequest): ChartActionResponse {
        val actionModule = chartActionRequest.action.module

        val sessionData = SpringApp.getSessionData(chartActionRequest.sessionId) ?: return ChartActionResponse(ResponseCode.LOGON_NEED)
        val userConfig = sessionData.serverUserConfig ?: return ChartActionResponse(ResponseCode.LOGON_NEED)
        if (!checkAccessPermission(actionModule, userConfig.roles)) {
            return ChartActionResponse(ResponseCode.LOGON_NEED)
        }

        return when (chartActionRequest.action.type) {

            ActionType.GET_ELEMENTS -> {
                getCharts(userConfig, chartActionRequest)
            }

            else -> {
                ChartActionResponse(
                    responseCode = ResponseCode.ERROR,
                )
            }
        }
    }

    protected abstract fun getChartHeader(userConfig: ServerUserConfig, action: AppAction): HeaderData

    protected abstract fun getCharts(userConfig: ServerUserConfig, chartActionRequest: ChartActionRequest): ChartActionResponse

}
