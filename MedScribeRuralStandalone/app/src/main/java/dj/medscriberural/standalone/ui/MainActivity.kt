package dj.medscriberural.standalone.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dj.medscriberural.standalone.MedScribeStandaloneApp
import dj.medscriberural.standalone.R
import dj.medscriberural.standalone.databinding.ActivityMainBinding
import dj.medscriberural.standalone.engine.ModelManager
import dj.medscriberural.standalone.export.ExportHelper
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: RegisterEntryAdapter

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) launchCapture(CaptureActivity.SOURCE_CAMERA) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        adapter = RegisterEntryAdapter(
            onRowClick = { entry ->
                startActivity(
                    Intent(this, EntryDetailActivity::class.java)
                        .putExtra(EntryDetailActivity.EXTRA_ENTRY_ID, entry.id)
                )
            },
            onSelectionChanged = { count ->
                binding.textSelectionCount.visibility = if (count > 0) android.view.View.VISIBLE else android.view.View.GONE
                binding.textSelectionCount.text = getString(R.string.selection_count, count)
            }
        )
        binding.recyclerEntries.layoutManager = LinearLayoutManager(this)
        binding.recyclerEntries.adapter = adapter

        binding.fabCapture.setOnClickListener { showSourcePicker() }
        binding.bannerModelMissing.setOnClickListener {
            startActivity(Intent(this, ModelSetupActivity::class.java))
        }

        if (!ModelManager.isModelReady(this)) {
            startActivity(Intent(this, ModelSetupActivity::class.java))
        }

        observeEntries()
    }

    override fun onResume() {
        super.onResume()
        binding.bannerModelMissing.visibility =
            if (ModelManager.isModelReady(this)) android.view.View.GONE else android.view.View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export_csv -> {
                ExportHelper.shareCsv(this, adapter.getEntriesForExport())
                true
            }
            R.id.action_share_text -> {
                ExportHelper.sharePlainText(this, adapter.getEntriesForExport())
                true
            }
            R.id.action_delete_selected -> {
                confirmDeleteSelected()
                true
            }
            R.id.action_delete_all -> {
                confirmDeleteAll()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun confirmDeleteSelected() {
        val selected = adapter.getSelectedEntries()
        if (selected.isEmpty()) {
            android.widget.Toast.makeText(this, R.string.delete_none_selected, android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_confirm_title, selected.size))
            .setMessage(R.string.delete_confirm_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete_selected) { _, _ ->
                val app = application as MedScribeStandaloneApp
                lifecycleScope.launch {
                    app.database.registerEntryDao().deleteByIds(selected.map { it.id })
                    adapter.clearSelection()
                }
            }
            .show()
    }

    private fun confirmDeleteAll() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_all_confirm_title)
            .setMessage(R.string.delete_all_confirm_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete_all) { _, _ ->
                val app = application as MedScribeStandaloneApp
                lifecycleScope.launch {
                    app.database.registerEntryDao().deleteAll()
                    adapter.clearSelection()
                }
            }
            .show()
    }

    private fun observeEntries() {
        val app = application as MedScribeStandaloneApp
        lifecycleScope.launch {
            app.database.registerEntryDao().observeAll().collect { entries ->
                adapter.submitEntries(entries)
                binding.textEmpty.visibility =
                    if (entries.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }

    private fun showSourcePicker() {
        val options = arrayOf(
            getString(R.string.source_camera),
            getString(R.string.source_gallery)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.source_picker_title)
            .setItems(options) { _, which ->
                if (which == 0) ensureCameraPermissionThenCapture() else launchCapture(CaptureActivity.SOURCE_GALLERY)
            }
            .show()
    }

    private fun ensureCameraPermissionThenCapture() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            launchCapture(CaptureActivity.SOURCE_CAMERA)
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCapture(source: String) {
        startActivity(
            Intent(this, CaptureActivity::class.java).putExtra(CaptureActivity.EXTRA_SOURCE, source)
        )
    }
}
