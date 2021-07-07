package com.limerse.quanage.experiments

import arrow.core.Either
import com.limerse.quanage.JobInteractor
import com.limerse.quanage.model.QAData
import com.limerse.quanage.model.QADevice
import com.limerse.quanage.network.QAsm

abstract class Experiment(private val interactor: JobInteractor,
                          private val result: (Either<String, QAData>) -> Unit) {

    abstract val describe: String
    abstract val qasm: QAsm

    suspend fun run(device: QADevice) {

        val jobQ = interactor.submitJob(
                device = device,
                shots = 1024,
                maxCredits = 1,
                sources = arrayOf(qasm)
        )

        if (jobQ.error != null) {
            result(Either.Left(jobQ.error.toString()))
            return
        }

        interactor.onStatus(jobQ, 60,
                { finishedJob ->
                    finishedJob.qasms?.forEach { qasm ->
                        result(Either.Right(qasm.result?.data ?: QAData()))
                    }
                },
                { error ->
                    result(Either.Left(error))
                }
        )
    }

    override fun toString(): String = describe
}