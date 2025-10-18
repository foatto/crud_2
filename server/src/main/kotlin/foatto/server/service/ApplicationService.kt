package foatto.server.service

import foatto.core.ActionType
import foatto.core.AppModule
import foatto.core.IconName
import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.AppResponse
import foatto.core.model.response.ClientActionButton
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.HeaderData
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.ServerActionButton
import foatto.core.model.response.TitleData
import foatto.core.model.response.form.FormButton
import foatto.core.model.response.form.FormButtonKey
import foatto.core.model.response.form.FormResponse
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.form.cells.FormFileData
import foatto.core.model.response.form.cells.FormSimpleCell
import foatto.core.model.response.table.TableCaption
import foatto.core.model.response.table.TablePageButton
import foatto.core.model.response.table.TableResponse
import foatto.core.model.response.table.TableRow
import foatto.core.model.response.table.cell.TableBaseCell
import foatto.core.model.response.table.cell.TableButtonCell
import foatto.core.model.response.table.cell.TableButtonCellData
import foatto.core.model.response.table.cell.TableSimpleCell
import foatto.core.util.getDateDMYString
import foatto.core.util.getDateTimeDMYHMSString
import foatto.server.AppRole
import foatto.server.SpringApp
import foatto.server.appModuleConfigs
import foatto.server.checkAccessPermission
import foatto.server.checkFormAddPermission
import foatto.server.entity.DateEntity
import foatto.server.entity.DateTimeEntity
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.sql.CoreAdvancedConnection
import foatto.server.sql.SpringConnection
import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.sql.ResultSet
import java.sql.Statement

abstract class ApplicationService(
    private val fileStoreService: FileStoreService,
) {

    @Value("\${root_dir}")
    val rootDirName: String = ""

    @Value("\${temp_dir}")
    private val tempDirName: String = ""

    @Value("\${file_access_period}")
    private val fileAccessPeriod: String = "24"  // 24 hour file accessibility by default

    companion object {

        fun withConnection(entityManager: EntityManager, func: (conn: CoreAdvancedConnection) -> Unit) {
            entityManager.unwrap(Session::class.java).let { session ->
                session.doWork { conn ->
                    func(SpringConnection(conn))
                }
            }
        }

        fun queryNativeSql(entityManager: EntityManager, sql: String, func: (rs: ResultSet) -> Unit) {
            entityManager.unwrap(Session::class.java).let { session ->
                session.doWork { conn ->
                    var stm: Statement? = null
                    var rs: ResultSet? = null
                    try {
                        stm = conn.createStatement()
                        rs = stm.executeQuery(sql.trimIndent())
                        func(rs)
                    } catch (t: Throwable) {
                        println("sql = '$sql'")
                        t.printStackTrace()
                    } finally {
                        rs?.close()
                        stm?.close()
                    }
                }
            }
        }

        fun executeNativeSql(entityManager: EntityManager, vararg sqls: String): Int {
            var result = 0

            entityManager.unwrap(Session::class.java).let { session ->
                session.doWork { conn ->
                    var stm: Statement? = null
                    var currentSql: String? = null
                    try {
                        stm = conn.createStatement()
                        sqls.forEach { sql ->
                            currentSql = sql
                            result += stm.executeUpdate(sql.trimIndent())
                        }
                    } catch (t: Throwable) {
                        println("currentSql = '$currentSql'")
                        t.printStackTrace()
                    } finally {
                        stm?.close()
                    }
                }
            }
            return result
        }
    }

    fun app(
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

        return when (action.type) {
            ActionType.MODULE_TABLE -> {
                getTableResponse(
                    action = action,
                    userConfig = userConfig,
                    moduleConfig = moduleConfig,
                )
            }

            ActionType.MODULE_FORM -> {
                getFormResponse(
                    action = action,
                    userConfig = userConfig,
                    moduleConfig = moduleConfig,
                )
            }

            else -> {
                doOtherActionType(
                    action = action,
                    userConfig = userConfig,
                    moduleConfig = moduleConfig,
                )
            }
        }
    }

    private fun getTableResponse(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): AppResponse {
        val tableCaption = moduleConfig.caption
        val columnCaptions = getTableColumnCaptions(action, userConfig)

        val alTableCell = mutableListOf<TableBaseCell>()
        val alTableRowData = mutableListOf<TableRow>()
        val tablePageButtonData = mutableListOf<TablePageButton>()

        val currentRowNo = fillTableGridData(
            action = action,
            userConfig = userConfig,
            moduleConfig = moduleConfig,
            tableCells = alTableCell,
            tableRows = alTableRowData,
            pageButtons = tablePageButtonData,
        )

        return AppResponse(
            responseCode = ResponseCode.MODULE_TABLE,
            table = TableResponse(
                tabCaption = tableCaption,
                headerData = getTableHeaderData(
                    action = action,
                    userConfig = userConfig,
                    moduleConfig = moduleConfig,
                ),
                findText = action.findText ?: "",
                serverActionButtons = getTableServerActionButtons(action, userConfig, moduleConfig),
                clientActionButtons = getTableClientActionButtons(),
                columnCaptions = columnCaptions,
                tableCells = alTableCell,
                tableRows = alTableRowData,
                selectedRowNo = currentRowNo,
                tablePageButtonData = tablePageButtonData,
            )
        )
    }

    protected open fun getTableHeaderData(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): HeaderData = HeaderData(
        titles = listOf(
            TitleData(
                action = null,
                text = if (action.isSelectorMode) {
                    "Выбор: "
                } else {
                    ""
                } + moduleConfig.caption,
                isBold = true,
            ),
        ),
        rows = emptyList()
    )

    protected open fun getTableServerActionButtons(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): List<ServerActionButton> =
        if (checkFormAddPermission(moduleConfig, userConfig.roles)) {
            listOf(
                ServerActionButton(
                    name = IconName.ADD_ITEM,
                    action = getTableAddAction(action),
                    inNewTab = false,
                    isForWideScreenOnly = true,
                )
            )
        } else {
            emptyList()
        }

    protected open fun getTableClientActionButtons(): List<ClientActionButton> = emptyList()

    protected abstract fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<TableCaption>

    protected fun getTableColumnCaptionActions(action: AppAction, alColumnInfo: List<Pair<String?, String>>): List<TableCaption> =
        alColumnInfo.map { (fieldName, fieldCaption) ->
            TableCaption(
                name = fieldCaption,
                action = fieldName?.let {
                    action.copy(
                        sortName = fieldName,
                        isSortAsc = if (fieldName == action.sortName) {
                            !action.isSortAsc
                        } else {
                            true
                        }
                    )
                },
            )
        }

    protected abstract fun fillTableGridData(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        tableCells: MutableList<TableBaseCell>,
        tableRows: MutableList<TableRow>,
        pageButtons: MutableList<TablePageButton>,
    ): Int?

    protected fun getTableSortedPageRequest(
        action: AppAction,
        vararg orders: Sort.Order,
    ): PageRequest {
        var pageSize = appModuleConfigs[action.module]?.pageSize ?: 0
        if (pageSize < 1) {
            pageSize = Int.MAX_VALUE
        }

        return PageRequest.of(
            action.pageNo,
            pageSize,
            action.sortName?.let {
                Sort.by(
                    if (action.isSortAsc) {
                        Sort.Direction.ASC
                    } else {
                        Sort.Direction.DESC
                    },
                    action.sortName,
                )
            } ?: Sort.by(*orders)
        )
    }

    protected fun getTableSelectorButtonCell(row: Int, col: Int, selectorAction: AppAction): TableBaseCell = TableButtonCell(
        row = row,
        col = col,
        dataRow = row,
        values = listOf(
            TableButtonCellData(
                name = IconName.SELECT,
                action = selectorAction,
            ),
        ),
    )

    protected fun getTableUserNameCell(
        row: Int,
        col: Int,
        userId: Int,
        rowUserId: Int?,
        rowOwnerShortName: String?,
        rowOwnerFullName: String?,
    ): TableBaseCell = TableSimpleCell(
        row = row,
        col = col,
        dataRow = row,
        name = if (rowUserId == null) {
            "-"
        } else if (rowUserId == 0) {
            "-"
        } else if (rowUserId == userId) {
            ""
        } else if (!rowOwnerShortName.isNullOrEmpty()) {
            rowOwnerShortName
        } else {
            rowOwnerFullName ?: "(неизвестно)"
        }
    )

    protected fun getTableFileButtonCellData(fileId: Int?): List<TableButtonCellData> = fileId?.let {
        fileStoreService.getFileList(fileId, fileAccessPeriod.toIntOrNull() ?: 24).map { ref ->
            TableButtonCellData(
                name = "${fileStoreService.getFileName(ref)} [${((fileStoreService.getFileSize(ref) ?: 0) / 1000) + 1} Кб]",
                action = AppAction(type = ActionType.FILE, url = fileStoreService.getFileRefUrl(ref)),
                inNewTab = true,
            )
        }
    } ?: emptyList()

    protected open fun getTableAddAction(action: AppAction): AppAction =
        AppAction(
            type = ActionType.MODULE_FORM,
            module = action.module,
            parentModule = action.parentModule,
            parentId = action.parentId,
        )

    protected fun fillTablePageButtons(
        action: AppAction,
        pageCount: Int,
        pageButtons: MutableList<TablePageButton>,
    ) {
        val NEAR_PAGE_BUTTON_COUNT = 11
        val pageNo = action.pageNo

        //--- current page
        pageButtons += getPageButton(null, pageNo)

        //--- previous pages
        for (i in 1..NEAR_PAGE_BUTTON_COUNT) {
            val prevPageNo = pageNo - i
            if (prevPageNo >= 0) {
                pageButtons.add(0, getPageButton(action, prevPageNo))
            } else {
                break
            }
        }

        //--- previous pages / left side
        if (pageNo - NEAR_PAGE_BUTTON_COUNT > 2) {
            pageButtons.add(0, TablePageButton(null, "..."))
            pageButtons.add(0, getPageButton(action, 0))
        } else if (pageNo - NEAR_PAGE_BUTTON_COUNT > 1) {
            pageButtons.add(0, getPageButton(action, 1))
            pageButtons.add(0, getPageButton(action, 0))
        } else if (pageNo - NEAR_PAGE_BUTTON_COUNT > 0) {
            pageButtons.add(0, getPageButton(action, 0))
        }

        //--- next pages
        for (i in 1..NEAR_PAGE_BUTTON_COUNT) {
            val nextPageNo = pageNo + i
            if (nextPageNo <= pageCount - 1) {
                pageButtons += getPageButton(action, nextPageNo)
            } else {
                break
            }
        }

        //--- next pages / right side
        if (pageNo + NEAR_PAGE_BUTTON_COUNT < pageCount - 3) {
            pageButtons += TablePageButton(null, "...")
            pageButtons += getPageButton(action, pageCount - 1)
        } else if (pageNo + NEAR_PAGE_BUTTON_COUNT < pageCount - 2) {
            pageButtons += getPageButton(action, pageCount - 2)
            pageButtons += getPageButton(action, pageCount - 1)
        } else if (pageNo + NEAR_PAGE_BUTTON_COUNT < pageCount - 1) {
            pageButtons += getPageButton(action, pageCount - 1)
        }
    }

    private fun getPageButton(
        action: AppAction? = null,
        pageNo: Int,
    ): TablePageButton = action?.let {
        TablePageButton(action.copy(pageNo = pageNo), "${pageNo + 1}")
    } ?: TablePageButton(null, "${pageNo + 1}")

    private fun getFormResponse(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): AppResponse {
        val (addEnabled, editEnabled, deleteEnabled) = getFormActionPermissions(
            action = action,
            userConfig = userConfig,
            moduleConfig = moduleConfig,
        )
        val alFormCell = getFormCells(
            action = action,
            userConfig = userConfig,
            moduleConfig = moduleConfig,
            addEnabled = addEnabled,
            editEnabled = editEnabled,
        )

        val alFormButton = getFormButtons(
            action = action,
            userConfig = userConfig,
            moduleConfig = moduleConfig,
            addEnabled = addEnabled,
            editEnabled = editEnabled,
            deleteEnabled = deleteEnabled,
        )

        return AppResponse(
            responseCode = ResponseCode.MODULE_FORM,
            form = FormResponse(
                id = action.id,
                tabCaption = moduleConfig.caption,
                headerData = getFormHeaderData(
                    action = action,
                    userConfig = userConfig,
                    moduleConfig = moduleConfig
                ),
                alFormColumn = emptyList(),
                alFormCell = alFormCell,
                alFormButton = alFormButton,
                prevAction = action.prevAction,
            )
        )
    }

    protected open fun getFormHeaderData(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): HeaderData = HeaderData(
        titles = listOf(
            TitleData(
                action = null,
                text = moduleConfig.caption,
                isBold = true,
            ),
        ),
        rows = emptyList()
    )

    protected abstract fun getFormCells(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        addEnabled: Boolean,
        editEnabled: Boolean,
    ): List<FormBaseCell>

    protected fun fillFormUserCells(
        fieldUserId: String,
        fieldOwnerFullName: String,
        userId: Int?,
        userConfig: ServerUserConfig,
        changeEnabled: Boolean,
        formCells: MutableList<FormBaseCell>,
    ) {
        formCells += FormSimpleCell(
            name = fieldUserId,
            caption = "",
            isEditable = false,
            value = userId.toString(),
        )
        formCells += FormSimpleCell(
            name = fieldOwnerFullName,
            caption = "Владелец",
            isEditable = false,
            value = if (userId == null || userId == 0) {
                "-"
            } else {
                userConfig.fullNames[userId] ?: "(неизвестно)"
            },
            selectorAction = if (changeEnabled) {
                AppAction(
                    type = ActionType.MODULE_TABLE,
                    module = AppModule.USER,
                    isSelectorMode = true,
                    selectorPath = mapOf(
                        UserService.FIELD_ID to fieldUserId,
                        UserService.FIELD_FULL_NAME to fieldOwnerFullName,
                    ),
                    selectorClear = mapOf(
                        fieldUserId to "0",
                        fieldOwnerFullName to "",
                    ),
                )
            } else {
                null
            },
        )
    }

    protected fun getFormFileCellData(fileId: Int?): List<FormFileData> = fileId?.let {
        fileStoreService.getFileList(fileId, fileAccessPeriod.toIntOrNull() ?: 24).map { ref ->
            FormFileData(
                id = fileStoreService.getFileStoreData(ref)?.id ?: -1,
                ref = ref,
                action = AppAction(type = ActionType.FILE, url = fileStoreService.getFileRefUrl(ref)),
                name = "${fileStoreService.getFileName(ref)} [${((fileStoreService.getFileSize(ref) ?: 0) / 1000) + 1} Кб]",
            )
        }
    } ?: emptyList()

    protected open fun getFormButtons(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        addEnabled: Boolean,
        editEnabled: Boolean,
        deleteEnabled: Boolean,
    ): List<FormButton> {

        val alFormButton = mutableListOf<FormButton>()

        val id = action.id

        if (id == null && addEnabled || id != null && editEnabled) {
            alFormButton += FormButton(
                actionType = id?.let { ActionType.FORM_EDIT } ?: ActionType.FORM_ADD,
                withNewData = true,
                name = IconName.SAVE,
                key = FormButtonKey.SAVE,
            )
        }

        if (id != null && deleteEnabled) {
            alFormButton += FormButton(
                actionType = ActionType.FORM_DELETE,
                withNewData = false,
                name = IconName.DELETE,
            )
        }

        alFormButton += FormButton(
            actionType = ActionType.FORM_EXIT,
            withNewData = false,
            name = IconName.EXIT,
            key = FormButtonKey.EXIT,
        )

        return alFormButton
    }

    protected open fun doOtherActionType(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): AppResponse = AppResponse(ResponseCode.LOGON_NEED)

    protected abstract fun getFormActionPermissions(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): Triple<Boolean, Boolean, Boolean>

    open fun formAction(
        sessionId: Long,
        action: AppAction,
        formActionData: Map<String, FormActionData>,
    ): FormActionResponse {
        val sessionData = SpringApp.getSessionData(sessionId) ?: return FormActionResponse(ResponseCode.ERROR)
        val userConfig = sessionData.serverUserConfig ?: return FormActionResponse(ResponseCode.ERROR)
        val moduleConfig = appModuleConfigs[action.module] ?: return FormActionResponse(ResponseCode.ERROR)

        val (addEnabled, editEnabled, deleteEnabled) = getFormActionPermissions(
            action = action,
            userConfig = userConfig,
            moduleConfig = moduleConfig,
        )

        return when (action.type) {
            ActionType.FORM_ADD -> {
                if (addEnabled) {
                    formActionSave(
                        action = action,
                        userConfig = userConfig,
                        moduleConfig = moduleConfig,
                        formActionData = formActionData
                    )
                } else {
                    FormActionResponse(ResponseCode.ERROR)
                }
            }

            ActionType.FORM_EDIT -> {
                if (editEnabled) {
                    formActionSave(
                        action = action,
                        userConfig = userConfig,
                        moduleConfig = moduleConfig,
                        formActionData = formActionData
                    )
                } else {
                    FormActionResponse(ResponseCode.ERROR)
                }
            }

            ActionType.FORM_DELETE -> {
                if (deleteEnabled) {
                    action.id?.let { id ->
                        formActionDelete(userConfig.id, id)
                    } ?: FormActionResponse(ResponseCode.ERROR)
                } else {
                    FormActionResponse(ResponseCode.ERROR)
                }
            }

            else -> {
                formActionExit(action)
            }
        }
    }

    protected abstract fun formActionSave(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        formActionData: Map<String, FormActionData>
    ): FormActionResponse

    protected abstract fun formActionDelete(userId: Int, id: Int): FormActionResponse

    protected open fun formActionExit(action: AppAction): FormActionResponse = FormActionResponse(ResponseCode.OK)

    protected fun isAdminOnly(userConfig: ServerUserConfig): Boolean = userConfig.roles.size == 1 && userConfig.roles.contains(AppRole.ADMIN)

    protected fun formActionSaveFiles(formActionData: FormActionData): Int {
        val fileId = formActionData.fileId ?: fileStoreService.getNextFileId()

        formActionData.addFiles?.forEach { (fromClientId, fileName) ->
            fileStoreService.addFile(fileId, fromClientId, fileName)
        }

        formActionData.fileRemovedIds?.forEach { idForDelete ->
            fileStoreService.deleteFile(fileId, idForDelete)
        }

        return fileId
    }

//--- date/time helper functions

    protected fun getDateEntityDMYString(dateEntity: DateEntity?): String =
        dateEntity?.let { de ->
            if (de.ye != null && de.mo != null && de.da != null) {
                getDateDMYString(de.ye, de.mo, de.da)
            } else {
                "-"
            }
        } ?: "-"

    protected fun getDateTimeEntityDMYHMSString(dateTimeEntity: DateTimeEntity?): String =
        dateTimeEntity?.let { dte ->
            if (dte.ye != null && dte.mo != null && dte.da != null && dte.ho != null && dte.mi != null && dte.se != null) {
                getDateTimeDMYHMSString(dte.ye, dte.mo, dte.da, dte.ho, dte.mi, dte.se)
            } else {
                "-"
            }
        } ?: "-"

}