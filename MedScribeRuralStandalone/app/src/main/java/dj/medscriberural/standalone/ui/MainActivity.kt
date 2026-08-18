package dj.medscriberural.standalone.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dj.medscriberural.standalone.MedScribeStandaloneApp
import dj.medscriberural.standalone.databinding.ActivityMainBinding
import dj.medscriberural.standalone.engine.ModelManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: RegisterEntryAdapter

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) launchCapture() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = RegisterEntryAdapter { entry ->
            startActivity(
                Intent(this, EntryDetailActivity::class.java)
                    .putExtra(EntryDetailActivity.EXTRA_ENTRY_ID, entry.id)
            )
        }
        binding.recyclerEntries.layoutManager = LinearLayoutManager(this)
        binding.recyclerEntries.adapter = adapter

        binding.fabCapture.setOnClickListener { ensureCameraPermissionThenCapture() }
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

    private fun observeEntries() {
        val app = application as MedScribeStandaloneApp
        lifecycleScope.launch {
            app.database.registerEntryDao().observeAll().collect { entries ->
                adapter.submitList(entries)
                binding.textEmpty.visibility =
                    if (entries.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }

    private fun ensureCameraPermissionThenCapture() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) launchCapture() else requestCameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun launchCapture() {
        startActivity(Intent(this, CaptureActivity::class.java))
    }
}
