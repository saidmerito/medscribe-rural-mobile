package dj.medscriberural.companion.net

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dj.medscriberural.companion.MedScribeApp
import dj.medscriberural.companion.data.RegisterEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Service qui garde le serveur HTTP local vivant tant que l'appli attend
 * une réponse du Agent Skill MedScribe Rural chargé dans Google AI Edge Gallery.
 *
 * Le skill (voir /skill/medscribe-rural-skill/skill.js) fait un
 * fetch('http://127.0.0.1:PORT/ingest', { method: 'POST', body: JSON })
 * une fois l'extraction terminée par le modèle Gemma dans Gallery.
 */
class IngestServerService : Service() {

    private var server: IngestHttpServer? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())
        server = IngestHttpServer(PORT) { json -> handlePayload(json) }
        server?.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        server?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handlePayload(json: JSONObject) {
        val entryId = json.optLong("entryId", -1L)
        if (entryId <= 0) return

        scope.launch {
            val dao = (application as MedScribeApp).database.registerEntryDao()
            val existing = dao.getById(entryId) ?: return@launch
            val updated = existing.copy(
                patientName = json.optString("patientName", existing.patientName),
                age = json.optString("age", existing.age),
                sex = json.optString("sex", existing.sex),
                visitDate = json.optString("visitDate", existing.visitDate),
                diagnosis = json.optString("diagnosis", existing.diagnosis),
                treatment = json.optString("treatment", existing.treatment),
                healthCenter = json.optString("healthCenter", existing.healthCenter),
                rawExtractionJson = json.toString(),
                status = RegisterEntry.STATUS_EXTRACTED
            )
            dao.update(updated)
        }
    }

    private fun buildNotification(): Notification {
        val mgr = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID, "Réception MedScribe Rural", NotificationManager.IMPORTANCE_LOW
        )
        mgr.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MedScribe Rural")
            .setContentText("En attente des résultats d'extraction depuis Gallery…")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val PORT = 8765
        private const val CHANNEL_ID = "medscribe_ingest"
        private const val NOTIF_ID = 42
    }
}
