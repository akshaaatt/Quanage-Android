package com.aemerse.quanage.model

import com.aemerse.quanage.network.IbmProvider
import com.aemerse.quanage.network.QAsm
import java.util.*

data class QAJob(
        var backend: QADevice? = null,
        var shots: Int = 0,
        var maxCredits: Int = 0,
        val qasms: List<QAsm>? = null,
        val status: StatusEnum? = null,
        val usedCredits: Int = 0,
        val creationDate: Date? = null,
        val id: String = "",
        val userId: String = "",
        var deleted: Boolean = false,
        var calibration: QACalibration? = null,
        var error: QAError? = null,
        @Transient
        var api: IbmProvider? = null) {
    override fun toString()= "$backend, id:$id, status:$status"
}

enum class StatusEnum {
    RUNNING, COMPLETED
}