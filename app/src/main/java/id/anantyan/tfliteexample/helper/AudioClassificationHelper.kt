package id.anantyan.tfliteexample.helper

import android.content.Context
import android.media.AudioRecord
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.task.core.BaseOptions
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class AudioClassificationHelper(
    private val context: Context,
    private val listener: AudioClassificationListener,
    var currentModel: String = YAMNET_MODEL,
    var classificationThreshold: Float = DISPLAY_THRESHOLD,
    var overlap: Float = DEFAULT_OVERLAP_VALUE,
    var numOfResults: Int = DEFAULT_NUM_OF_RESULTS,
    var currentDelegate: Int = 0,
    var numThreads: Int = 4
) {
    private lateinit var classifier: AudioClassifier
    private lateinit var tensorAudio: TensorAudio
    private lateinit var recorder: AudioRecord
    private lateinit var executor: ScheduledThreadPoolExecutor

    private val classifyRunnable = Runnable {
        classifyAudio()
    }

    init {
        initClassifier()
    }

    fun initClassifier() {
        // Mengatur opsi deteksi umum, misalnya jumlah utas yang digunakan
        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)

        // Gunakan perangkat keras yang ditentukan untuk menjalankan model. Setelan default ke CPU.
        // Dapat juga menggunakan delegasi GPU, tetapi ini mengharuskan pengklasifikasi dibuat
        // pada utas yang sama yang menggunakan pengklasifikasi, yang berada di luar cakupan ini
        // desain sampel.
        when (currentDelegate) {
            DELEGATE_CPU -> {
                // Default
            }

            DELEGATE_NNAPI -> {
                baseOptionsBuilder.useNnapi()
            }
        }

        // Mengkonfigurasi serangkaian parameter untuk pengklasifikasi dan hasil apa yang akan dikembalikan.
        val options = AudioClassifier.AudioClassifierOptions.builder()
            .setScoreThreshold(classificationThreshold)
            .setMaxResults(numOfResults)
            .setBaseOptions(baseOptionsBuilder.build())
            .build()

        try {
            // Membuat pengklasifikasi dan objek pendukung yang diperlukan
            classifier = AudioClassifier.createFromFileAndOptions(context, currentModel, options)
            tensorAudio = classifier.createInputTensorAudio()
            recorder = classifier.createAudioRecord()
            startAudioClassification()
        } catch (e: IllegalStateException) {
            listener.onError("Pengklasifikasi Audio gagal melakukan inisialisasi. Lihat log kesalahan untuk rinciannya $e")
            Log.e("AudioClassification", "TFLite gagal memuat dengan kesalahan: " + e.message)
        }
    }

    fun startAudioClassification() {
        if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            return
        }

        recorder.startRecording()
        executor = ScheduledThreadPoolExecutor(1)

        // Setiap model akan mengharapkan panjang perekaman audio tertentu. Rumus ini menghitung bahwa
        // panjang menggunakan ukuran buffer input dan laju sampel format tensor.
        // Sebagai contoh, YAMNET mengharapkan rekaman berdurasi 0,975 detik.
        // Ini harus dalam milidetik untuk menghindari desimal penurunan nilai panjang yang diperlukan.
        val lengthInMilliSeconds = ((classifier.requiredInputBufferSize * 1.0f) / classifier.requiredTensorAudioFormat.sampleRate) * 1000
        val interval = (lengthInMilliSeconds * (1 - overlap)).toLong()

        executor.scheduleAtFixedRate(
            classifyRunnable,
            0,
            interval,
            TimeUnit.MILLISECONDS
        )
    }

    private fun classifyAudio() {
        tensorAudio.load(recorder)
        var inferenceTime = SystemClock.uptimeMillis()
        val output = classifier.classify(tensorAudio)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
        output.forEach { classification ->
            listener.onResult(classification.categories, inferenceTime)
        }
    }

    fun stopAudioClassification() {
        recorder.stop()
        executor.shutdownNow()
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_NNAPI = 1
        const val DISPLAY_THRESHOLD = 0.1f
        const val DEFAULT_NUM_OF_RESULTS = 1
        const val DEFAULT_OVERLAP_VALUE = 0.5f
        const val YAMNET_MODEL = "yamnet.tflite"
        const val SPEECH_COMMAND_MODEL = "speech.tflite"
        const val NOISE = "noise_model.tflite"
        const val BIRD_MODEL = "birds_model.tflite"
    }

    interface AudioClassificationListener {
        fun onError(error: String)
        fun onResult(results: List<Category>, inferenceTime: Long)
    }
}