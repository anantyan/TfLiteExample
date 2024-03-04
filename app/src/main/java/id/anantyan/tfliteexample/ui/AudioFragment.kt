package id.anantyan.tfliteexample.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import id.anantyan.tfliteexample.R
import id.anantyan.tfliteexample.databinding.FragmentAudioBinding
import id.anantyan.tfliteexample.helper.AudioClassificationHelper
import org.tensorflow.lite.support.label.Category

class AudioFragment : Fragment(), AudioClassificationHelper.AudioClassificationListener {

    private var _fragmentBinding: FragmentAudioBinding? = null
    private val fragmentAudioBinding get() = _fragmentBinding!!
    private val adapter by lazy { AudioProbabilitiesAdapter() }

    private lateinit var audioHelper: AudioClassificationHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentBinding = FragmentAudioBinding.inflate(inflater, container, false)
        return fragmentAudioBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentAudioBinding.recyclerView.adapter = adapter
        audioHelper = AudioClassificationHelper(
            requireContext(),
            this
        )

        fragmentAudioBinding.bottomSheetLayout.modelSelector.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.yamnet -> {
                    audioHelper.stopAudioClassification()
                    audioHelper.currentModel = AudioClassificationHelper.YAMNET_MODEL
                    audioHelper.initClassifier()
                }

                R.id.speech_command -> {
                    audioHelper.stopAudioClassification()
                    audioHelper.currentModel = AudioClassificationHelper.SPEECH_COMMAND_MODEL
                    audioHelper.initClassifier()
                }
                R.id.noise_model -> {
                    audioHelper.stopAudioClassification()
                    audioHelper.currentModel = AudioClassificationHelper.NOISE
                    audioHelper.initClassifier()
                }
                R.id.bird_model -> {
                    audioHelper.stopAudioClassification()
                    audioHelper.currentModel = AudioClassificationHelper.BIRD_MODEL
                    audioHelper.initClassifier()
                }
            }
        }

        fragmentAudioBinding.bottomSheetLayout.spinnerOverlap.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                audioHelper.stopAudioClassification()
                audioHelper.overlap = 0.25f * position
                audioHelper.startAudioClassification()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                // no op
            }
        }

        fragmentAudioBinding.bottomSheetLayout.resultsMinus.setOnClickListener {
            if (audioHelper.numOfResults > 1) {
                audioHelper.numOfResults--
                audioHelper.stopAudioClassification()
                audioHelper.initClassifier()
                fragmentAudioBinding.bottomSheetLayout.resultsValue.text =
                    audioHelper.numOfResults.toString()
            }
        }

        fragmentAudioBinding.bottomSheetLayout.resultsPlus.setOnClickListener {
            if (audioHelper.numOfResults < 5) {
                audioHelper.numOfResults++
                audioHelper.stopAudioClassification()
                audioHelper.initClassifier()
                fragmentAudioBinding.bottomSheetLayout.resultsValue.text =
                    audioHelper.numOfResults.toString()
            }
        }

        fragmentAudioBinding.bottomSheetLayout.thresholdMinus.setOnClickListener {
            if (audioHelper.classificationThreshold >= 0.2) {
                audioHelper.stopAudioClassification()
                audioHelper.classificationThreshold -= 0.1f
                audioHelper.initClassifier()
                fragmentAudioBinding.bottomSheetLayout.thresholdValue.text =
                    String.format("%.2f", audioHelper.classificationThreshold)
            }
        }

        fragmentAudioBinding.bottomSheetLayout.thresholdPlus.setOnClickListener {
            if (audioHelper.classificationThreshold <= 0.8) {
                audioHelper.stopAudioClassification()
                audioHelper.classificationThreshold += 0.1f
                audioHelper.initClassifier()
                fragmentAudioBinding.bottomSheetLayout.thresholdValue.text =
                    String.format("%.2f", audioHelper.classificationThreshold)
            }
        }

        fragmentAudioBinding.bottomSheetLayout.threadsMinus.setOnClickListener {
            if (audioHelper.numThreads > 1) {
                audioHelper.stopAudioClassification()
                audioHelper.numThreads--
                fragmentAudioBinding.bottomSheetLayout.threadsValue.text = audioHelper
                    .numThreads
                    .toString()
                audioHelper.initClassifier()
            }
        }

        fragmentAudioBinding.bottomSheetLayout.threadsPlus.setOnClickListener {
            if (audioHelper.numThreads < 4) {
                audioHelper.stopAudioClassification()
                audioHelper.numThreads++
                fragmentAudioBinding.bottomSheetLayout.threadsValue.text = audioHelper
                    .numThreads
                    .toString()
                audioHelper.initClassifier()
            }
        }

        fragmentAudioBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                audioHelper.stopAudioClassification()
                audioHelper.currentDelegate = position
                audioHelper.initClassifier()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                /* no op */
            }
        }

        fragmentAudioBinding.bottomSheetLayout.spinnerOverlap.setSelection(2, false)
        fragmentAudioBinding.bottomSheetLayout.spinnerDelegate.setSelection(0, false)
    }

    override fun onResume() {
        super.onResume()
        /*if (!MainActivity.hasPermissions(requireContext())) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", requireContext().packageName, null)
            startActivity(intent)
        }*/

        if (::audioHelper.isInitialized) {
            audioHelper.startAudioClassification()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::audioHelper.isInitialized) {
            audioHelper.stopAudioClassification()
        }
    }

    override fun onDestroyView() {
        _fragmentBinding = null
        super.onDestroyView()
    }

    override fun onResult(results: List<Category>, inferenceTime: Long) {
        fragmentAudioBinding.bottomSheetLayout.inferenceTimeVal.text = String.format("%d ms", inferenceTime)
        adapter.submitList(results)
    }

    override fun onError(error: String) {
        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        adapter.submitList(emptyList())
    }
}