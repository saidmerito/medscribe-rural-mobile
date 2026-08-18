# MedScribe Rural — mobile

Deux projets Android, compilés automatiquement dans le cloud via GitHub
Actions (aucun PC requis) :

- **`MedScribeRuralCompanion/`** — envoie la photo du registre à Google
  AI Edge Gallery installée sur le téléphone (via Intent), avec un Agent
  Skill (`skill/medscribe-rural-skill/`) à charger dans Gallery pour
  fermer la boucle.
- **`MedScribeRuralStandalone/`** — moteur LiteRT-LM embarqué
  directement dans l'appli, aucune dépendance externe une fois le
  modèle `.litertlm` installé.

## Récupérer les APK compilées

1. Onglet **Actions** de ce dépôt (sur GitHub, y compris depuis
   l'appli mobile ou le navigateur du téléphone).
2. Choisis le run le plus récent de **"Build APK (Companion)"** ou
   **"Build APK (Standalone)"**.
3. En bas de la page du run, section **Artifacts** : télécharge le
   `.zip` (il contient l'APK debug).
4. Dézippe (la plupart des gestionnaires de fichiers Android savent
   le faire), puis ouvre le `.apk` pour l'installer — Android demandera
   d'autoriser "Installer des applications inconnues" pour l'appli
   utilisée (navigateur ou gestionnaire de fichiers) la première fois.

Chaque `push` sur `main` qui touche l'un des deux dossiers relance
automatiquement le build correspondant. Tu peux aussi déclencher un
build manuellement depuis l'onglet Actions → sélectionne le workflow →
**Run workflow**.
