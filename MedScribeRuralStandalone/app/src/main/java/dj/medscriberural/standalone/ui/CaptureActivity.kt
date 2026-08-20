package dj.medscriberural.standalone.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import dj.medscriberural.standalone.MedScribeStandaloneApp
import dj.medscriberural.standalone.data.RegisterEntry
import dj.medscriberural.standalone.engine.JsonExtraction
import dj.medscriberural.standalone.engine.LlmEngineManager
import dj.medscriberural.standalone.engine.ModelManager
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Obtient une photo du registre (appareil photo OU galerie, selon
 * EXTRA_SOURCE), crée la fiche en base, puis lance directement
 * l'inférence via le moteur LiteRT-LM embarqué.
 */
class CaptureActivity : AppCompatActivity() {

    private lateinit var photoFile: File
    private lateinit var photoUri: Uri

    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) onPhotoCaptured() else finish() }

    private val pickFromGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) copyFromGalleryThenProceed(uri) else finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ModelManager.isModelReady(this)) {
            startActivity(android.content.Intent(this, ModelSetupActivity::class.java))
            finish()
            return
        }

        photoFile = createPhotoFile()
        photoUri = FileProvider.getUriForFile(
            this, "dj.medscriberural.standalone.fileprovider", photoFile
        )

        when (intent.getStringExtra(EXTRA_SOURCE)) {
            SOURCE_GALLERY -> pickFromGallery.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
            else -> takePicture.launch(photoUri)
        }
    }

    private fun createPhotoFile(): File {
        val dir = File(getExternalFilesDir(null), "registers").apply { mkdirs() }
        val name = "registre_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRANCE).format(Date()) + ".jpg"
        return File(dir, name)
    }

    /** Copie l'image choisie dans la galerie vers notre propre fichier local
     * (le moteur LiteRT-LM a besoin d'un chemin de fichier réel, pas d'une content:// Uri). */
    private fun copyFromGalleryThenProceed(sourceUri: Uri) {
        try {
            contentResolver.openInputStream(sourceUri)?.use { input ->
                photoFile.outputStream().use { output -> input.copyTo(output) }
            }
            onPhotoCaptured()
        } catch (e: Exception) {
            finish()
        }
    }

    private fun onPhotoCaptured() {
        val app = application as MedScribeStandaloneApp
        // IMPORTANT : on utilise applicationScope (et non lifecycleScope) car
        // l'activité se ferme tout de suite ci-dessous.
        app.applicationScope.launch {
            val dao = app.database.registerEntryDao()
            val entryId = dao.insert(
                RegisterEntry(photoPath = photoFile.absolutePath, status = RegisterEntry.STATUS_PENDING)
            )

            try {
                val rawResponse = app.llmEngineManager.extractFromImage(
                    photoFile.absolutePath,
                    LlmEngineManager.EXTRACTION_USER_PROMPT
                )

                val jsonArray = JsonExtraction.extractJsonArray(rawResponse)
                val rows: List<JSONObject> = when {
                    jsonArray != null -> (0 until jsonArray.length()).mapNotNull { i ->
                        jsonArray.optJSONObject(i)
                    }
                    else -> {
                        // Repli : le modèle a renvoyé un seul objet au lieu d'un tableau
                        // (arrive parfois quand une seule ligne est visible sur la photo).
                        JsonExtraction.extractJsonObject(rawResponse)?.let { listOf(it) } ?: emptyList()
                    }
                }

                // La fiche "pending" créée plus haut portait déjà rowIndex=0 ; on la
                // met à jour pour la première ligne, puis on insère les suivantes.
                if (rows.isEmpty()) {
                    val current = dao.getById(entryId) ?: return@launch
                    dao.update(
                        current.copy(
                            status = RegisterEntry.STATUS_ERROR,
                            rawExtractionJson = rawResponse,
                            errorMessage = "Le modèle n'a pas renvoyé de JSON exploitable."
                        )
                    )
                } else {
                    val current = dao.getById(entryId) ?: return@launch
                    dao.update(rowToEntry(current, rows[0], rowIndex = 0, rawResponse))

                    if (rows.size > 1) {
                        val extraEntries = rows.drop(1).mapIndexed { index, row ->
                            rowToEntry(
                                base = RegisterEntry(photoPath = photoFile.absolutePath),
                                row = row,
                                rowIndex = index + 1,
                                rawResponse = rawResponse
                            )
                        }
                        dao.insertAll(extraEntries)
                    }
                }
            } catch (e: Exception) {
                val current = dao.getById(entryId) ?: return@launch
                dao.update(
                    current.copy(status = RegisterEntry.STATUS_ERROR, errorMessage = e.message)
                )
            }
        }
        // L'activité se ferme immédiatement : l'extraction continue en tâche
        // de fond (applicationScope) et met à jour la fiche quand elle est
        // prête, visible au retour sur le dashboard.
        finish()
    }

    /** Applique les champs d'une ligne extraite (JSON) à une fiche RegisterEntry. */
    private fun rowToEntry(base: RegisterEntry, row: JSONObject, rowIndex: Int, rawResponse: String): RegisterEntry =
        base.copy(
            rowIndex = rowIndex,
            sexe = row.optString("sexe").ifBlank { null },
            age = row.optString("age").ifBlank { null },
            adresse = row.optString("adresse").ifBlank { null },
            uniteDeService = row.optString("uniteDeService").ifBlank { null },
            motifHospitalisation = row.optString("motifHospitalisation").ifBlank { null },
            rawExtractionJson = row.toString(),
            status = RegisterEntry.STATUS_EXTRACTED
        )

    companion object {
        const val EXTRA_SOURCE = "extra_source"
        const val SOURCE_CAMERA = "camera"
        const val SOURCE_GALLERY = "gallery"
    }
}
