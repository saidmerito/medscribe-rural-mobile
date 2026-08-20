package dj.medscriberural.standalone.engine

import org.json.JSONArray
import org.json.JSONObject

/**
 * Les petits modèles on-device respectent en général bien l'instruction
 * "réponds uniquement en JSON", mais ajoutent parfois un peu de texte
 * autour (ex. des ```json ... ``` markdown, ou une phrase d'intro). On
 * isole donc le premier tableau/objet équilibré plutôt que de faire
 * confiance à la sortie brute.
 */
object JsonExtraction {

    /** Isole le premier tableau JSON `[...]` équilibré dans le texte. */
    fun extractJsonArray(rawText: String): JSONArray? {
        val start = rawText.indexOf('[')
        if (start < 0) return null

        var depth = 0
        for (i in start until rawText.length) {
            when (rawText[i]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) {
                        val candidate = rawText.substring(start, i + 1)
                        return try {
                            JSONArray(candidate)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
            }
        }
        return null
    }

    /** Isole le premier objet JSON `{...}` équilibré (repli si le modèle
     * renvoie un seul objet au lieu d'un tableau, ex. une seule ligne détectée). */
    fun extractJsonObject(rawText: String): JSONObject? {
        val start = rawText.indexOf('{')
        if (start < 0) return null

        var depth = 0
        for (i in start until rawText.length) {
            when (rawText[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        val candidate = rawText.substring(start, i + 1)
                        return try {
                            JSONObject(candidate)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
            }
        }
        return null
    }
}
