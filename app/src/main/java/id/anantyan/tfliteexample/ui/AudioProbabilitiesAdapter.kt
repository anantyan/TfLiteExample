package id.anantyan.tfliteexample.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import id.anantyan.tfliteexample.R
import id.anantyan.tfliteexample.databinding.ItemProbabilityAudioBinding
import org.tensorflow.lite.support.label.Category

class AudioProbabilitiesAdapter : ListAdapter<Category, AudioProbabilitiesAdapter.ViewHolder>(Comparator) {

    private object Comparator : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(
            oldItem: Category,
            newItem: Category
        ): Boolean {
            return oldItem.index == newItem.index
        }

        override fun areContentsTheSame(
            oldItem: Category,
            newItem: Category
        ): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProbabilityAudioBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = getItem(position)
        holder.bind(category.label, category.score, category.index)
    }

    class ViewHolder(private val binding: ItemProbabilityAudioBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var primaryProgressColorList: IntArray = binding.root.resources.getIntArray((R.array.colors_progress_primary))
        private var backgroundProgressColorList: IntArray = binding.root.resources.getIntArray((R.array.colors_progress_background))

        fun bind(label: String, score: Float, index: Int) {
            with(binding) {
                labelTextView.text = label
                progressBar.progressBackgroundTintList = ColorStateList.valueOf(backgroundProgressColorList[index % backgroundProgressColorList.size])
                progressBar.progressTintList = ColorStateList.valueOf(primaryProgressColorList[index % primaryProgressColorList.size])
                val newValue = (score * 100).toInt()
                progressBar.setProgress(newValue, true)
            }
        }
    }
}