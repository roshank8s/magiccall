package com.magiccall.voicechanger.ui.effects

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.magiccall.voicechanger.R
import com.magiccall.voicechanger.model.VoicePreset

class EffectsAdapter(
    private val onEffectClicked: (VoicePreset) -> Unit
) : ListAdapter<VoicePreset, EffectsAdapter.EffectViewHolder>(EffectDiffCallback()) {

    private var selectedId: String? = null

    fun setSelectedId(id: String?) {
        val oldId = selectedId
        selectedId = id
        // Refresh the old and new selected items
        currentList.forEachIndexed { index, preset ->
            if (preset.id == oldId || preset.id == id) {
                notifyItemChanged(index)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EffectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_voice_effect, parent, false)
        return EffectViewHolder(view)
    }

    override fun onBindViewHolder(holder: EffectViewHolder, position: Int) {
        holder.bind(getItem(position), selectedId)
    }

    inner class EffectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: CardView = itemView.findViewById(R.id.effectCard)
        private val icon: ImageView = itemView.findViewById(R.id.effectIcon)
        private val name: TextView = itemView.findViewById(R.id.effectName)
        private val cost: TextView = itemView.findViewById(R.id.effectCost)

        fun bind(preset: VoicePreset, selectedId: String?) {
            name.text = preset.displayName
            cost.text = "${preset.creditCost} cr"

            val isSelected = preset.id == selectedId
            val context = itemView.context

            if (isSelected) {
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.effect_selected))
                card.cardElevation = 8f
            } else {
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.effect_normal))
                card.cardElevation = 4f
            }

            // Map icon resource
            val iconResId = getIconResource(preset.iconRes)
            icon.setImageResource(iconResId)

            card.setOnClickListener { onEffectClicked(preset) }
        }

        private fun getIconResource(iconName: String): Int {
            return when (iconName) {
                "ic_female" -> R.drawable.ic_female
                "ic_male" -> R.drawable.ic_male
                "ic_child" -> R.drawable.ic_child
                "ic_helium" -> R.drawable.ic_helium
                "ic_monster" -> R.drawable.ic_monster
                "ic_robot" -> R.drawable.ic_robot
                "ic_echo" -> R.drawable.ic_echo
                "ic_reverb" -> R.drawable.ic_reverb
                "ic_whisper" -> R.drawable.ic_whisper
                "ic_tremor" -> R.drawable.ic_tremor
                "ic_alien" -> R.drawable.ic_alien
                "ic_old" -> R.drawable.ic_old
                else -> R.drawable.ic_mic
            }
        }
    }

    class EffectDiffCallback : DiffUtil.ItemCallback<VoicePreset>() {
        override fun areItemsTheSame(oldItem: VoicePreset, newItem: VoicePreset) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: VoicePreset, newItem: VoicePreset) =
            oldItem.id == newItem.id && oldItem.displayName == newItem.displayName
    }
}
