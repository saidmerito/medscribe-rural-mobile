package dj.medscriberural.standalone.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dj.medscriberural.standalone.R
import dj.medscriberural.standalone.data.RegisterEntry

class RegisterEntryAdapter(
    private val onClick: (RegisterEntry) -> Unit
) : ListAdapter<RegisterEntry, RegisterEntryAdapter.VH>(DIFF) {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.textTitle)
        val subtitle: TextView = itemView.findViewById(R.id.textSubtitle)
        val status: TextView = itemView.findViewById(R.id.textStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_register_entry, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val entry = getItem(position)
        val photoLabel = entry.photoPath.substringAfterLast("/")
        holder.title.text = listOfNotNull(entry.sexe, entry.age?.let { "$it ans" })
            .joinToString(" · ")
            .ifBlank { "Ligne ${entry.rowIndex + 1}" }
        holder.subtitle.text = listOfNotNull(entry.uniteDeService, entry.motifHospitalisation, entry.adresse)
            .joinToString(" · ")
            .ifBlank { photoLabel }
        holder.status.text = when (entry.status) {
            RegisterEntry.STATUS_PENDING -> "⏳ extraction en cours…"
            RegisterEntry.STATUS_EXTRACTED -> "✅ $photoLabel (ligne ${entry.rowIndex + 1})"
            RegisterEntry.STATUS_ERROR -> "❌ ${entry.errorMessage ?: "erreur"}"
            RegisterEntry.STATUS_VALIDATED -> "☑️ validé"
            else -> entry.status
        }
        holder.itemView.setOnClickListener { onClick(entry) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<RegisterEntry>() {
            override fun areItemsTheSame(a: RegisterEntry, b: RegisterEntry) = a.id == b.id
            override fun areContentsTheSame(a: RegisterEntry, b: RegisterEntry) = a == b
        }
    }
}
