package dj.medscriberural.standalone.ui

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dj.medscriberural.standalone.MedScribeStandaloneApp
import dj.medscriberural.standalone.R
import dj.medscriberural.standalone.data.RegisterEntry
import dj.medscriberural.standalone.databinding.ActivityEntryDetailBinding
import kotlinx.coroutines.launch

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
        binding.buttonDelete.setOnClickListener { confirmDelete() }
    }

    private fun loadEntry() {
        val app = application as MedScribeStandaloneApp
        lifecycleScope.launch {
            val entry = app.database.registerEntryDao().getById(entryId) ?: return@launch
            binding.editSexe.setText(entry.sexe)
            binding.editAge.setText(entry.age)
            binding.editAdresse.setText(entry.adresse)
            binding.editUniteDeService.setText(entry.uniteDeService)
            binding.editMotif.setText(entry.motifHospitalisation)
        }
    }

    private fun saveAndValidate() {
        val app = application as MedScribeStandaloneApp
        lifecycleScope.launch {
            val existing = app.database.registerEntryDao().getById(entryId) ?: return@launch
            val updated = existing.copy(
                sexe = binding.editSexe.text?.toString(),
                age = binding.editAge.text?.toString(),
                adresse = binding.editAdresse.text?.toString(),
                uniteDeService = binding.editUniteDeService.text?.toString(),
                motifHospitalisation = binding.editMotif.text?.toString(),
                status = RegisterEntry.STATUS_VALIDATED
            )
            app.database.registerEntryDao().update(updated)
            finish()
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_entry_confirm_title)
            .setMessage(R.string.delete_confirm_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete_selected) { _, _ ->
                val app = application as MedScribeStandaloneApp
                lifecycleScope.launch {
                    app.database.registerEntryDao().deleteByIds(listOf(entryId))
                    finish()
                }
            }
            .show()
    }

    companion object {
        const val EXTRA_ENTRY_ID = "extra_entry_id"
    }
}
