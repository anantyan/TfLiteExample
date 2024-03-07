package id.anantyan.tfliteexample.helper

/**
 * Created by Arya Rezza Anantya on 07/03/2024.
 */
object NoiseHelper {
    private val noiseMap = mapOf(
        "Silence" to "Non Noise"
    )

    fun getNoiseLabel(word: String?): String {
        return noiseMap[word] ?: "Noise"
    }
}