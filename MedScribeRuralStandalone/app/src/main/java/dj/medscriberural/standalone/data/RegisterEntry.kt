package dj.medscriberural.standalone.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Une ligne du registre papier (un patient), extraite d'une photo de
 * page qui peut en contenir plusieurs. Les colonnes correspondent au
 * format réel des registres de santé utilisés au Ministère (Djibouti) :
 * SEXE / AGE / ADRESSE / UNITE DE SERVICE / MOTIF D'HOSPITALISATION.
 */
@Entity(tableName = "register_entries")
data class RegisterEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // Chemin de la photo source — plusieurs fiches partagent le même photoPath
    // puisqu'une page contient plusieurs lignes/patients.
    val photoPath: String,
    // Position de la ligne dans le tableau de la page (0 = première ligne), pour
    // garder l'ordre d'affichage identique à celui du registre papier.
    val rowIndex: Int = 0,
    val sexe: String? = null,
    val age: String? = null,
    val adresse: String? = null,
    val uniteDeService: String? = null,
    val motifHospitalisation: String? = null,
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
