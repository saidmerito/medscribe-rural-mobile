package dj.medscriberural.standalone

import android.app.Application
import dj.medscriberural.standalone.crash.CrashHandler
import dj.medscriberural.standalone.data.AppDatabase
import dj.medscriberural.standalone.engine.LlmEngineManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

class MedScribeStandaloneApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val llmEngineManager: LlmEngineManager by lazy { LlmEngineManager.getInstance(this) }

    // Portée de vie = celle de l'appli, pas de l'activité : une extraction
    // démarrée depuis CaptureActivity doit continuer même si l'utilisateur
    // revient tout de suite au dashboard (l'activité se ferme aussitôt la
    // photo prise).
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
    }
}
