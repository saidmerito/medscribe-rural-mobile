package dj.medscriberural.companion.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import dj.medscriberural.companion.MedScribeApp
import dj.medscriberural.companion.data.RegisterEntry
import dj.medscriberural.companion.net.GalleryLauncher
import dj.medscriberural.companion.net.IngestServerService
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Écran invisible : prend une photo avec l'appli caméra du téléphone,
 * l'enregistre, crée une fiche RegisterEntry en base, démarre le serveur
 * d'ingestion, puis envoie la photo vers Google AI Edge Gallery.
 */
class CaptureActivity : AppCompatActivity() {

    private lateinit var photoFile: File
    private lateinit var photoUri: Uri

    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            onPhotoCaptured()
        } else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photoFile = createPhotoFile()
        photoUri = FileProvider.getUriForFile(
            this, "dj.medscriberural.companion.fileprovider", photoFile
        )
        takePicture.launch(photoUri)
    }

    private fun createPhotoFile(): File {
        val dir = File(getExternalFilesDir(null), "registers").apply { mkdirs() }
        val name = "registre_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRANCE).format(Date()) + ".jpg"
        return File(dir, name)
    }

    private fun onPhotoCaptured() {
        val app = application as MedScribeApp
        lifecycleScope.launch {
            val dao = app.database.registerEntryDao()
            val entryId = dao.insert(
                RegisterEntry(
                    photoPath = photoFile.absolutePath,
                    status = RegisterEntry.STATUS_PENDING
                )
            )

            // Démarre (ou garde vivant) le serveur qui attend la réponse du skill.
            startForegroundService(Intent(this@CaptureActivity, IngestServerService::class.java))

            GalleryLauncher.sendPhotoToGallery(
                context = this@CaptureActivity,
                photoFile = photoFile,
                entryId = entryId,
                callbackPort = IngestServerService.PORT
            )
            finish()
        }
    }
}
