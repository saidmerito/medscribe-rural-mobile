/**
 * Skill "MedScribe Rural" pour Google AI Edge Gallery.
 *
 * Ce fichier s'exécute dans le moteur JS sandboxé de Gallery (WebView caché).
 * La fonction ci-dessous est exposée au modèle (Gemma 4, function calling) :
 * le LLM l'invoque une fois qu'il a extrait les champs de la photo du
 * registre. Elle transmet ensuite les données, via une requête HTTP locale,
 * à l'appli MedScribe Rural Companion qui tourne sur le même téléphone.
 *
 * Aucune donnée ne quitte l'appareil : la requête est adressée à 127.0.0.1.
 */

/**
 * @param {Object} params
 * @param {number} params.entryId       identifiant de la fiche côté appli (fourni dans le prompt)
 * @param {number} params.callbackPort  port du serveur local de l'appli (fourni dans le prompt)
 * @param {string} [params.patientName]
 * @param {string} [params.age]
 * @param {string} [params.sex]
 * @param {string} [params.visitDate]
 * @param {string} [params.diagnosis]
 * @param {string} [params.treatment]
 * @param {string} [params.healthCenter]
 */
async function submitToMedScribeRural(params) {
  const { entryId, callbackPort } = params;

  if (!entryId || !callbackPort) {
    return {
      ok: false,
      message: "entryId et callbackPort sont requis (récupère-les dans le prompt de l'utilisateur)."
    };
  }

  const url = `http://127.0.0.1:${callbackPort}/ingest`;

  try {
    const response = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        entryId,
        patientName: params.patientName || "",
        age: params.age || "",
        sex: params.sex || "",
        visitDate: params.visitDate || "",
        diagnosis: params.diagnosis || "",
        treatment: params.treatment || "",
        healthCenter: params.healthCenter || ""
      })
    });

    if (!response.ok) {
      return { ok: false, message: `L'appli a répondu avec le statut ${response.status}.` };
    }

    return { ok: true, message: "Données transmises à MedScribe Rural Companion." };
  } catch (err) {
    return {
      ok: false,
      message:
        "Impossible de joindre l'appli MedScribe Rural Companion en local. " +
        "Vérifie qu'elle est bien ouverte et en attente sur ce téléphone. Détail: " + err
    };
  }
}

// Déclaration exposée au system prompt / function-calling de Gallery.
const MEDSCRIBE_RURAL_TOOLS = [
  {
    name: "submitToMedScribeRural",
    description:
      "Transmet les champs extraits d'une page de registre de santé manuscrit à l'appli MedScribe Rural Companion, via HTTP local.",
    parameters: {
      type: "object",
      properties: {
        entryId: { type: "number" },
        callbackPort: { type: "number" },
        patientName: { type: "string" },
        age: { type: "string" },
        sex: { type: "string" },
        visitDate: { type: "string" },
        diagnosis: { type: "string" },
        treatment: { type: "string" },
        healthCenter: { type: "string" }
      },
      required: ["entryId", "callbackPort"]
    },
    handler: submitToMedScribeRural
  }
];
