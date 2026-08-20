package dj.medscriberural.standalone.crash

import android.content.Context
import android.content.Intent
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

/**
 * Remplace l'écran générique "L'application s'est arrêtée" par un écran
 * lisible affichant la trace complète de l'exception, avec un bouton pour
 * la copier. Permet de diagnostiquer un plantage sans PC ni logcat.
 */
object CrashHandler {

    private const val PREFS_NAME = "crash_reports"
    private const val KEY_LAST_CRASH = "last_crash"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRANCE).format(Date())
                val report = "Plantage le $timestamp\nThread: ${thread.name}\n\n${sw}"

                appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_LAST_CRASH, report)
                    .commit() // synchrone : le process va être tué juste après

                val intent = Intent(appContext, CrashReportActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                appContext.startActivity(intent)
            } catch (inner: Exception) {
                // Si même la capture du crash échoue, on ne bloque pas —
                // on laisse le comportement par défaut prendre le relais.
            } finally {
                android.os.Process.killProcess(android.os.Process.myPid())
                exitProcess(10)
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun getLastCrashReport(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_CRASH, null)

    fun clearLastCrashReport(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_LAST_CRASH).apply()
    }
}
