package foatto.server.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "SYSTEM_action_log")
class ActionLogEntity(

    @Id
    val id: Int,

    @Column(name = "user_id")
    val userId: Int?,

    @Column(name = "ontime")
    val onTime: Int?,

    val type: String?,

    val module: String?,

    @Column(name = "record_id")
    val recordId: Int?,

    @Column(name = "parent_module")
    val parentModule: String?,

    @Column(name = "parent_id")
    val parentId: Int?,

    val action: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActionLogEntity) return false

        if (userId != other.userId) return false
        if (onTime != other.onTime) return false
        if (recordId != other.recordId) return false
        if (parentId != other.parentId) return false
        if (type != other.type) return false
        if (module != other.module) return false
        if (parentModule != other.parentModule) return false
        if (action != other.action) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId ?: 0
        result = 31 * result + (onTime ?: 0)
        result = 31 * result + (recordId ?: 0)
        result = 31 * result + (parentId ?: 0)
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + (module?.hashCode() ?: 0)
        result = 31 * result + (parentModule?.hashCode() ?: 0)
        result = 31 * result + (action?.hashCode() ?: 0)
        return result
    }
}