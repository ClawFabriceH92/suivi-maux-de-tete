# Spécification : app « Suivi des maux de tête »

## Décisions de cadrage (validées le 01/11/2026)

| Sujet | Décision | Conséquence technique |
|---|---|---|
| Champs de saisie | Date, niveau de douleur, sommeil de la veille, repas d'avant, commentaires **+ symptômes (aura, nausée, gêne à la lumière, gêne au bruit, vertiges, odeurs) + localisation (tempes, front, arrière, nuque, un seul côté, les deux côtés, autour des yeux)** | Table Room avec deux listes sérialisées (symptômes, localisations), saisie en cases à cocher |
| Repas d'avant | Cases à cocher de types d'aliments : sucré, alcool, fromage, caféine, charcuterie, friture, viande rouge, repas sauté | Colonne texte (valeurs séparées par `|`), liste fixe dans l'app, plusieurs choix possibles |
| Sommeil de la veille | Qualité seule (bon / moyen / mauvais) + compteur de réveils nocturnes | Deux colonnes : `sommeil` (enum) et `reveils` (entier 0 à 9) |
| Export | CSV pour Excel **et** JSON de sauvegarde / restauration | Deux générateurs, partage par FileProvider, restauration qui remplace la base après confirmation |
| Rappels | Aucun rappel en v1 | Pas de notification, pas d'alarme, pas de permission POST_NOTIFICATIONS |
| Mise à jour | Mécanisme identique à l'app Notes de frais | Release roulante `latest` + `version.json`, UpdateChecker / UpdateManager / AutoUpdater recopiés |
| Dépôt | Public (obligatoire pour l'auto-update), un dépôt par app | `ClawFabriceH92/suivi-maux-de-tete`, package `com.trucdecomptable.mauxdetete` |

## Modèle de données

Une crise = une ligne.

```
id            Long, auto
dateHeure     Instant (horodatage de la crise, modifiable)
niveau        Int 0 à 10
sommeil       BON | MOYEN | MAUVAIS
reveils       Int 0 à 9
repas         List<String> (types d'aliments cochés)
symptomes     List<String>
localisations List<String>
commentaire   String
creeLe        Instant (horodatage de la saisie, non modifiable)
```

## Écrans

### 1. Saisir (écran d'ouverture)

- Date et heure pré-remplies à maintenant, modifiables.
- Niveau de douleur : curseur 0 à 10, couleur et libellé qui changent en direct
  (aucune, très légère, gênante, modérée, forte, invalidante).
- Sommeil de la veille : trois boutons (bon, moyen, mauvais) + compteur de réveils.
- Repas d'avant : cases à cocher.
- Symptômes : cases à cocher.
- Localisation : cases à cocher.
- Commentaires : zone de texte libre.
- Bouton « Enregistrer » unique. Après enregistrement : confirmation brève et formulaire remis à zéro.
- Toucher une ligne de l'historique rouvre ce même écran en modification (« Mettre à jour »).

### 2. Historique

- Crises triées de la plus récente à la plus ancienne, groupées par mois.
- Chaque ligne : jour et heure, sommeil et réveils, premiers symptômes, intensité colorée sur 10.
- Toucher une ligne = modifier.

### 3. Statistiques

- Crises du mois en cours.
- Intensité moyenne sur 30 jours glissants.
- Jours avec et sans douleur sur 30 jours.
- Graphique à barres des 30 derniers jours (hauteur = intensité maximale du jour).
- Effet du sommeil : intensité moyenne après une bonne nuit, après une mauvaise nuit, et l'écart.
- Types d'aliments les plus fréquents avant une crise.

### 4. Export

- Export CSV (séparateur `;`, BOM UTF-8, dates ISO, listes séparées par `|`).
- Sauvegarde complète JSON.
- Restauration depuis un fichier JSON (confirmation explicite, remplacement total).
- Version de l'app + vérification manuelle de mise à jour, avec état visible
  (jamais vérifié, vérification en cours, à jour, erreur API).

## Hors périmètre v1

- Aucun rappel ni notification.
- Aucun compte, aucune synchronisation, aucun serveur.
- Pas de PDF pour le médecin, pas de partage multi-appareils.
- Pas de médicaments ni de durée de crise (écartés au cadrage).

## Mise à jour automatique

Identique à l'app Notes de frais (`/root/projects/notes-de-frais`) :

- `UpdateChecker` : lit d'abord `version.json` de la release `latest`, puis l'API GitHub si absent.
  Comparaison de versions segment par segment, `lastError` distinct de « à jour ».
- `UpdateManager` : StateFlow IDLE / AVAILABLE / DOWNLOADING / READY_TO_INSTALL / UP_TO_DATE / FAILED,
  plancher d'une heure entre deux vérifications, pré-téléchargement silencieux sur réseau non facturé.
- `AutoUpdater` : téléchargement direct `HttpURLConnection` avec suivi des redirections, contrôle des
  octets magiques `PK`, installation via FileProvider.
- Dialogue de mise à jour visible au-dessus de tous les onglets, « Plus tard » explicite.
- Sauvegarde de la base **avant** chaque installation.

## Vérifications avant livraison

1. Tests unitaires JVM sur le parseur de version, la génération CSV et JSON, l'export des listes.
2. `apksigner verify --print-certs` sur l'APK publié : même SHA-256 que le keystore local.
3. `version.json` et `versionCode` de l'APK téléchargé depuis la release `latest` cohérents.
4. Migrations Room additives uniquement, jamais de `fallbackToDestructiveMigration`.
