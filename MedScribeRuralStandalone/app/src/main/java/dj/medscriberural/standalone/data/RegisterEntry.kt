package dj.medscriberural.standalone.data

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val errorMessage: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val syncedToDhis2: Boolean = false
) {
    companion object {
        const val STATUS_PENDING = "pending"      // photo prise, inférence en cours
        const val STATUS_EXTRACTED = "extracted"  // JSON reçu du modèle local
        const val STATUS_ERROR = "error"          // échec de l'inférence ou du parsing JSON
        const val STATUS_VALIDATED = "validated"  // relue et validée par l'agent de santé
    }
}
