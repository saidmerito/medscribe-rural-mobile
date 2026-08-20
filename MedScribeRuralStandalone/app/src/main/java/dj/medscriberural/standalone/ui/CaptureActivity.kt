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
                val json = JsonExtraction.extractJsonObject(rawResponse)
                val current = dao.getById(entryId) ?: return@launch

                if (json != null) {
                    dao.update(
                        current.copy(
                            patientName = json.optString("patientName").ifBlank { null },
                            age = json.optString("age").ifBlank { null },
                            sex = json.optString("sex").ifBlank { null },
                            visitDate = json.optString("visitDate").ifBlank { null },
                            diagnosis = json.optString("diagnosis").ifBlank { null },
                            treatment = json.optString("treatment").ifBlank { null },
                            healthCenter = json.optString("healthCenter").ifBlank { null },
                            rawExtractionJson = json.toString(),
                            status = RegisterEntry.STATUS_EXTRACTED
                        )
                    )
                } else {
                    dao.update(
                        current.copy(
                            status = RegisterEntry.STATUS_ERROR,
                            rawExtractionJson = rawResponse,
                            errorMessage = "Le modèle n'a pas renvoyé de JSON exploitable."
                        )
                    )
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

    companion object {
        const val EXTRA_SOURCE = "extra_source"
        const val SOURCE_CAMERA = "camera"
        const val SOURCE_GALLERY = "gallery"
    }
}
