package com.aemerse.quanage.network

import com.aemerse.quanage.model.QAResult

data class QAsm(
        var qasm: String = "",
        val status: QasmStatus? = null,
        val executionId: String = "",
        val result: QAResult? = null
)

enum class QasmStatus {
    DONE, WORK_IN_PROGRESS
}
