package foatto.server.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "MMS_work_shift")
class WorkShiftEntity(

    @Id
    val id: Int,

    @Column(name = "user_id")
    val userId: Int?,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "object_id")
    var obj: ObjectEntity?,

    @Column(name = "beg_dt")
    var begTime: Int?,

    @Column(name = "end_dt")
    var endTime: Int?,

    //!!! для совместимости со старой версией

    @Column(name = "beg_dt_fact")
    var begTimeFact: Int?,

    @Column(name = "end_dt_fact")
    var endTimeFact: Int?,

    @Column(name = "worker_id")
    val workerId: Int = 0,

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WorkShiftEntity) return false

        if (userId != other.userId) return false
        if (begTime != other.begTime) return false
        if (endTime != other.endTime) return false
        if (obj != other.obj) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId ?: 0
        result = 31 * result + (begTime ?: 0)
        result = 31 * result + (endTime ?: 0)
        result = 31 * result + (obj?.hashCode() ?: 0)
        return result
    }
}