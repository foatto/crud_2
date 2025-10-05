package foatto.core_mms

import foatto.core.appModuleUrls
import foatto.core.model.AppModuleUrl

fun addAppModuleUrls() {
    appModuleUrls[AppModuleMMS.OBJECT] = AppModuleUrl(
        appUrl = ApiUrlMMS.OBJECT,
        formActionUrl = ApiUrlMMS.OBJECT_FORM_ACTION,
    )

    appModuleUrls[AppModuleMMS.DEPARTMENT] = AppModuleUrl(
        appUrl = ApiUrlMMS.DEPARTMENT,
        formActionUrl = ApiUrlMMS.DEPARTMENT_FORM_ACTION,
    )
    appModuleUrls[AppModuleMMS.GROUP] = AppModuleUrl(
        appUrl = ApiUrlMMS.GROUP,
        formActionUrl = ApiUrlMMS.GROUP_FORM_ACTION,
    )

    appModuleUrls[AppModuleMMS.SENSOR] = AppModuleUrl(
        appUrl = ApiUrlMMS.SENSOR,
        formActionUrl = ApiUrlMMS.SENSOR_FORM_ACTION,
    )
    appModuleUrls[AppModuleMMS.SENSOR_CALIBRATION] = AppModuleUrl(
        appUrl = ApiUrlMMS.SENSOR_CALIBRATION,
        formActionUrl = ApiUrlMMS.SENSOR_CALIBRATION_FORM_ACTION,
    )

    appModuleUrls[AppModuleMMS.OBJECT_DATA] = AppModuleUrl(
        appUrl = ApiUrlMMS.OBJECT_DATA,
        formActionUrl = null,
    )
    appModuleUrls[AppModuleMMS.SENSOR_DATA] = AppModuleUrl(
        appUrl = ApiUrlMMS.SENSOR_DATA,
        formActionUrl = null,
    )

    appModuleUrls[AppModuleMMS.DEVICE] = AppModuleUrl(
        appUrl = ApiUrlMMS.DEVICE,
        formActionUrl = ApiUrlMMS.DEVICE_FORM_ACTION,
    )
    appModuleUrls[AppModuleMMS.DEVICE_MANAGE] = AppModuleUrl(
        appUrl = ApiUrlMMS.DEVICE_MANAGE,
        formActionUrl = ApiUrlMMS.DEVICE_MANAGE_FORM_ACTION,
    )

//    appModuleUrls[AppModuleMMS.DAY_WORK] = AppModuleUrl(
//        appUrl = ApiUrlMMS.DAY_WORK,
//        formActionUrl = ApiUrlMMS.DAY_WORK_FORM_ACTION,
//    )

    appModuleUrls[AppModuleMMS.CHART_SENSOR] = AppModuleUrl(
        appUrl = ApiUrlMMS.CHART_SENSOR,
        chartActionUrl = ApiUrlMMS.CHART_SENSOR_ACTION,
    )
//    appModuleUrls[AppModuleMMS.CHART_LIQUID_LEVEL] = AppModuleUrl(
//        appUrl = ApiUrlMMS.CHART_LIQUID_LEVEL,
//        chartActionUrl = ApiUrlMMS.CHART_LIQUID_LEVEL_ACTION,
//    )

    appModuleUrls[AppModuleMMS.MAP_TRACE] = AppModuleUrl(
        appUrl = ApiUrlMMS.MAP_TRACE,
        mapActionUrl = ApiUrlMMS.MAP_TRACE_ACTION,
    )

    appModuleUrls[AppModuleMMS.SCHEME_ANALOGUE_INDICATOR_STATE] = AppModuleUrl(
        appUrl = ApiUrlMMS.SCHEME_ANALOGUE_INDICATOR_STATE,
        schemeActionUrl = ApiUrlMMS.SCHEME_ANALOGUE_INDICATOR_STATE_ACTION,
    )
    appModuleUrls[AppModuleMMS.SCHEME_COUNTER_INDICATOR_STATE] = AppModuleUrl(
        appUrl = ApiUrlMMS.SCHEME_COUNTER_INDICATOR_STATE,
        schemeActionUrl = ApiUrlMMS.SCHEME_COUNTER_INDICATOR_STATE_ACTION,
    )
    appModuleUrls[AppModuleMMS.SCHEME_WORK_INDICATOR_STATE] = AppModuleUrl(
        appUrl = ApiUrlMMS.SCHEME_WORK_INDICATOR_STATE,
        schemeActionUrl = ApiUrlMMS.SCHEME_WORK_INDICATOR_STATE_ACTION,
    )

    appModuleUrls[AppModuleMMS.OBJECT_SCHEME_DASHBOARD] = AppModuleUrl(
        appUrl = ApiUrlMMS.OBJECT_SCHEME_DASHBOARD,
        compositeActionUrl = ApiUrlMMS.OBJECT_SCHEME_DASHBOARD_ACTION,
    )
    appModuleUrls[AppModuleMMS.OBJECT_SCHEME_LIST_DASHBOARD] = AppModuleUrl(
        appUrl = ApiUrlMMS.OBJECT_LIST_SCHEME_DASHBOARD,
        compositeActionUrl = ApiUrlMMS.OBJECT_LIST_SCHEME_DASHBOARD_ACTION,
    )

    appModuleUrls[AppModuleMMS.OBJECT_CHART_DASHBOARD] = AppModuleUrl(
        appUrl = ApiUrlMMS.OBJECT_CHART_DASHBOARD,
        compositeActionUrl = ApiUrlMMS.OBJECT_CHART_DASHBOARD_ACTION,
    )
    appModuleUrls[AppModuleMMS.OBJECT_CHART_LIST_DASHBOARD] = AppModuleUrl(
        appUrl = ApiUrlMMS.OBJECT_LIST_CHART_DASHBOARD,
        compositeActionUrl = ApiUrlMMS.OBJECT_LIST_CHART_DASHBOARD_ACTION,
    )

    appModuleUrls[AppModuleMMS.REPORT_SUMMARY] = AppModuleUrl(
        appUrl = ApiUrlMMS.REPORT_SUMMARY,
        formActionUrl = ApiUrlMMS.REPORT_SUMMARY_FORM_ACTION,
    )
}
