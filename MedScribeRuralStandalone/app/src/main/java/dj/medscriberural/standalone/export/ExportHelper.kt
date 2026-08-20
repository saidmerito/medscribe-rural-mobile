package dj.medscriberural.standalone.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dj.medscriberural.standalone.data.RegisterEntry
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    private const val AUTHORITY = "dj.medscriberural.standalone.fileprovider"

    /** Construit un CSV (séparateur virgule, champs échappés) groupé par photo. */
    fun buildCsv(entries: List<RegisterEntry>): String {
        val sb = StringBuilder()
        sb.append("Photo,Ligne,Sexe,Age,Adresse,Unite de service,Motif hospitalisation,Statut\n")
        entries.sortedWith(compareBy({ it.photoPath }, { it.rowIndex })).forEach { e ->
            sb.append(csvField(e.photoPath.substringAfterLast("/"))).append(',')
            sb.append(e.rowIndex + 1).append(',')
            sb.append(csvField(e.sexe)).append(',')
            sb.append(csvField(e.age)).append(',')
            sb.append(csvField(e.adresse)).append(',')
            sb.append(csvField(e.uniteDeService)).append(',')
            sb.append(csvField(e.motifHospitalisation)).append(',')
            sb.append(csvField(e.status)).append('\n')
        }
        return sb.toString()
    }

    /** Texte simple, lisible, adapté au partage WhatsApp/email. */
    fun buildPlainText(entries: List<RegisterEntry>): String {
        val sb = StringBuilder()
        entries.groupBy { it.photoPath }.forEach { (photoPath, rows) ->
            sb.append("— ${photoPath.substringAfterLast("/")} —\n")
            rows.sortedBy { it.rowIndex }.forEach { e ->
                val parts = listOfNotNull(
                    e.sexe, e.age?.let { "$it ans" }, e.adresse,
                    e.uniteDeService, e.motifHospitalisation
                ).joinToString(" · ")
                sb.append("${e.rowIndex + 1}. $parts\n")
            }
            sb.append('\n')
        }
        return sb.toString().trim()
    }

    private fun csvField(value: String?): String {
        val v = value ?: ""
        return if (v.contains(',') || v.contains('"') || v.contains('\n')) {
            "\"${v.replace("\"", "\"\"")}\""
        } else v
    }

    /** Écrit le CSV sur disque et lance le sélecteur de partage Android. */
    fun shareCsv(context: Context, entries: List<RegisterEntry>) {
        val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val name = "medscribe_export_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRANCE).format(Date()) + ".csv"
        val file = File(dir, name)
        file.writeText(buildCsv(entries))

        val uri = FileProvider.getUriForFile(context, AUTHORITY, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Exporter le CSV vers…"))
    }

    /** Partage le texte formaté directement (sans fichier). */
    fun sharePlainText(context: Context, entries: List<RegisterEntry>) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, buildPlainText(entries))
        }
        context.startActivity(Intent.createChooser(intent, "Partager les fiches vers…"))
    }
}
