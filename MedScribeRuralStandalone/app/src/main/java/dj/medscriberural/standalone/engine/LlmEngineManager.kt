package dj.medscriberural.standalone.engine

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Enveloppe le moteur LiteRT-LM : chargement du modèle en mémoire une
 * seule fois (coûteux, jusqu'à ~10s), puis réutilisation pour chaque
 * extraction. C'est ce composant qui remplace complètement la dépendance
 * à Google AI Edge Gallery — l'inférence tourne dans le process de
 * cette appli.
 */
class LlmEngineManager(private val context: Context) {

    private var engine: Engine? = null
    private val initMutex = Mutex()

    suspend fun ensureInitialized(): Engine = initMutex.withLock {
        engine?.let { return it }

        val modelPath = ModelManager.modelFile(context).absolutePath
        val config = EngineConfig(
            modelPath = modelPath,
            backend = Backend.CPU(),
            // Le backend vision accélère la lecture d'image sur GPU si disponible ;
            // retombe sur CPU automatiquement si le device ne le supporte pas.
            visionBackend = Backend.GPU(),
            cacheDir = context.cacheDir.path
        )
        val newEngine = Engine(config)
        newEngine.initialize()
        engine = newEngine
        newEngine
    }

    /**
     * Lance une extraction sur une photo de registre et renvoie la réponse
     * brute du modèle (texte contenant idéalement un objet JSON).
     */
    suspend fun extractFromImage(photoPath: String, prompt: String): String {
        val eng = ensureInitialized()
        eng.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(EXTRACTION_SYSTEM_PROMPT)
            )
        ).use { conversation ->
            val response = conversation.sendMessage(
                Contents.of(
                    Content.ImageFile(photoPath),
                    Content.Text(prompt)
                )
            )
            return response.text
        }
    }

    fun close() {
        engine?.close()
        engine = null
    }

    companion object {
        private const val EXTRACTION_SYSTEM_PROMPT = """
Tu es un assistant qui digitalise des registres de santé manuscrits pour
un centre de santé rural à Djibouti. Réponds UNIQUEMENT avec un objet JSON
valide, sans texte autour, avec exactement ces clés (chaîne vide si un
champ est illisible ou absent) :
{"patientName":"","age":"","sex":"","visitDate":"","diagnosis":"","treatment":"","healthCenter":""}
Ne devine jamais une valeur que tu ne peux pas lire clairement sur l'image.
"""

        // Prompt envoyé à chaque extraction, avec l'image en pièce jointe.
        const val EXTRACTION_USER_PROMPT =
            "Extrait les informations de cette page de registre de santé manuscrit au format JSON demandé."

        @Volatile private var INSTANCE: LlmEngineManager? = null

        fun getInstance(context: Context): LlmEngineManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LlmEngineManager(context.applicationContext).also { INSTANCE = it }
            }
    }
}
