package com.aemerse.quanage

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import arrow.core.Either
import com.aemerse.quanage.model.QAData
import com.aemerse.quanage.model.QADevice
import com.aemerse.quanage.network.IbmProvider
import com.aemerse.quanage.databinding.ActivityMainBinding
import com.aemerse.quanage.experiments.BellExperiment
import com.aemerse.quanage.experiments.FourierExperiment
import com.aemerse.quanage.experiments.GhzExperiment
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext


class MainActivity : AppCompatActivity(), CoroutineScope {
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext get() = job + Dispatchers.Main

    private val provider = IbmProvider()
    private val interactor = JobInteractor(provider)
    private val graph = Graph()

    private val bell = BellExperiment(interactor, ::result)
    private val fourier = FourierExperiment(interactor, ::result)
    private val ghz = GhzExperiment(interactor, ::result)
    private val experiments = listOf(bell, fourier, ghz)

    private var lastData: QAData? = null
    private var lastExperiment = 0
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        job = Job()

        setGraphDimension()

//        lastData = QAData(counts = mapOf(Pair("00000", 300), Pair("00001", 700)))//Test
        enableUx(false)
        binding.fab.hide()

        binding.btConnected.setOnClickListener { connect() }
        binding.btRun.setOnClickListener { runExperiment() }
        binding.fab.setOnClickListener { showHistogram() }
        binding.tvCode.movementMethod = ScrollingMovementMethod()

        initExperimentSpinner()
    }

    override fun onDestroy() {
        super.onDestroy()
        interactor.onDestroy()
    }

    private fun initExperimentSpinner() {
        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, experiments)
        with(binding.spExperiment) {
            adapter = arrayAdapter
            setSelection(0)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position == lastExperiment) return
                    lastExperiment = position
                    binding.tvResult.text = ""
                    lastData = null
                    binding.fab.hide()
                    console(experiments[position].qasm.qasm)
                }
            }
        }
    }

    private fun initDeviceSpinner(devices: List<QADevice>) {
        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, devices)
        with(binding.spDevice) {
            adapter = arrayAdapter
            setSelection(devices.indexOf(devices.first { it.simulator }))
        }
    }

    private fun connect() {
        binding.btConnected.isEnabled = false
        binding.btConnected.setText(R.string.connecting)
        launch(Dispatchers.Main) {
            if (!interactor.init(getString(R.string.ibm_api_token))) {
                binding.btConnected.setText(R.string.disconnected)
                initDeviceSpinner(emptyList())
                enableUx(false)
                binding.btConnected.isEnabled = true
                return@launch
            }
            withContext(Dispatchers.Main) {
                initDeviceSpinner(interactor.devices)
                enableUx(true)
                binding.btConnected.setText(R.string.connected)
            }
        }
    }

    private fun runExperiment() {
        enableUx(false)
        val experiment = experiments[binding.spExperiment.selectedItemPosition]
        val device = interactor.devices[binding.spDevice.selectedItemPosition]
        launch(Dispatchers.IO) { experiment.run(device) }
    }

    private fun enableUx(enable: Boolean) {
        binding.btRun.isEnabled = enable
        binding.spDevice.isEnabled = enable
    }

    @SuppressLint("SetTextI18n")
    private fun console(text: String) {
        binding.tvCode.text = "$text\n\n"
    }

    @SuppressLint("SetTextI18n")
    private fun result(result: Either<String, QAData>) {
        launch(Dispatchers.Main) {
            result.fold({ error ->
                binding.barResult.visibility = INVISIBLE
                binding.tvResult.text = "$error\n"
                enableUx(error != "Running") //TODO Create a enum or sealed class of errors
            }, { data ->
                enableUx(true)
                binding.fab.show()
                binding.tvResult.text = "$data\n"
                lastData = data
            })
        }
    }

    private fun showHistogram() {
        val data = lastData ?: return
        binding.barResult.visibility = if (binding.barResult.visibility == INVISIBLE) {
            graph.drawResult(binding.barResult, data)
            binding.fab.rotation = 90F
            VISIBLE
        } else {
            binding.fab.rotation = 0F
            INVISIBLE
        }
    }

    private fun setGraphDimension() {
        binding.barResult.onGlobalLayout {
            with(binding.barResult) {
                val params = layoutParams
                params.width = height
                params.height = width
                layoutParams = params
                translationY = -height / 2f
            }
        }
    }
}
