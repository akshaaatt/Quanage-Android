package com.aemerse.quanage.experiments

import arrow.core.Either
import com.aemerse.quanage.*
import com.aemerse.quanage.model.QAData

class FourierExperiment(interactor: JobInteractor,
                        result: (Either<String, QAData>) -> Unit) : Experiment(interactor, result) {

    override val describe = "Quantum Fourier transform experiment."

    override val qasm = qasm {
        qreg(4)
        creg(4)
        x(0)
        x(2)
        barrier()
        h(0)
        cu1(Math.PI / 2, 1, 0)
        h(1)
        cu1(Math.PI / 4, 2, 0)
        cu1(Math.PI / 2, 2, 1)
        h(2)
        cu1(Math.PI / 8, 3, 0)
        cu1(Math.PI / 4, 3, 1)
        cu1(Math.PI / 2, 3, 2)
        h(3)
        measure()
    }
}