# Suivi des maux de tête

Application Android personnelle de suivi des crises de maux de tête. Saisie rapide, historique,
statistiques, export CSV et sauvegarde JSON. Tout reste sur le téléphone : aucun compte, aucun
serveur, aucun cloud.

## État

- Cadrage validé le 01/11/2026 (voir `docs/SPEC.md`).
- Maquette cliquable : `maquette.html` (simulateur de téléphone, thème clair et sombre).
- Code de l'app : à venir après validation de la maquette.

## Maquette

Ouvrir `maquette.html` dans un navigateur (Chrome, mode mobile). Quatre onglets : Saisir,
Historique, Stats, Export.

Vérification automatique de la maquette (Playwright, 37 contrôles, captures dans `docs/captures/`) :

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
