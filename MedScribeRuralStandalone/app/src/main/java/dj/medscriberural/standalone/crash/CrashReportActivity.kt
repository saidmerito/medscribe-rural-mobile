package dj.medscriberural.standalone.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dj.medscriberural.standalone.databinding.ActivityCrashReportBinding

class CrashReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCrashReportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrashReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val report = CrashHandler.getLastCrashReport(this) ?: "Aucun détail disponible."
        binding.textCrashReport.text = report

        binding.buttonCopy.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Rapport de plantage", report))
            Toast.makeText(this, "Copié — colle-le dans le chat", Toast.LENGTH_SHORT).show()
        }

        binding.buttonClose.setOnClickListener {
            CrashHandler.clearLastCrashReport(this)
            finish()
        }
    }
}
