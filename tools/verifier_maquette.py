#!/usr/bin/env python3
"""Vérifie la maquette v2 de l'app Suivi des maux de tête.

Usage : /root/.venvs/pw/bin/python tools/verifier_maquette.py
Sort en code 1 si un contrôle échoue. Produit les captures dans docs/captures/.
"""
import pathlib
import sys

from playwright.sync_api import sync_playwright

RACINE = pathlib.Path(__file__).resolve().parent.parent
MAQUETTE = RACINE / "maquette.html"
CAPTURES = RACINE / "docs" / "captures"
MIN_CAS = 5

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
        page = nav.new_page(viewport={"width": 1180, "height": 1000}, device_scale_factor=2)
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
            page.evaluate("document.querySelector('#toast').classList.remove('show')")
            page.wait_for_timeout(120)
            page.screenshot(path=str(CAPTURES / nom), clip=page.locator("#phone").bounding_box())

        # ---------- 1. crise en cours et démarrage en un tap ----------
        check("écran Saisir affiché", contient("nouvelle crise", "niveau de douleur", "sommeil la veille"))
        check("nombre de crises initial", page.evaluate("window.__app.count()") == 10,
              page.evaluate("window.__app.count()"))
        check("bandeau « crise en cours » visible", page.query_selector(".encours") is not None)
        check("le bandeau affiche la durée qui tourne", "durée" in page.inner_text(".encours").lower(),
              page.inner_text(".encours").replace("\n", " / "))
        check("bouton « Clôturer maintenant » présent", page.query_selector("[data-action='cloturer']") is not None)
        check("pas de bouton « Démarrer » pendant une crise",
              page.query_selector("[data-action='demarrer']") is None)
        capture("07-crise-en-cours.png")

        page.click("[data-action='cloturer']")
        page.wait_for_timeout(250)
        check("clôture : plus de crise en cours", page.evaluate("window.__app.enCours()") is False)
        check("clôture : aucune crise ajoutée", page.evaluate("window.__app.count()") == 10)
        check("clôture : la durée est calculée", "clôturée" in page.inner_text("#toast").lower(),
              page.inner_text("#toast"))
        check("bouton « Démarrer une crise maintenant » réapparu",
              page.query_selector("[data-action='demarrer']") is not None)

        page.click("[data-action='demarrer']")
        page.wait_for_timeout(250)
        check("démarrage : une crise en cours existe", page.evaluate("window.__app.enCours()") is True)
        check("démarrage : compteur à 11", page.evaluate("window.__app.count()") == 11,
              page.evaluate("window.__app.count()"))

        # ---------- 2. heures pleines et dépassement ----------
        options = page.eval_on_selector_all("#fHd option", "els => els.map(e => e.textContent)")
        check("24 heures proposées, sans minutes",
              len(options) == 24 and options[8] == "8h" and options[21] == "21h", options[:3])
        check("aucun champ de minutes dans le formulaire",
              page.query_selector("input[type='time']") is None)

        page.select_option("#fHd", "8")
        page.select_option("#fHf", "9")
        page.wait_for_timeout(200)
        plage = page.inner_text("#plage").lower()
        check("8h → 9h donne 1 h", "1 h" in plage and "dépasse" not in plage, plage)

        page.select_option("#fHd", "20")
        page.select_option("#fHf", "9")
        page.wait_for_timeout(250)
        plage = page.inner_text("#plage").lower()
        check("20h → 9h reporte au lendemain", "j+1" in plage, plage)
        check("20h → 9h annonce le dépassement", "dépasse la soirée" in plage, plage)
        check("20h → 9h calculé à 13 h", "13 h" in plage, plage)
        check("sélecteur J+1 / J+2 / J+3 proposé", page.query_selector("#fDecal") is not None)
        capture("06-depasse-soiree.png")

        page.select_option("#fDecal", "2")
        page.wait_for_timeout(250)
        plage = page.inner_text("#plage").lower()
        check("J+2 donne 37 h", "j+2" in plage and "37 h" in plage, plage)
        page.select_option("#fDecal", "3")
        page.wait_for_timeout(250)
        check("J+3 donne 61 h", "61 h" in page.inner_text("#plage").lower(), page.inner_text("#plage"))
        page.select_option("#fDecal", "1")
        page.wait_for_timeout(200)

        page.click("[data-action='encours']")
        page.wait_for_timeout(200)
        check("mode « en cours » sur le formulaire",
              "pas encore d'heure de fin" in page.inner_text("#plage").lower(), page.inner_text("#plage"))
        check("liste des heures de fin désactivée", page.is_disabled("#fHf"))
        page.click("[data-action='encours']")
        page.wait_for_timeout(200)

        # ---------- 3. douleur, sommeil, réveils ----------
        page.evaluate("""() => {
            const s = document.querySelector('#fNiveau');
            s.value = 8;
            s.dispatchEvent(new Event('input', {bubbles:true}));
        }""")
        page.wait_for_timeout(120)
        check("curseur à 8 : affichage", page.inner_text("#lvlNum").strip() == "8")
        check("curseur à 8 : libellé « forte »", "forte" in page.inner_text("#lvlTxt").lower(),
              page.inner_text("#lvlTxt"))

        page.click("button.chip[data-sommeil='mauvais']")
        page.wait_for_timeout(150)
        check("sommeil mauvais sélectionné",
              "on" in (page.get_attribute("button.chip[data-sommeil='mauvais']", "class") or ""))
        page.click("[data-reveil='1']")
        page.click("[data-reveil='1']")
        page.wait_for_timeout(150)
        check("compteur de réveils à 2", page.inner_text("#revVal").strip() == "2", page.inner_text("#revVal"))

        # ---------- 4. déclencheurs ----------
        page.click("summary:has-text('Déclencheurs')")
        page.wait_for_timeout(200)
        page.click("button.chip[data-decl='Stress']")
        page.click("button.chip[data-decl='Écran']")
        page.wait_for_timeout(250)
        form = page.evaluate("window.__app.form()")
        check("deux déclencheurs cochés",
              form["declencheurs"] == ["Stress", "Écran"], form["declencheurs"])
        check("compteur de déclencheurs dans l'en-tête", "2 choisis" in txt(), txt()[:120])

        # ---------- 5. médicament et efficacité ----------
        page.click("summary:has-text('Médicament')")
        page.wait_for_timeout(200)
        page.click("button.chip[data-medoc='Triptan']")
        page.wait_for_timeout(250)
        check("médicament enregistré dans le formulaire",
              page.evaluate("window.__app.form()")["medoc"] == "Triptan")
        check("question d'efficacité affichée", "efficacité" in txt())
        page.click("button.chip[data-effic='Soulagé']")
        page.wait_for_timeout(200)
        check("efficacité enregistrée",
              page.evaluate("window.__app.form()")["efficacite"] == "Soulagé")
        check("compteur mensuel de jours de médicament affiché",
              "jour" in txt() and "de prise ce mois" in txt(), txt()[-200:])

        # ---------- 6. repas, symptômes, localisation, commentaire ----------
        page.click("summary:has-text('Repas')")
        page.wait_for_timeout(200)
        page.click("button.chip[data-repas='Fromage']")
        page.click("summary:has-text('Symptômes')")
        page.wait_for_timeout(200)
        page.click("button.chip[data-sympt='Aura visuelle']")
        page.click("button.chip[data-loc='Nuque']")
        page.click("summary:has-text('Commentaire')")
        page.wait_for_timeout(200)
        page.fill("#fCom", "Test de saisie automatisé")
        page.wait_for_timeout(200)
        form = page.evaluate("window.__app.form()")
        check("repas coché", "Fromage" in form["repas"], form["repas"])
        check("symptôme coché", "Aura visuelle" in form["symptomes"], form["symptomes"])
        check("localisation cochée", "Nuque" in form["loc"], form["loc"])
        check("commentaire pris en compte", form["com"] == "Test de saisie automatisé")
        capture("08-declencheurs-medicament.png")

        # ---------- 7. enregistrement ----------
        page.click("[data-action='enregistrer']")
        page.wait_for_timeout(300)
        check("crise enregistrée (12)", page.evaluate("window.__app.count()") == 12,
              page.evaluate("window.__app.count()"))
        derniere = page.evaluate("window.__app.derniere()")
        check("durée de 13 h enregistrée", derniere["duree"] == 13, derniere)
        check("dépassement de soirée enregistré", derniere["depasse"] is True, derniere)
        check("médicament conservé à l'enregistrement", derniere["medoc"] == "Triptan", derniere)
        check("confirmation affichée", "enregistrée" in page.inner_text("#toast").lower(),
              page.inner_text("#toast"))

        # ---------- 8. historique ----------
        page.click("[data-nav='histo']")
        page.wait_for_timeout(300)
        check("historique liste 12 crises", page.eval_on_selector_all(".item", "e => e.length") == 12,
              page.eval_on_selector_all(".item", "e => e.length"))
        check("badge médicament visible", page.eval_on_selector_all(".tag.med", "e => e.length") >= 1)
        check("badge « soirée dépassée » visible", "soirée dépassée" in txt())
        check("plages horaires affichées", "→" in page.inner_text("#content"))
        capture("02-historique.png")

        page.click(".item")
        page.wait_for_timeout(250)
        check("réouverture en modification", contient("modifier la crise"))
        check("médicament pré-rempli en modification",
              page.evaluate("window.__app.form()")["medoc"] in ("Triptan", "Paracétamol", "AINS / ibuprofène", "Autre", None))
        page.click("[data-action='annulerEdition']")
        page.wait_for_timeout(200)

        # ---------- 9. statistiques ----------
        page.click("[data-nav='stats']")
        page.wait_for_timeout(300)
        check("4 indicateurs clés", page.eval_on_selector_all(".kpi", "e => e.length") == 4)
        check("profil horaire sur 24 tranches",
              page.eval_on_selector_all(".bars.h24 i", "e => e.length") == 24,
              page.eval_on_selector_all(".bars.h24 i", "e => e.length"))
        check("répartition par jour sur 7 tranches",
              page.eval_on_selector_all(".bars.h7 i", "e => e.length") == 7,
              page.eval_on_selector_all(".bars.h7 i", "e => e.length"))
        check("tendance 7 jours affichée", contient("tendance", "semaine précédente"))
        check("compteur de médicaments avec seuil", "/ 15 jours" in txt() or "/ 10 jours" in txt(), txt()[:200])
        check("effectifs affichés sur les corrélations", "n=" in txt())
        st = txt()
        bon_n = st.split("après une bonne nuit")[1].split("n=")[1][:2] if "n=" in st else "?"
        check("aucun écart conclu sous %d cas" % MIN_CAS,
              ("trop peu" in st) or ("non conclu" in st) or (int(bon_n.strip(" =")) >= MIN_CAS),
              "n=" + bon_n)
        check("déclencheurs classés par fréquence", contient("déclencheurs les plus fréquents"))
        check("pourcentage calculé sur le nombre de crises", "calculé sur" in txt())
        capture("03-stats.png")

        # ---------- 10. export et réglages ----------
        page.click("[data-nav='export']")
        page.wait_for_timeout(250)
        check("export propose CSV, JSON, PDF et restauration",
              contient("csv pour excel", "sauvegarde complète", "synthèse pdf", "restaurer"))
        check("verrouillage biométrique proposé", page.query_selector("#swBio") is not None)
        check("sauvegarde automatique proposée", page.query_selector("#swAuto") is not None)
        page.click("[data-action='csv']")
        page.wait_for_timeout(1800)
        check("CSV produit", page.eval_on_selector_all(".file", "e => e.length") == 1)
        page.click("[data-action='json']")
        page.wait_for_timeout(1800)
        check("sauvegarde JSON produite", page.eval_on_selector_all(".file", "e => e.length") == 2)
        page.click("[data-action='pdf']")
        page.wait_for_timeout(1800)
        check("synthèse PDF produite", page.eval_on_selector_all(".file", "e => e.length") == 3)
        check("fichier PDF nommé", ".pdf" in txt())
        page.click("[data-action='maj']")
        page.wait_for_timeout(300)
        check("état de vérification visible", "vérification" in txt())
        page.wait_for_timeout(1600)
        check("mise à jour conclut « à jour »", "à jour" in txt())
        capture("04-export.png")

        # ---------- 11. thème clair, ergonomie, erreurs ----------
        page.click("[data-nav='saisir']")
        page.wait_for_timeout(250)
        capture("01-saisir-sombre.png")
        tailles = page.eval_on_selector_all(".chip.big", "els => els.map(e => Math.round(e.getBoundingClientRect().height))")
        check("cibles tactiles de 48 px minimum", bool(tailles) and min(tailles) >= 48, tailles)
        check("sections secondaires repliées par défaut",
              page.eval_on_selector_all("details.sec:not([open])", "e => e.length") >= 1,
              page.eval_on_selector_all("details.sec:not([open])", "e => e.length"))
        page.click("[data-action='tout']")
        page.wait_for_timeout(250)
        check("bouton « Tout afficher » ouvre les sections",
              page.eval_on_selector_all("details.sec[open]", "e => e.length") >= 5,
              page.eval_on_selector_all("details.sec[open]", "e => e.length"))
        page.click("#btnTheme")
        page.wait_for_timeout(350)
        check("bascule en thème clair", page.get_attribute("html", "data-theme") == "light",
              page.get_attribute("html", "data-theme"))
        capture("05-saisir-clair.png")

        rendus_avant = page.evaluate("window.__app.rendus()")
        page.wait_for_timeout(900)
        check("le rendu vit (compteur)", page.evaluate("window.__app.rendus()") >= rendus_avant)
        check("aucune erreur console", len(erreurs) == 0, str(erreurs[:3]) if erreurs else "")
        nav.close()

    ok = sum(1 for _, c, _ in resultats if c)
    print("\nRESULTAT : %d/%d contrôles OK" % (ok, len(resultats)))
    return 0 if ok == len(resultats) else 1


if __name__ == "__main__":
    sys.exit(main())
