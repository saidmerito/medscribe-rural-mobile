package dj.medscriberural.standalone.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dj.medscriberural.standalone.R
import dj.medscriberural.standalone.data.RegisterEntry

/** Un item de la liste : soit l'en-tête d'une page (nom photo + colonnes),
 * soit une ligne de données extraite de cette page. */
sealed class TableListItem {
    data class PhotoHeader(val photoPath: String, val statusSummary: String, val rowIds: List<Long>) : TableListItem()
    data class Row(val entry: RegisterEntry) : TableListItem()
}

class RegisterEntryAdapter(
    private val onRowClick: (RegisterEntry) -> Unit,
    private val onSelectionChanged: (count: Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<TableListItem> = emptyList()
    private var allEntries: List<RegisterEntry> = emptyList()
    private val selectedIds = mutableSetOf<Long>()

    /** Regroupe les fiches par photo (une page = un tableau), triées par
     * date décroissante, avec les lignes dans l'ordre du registre papier. */
    fun submitEntries(entries: List<RegisterEntry>) {
        allEntries = entries
        // On ne garde en sélection que les ids qui existent encore.
        selectedIds.retainAll(entries.map { it.id }.toSet())

        val grouped = entries.groupBy { it.photoPath }
        val orderedPhotos = grouped.entries.sortedByDescending { (_, rows) ->
            rows.maxOf { it.createdAtMillis }
        }

        val newItems = mutableListOf<TableListItem>()
        for ((photoPath, rows) in orderedPhotos) {
            val sorted = rows.sortedBy { it.rowIndex }
            val extractedCount = sorted.count { it.status != RegisterEntry.STATUS_PENDING }
            val summary = "${extractedCount}/${sorted.size} ligne(s) — ${photoPath.substringAfterLast("/")}"
            newItems += TableListItem.PhotoHeader(photoPath, summary, sorted.map { it.id })
            sorted.forEach { newItems += TableListItem.Row(it) }
        }
        items = newItems
        notifyDataSetChanged()
        onSelectionChanged(selectedIds.size)
    }

    fun getSelectedEntries(): List<RegisterEntry> =
        allEntries.filter { it.id in selectedIds }

    /** Si rien n'est coché, on considère que l'utilisateur veut tout exporter. */
    fun getEntriesForExport(): List<RegisterEntry> =
        getSelectedEntries().ifEmpty { allEntries }

    fun clearSelection() {
        selectedIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    private fun toggle(id: Long) {
        if (!selectedIds.add(id)) selectedIds.remove(id)
        onSelectionChanged(selectedIds.size)
    }

    private fun setGroupSelected(ids: List<Long>, selected: Boolean) {
        if (selected) selectedIds.addAll(ids) else selectedIds.removeAll(ids.toSet())
        notifyDataSetChanged()
        onSelectionChanged(selectedIds.size)
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
            is TableListItem.PhotoHeader -> (holder as HeaderVH).bind(item, selectedIds, ::setGroupSelected)
            is TableListItem.Row -> (holder as RowVH).bind(item.entry, item.entry.id in selectedIds, onRowClick, ::toggle)
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val label: TextView = itemView.findViewById(R.id.textPhotoLabel)
        private val checkboxAll: CheckBox = itemView.findViewById(R.id.checkboxSelectAll)

        fun bind(item: TableListItem.PhotoHeader, selectedIds: Set<Long>, onToggleGroup: (List<Long>, Boolean) -> Unit) {
            label.text = item.statusSummary
            checkboxAll.setOnCheckedChangeListener(null)
            checkboxAll.isChecked = item.rowIds.isNotEmpty() && selectedIds.containsAll(item.rowIds)
            checkboxAll.setOnCheckedChangeListener { _, isChecked ->
                onToggleGroup(item.rowIds, isChecked)
            }
        }
    }

    class RowVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkbox: CheckBox = itemView.findViewById(R.id.checkboxSelect)
        private val sexe: TextView = itemView.findViewById(R.id.cellSexe)
        private val age: TextView = itemView.findViewById(R.id.cellAge)
        private val adresse: TextView = itemView.findViewById(R.id.cellAdresse)
        private val unite: TextView = itemView.findViewById(R.id.cellUnite)
        private val motif: TextView = itemView.findViewById(R.id.cellMotif)

        fun bind(entry: RegisterEntry, isSelected: Boolean, onClick: (RegisterEntry) -> Unit, onToggle: (Long) -> Unit) {
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

            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked = isSelected
            checkbox.setOnCheckedChangeListener { _, _ -> onToggle(entry.id) }

            // Tap sur la ligne (hors case à cocher et hors sélection de texte) ouvre le détail.
            itemView.setOnClickListener { onClick(entry) }
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ROW = 1
    }
}
