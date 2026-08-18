package dj.medscriberural.standalone.engine

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Gère le fichier modèle .litertlm utilisé par le moteur embarqué.
 *
 * Le modèle (Gemma 3n E2B multimodal, ~2-3 Go) n'est PAS embarqué dans
 * l'APK — il serait beaucoup trop volumineux pour un déploiement terrain
 * à faible bande passante. Deux façons de l'obtenir :
 *
 *  1. Import manuel : copier un fichier .litertlm déjà téléchargé (par ex.
 *     via le PC, la même façon dont tu récupères déjà des modèles Ollama)
 *     sur le téléphone, puis "Importer un modèle" dans l'appli.
 *  2. Téléchargement one-shot depuis Hugging Face LiteRT Community
 *     (nécessite une connexion, une seule fois — ensuite tout fonctionne
 *     hors-ligne).
 */
object ModelManager {

    private const val MODEL_FILE_NAME = "gemma-4-E2B-it.litertlm"

    // Modèle multimodal (texte + image) recommandé pour l'extraction de
    // registres manuscrits. Gemma 4 E2B — le plus petit modèle multimodal
    // de la famille, ~2-3 Go. À adapter si tu veux un modèle plus/moins lourd
    // (voir huggingface.co/litert-community pour les autres tailles).
    const val DEFAULT_MODEL_DOWNLOAD_URL =
        "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"

    fun modelFile(context: Context): File =
        File(context.getExternalFilesDir("models") ?: context.filesDir, MODEL_FILE_NAME)

    fun isModelReady(context: Context): Boolean {
        val f = modelFile(context)
        return f.exists() && f.length() > 0L
    }

    /** Copie un fichier .litertlm choisi par l'utilisateur (Storage Access Framework) vers l'emplacement attendu. */
    fun importFromUri(context: Context, uri: Uri): Boolean {
        return try {
            val dest = modelFile(context)
            dest.parentFile?.mkdirs()
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            dest.exists() && dest.length() > 0L
        } catch (e: Exception) {
            false
        }
    }

    /** Lance un téléchargement via le DownloadManager système (gère la reprise et la progression). */
    fun enqueueDownload(context: Context, url: String = DEFAULT_MODEL_DOWNLOAD_URL): Long {
        val dest = modelFile(context)
        dest.parentFile?.mkdirs()
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Modèle MedScribe Rural (Gemma 3n)")
            .setDescription("Téléchargement unique — ensuite l'extraction fonctionne hors-ligne")
            .setDestinationUri(Uri.fromFile(dest))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(false) // évite de consommer du forfait data par accident sur le terrain

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }
}
