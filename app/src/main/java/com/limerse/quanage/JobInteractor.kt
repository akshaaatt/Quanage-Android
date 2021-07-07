package com.limerse.quanage

import arrow.core.Either
import com.limerse.quanage.model.QADevice
import com.limerse.quanage.model.QAError
import com.limerse.quanage.model.QAJob
import com.limerse.quanage.network.IbmProvider
import com.limerse.quanage.network.QAsm
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class JobInteractor(private val qex: IbmProvider) : CoroutineScope {

    private val coroutineJob = Job()
    override val coroutineContext: CoroutineContext = Dispatchers.IO + coroutineJob +
            CoroutineExceptionHandler { _, exception -> println("Caught $exception") }

    private var token: String = ""
    var devices: List<QADevice> = emptyList()

    suspend fun init(apiKey: String): Boolean {
        if (devices.isNotEmpty()) return true

        async {
            when (val result = qex.login(apiKey)) {
                is Either.Right-> token = result.value
            }
        }.await()
        if (token.isEmpty()) return false

        async {
            when (val result = qex.getDevices(token)) {
                is Either.Right -> devices = result.value
            }
        }.await()

        return devices.isNotEmpty()
    }

    suspend fun submitJob(device: QADevice,
                  shots: Int = 1,
                  maxCredits: Int = 1,
                  vararg sources: QAsm): QAJob {
        val job = QAJob(backend = device, shots = shots, maxCredits = maxCredits, qasms = listOf(*sources))

        return async {
            when (val result = qex.submitJob(token, job)) {
                is Either.Left -> {
                    QAJob(error = QAError(message = result.value))
                }
                is Either.Right -> result.value
            }
        }.await()
    }

    suspend fun onStatus(job: QAJob,
                 timeoutSeconds: Int,
                 onCompleted: (QAJob) -> Unit,
                 onError: (String) -> Unit) {
        val repeatJob = Job(coroutineJob)
        launch(repeatJob) {
            repeat(timeoutSeconds) {
                when (val result = qex.receiveJob(token, job)) {
                    is Either.Left -> {
                        onError(result.value)
                        delay(TimeUnit.SECONDS.toMillis(1))
                    }
                    is Either.Right -> {
                        onCompleted(result.value)
                        repeatJob.cancel()
                    }
                }
            }
        }
    }

    fun onDestroy() {
        coroutineJob.cancel()
    }
}