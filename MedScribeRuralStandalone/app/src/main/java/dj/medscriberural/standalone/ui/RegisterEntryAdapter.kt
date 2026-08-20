package dj.medscriberural.standalone.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dj.medscriberural.standalone.R
import dj.medscriberural.standalone.data.RegisterEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Un item de la liste : soit l'en-tête d'une page (nom photo + colonnes),
 * soit une ligne de données extraite de cette page. */
sealed class TableListItem {
    data class PhotoHeader(val photoPath: String, val statusSummary: String) : TableListItem()
    data class Row(val entry: RegisterEntry) : TableListItem()
}

class RegisterEntryAdapter(
    private val onRowClick: (RegisterEntry) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<TableListItem> = emptyList()

    /** Regroupe les fiches par photo (une page = un tableau), triées par
     * date décroissante, avec les lignes dans l'ordre du registre papier. */
    fun submitEntries(entries: List<RegisterEntry>) {
        val grouped = entries.groupBy { it.photoPath }
        // Conserve l'ordre "photo la plus récente en premier" en se basant
        // sur la fiche la plus récente de chaque groupe.
        val orderedPhotos = grouped.entries.sortedByDescending { (_, rows) ->
            rows.maxOf { it.createdAtMillis }
        }

        val newItems = mutableListOf<TableListItem>()
        for ((photoPath, rows) in orderedPhotos) {
            val sorted = rows.sortedBy { it.rowIndex }
            val extractedCount = sorted.count { it.status != RegisterEntry.STATUS_PENDING }
            val summary = "${extractedCount}/${sorted.size} ligne(s) — ${formatPhotoName(photoPath)}"
            newItems += TableListItem.PhotoHeader(photoPath, summary)
            sorted.forEach { newItems += TableListItem.Row(it) }
        }
        items = newItems
        notifyDataSetChanged()
    }

    private fun formatPhotoName(path: String): String {
        val name = path.substringAfterLast("/")
        return name
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is TableListItem.PhotoHeader -> TYPE_HEADER
        is TableListItem.Row -> TYPE_ROW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderVH(inflater.inflate(R.layout.item_table_header, parent, false))
        } else {
            RowVH(inflater.inflate(R.layout.item_table_row, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is TableListItem.PhotoHeader -> (holder as HeaderVH).bind(item)
            is TableListItem.Row -> (holder as RowVH).bind(item.entry, onRowClick)
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val label: TextView = itemView.findViewById(R.id.textPhotoLabel)
        fun bind(item: TableListItem.PhotoHeader) {
            label.text = item.statusSummary
        }
    }

    class RowVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sexe: TextView = itemView.findViewById(R.id.cellSexe)
        private val age: TextView = itemView.findViewById(R.id.cellAge)
        private val adresse: TextView = itemView.findViewById(R.id.cellAdresse)
        private val unite: TextView = itemView.findViewById(R.id.cellUnite)
        private val motif: TextView = itemView.findViewById(R.id.cellMotif)

        fun bind(entry: RegisterEntry, onClick: (RegisterEntry) -> Unit) {
            when (entry.status) {
                RegisterEntry.STATUS_PENDING -> {
                    sexe.text = "…"; age.text = ""; adresse.text = ""
                    unite.text = ""; motif.text = "extraction en cours"
                }
                RegisterEntry.STATUS_ERROR -> {
                    sexe.text = "❌"; age.text = ""; adresse.text = ""
                    unite.text = ""; motif.text = entry.errorMessage ?: "erreur"
                }
                else -> {
                    sexe.text = entry.sexe.orEmpty().ifBlank { "-" }
                    age.text = entry.age.orEmpty().ifBlank { "-" }
                    adresse.text = entry.adresse.orEmpty().ifBlank { "-" }
                    unite.text = entry.uniteDeService.orEmpty().ifBlank { "-" }
                    motif.text = entry.motifHospitalisation.orEmpty().ifBlank { "-" }
                }
            }
            itemView.setOnClickListener { onClick(entry) }
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ROW = 1
    }
}
