package foatto.server.service

import foatto.server.ObjectType
import foatto.server.repository.ActionLogRepository
import foatto.server.repository.DayWorkRepository
import foatto.server.repository.DepartmentRepository
import foatto.server.repository.DeviceRepository
import foatto.server.repository.GroupRepository
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorCalibrationRepository
import foatto.server.repository.SensorRepository
import foatto.server.repository.WorkShiftRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service

@Service
class MobileObjectService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val departmentRepository: DepartmentRepository,
    private val groupRepository: GroupRepository,
    private val sensorRepository: SensorRepository,
    private val sensorCalibrationRepository: SensorCalibrationRepository,
    private val deviceRepository: DeviceRepository,
    private val dayWorkRepository: DayWorkRepository,
    private val workShiftRepository: WorkShiftRepository,
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
    dayWorkRepository = dayWorkRepository,
    workShiftRepository = workShiftRepository,
    fileStoreService = fileStoreService,
    actionLogRepository = actionLogRepository,
    objectType = ObjectType.MOBILE,
)