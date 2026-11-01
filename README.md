# Suivi des maux de tête

Application Android personnelle de suivi des crises de maux de tête. Saisie rapide, historique,
statistiques, export CSV et sauvegarde JSON. Tout reste sur le téléphone : aucun compte, aucun
serveur, aucun cloud.

## État

- Cadrage validé le 01/11/2026, second lot d'améliorations intégré (voir `docs/SPEC.md`).
- **v1.0.0 livrée** : `maux-de-tete-v1.0.0.apk` (7,5 Mo, minSdk 26), release GitHub et mise à jour
  automatique en place. Lien roulant :
  https://github.com/ClawFabriceH92/suivi-maux-de-tete/releases/download/latest/app-release.apk
- Signature : keystore dédié `mauxdetete`, SHA-256 `E4:0A:4C:15:…:77:E7` vérifié sur l'APK publié,
  donc les mises à jour s'installent par-dessus sans désinstallation.
- 32 tests unitaires JVM verts (durées et dépassement de soirée, statistiques, export CSV et JSON,
  versions et analyse des réponses GitHub) ; CI verte sur `master`.
- Maquette cliquable : `maquette.html`, vérifiée par `tools/verifier_maquette.py` (72 contrôles).

## Ce que fait l'app

- **Saisir** : date, heure de début et de fin en heures pleines, douleur 0 à 10, sommeil de la veille
  et réveils, puis déclencheurs, médicament, repas, symptômes, localisation et commentaire.
- **Dépasse la soirée** : une fin antérieure au début est reportée à J+1, J+2 ou J+3, durée calculée,
  badge dans l'historique. Crise en cours : démarrage et clôture en un tap.
- **Historique** : groupé par mois, plage horaire, durée, modification et suppression.
- **Statistiques** : intensité et durée moyennes, jours sans douleur, tendance, profil horaire, jour de
  la semaine, crises longues, médicaments avec seuil ICHD-3 (15 j/mois, 10 j pour un triptan), effet du
  sommeil et déclencheurs, avec effectifs et aucune conclusion sous 5 cas.
- **Export** : CSV Excel (`;` + BOM UTF-8), sauvegarde JSON avec restauration, synthèse PDF.
- **Réglages** : verrouillage biométrique, sauvegarde mensuelle automatique, mise à jour intégrée.

## Développement

```bash
export ANDROID_HOME=/opt/android-sdk ANDROID_SDK_ROOT=/opt/android-sdk
./gradlew testDebugUnitTest assembleRelease --no-daemon
```

Signature locale : `/root/.secrets/keystores-android/mauxdetete-keystore.env`, les mêmes valeurs étant
en secrets GitHub (`MAUXDETETE_*`). CI : `.github/workflows/build.yml` sur `master`.

## Maquette

Ouvrir `maquette.html` dans un navigateur (Chrome, mode mobile). Vérification automatique :

```bash
/root/.venvs/pw/bin/python tools/verifier_maquette.py
```

## Cible technique

- Kotlin + Jetpack Compose, Material 3, thème clair et sombre.
- Room pour le stockage local.
- Export CSV (Excel, séparateur `;` + BOM UTF-8) et JSON (sauvegarde et restauration) via FileProvider.
- Mise à jour automatique depuis GitHub Releases, mécanisme identique à l'app Notes de frais :
  release roulante `latest` + `version.json`, vérification au lancement puis toutes les heures,
  téléchargement et installation avec confirmation système.
- Keystore dédié, APK signé en release, workflow GitHub Actions sur la branche `master`.

## Livraison

- APK versionné : `<app>-v<version>.apk`.
- Lien roulant : `https://github.com/ClawFabriceH92/suivi-maux-de-tete/releases/download/latest/app-release.apk`
