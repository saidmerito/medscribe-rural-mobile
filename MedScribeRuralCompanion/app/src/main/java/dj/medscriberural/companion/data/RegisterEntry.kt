package dj.medscriberural.companion.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Une fiche extraite d'un registre papier de santé, remplie soit
 * manuellement, soit automatiquement par le Agent Skill MedScribe Rural
 * qui tourne dans Google AI Edge Gallery.
 */
@Entity(tableName = "register_entries")
data class RegisterEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val photoPath: String,
    val patientName: String? = null,
    val age: String? = null,
    val sex: String? = null,
    val visitDate: String? = null,
    val diagnosis: String? = null,
    val treatment: String? = null,
    val healthCenter: String? = null,
    val rawExtractionJson: String? = null,
    val status: String = STATUS_PENDING,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val syncedToDhis2: Boolean = false
) {
    companion object {
        const val STATUS_PENDING = "pending"      // photo prise, en attente d'extraction par Gallery
        const val STATUS_EXTRACTED = "extracted"  // données reçues du skill
        const val STATUS_VALIDATED = "validated"  // relues et validées par l'agent de santé
    }
}
