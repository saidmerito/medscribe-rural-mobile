---
name: MedScribe Rural
description: Extrait les informations d'une page de registre de santé manuscrit (nom, âge, sexe, date, diagnostic, traitement, centre de santé) et les transmet à l'appli MedScribe Rural Companion pour digitalisation. À utiliser quand l'utilisateur envoie une photo d'un registre papier de santé et demande une extraction ou une digitalisation.
version: 1.0.0
author: Said — Ministère de la Santé de Djibouti
---

# MedScribe Rural

## Quand utiliser ce skill

Déclenche ce skill quand :
- l'utilisateur partage une photo d'une page de registre de santé manuscrit, ET
- le prompt mentionne "MedScribe Rural", "digitaliser", "extraire le registre", ou contient `entryId=` / `callbackPort=`.

## Instructions

1. Analyse l'image reçue (registre manuscrit). Identifie, pour la ligne ou
   l'entrée la plus récente/lisible, les champs suivants : `patientName`,
   `age`, `sex` (M/F), `visitDate` (format `YYYY-MM-DD` si possible),
   `diagnosis`, `treatment`, `healthCenter`.
2. Si un champ est illisible ou absent, laisse-le vide plutôt que de
   deviner — la fiche sera relue par un agent de santé côté appli.
3. Récupère `entryId` et `callbackPort` dans le prompt de l'utilisateur.
4. Appelle la fonction `submitToMedScribeRural` (voir skill.js) avec un
   objet JSON contenant tous les champs ci-dessus plus `entryId`.
5. Confirme à l'utilisateur en une phrase que les données ont été
   transmises à l'appli MedScribe Rural Companion.

## Notes de confidentialité

Toutes les données restent sur l'appareil : l'extraction se fait
localement avec le modèle chargé dans Gallery, et le transfert vers
l'appli se fait uniquement en HTTP local (`127.0.0.1`), jamais sur
Internet.
