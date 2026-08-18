package dj.medscriberural.standalone.ui

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import dj.medscriberural.standalone.databinding.ActivityModelSetupBinding
import dj.medscriberural.standalone.engine.ModelManager

/**
 * Premier lancement : l'utilisateur doit fournir le fichier modèle .litertlm
 * une fois (import depuis stockage, ou téléchargement one-shot). Ensuite,
 * l'extraction fonctionne intégralement hors-ligne.
 */
class ModelSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModelSetupBinding

    private val pickModelFile = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            binding.textStatus.text = "Copie du fichier en cours…"
            val ok = ModelManager.importFromUri(this, uri)
            onModelReadyCheck(ok)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModelSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        refreshStatus()

        binding.buttonImport.setOnClickListener {
            pickModelFile.launch(arrayOf("*/*"))
        }

        binding.buttonDownload.setOnClickListener {
            ModelManager.enqueueDownload(this)
            binding.textStatus.text =
                "Téléchargement lancé — suis la progression dans la barre de notifications. " +
                "Relance cet écran une fois terminé."
        }

        binding.buttonContinue.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val ready = ModelManager.isModelReady(this)
        binding.textStatus.text = if (ready) {
            "✅ Modèle prêt : ${ModelManager.modelFile(this).name}"
        } else {
            "Aucun modèle installé. Importe un fichier .litertlm déjà téléchargé, " +
                "ou lance le téléchargement (une seule fois, connexion requise)."
        }
        binding.buttonContinue.isEnabled = ready
    }

    private fun onModelReadyCheck(importSucceeded: Boolean) {
        binding.textStatus.text = if (importSucceeded) {
            "✅ Modèle importé avec succès."
        } else {
            "❌ Échec de l'import. Vérifie que le fichier est bien un .litertlm valide."
        }
        binding.buttonContinue.isEnabled = ModelManager.isModelReady(this)
    }
}
