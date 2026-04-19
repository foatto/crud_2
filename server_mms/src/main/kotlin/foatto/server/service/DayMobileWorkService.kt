package foatto.server.service

import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.response.table.TableCaption
import foatto.core.model.response.table.TablePageButton
import foatto.core.model.response.table.TableRow
import foatto.core.model.response.table.cell.TableBaseCell
import foatto.core.model.response.table.cell.TableCellAlign
import foatto.core.model.response.table.cell.TableCellBackColorType
import foatto.core.model.response.table.cell.TableSimpleCell
import foatto.core.util.getSplittedDouble
import foatto.core.util.getTimeZone
import foatto.core_mms.i18n.LocalizedMMSMessages
import foatto.core_mms.i18n.getLocalizedMMSMessage
import foatto.server.ObjectType
import foatto.server.checkRowPermission
import foatto.server.entity.DayWorkEntity
import foatto.server.getEnabledUserIds
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.ActionLogRepository
import foatto.server.repository.DayWorkRepository
import foatto.server.repository.ObjectRepository
import foatto.server.repository.UserRepository
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Service
class DayMobileWorkService(
    private val dayWorkRepository: DayWorkRepository,
    private val objectRepository: ObjectRepository,
    private val calcService: CalcService,
    private val userRepository: UserRepository,
    private val actionLogRepository: ActionLogRepository,
    private val fileStoreService: FileStoreService,
) : AbstractDayWorkService(
    dayWorkRepository = dayWorkRepository,
    objectRepository = objectRepository,
    calcService = calcService,
    userRepository = userRepository,
    actionLogRepository = actionLogRepository,
    fileStoreService = fileStoreService,
) {

    override fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<TableCaption> {
        val alColumnInfo = mutableListOf<Pair<String?, String>>()

        alColumnInfo += null to "" // by date group
        alColumnInfo += null to "" // userId
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.OBJECT, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.MILEAGE, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.EQUIPMENT, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.OPERATION, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.FUEL_METERS, userConfig.lang)
        alColumnInfo += null to getLocalizedMMSMessage(LocalizedMMSMessages.FUEL_CONSUMPTION, userConfig.lang)

        return getTableColumnCaptionActions(
            action = action,
            columnInfos = alColumnInfo,
        )
    }

    override fun fillTableGridData(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        tableCells: MutableList<TableBaseCell>,
        tableRows: MutableList<TableRow>,
        pageButtons: MutableList<TablePageButton>,
    ): Int? {
        var currentRowNo: Int? = null
        var row = 0
        var dataRow = 0

        val timeZone = getTimeZone(userConfig.timeOffset)

        val pageRequest = getTableSortedPageRequest(
            action,
            Sort.Order(Sort.Direction.DESC, FIELD_DAY_YE),
            Sort.Order(Sort.Direction.DESC, FIELD_DAY_MO),
            Sort.Order(Sort.Direction.DESC, FIELD_DAY_DA),
            Sort.Order(Sort.Direction.ASC, FIELD_OBJECT_NAME),
        )
        val findText = action.findText?.trim() ?: ""

        val parentUserIds = getParentUserIds(action)

        val parentObjectId = getParentObjectId(action)
        val parentObjectEntity = parentObjectId?.let {
            objectRepository.findByIdOrNull(parentObjectId)
        }

        val enabledUserIds = getEnabledUserIds(
            module = action.module,
            actionType = action.type,
            relatedUserIds = userConfig.relatedUserIds,
            roles = userConfig.roles
        )
        val page: Page<DayWorkEntity> = dayWorkRepository.findByParentUserIdAndObjAndUserIdInAndFilter(
            parentUserIds = parentUserIds,
            obj = parentObjectEntity,
            objectType = ObjectType.MOBILE,
            userIds = enabledUserIds,
            findText = findText,
            begDateTime = action.begDateTimeValue ?: -1,
            endDateTime = action.endDateTimeValue ?: -1,
            pageRequest = pageRequest,
        )

        fillTablePageButtons(userConfig, action, page.totalPages, pageButtons)
        val dayWorkEntities = page.content

        val groupColSpan = getTableColumnCaptions(action, userConfig).size
        var prevGroupName: String? = null
//var runTime = 0
//var worksTime = 0
//var usingsTime = 0
//var energosTime = 0
//var liquidLevelsTime = 0
//var temperaturesTime = 0
//var densitiesTime = 0
        for (dayWorkEntity in dayWorkEntities) {

            val objectEntity = dayWorkEntity.obj ?: continue
            val dayEntity = dayWorkEntity.day ?: continue
            val ye = dayEntity.ye ?: continue
            val mo = dayEntity.mo ?: continue
            val da = dayEntity.da ?: continue

            val begTime = LocalDateTime(ye, mo, da, 0, 0).toInstant(timeZone).epochSeconds.toInt()
            val endTime = begTime + 86_400

//var bt = getCurrentTimeInt()
            val run = calcService.calcRun(objectEntity, begTime, endTime)
//runTime += getCurrentTimeInt() - bt
//bt = getCurrentTimeInt()
            val works = calcService.calcWorks(objectEntity, begTime, endTime).sortedBy { wcd -> wcd.sensorEntity.descr ?: "-" }
//worksTime += getCurrentTimeInt() - bt
//bt = getCurrentTimeInt()
            val usings = calcService.calcUsings(objectEntity, begTime, endTime).sortedBy { ccd -> ccd.sensorEntity.descr ?: "-" }
//usingsTime += getCurrentTimeInt() - bt

            var col = 0

            val rowOwnerShortName = userConfig.shortNames[dayWorkEntity.userId]
            val rowOwnerFullName = userConfig.fullNames[dayWorkEntity.userId]

            val isFormEnabled = checkRowPermission(
                module = action.module,
                actionType = ActionType.MODULE_FORM,
                rowUserRelation = userConfig.relatedUserIds[dayWorkEntity.userId],
                userRoles = userConfig.roles
            )

            val groupName = getDateEntityDMYString(dayEntity)
            if (prevGroupName != groupName) {
                tableCells += TableSimpleCell(
                    row = row,
                    col = col,
                    colSpan = groupColSpan,
                    dataRow = dataRow,
                    name = groupName,
                    backColorType = TableCellBackColorType.GROUP_0,
                    isBoldText = true,
                )
                prevGroupName = groupName
                tableRows += TableRow()
                row++
                dataRow++
            }
            tableCells += TableSimpleCell(row = row, col = col++, dataRow = dataRow, name = "", backColorType = TableCellBackColorType.GROUP_0)

            tableCells += getTableUserNameCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                userConfig = userConfig,
                rowUserId = dayWorkEntity.userId,
                rowOwnerShortName = rowOwnerShortName,
                rowOwnerFullName = rowOwnerFullName
            )

            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = (objectEntity.name ?: "-") +
                        "\n${objectEntity.model ?: "-"}" +
                        "\n${objectEntity.department?.name ?: "-"}" +
                        "\n${objectEntity.group?.name ?: "-"}"
            )

            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = "${getSplittedDouble(run / 1000.0)} ${getLocalizedMMSMessage(LocalizedMMSMessages.UNIT_KM, userConfig.lang)}",
                align = TableCellAlign.LEFT,
            )

            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = works.joinToString("\n") { wcd -> wcd.sensorEntity.descr?.let { descr -> "$descr = " } ?: "-" },
                align = TableCellAlign.RIGHT,
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = works.joinToString("\n") { wcd -> "${getSplittedDouble(wcd.onTime / 3600.0)} ${getLocalizedMMSMessage(LocalizedMMSMessages.UNIT_HOUR, userConfig.lang)}" },
                align = TableCellAlign.LEFT,
            )

            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = usings.joinToString("\n") { ccd -> ccd.sensorEntity.descr?.let { descr -> "$descr = " } ?: "-" },
                align = TableCellAlign.RIGHT,
            )
            tableCells += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = dataRow,
                name = usings.joinToString("\n") { ccd -> getSplittedDouble(ccd.value) + (ccd.sensorEntity.dim ?: "") },
                align = TableCellAlign.LEFT,
            )

            val formOpenAction = action.copy(
                type = ActionType.MODULE_FORM,
                id = dayWorkEntity.id,
            )

            val popupDatas = getTablePopupDatas(
                userConfig = userConfig,
                objectId = objectEntity.id,
                begTime = begTime,
                endTime = endTime,
                isFormEnabled = isFormEnabled,
                formOpenAction = formOpenAction,
            )

            tableRows += TableRow(
                rowAction = if (isFormEnabled) {
                    formOpenAction
                } else {
                    null
                },
                isRowUrlInNewTab = false,
                tablePopups = popupDatas,
            )

            if (dayWorkEntity.id == action.id) {
                currentRowNo = row
            }

            //if (userConfig.isWideScreen) {
                row++
            //}
            dataRow++
        }
//println("run = $runTime")
//println("works = $worksTime")
//println("usings = $usingsTime")
        /*
        run = ?
        works = 12 / 4 = 3
        usings = 8 / 4 = 2
        */
        return currentRowNo
    }


}