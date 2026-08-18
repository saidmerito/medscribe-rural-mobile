package dj.medscriberural.companion.net

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Envoie une photo de registre vers l'application Google AI Edge Gallery
 * via un Intent implicite ACTION_SEND (comme n'importe quel "Partager vers").
 *
 * IMPORTANT — limite connue : Google AI Edge Gallery n'expose aucune API
 * d'intent publique documentée permettant à une appli tierce d'envoyer une
 * image ET de recevoir automatiquement un résultat structuré en retour.
 * Ce lanceur ouvre donc Gallery avec la photo + un prompt pré-rempli
 * (mode "Ask Image" / Agent Skills). C'est le Agent Skill MedScribe Rural
 * chargé dans Gallery (voir dossier /skill) qui referme la boucle en
 * postant le résultat vers le petit serveur local de cette appli.
 */
object GalleryLauncher {

    // Package officiel de Google AI Edge Gallery sur le Play Store.
    const val GALLERY_PACKAGE = "com.google.ai.edge.gallery"

    private const val AUTHORITY = "dj.medscriberural.companion.fileprovider"

    fun isGalleryInstalled(context: Context): Boolean =
        try {
            context.packageManager.getPackageInfo(GALLERY_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

    /**
     * @param entryId identifiant de la fiche RegisterEntry en base locale,
     *   inclus dans le prompt pour que le skill sache à quelle fiche
     *   rattacher sa réponse (le skill le renvoie tel quel dans le POST).
     * @param callbackPort port sur lequel le serveur local écoute (voir IngestServerService)
     */
    fun sendPhotoToGallery(
        context: Context,
        photoFile: File,
        entryId: Long,
        callbackPort: Int
    ) {
        val photoUri: Uri = FileProvider.getUriForFile(context, AUTHORITY, photoFile)

        val prompt = buildPrompt(entryId, callbackPort)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, photoUri)
            putExtra(Intent.EXTRA_TEXT, prompt)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // On cible Gallery explicitement si elle est installée, pour éviter
            // que l'utilisateur ne se retrompe d'appli dans le chooser.
            if (isGalleryInstalled(context)) {
                setPackage(GALLERY_PACKAGE)
            }
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Gallery n'est pas installée ou ne gère pas ACTION_SEND pour les images :
            // on retombe sur le chooser générique.
            val chooser = Intent.createChooser(
                intent.apply { setPackage(null) },
                "Envoyer la photo du registre à…"
            )
            context.startActivity(chooser)
        }
    }

    private fun buildPrompt(entryId: Long, callbackPort: Int): String = """
        Utilise le skill "MedScribe Rural" pour extraire les informations de cette
        page de registre de santé manuscrit (nom du patient, âge, sexe, date de
        visite, diagnostic, traitement, centre de santé). Réponds au format JSON
        demandé par le skill, puis transmets le résultat à l'appli MedScribe Rural
        Companion avec entryId=$entryId et callbackPort=$callbackPort.
    """.trimIndent()
}
