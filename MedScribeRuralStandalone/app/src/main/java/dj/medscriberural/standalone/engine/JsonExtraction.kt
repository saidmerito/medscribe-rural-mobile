package dj.medscriberural.standalone.engine

import org.json.JSONObject

/**
 * Les petits modèles on-device respectent en général bien l'instruction
 * "réponds uniquement en JSON", mais ajoutent parfois un peu de texte
 * autour (ex. des ```json ... ``` markdown, ou une phrase d'intro). On
 * isole donc la première accolade équilibrée plutôt que de faire confiance
 * à la sortie brute.
 */
object JsonExtraction {

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
