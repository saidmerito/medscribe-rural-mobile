package dj.medscriberural.companion.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dj.medscriberural.companion.MedScribeApp
import dj.medscriberural.companion.data.RegisterEntry
import dj.medscriberural.companion.databinding.ActivityEntryDetailBinding
import kotlinx.coroutines.launch

/**
 * Permet à l'agent de santé de relire les champs extraits par le modèle
 * Gemma (via Gallery) et de les corriger avant validation finale.
 * La validation ici correspond à l'étape qui, dans le pipeline desktop
 * MedScribe Rural, déclenche l'écriture vers SQLite / le dashboard.
 */
class EntryDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEntryDetailBinding
    private var entryId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        entryId = intent.getLongExtra(EXTRA_ENTRY_ID, -1)
        loadEntry()

        binding.buttonValidate.setOnClickListener { saveAndValidate() }
    }

    private fun loadEntry() {
        val app = application as MedScribeApp
        lifecycleScope.launch {
            val entry = app.database.registerEntryDao().getById(entryId) ?: return@launch
            binding.editPatientName.setText(entry.patientName)
            binding.editAge.setText(entry.age)
            binding.editSex.setText(entry.sex)
            binding.editVisitDate.setText(entry.visitDate)
            binding.editDiagnosis.setText(entry.diagnosis)
            binding.editTreatment.setText(entry.treatment)
            binding.editHealthCenter.setText(entry.healthCenter)
        }
    }

    private fun saveAndValidate() {
        val app = application as MedScribeApp
        lifecycleScope.launch {
            val existing = app.database.registerEntryDao().getById(entryId) ?: return@launch
            val updated = existing.copy(
                patientName = binding.editPatientName.text?.toString(),
                age = binding.editAge.text?.toString(),
                sex = binding.editSex.text?.toString(),
                visitDate = binding.editVisitDate.text?.toString(),
                diagnosis = binding.editDiagnosis.text?.toString(),
                treatment = binding.editTreatment.text?.toString(),
                healthCenter = binding.editHealthCenter.text?.toString(),
                status = RegisterEntry.STATUS_VALIDATED
            )
            app.database.registerEntryDao().update(updated)
            finish()
        }
    }

    companion object {
        const val EXTRA_ENTRY_ID = "extra_entry_id"
    }
}
