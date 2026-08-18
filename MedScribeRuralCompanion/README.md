# MedScribe Rural Companion

Application Android compagnon de **MedScribe Rural**, qui délègue
l'extraction de texte manuscrit à **Google AI Edge Gallery** (Gemma 4,
100% on-device) plutôt qu'à Ollama, pour un usage terrain sans PC.

## ⚠️ Limite importante à connaître avant de commencer

Google AI Edge Gallery **n'expose aucune API d'intent publique**
permettant à une appli tierce de lui envoyer une image et de recevoir
automatiquement un résultat structuré en retour. Son seul mécanisme
d'extensibilité officiel est le système d'**Agent Skills** (fichiers
JS chargés dans l'appli, invoqués par le modèle via function calling).

Ce projet contourne cette limite en deux morceaux qui se complètent :

1. **`app/`** — l'APK Android que tu vas compiler. Il :
   - prend la photo du registre (caméra),
   - l'envoie à Gallery via un `Intent.ACTION_SEND` classique (comme un
     partage vers une autre appli), avec un prompt pré-rempli,
   - fait tourner un petit serveur HTTP local (`127.0.0.1:8765`) qui
     attend le résultat,
   - stocke les fiches dans une base SQLite locale (Room) et affiche un
     dashboard simple.

2. **`skill/medscribe-rural-skill/`** — un Agent Skill à charger **une
   fois** dans Gallery (`SKILL.md` + `skill.js`). C'est lui qui, une fois
   que Gemma a lu la photo, poste le JSON extrait vers le serveur local
   de l'appli — c'est la pièce qui referme la boucle.

Sans l'étape 2, l'appli enverra bien la photo à Gallery, mais aucune
donnée ne reviendra automatiquement : il faudra copier-coller le
résultat à la main. Avec l'étape 2, le flux est bout-en-bout.

## Compiler l'APK

Je n'ai pas le SDK Android/Gradle dans mon environnement d'exécution,
donc je ne peux pas te livrer directement le binaire `.apk` — il faut
compiler en local. Chez toi (Windows, puisque c'est ton environnement
habituel pour MedScribe Rural) :

1. Installe **Android Studio** (Hedgehog ou plus récent).
2. `File > Open` → sélectionne le dossier `MedScribeRuralCompanion/`.
3. Laisse Gradle synchroniser (il téléchargera les dépendances listées
   dans `app/build.gradle.kts` — Room, NanoHTTPD, AndroidX, Material).
4. Branche ton téléphone (mode développeur + débogage USB activé), puis
   `Run ▶` — ou `Build > Build Bundle(s)/APK(s) > Build APK(s)` pour
   générer directement le fichier `.apk` dans
   `app/build/outputs/apk/debug/`.

## Installer le skill dans Gallery

1. Ouvre Google AI Edge Gallery, charge un modèle **Gemma 4** (E2B ou
   E4B — nécessaire pour le function calling des Agent Skills).
2. Va dans **Agent Skills → Skill Manager** (bouton "Skills").
3. Tape sur **+ → Import Local Skill**, et pointe vers le dossier
   `skill/medscribe-rural-skill/` (ou zippe-le si l'import demande un
   fichier unique — voir la doc du dépôt `google-ai-edge/gallery` pour
   le format exact attendu selon la version installée).
4. Active le skill.

## Utilisation sur le terrain

1. Ouvre **MedScribe Rural Companion**, tape sur **+**.
2. Photographie la page du registre.
3. L'appli ouvre automatiquement Gallery avec la photo et un prompt
   pré-rempli qui invoque le skill.
4. Dans Gallery, laisse Gemma analyser la photo — le skill transmet le
   résultat en arrière-plan.
5. Reviens sur MedScribe Rural Companion : la fiche passe de
   "⏳ en attente" à "✅ extrait". Tape dessus pour relire/corriger les
   champs avant de les valider.

## Pistes d'évolution

- Ajouter un export périodique des fiches validées vers ton pipeline
  n8n existant (webhook local → même serveur que `IngestHttpServer`).
- Ajouter la synchronisation DHIS2 directement depuis l'appli (champ
  `syncedToDhis2` déjà prévu dans `RegisterEntry`).
- Support multilingue (français/arabe/somali) dans les prompts envoyés
  à Gallery, comme dans ta version Kaggle Gemma 4 Good Hackathon.
