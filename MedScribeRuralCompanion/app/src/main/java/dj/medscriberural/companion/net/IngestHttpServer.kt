package dj.medscriberural.companion.net

import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject

/**
 * Petit serveur HTTP local (127.0.0.1 uniquement — jamais exposé sur le
 * réseau) qui reçoit le résultat d'extraction du Agent Skill Gallery.
 */
class IngestHttpServer(
    port: Int,
    private val onPayload: (JSONObject) -> Unit
) : NanoHTTPD("127.0.0.1", port) {

    override fun serve(session: IHTTPSession): Response {
        return when {
            session.method == Method.POST && session.uri == "/ingest" -> handleIngest(session)
            session.method == Method.GET && session.uri == "/ping" ->
                newFixedLengthResponse(Response.Status.OK, "text/plain", "ok")
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "not found")
        }
    }

    private fun handleIngest(session: IHTTPSession): Response {
        return try {
            val files = HashMap<String, String>()
            session.parseBody(files)
            val body = files["postData"] ?: session.queryParameterString ?: "{}"
            val json = JSONObject(body)
            onPayload(json)
            newFixedLengthResponse(
                Response.Status.OK, "application/json", """{"status":"received"}"""
            )
        } catch (e: Exception) {
            newFixedLengthResponse(
                Response.Status.BAD_REQUEST, "application/json",
                """{"status":"error","message":"${e.message}"}"""
            )
        }
    }
}
