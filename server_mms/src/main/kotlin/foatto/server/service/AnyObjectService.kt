package foatto.server.service

import foatto.core.model.request.FormActionData
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.form.cells.FormBooleanCell
import foatto.server.entity.ObjectEntity
import foatto.server.repository.ActionLogRepository
import foatto.server.repository.DepartmentRepository
import foatto.server.repository.DeviceRepository
import foatto.server.repository.GroupRepository
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorCalibrationRepository
import foatto.server.repository.SensorRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service

@Service
class AnyObjectService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val departmentRepository: DepartmentRepository,
    private val groupRepository: GroupRepository,
    private val sensorRepository: SensorRepository,
    private val sensorCalibrationRepository: SensorCalibrationRepository,
    private val deviceRepository: DeviceRepository,
    private val fileStoreService: FileStoreService,
    private val actionLogRepository: ActionLogRepository,
) : AbstractObjectService(
    entityManager = entityManager,
    objectRepository = objectRepository,
    departmentRepository = departmentRepository,
    groupRepository = groupRepository,
    sensorRepository = sensorRepository,
    sensorCalibrationRepository = sensorCalibrationRepository,
    deviceRepository = deviceRepository,
    fileStoreService = fileStoreService,
    actionLogRepository = actionLogRepository,
    objectType = null,
) {
    companion object {
        private const val FIELD_IS_AUTO_WORK_SHIFT_ENABLED = "isAutoWorkShiftEnabled"
    }

    override fun addFormCells(changeEnabled: Boolean, objectEntity: ObjectEntity?, formCells: MutableList<FormBaseCell>) {
        super.addFormCells(changeEnabled, objectEntity, formCells)

        formCells += FormBooleanCell(
            name = FIELD_IS_AUTO_WORK_SHIFT_ENABLED,
            caption = "Автосоздание рабочих смен",
            isEditable = changeEnabled,
            value = objectEntity?.isAutoWorkShiftEnabled ?: false,
        )
    }

    override fun getAutoWorkShiftEnabledData(formActionData: Map<String, FormActionData>): Boolean =
        formActionData[FIELD_IS_AUTO_WORK_SHIFT_ENABLED]?.booleanValue == true
}