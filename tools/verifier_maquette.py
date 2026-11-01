#!/usr/bin/env python3
"""Vérifie la maquette cliquable de l'app Suivi des maux de tête.

Usage : /root/.venvs/pw/bin/python tools/verifier_maquette.py
Sort en code 1 si un contrôle échoue. Produit les captures dans docs/captures/.
"""
import pathlib
import sys

from playwright.sync_api import sync_playwright

RACINE = pathlib.Path(__file__).resolve().parent.parent
MAQUETTE = RACINE / "maquette.html"
CAPTURES = RACINE / "docs" / "captures"

resultats = []


def check(nom, condition, detail=""):
    resultats.append((nom, bool(condition), detail))
    print(("OK    " if condition else "ECHEC ") + nom + ((" | " + str(detail)) if detail else ""))
    return bool(condition)


def main():
    CAPTURES.mkdir(parents=True, exist_ok=True)
    erreurs = []

    with sync_playwright() as p:
        nav = p.chromium.launch()
        page = nav.new_page(viewport={"width": 1180, "height": 980}, device_scale_factor=2)
        page.on("console", lambda m: erreurs.append("console:" + m.text) if m.type == "error" else None)
        page.on("pageerror", lambda e: erreurs.append("pageerror:" + str(e)))
        page.goto(MAQUETTE.as_uri())
        page.wait_for_timeout(400)

        def txt():
            return page.inner_text("#content").lower()

        def contient(*morceaux):
            t = txt()
            return all(m.lower() in t for m in morceaux)

        def capture(nom):
            """Capture le cadre du téléphone, toast masqué pour ne pas polluer l'image."""
            page.evaluate("document.querySelector('#toast').classList.remove('show')")
            page.wait_for_timeout(120)
            page.screenshot(path=str(CAPTURES / nom), clip=page.locator("#phone").bounding_box())

        # 1. écran de saisie au chargement
        check("écran Saisir affiché", contient("nouvelle crise", "niveau de douleur", "sommeil la veille"))
        check("tous les champs sont là",
              contient("quand", "niveau de douleur", "sommeil la veille", "repas d'avant",
                       "symptômes", "localisation", "commentaires"))
        check("saisie par heures pleines (2 listes d'heures)",
              page.query_selector("#fHd") is not None and page.query_selector("#fHf") is not None)
        options = page.eval_on_selector_all("#fHd option", "els => els.map(e => e.textContent)")
        check("24 heures proposées, sans minutes", len(options) == 24 and options[8] == "8h" and options[21] == "21h",
              options[:3])
        check("aucun champ de minutes", page.query_selector("input[type='time']") is None
              and page.query_selector("#fDate") is None)
        check("niveau par défaut à 5", page.inner_text("#lvlNum").strip() == "5", page.inner_text("#lvlNum"))
        check("libellé du niveau cohérent", "modérée" in page.inner_text("#lvlTxt").lower(),
              page.inner_text("#lvlTxt"))
        check("date pré-remplie au format AAAA-MM-JJ", len(page.input_value("#fJour")) == 10,
              page.input_value("#fJour"))
        check("nombre de crises initial", page.evaluate("window.__app.count()") == 10,
              page.evaluate("window.__app.count()"))

        # 2. plage horaire : 8h -> 9h, même journée
        page.select_option("#fHd", "8")
        page.select_option("#fHf", "9")
        page.wait_for_timeout(150)
        plage = page.inner_text("#plage").lower()
        check("8h → 9h donne 1 h", "1 h" in plage and "lendemain" not in plage, plage)
        check("8h → 9h ne dépasse pas la soirée", "dépassé" not in plage, plage)

        # 3. plage horaire : 20h -> 9h, passage au lendemain
        page.select_option("#fHd", "20")
        page.select_option("#fHf", "9")
        page.wait_for_timeout(150)
        plage = page.inner_text("#plage").lower()
        check("20h → 9h bascule au lendemain", "lendemain" in plage, plage)
        check("20h → 9h annonce la soirée dépassée", "dépassé" in plage, plage)
        check("20h → 9h calculé à 13 h", "13 h" in plage, plage)
        capture("06-depasse-soiree.png")

        # 4. crise encore en cours
        page.click("[data-action='encours']")
        page.wait_for_timeout(150)
        plage = page.inner_text("#plage").lower()
        check("mode « en cours » sans heure de fin", "en cours" in plage, plage)
        check("liste des heures de fin désactivée", page.is_disabled("#fHf"))
        page.click("[data-action='encours']")
        page.wait_for_timeout(150)
        check("retour au mode avec heure de fin", not page.is_disabled("#fHf"))

        # 5. curseur de douleur
        page.evaluate("""() => {
            const s = document.querySelector('#fNiveau');
            s.value = 8;
            s.dispatchEvent(new Event('input', {bubbles:true}));
        }""")
        page.wait_for_timeout(120)
        check("curseur à 8 met à jour l'affichage", page.inner_text("#lvlNum").strip() == "8")
        check("curseur à 8 donne le libellé « forte »", "forte" in page.inner_text("#lvlTxt").lower(),
              page.inner_text("#lvlTxt"))

        # 6. sommeil + réveils
        page.click("button.chip[data-sommeil='mauvais']")
        page.wait_for_timeout(120)
        check("sommeil mauvais sélectionné",
              "on" in (page.get_attribute("button.chip[data-sommeil='mauvais']", "class") or ""))
        page.click("[data-reveil='1']")
        page.click("[data-reveil='1']")
        page.wait_for_timeout(120)
        check("compteur de réveils à 2", page.inner_text("#revVal").strip() == "2", page.inner_text("#revVal"))
        page.click("[data-reveil='-1']")
        page.wait_for_timeout(100)
        check("compteur de réveils descend à 1", page.inner_text("#revVal").strip() == "1")

        # 7. repas, symptômes, localisation, commentaire
        page.click("button.chip[data-repas='Fromage']")
        page.click("button.chip[data-sympt='Aura visuelle']")
        page.click("button.chip[data-loc='Nuque']")
        page.fill("#fCom", "Test de saisie automatisé")
        page.wait_for_timeout(150)
        form = page.evaluate("window.__app.form()")
        check("repas coché", "Fromage" in form["repas"], form["repas"])
        check("symptôme coché", "Aura visuelle" in form["symptomes"], form["symptomes"])
        check("localisation cochée", "Nuque" in form["loc"], form["loc"])
        check("commentaire pris en compte", form["com"] == "Test de saisie automatisé")
        check("heures retenues dans le formulaire", form["hDebut"] == 20 and form["hFin"] == 9,
              (form["hDebut"], form["hFin"]))

        # 8. enregistrement d'une crise qui dépasse la soirée
        page.click("[data-action='enregistrer']")
        page.wait_for_timeout(250)
        check("crise enregistrée (11)", page.evaluate("window.__app.count()") == 11,
              page.evaluate("window.__app.count()"))
        derniere = page.evaluate("window.__app.derniere()")
        check("durée enregistrée = 13 h", derniere["duree"] == 13, derniere)
        check("crise marquée comme dépassant la soirée", derniere["depasse"] is True, derniere)
        check("toast de confirmation", "enregistrée" in page.inner_text("#toast").lower(),
              page.inner_text("#toast"))
        check("formulaire remis à zéro", page.inner_text("#lvlNum").strip() == "5")

        # 9. historique
        page.click("[data-nav='histo']")
        page.wait_for_timeout(220)
        n_items = page.eval_on_selector_all(".item", "els => els.length")
        check("historique liste les 11 crises", n_items == 11, n_items)
        check("historique groupé par mois", page.eval_on_selector_all(".mois", "e => e.length") >= 2,
              page.eval_on_selector_all(".mois", "e => e.length"))
        check("historique affiche une plage horaire", "→" in page.inner_text("#content"), txt()[:80])
        check("badge « soirée dépassée » présent", "soirée dépassée" in txt())
        check("durée visible dans l'historique", "13 h" in txt())

        # 10. réouverture en modification
        page.click(".item")
        page.wait_for_timeout(220)
        check("titre en mode modification", contient("modifier la crise"))
        check("heures pré-remplies en modification",
              page.input_value("#fHd") == "20" and page.input_value("#fHf") == "9",
              (page.input_value("#fHd"), page.input_value("#fHf")))
        page.click("[data-action='annulerEdition']")
        page.wait_for_timeout(150)
        page.click("[data-nav='histo']")
        page.wait_for_timeout(150)
        check("annulation ne crée pas de doublon", page.evaluate("window.__app.count()") == 11)

        # 11. statistiques
        page.click("[data-nav='stats']")
        page.wait_for_timeout(250)
        nb_barres = page.eval_on_selector_all(".bars i", "els => els.length")
        check("graphique sur 30 jours", nb_barres == 30, nb_barres)
        check("4 indicateurs clés", page.eval_on_selector_all(".kpi", "els => els.length") == 4)
        check("durée moyenne affichée", "durée moyenne" in txt())
        check("crises longues comptées", "dépassé la soirée" in txt() and "sur" in txt())
        check("stats expliquent l'effet du sommeil",
              contient("effet du sommeil", "bonne nuit", "mauvaise nuit"))
        capture("03-stats.png")

        # 12. export
        page.click("[data-nav='export']")
        page.wait_for_timeout(200)
        check("écran export propose CSV, JSON et restauration",
              contient("exporter en csv", "sauvegarde complète", "restaurer"))
        page.click("[data-action='csv']")
        page.wait_for_timeout(1800)
        check("fichier CSV produit", page.eval_on_selector_all(".file", "e => e.length") == 1)
        check("nom du CSV horodaté", ".csv" in txt())
        page.click("[data-action='json']")
        page.wait_for_timeout(1800)
        check("sauvegarde JSON produite", page.eval_on_selector_all(".file", "e => e.length") == 2)

        # 13. mise à jour
        page.click("[data-action='maj']")
        page.wait_for_timeout(300)
        check("état de vérification visible", "vérification" in txt())
        page.wait_for_timeout(1600)
        check("mise à jour conclut « à jour »", "à jour" in txt())
        capture("04-export.png")

        # 14. captures + thème clair
        page.click("[data-nav='saisir']")
        page.wait_for_timeout(200)
        capture("01-saisir-sombre.png")
        page.click("[data-nav='histo']")
        page.wait_for_timeout(200)
        capture("02-historique.png")

        rendus_avant = page.evaluate("window.__app.rendus()")
        page.wait_for_timeout(1000)
        check("le rendu vit (compteur)", page.evaluate("window.__app.rendus()") >= rendus_avant)

        page.click("#btnTheme")
        page.wait_for_timeout(350)
        check("bascule en thème clair", page.get_attribute("html", "data-theme") == "light",
              page.get_attribute("html", "data-theme"))
        page.click("[data-nav='saisir']")
        page.wait_for_timeout(250)
        capture("05-saisir-clair.png")

        check("aucune erreur console", len(erreurs) == 0, str(erreurs[:3]) if erreurs else "")
        nav.close()

    ok = sum(1 for _, c, _ in resultats if c)
    print("\nRESULTAT : %d/%d contrôles OK" % (ok, len(resultats)))
    return 0 if ok == len(resultats) else 1


if __name__ == "__main__":
    sys.exit(main())
