package foatto.core

import foatto.core.model.AppModuleUrl

val appModuleUrls: MutableMap<String, AppModuleUrl> = mutableMapOf(
    AppModule.ACTION_LOG to AppModuleUrl(
        appUrl = ApiUrl.ACTION_LOG,
        formActionUrl = ApiUrl.ACTION_LOG_FORM_ACTION,
    ),
    AppModule.USER to AppModuleUrl(
        appUrl = ApiUrl.USER,
        formActionUrl = ApiUrl.USER_FORM_ACTION,
    ),
    AppModule.USER_PROPERTY_EDIT to AppModuleUrl(
        appUrl = ApiUrl.USER_PROPERTY_EDIT,
        formActionUrl = ApiUrl.USER_PROPERTY_EDIT_FORM_ACTION,
    ),
)