# Spécification : app « Suivi des maux de tête »

## Décisions de cadrage (validées le 01/11/2026)

| Sujet | Décision | Conséquence technique |
|---|---|---|
| Champs de saisie | Date, niveau de douleur, sommeil de la veille, repas d'avant, commentaires **+ symptômes (aura, nausée, gêne à la lumière, gêne au bruit, vertiges, odeurs) + localisation (tempes, front, arrière, nuque, un seul côté, les deux côtés, autour des yeux)** | Table Room avec deux listes sérialisées (symptômes, localisations), saisie en cases à cocher |
| Plage horaire | Début **et** fin saisis en **heures pleines uniquement** (8h, 9h), aucune minute à taper | Deux colonnes `heureDebut` / `heureFin` (0 à 23), widgets de type liste déroulante d'heures |
| Crise qui dépasse la soirée | Si l'heure de fin est antérieure à l'heure de début, la fin est reportée au lendemain et la crise est signalée comme ayant dépassé la soirée | `finLe` = date du lendemain, durée calculée, drapeau `depasseSoiree` (franchit minuit, dure 6 h ou plus, ou est en cours) |
| Crise en cours | Case « Crise en cours » : pas d'heure de fin, durée = maintenant moins le début | `finLe` nullable, affichage « 20h → en cours » |
| Repas d'avant | Cases à cocher de types d'aliments : sucré, alcool, fromage, caféine, charcuterie, friture, viande rouge, repas sauté | Colonne texte (valeurs séparées par `\|`), liste fixe dans l'app, plusieurs choix possibles |
| Sommeil de la veille | Qualité seule (bon / moyen / mauvais) + compteur de réveils nocturnes | Deux colonnes : `sommeil` (enum) et `reveils` (entier 0 à 9) |
| Export | CSV pour Excel **et** JSON de sauvegarde / restauration | Deux générateurs, partage par FileProvider, restauration qui remplace la base après confirmation |
| Rappels | Aucun rappel en v1 | Pas de notification, pas d'alarme, pas de permission POST_NOTIFICATIONS |
| Mise à jour | Mécanisme identique à l'app Notes de frais | Release roulante `latest` + `version.json`, UpdateChecker / UpdateManager / AutoUpdater recopiés |
| Dépôt | Public (obligatoire pour l'auto-update), un dépôt par app | `ClawFabriceH92/suivi-maux-de-tete`, package `com.trucdecomptable.mauxdetete` |

## Modèle de données

Une crise = une ligne.

```
id            Long, auto
debutLe       Instant (date + heure pleine de début, modifiable)
finLe         Instant?, null si la crise est encore en cours
niveau        Int 0 à 10
sommeil       BON | MOYEN | MAUVAIS
reveils       Int 0 à 9
repas         List<String> (types d'aliments cochés)
symptomes     List<String>
localisations List<String>
commentaire   String
creeLe        Instant (horodatage de la saisie, non modifiable)
```

Valeurs dérivées, jamais stockées :

```
dureeHeures   = (finLe ? finLe : maintenant) - debutLe, en heures pleines
lendemain     = finLe != null && jour(finLe) != jour(debutLe)
depasseSoiree = finLe == null || lendemain || dureeHeures >= 6
```

## Écrans

### 1. Saisir (écran d'ouverture)

- Bloc « Quand » : date, heure de début et heure de fin en listes déroulantes de 0h à 23h.
  Aucun champ de minutes.
- Sous le bloc, un bandeau récapitulatif en clair : « Même journée · 1 h », ou
  « Se termine le lendemain à 9h · 13 h · a dépassé la soirée », ou « En cours depuis 20h ».
- Case « Crise en cours » qui désactive l'heure de fin.
- Niveau de douleur : curseur 0 à 10, couleur et libellé qui changent en direct
  (aucune, très légère, gênante, modérée, forte, invalidante).
- Sommeil de la veille : trois boutons (bon, moyen, mauvais) + compteur de réveils.
- Repas d'avant, symptômes, localisation : cases à cocher.
- Commentaires : zone de texte libre.
- Bouton « Enregistrer » unique. Après enregistrement : confirmation brève avec la durée calculée,
  puis formulaire remis à zéro.
- Toucher une ligne de l'historique rouvre ce même écran en modification (« Mettre à jour »).

### 2. Historique

- Crises triées de la plus récente à la plus ancienne, groupées par mois.
- Chaque ligne : jour, plage horaire (20h → 9h), mention « lendemain » si la crise a franchi minuit,
  durée, sommeil et réveils, premiers symptômes, intensité colorée sur 10.
- Badge « soirée dépassée » sur les crises concernées.
- Toucher une ligne = modifier.

### 3. Statistiques

- Crises du mois en cours.
- Intensité moyenne et durée moyenne sur 30 jours glissants.
- Crises longues : nombre ayant dépassé la soirée et durée la plus longue.
- Jours avec et sans douleur sur 30 jours.
- Graphique à barres des 30 derniers jours (hauteur = intensité maximale du jour).
- Effet du sommeil : intensité moyenne après une bonne nuit, après une mauvaise nuit, et l'écart.
- Types d'aliments les plus fréquents avant une crise.

### 4. Export

- Export CSV : `date_debut;heure_debut;date_fin;heure_fin;duree_heures;depasse_soiree;niveau;sommeil;reveils;repas;symptomes;localisations;commentaire`
  (séparateur `;`, BOM UTF-8, dates ISO, listes séparées par `|`).
- Sauvegarde complète JSON.
- Restauration depuis un fichier JSON (confirmation explicite, remplacement total).
- Version de l'app + vérification manuelle de mise à jour, avec état visible
  (jamais vérifié, vérification en cours, à jour, erreur API).

## Hors périmètre v1

- Aucun rappel ni notification.
- Aucun compte, aucune synchronisation, aucun serveur.
- Pas de PDF pour le médecin, pas de partage multi-appareils.
- Pas de médicaments (écartés au cadrage).

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

1. Tests unitaires JVM sur le parseur de version, le calcul de durée et de dépassement de soirée,
   la génération CSV et JSON, l'export des listes.
2. `apksigner verify --print-certs` sur l'APK publié : même SHA-256 que le keystore local.
3. `version.json` et `versionCode` de l'APK téléchargé depuis la release `latest` cohérents.
4. Migrations Room additives uniquement, jamais de `fallbackToDestructiveMigration`.

## Historique du cadrage

- 01/11/2026 : 5 décisions validées (symptômes et localisation ajoutés, repas en cases à cocher,
  sommeil = qualité + réveils, export CSV + JSON, aucun rappel).
- 01/11/2026 : ajout d'une plage horaire début / fin en heures pleines et du cas de la crise qui
  dépasse la soirée (report au lendemain, durée calculée, badge dans l'historique).
- 01/11/2026 : second lot d'améliorations validé, toutes implémentées dans la v1.0.0 :
  - déclencheurs suspectés (stress, fatigue, écran, chaleur, règles, sport, bruit, parfum, saut de
    repas, sommeil court) ;
  - médicament pris, efficacité, compteur mensuel de jours de prise et alerte aux seuils ICHD-3
    (15 jours pour un antalgique simple, 10 jours pour un triptan) ;
  - démarrage et clôture d'une crise en un tap, durée qui tourne sur l'écran de saisie ;
  - report de fin à J+1, J+2 ou J+3 pour les crises de plus de 24 h ;
  - ergonomie « en crise » : gros boutons, sections secondaires repliées, thème sombre par défaut ;
  - statistiques honnêtes : profil horaire, jour de semaine, tendance 7 jours, effectifs affichés,
    aucune corrélation conclue sous 5 cas ;
  - synthèse PDF pour le médecin, verrouillage biométrique, sauvegarde JSON mensuelle automatique.
  - écartés : météo et pression atmosphérique (réseau + position, résultat non concluant),
    comptes et synchronisation cloud.

## Écartés du hors périmètre initial

- Le médicament, écarté au premier tour, a été réintégré au second : c'est le seul chiffre suivi
  par le médecin (risque de céphalée par abus médicamenteux).
