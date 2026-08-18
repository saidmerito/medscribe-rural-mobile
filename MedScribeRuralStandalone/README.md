# MedScribe Rural — appli autonome

Version **autonome** de MedScribe Rural : le moteur d'inférence tourne
**dans le processus de cette appli**, via l'API Kotlin officielle
**LiteRT-LM** (`com.google.ai.edge.litertlm`). Elle ne dépend plus de
Google AI Edge Gallery ni d'aucune autre appli installée — une fois le
modèle en place, tout fonctionne 100% hors-ligne.

C'est la même API que Gallery utilise en interne pour l'inférence
(MediaPipe LLM Inference étant désormais en maintenance-only, Google
recommande LiteRT-LM pour tout nouveau projet Android).

## Comment ça marche

1. **`ModelManager`** gère le fichier modèle `.litertlm` (Gemma 3n E2B,
   multimodal texte+image). Il n'est **pas embarqué dans l'APK** —
   trop volumineux (~2-3 Go) pour un déploiement terrain à faible
   bande passante. Deux façons de l'obtenir, proposées au premier
   lancement (`ModelSetupActivity`) :
   - **Import manuel** d'un fichier `.litertlm` déjà téléchargé (par
     ex. sur ton PC, transféré ensuite sur le téléphone) ;
   - **Téléchargement one-shot** depuis Hugging Face LiteRT Community
     (Wi-Fi requis une seule fois, via le `DownloadManager` système —
     reprise automatique en cas de coupure).

2. **`LlmEngineManager`** initialise un `Engine` LiteRT-LM avec le
   modèle, et expose `extractFromImage(photoPath, prompt)` qui envoie
   la photo + un prompt d'extraction strict au modèle et récupère sa
   réponse texte.

3. **`CaptureActivity`** prend la photo, l'enregistre en base (statut
   "pending"), puis lance l'extraction en tâche de fond
   (`applicationScope`, pas `lifecycleScope`, pour survivre à la
   fermeture de l'activité). Le résultat JSON est parsé
   (`JsonExtraction`) et vient mettre à jour la fiche.

4. **`MainActivity`** affiche le dashboard des fiches avec leur statut
   (⏳ en cours / ✅ extrait / ❌ erreur / ☑️ validé), **`EntryDetailActivity`**
   permet de relire et corriger avant validation finale.

## Compiler l'APK

Je n'ai pas le SDK Android dans mon environnement d'exécution, donc pas
de binaire `.apk` livré directement — à compiler chez toi :

1. **Android Studio** (Hedgehog ou plus récent), `File > Open` sur ce
   dossier.
2. Laisse Gradle synchroniser — il ira chercher
   `com.google.ai.edge.litertlm:litertlm-android` sur Google Maven
   (déjà configuré dans `settings.gradle.kts` via `google()`).
3. `Build > Build Bundle(s)/APK(s) > Build APK(s)`, ou `Run ▶` sur ton
   téléphone branché (mode développeur + débogage USB).

## Choisir le modèle

Le projet pointe par défaut vers **Gemma 3n E2B** (`ModelManager.DEFAULT_MODEL_DOWNLOAD_URL`),
le plus petit modèle multimodal texte+image de la famille — un bon
compromis pour un déploiement terrain sur téléphone milieu de gamme.
Si tu veux un modèle plus précis (E4B) ou plus léger, remplace l'URL par
celle du modèle voulu sur
[huggingface.co/litert-community](https://huggingface.co/litert-community)
(cherche un fichier `.litertlm` avec support vision).

## Comparaison avec la version "Intent vers Gallery"

| | Intent vers Gallery | Autonome (ce projet) |
|---|---|---|
| Dépendance à une autre appli | Oui (Gallery doit être installée) | Non |
| Taille de l'APK | Petite | Petite (le modèle est téléchargé/importé à part) |
| Boucle de retour des résultats | Nécessite un Agent Skill JS chargé dans Gallery | Directe, en interne |
| Contrôle du prompt/format de sortie | Partagé avec l'UI de Gallery | Total |
| Mise à jour du modèle | Gérée par Gallery | Gérée par toi (réimporter un nouveau `.litertlm`) |

## Pistes d'évolution

- Ajouter un export vers ton pipeline n8n existant (webhook local, comme
  dans la version précédente).
- Support DHIS2 direct (champ `syncedToDhis2` déjà prévu dans `RegisterEntry`).
- Basculer `Backend.NPU(...)` si le téléphone cible a un NPU supporté,
  pour accélérer encore l'inférence (voir la doc LiteRT-LM NPU).
- Prompts multilingues (français/arabe/somali), comme ta version Kaggle
  Gemma 4 Good Hackathon.
