package foatto.server.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "MMS_device_manage")
class DeviceManageEntity(
    @Id
    val id: Int,

    @Column(name = "user_id")
    val userId: Int?,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "device_id")
    var device: DeviceEntity?,

    val descr: String?,

    @Column(name = "cmd")
    val command: String?,

    @Column(name = "file_id")
    val fileId: Int?,

    @Column(name = "create_time")
    val createTime: Int?,

    @Column(name = "edit_time")
    val editTime: Int?,

    @Column(name = "send_time")
    var sendTime: Int?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeviceManageEntity) return false

        if (userId != other.userId) return false
        if (fileId != other.fileId) return false
        if (createTime != other.createTime) return false
        if (editTime != other.editTime) return false
        if (sendTime != other.sendTime) return false
        if (device != other.device) return false
        if (descr != other.descr) return false
        if (command != other.command) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId ?: 0
        result = 31 * result + (fileId ?: 0)
        result = 31 * result + (createTime ?: 0)
        result = 31 * result + (editTime ?: 0)
        result = 31 * result + (sendTime ?: 0)
        result = 31 * result + (device?.hashCode() ?: 0)
        result = 31 * result + (descr?.hashCode() ?: 0)
        result = 31 * result + (command?.hashCode() ?: 0)
        return result
    }
}