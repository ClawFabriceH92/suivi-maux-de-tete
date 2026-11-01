package com.trucdecomptable.mauxdetete.export

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.trucdecomptable.mauxdetete.data.Bilan
import com.trucdecomptable.mauxdetete.data.Crise
import com.trucdecomptable.mauxdetete.data.Referentiels
import com.trucdecomptable.mauxdetete.util.Temps
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class FichierExport(val nom: String, val fichier: File, val description: String)

/**
 * Trois sorties, toutes locales : CSV pour Excel, JSON pour la sauvegarde et la
 * restauration, PDF de synthèse pour une consultation. Aucun réseau.
 */
object ExportManager {

    private val HORODATAGE = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val HEURE = DateTimeFormatter.ofPattern("HH:mm") as DateTimeFormatter

    private fun dossier(context: Context): File =
        File(context.cacheDir, "exports").apply { mkdirs() }

    private fun maintenant(): LocalDate = LocalDate.now()

    // ---------------------------------------------------------------- CSV

    fun csv(context: Context, crises: List<Crise>, zone: ZoneId = ZoneId.systemDefault()): FichierExport {
        val texte = construireCsv(crises, zone)
        val fichier = File(dossier(context), "maux-de-tete_${maintenant().format(HORODATAGE)}.csv")
        // BOM UTF-8 : sans lui, Excel FR affiche « Ã© » à la place des accents.
        fichier.writeText("\uFEFF" + texte, Charsets.UTF_8)
        return FichierExport(fichier.name, fichier, "${crises.size} crises")
    }

    fun construireCsv(crises: List<Crise>, zone: ZoneId = ZoneId.systemDefault()): String {
        val entete = listOf(
            "date_debut", "heure_debut", "date_fin", "heure_fin", "duree_heures", "depasse_soiree",
            "niveau", "sommeil", "reveils", "declencheurs", "repas", "symptomes", "localisations",
            "medicament", "efficacite", "commentaire",
        )
        val lignes = mutableListOf(entete.joinToString(";"))
        crises.sortedBy { it.debutLe }.forEach { c ->
            val debut = Instant.ofEpochMilli(c.debutLe).atZone(zone)
            val fin = c.finLe?.let { Instant.ofEpochMilli(it).atZone(zone) }
            val duree = Temps.dureeHeures(c.debutLe, c.finLe, System.currentTimeMillis())
            lignes += listOf(
                debut.toLocalDate().toString(),
                debut.toLocalTime().format(HEURE),
                fin?.toLocalDate()?.toString() ?: "",
                fin?.toLocalTime()?.format(HEURE) ?: "",
                duree.toString(),
                if (Temps.depasseSoiree(c.debutLe, c.finLe, System.currentTimeMillis(), zone)) "oui" else "non",
                c.niveau.toString(),
                c.sommeil,
                c.reveils.toString(),
                c.declencheurs.replace(Crise.SEPARATEUR, " | "),
                c.repas.replace(Crise.SEPARATEUR, " | "),
                c.symptomes.replace(Crise.SEPARATEUR, " | "),
                c.localisations.replace(Crise.SEPARATEUR, " | "),
                c.medicament.orEmpty(),
                c.efficacite.orEmpty(),
                c.commentaire.replace("\n", " ").replace(";", ","),
            ).joinToString(";")
        }
        return lignes.joinToString("\r\n")
    }

    // --------------------------------------------------------------- JSON

    fun json(context: Context, crises: List<Crise>): FichierExport {
        val racine = JSONObject()
        racine.put("application", "suivi-maux-de-tete")
        racine.put("format", 1)
        racine.put("exporte_le", System.currentTimeMillis())
        racine.put("nombre", crises.size)
        val tableau = JSONArray()
        crises.sortedBy { it.debutLe }.forEach { c ->
            tableau.put(
                JSONObject()
                    .put("debut_le", c.debutLe)
                    .put("fin_le", c.finLe ?: JSONObject.NULL)
                    .put("niveau", c.niveau)
                    .put("sommeil", c.sommeil)
                    .put("reveils", c.reveils)
                    .put("declencheurs", c.declencheurs)
                    .put("repas", c.repas)
                    .put("symptomes", c.symptomes)
                    .put("localisations", c.localisations)
                    .put("medicament", c.medicament ?: JSONObject.NULL)
                    .put("efficacite", c.efficacite ?: JSONObject.NULL)
                    .put("commentaire", c.commentaire)
                    .put("cree_le", c.creeLe),
            )
        }
        racine.put("crises", tableau)
        val fichier = File(dossier(context), "sauvegarde_maux-de-tete_${maintenant().format(HORODATAGE)}.json")
        fichier.writeText(racine.toString(2), Charsets.UTF_8)
        return FichierExport(fichier.name, fichier, "${crises.size} crises")
    }

    /** Relit un fichier de sauvegarde. Lève une exception explicite si le format ne tient pas. */
    fun lireJson(texte: String): List<Crise> {
        val racine = JSONObject(texte)
        require(racine.optString("application") == "suivi-maux-de-tete") {
            "Ce fichier n'est pas une sauvegarde de l'app Maux de tête."
        }
        val tableau = racine.getJSONArray("crises")
        val crises = mutableListOf<Crise>()
        for (i in 0 until tableau.length()) {
            val o = tableau.getJSONObject(i)
            crises += Crise(
                debutLe = o.getLong("debut_le"),
                finLe = if (o.isNull("fin_le")) null else o.getLong("fin_le"),
                niveau = o.optInt("niveau", 0).coerceIn(0, 10),
                sommeil = o.optString("sommeil", Referentiels.SOMMEIL_MOYEN),
                reveils = o.optInt("reveils", 0),
                declencheurs = o.optString("declencheurs", ""),
                repas = o.optString("repas", ""),
                symptomes = o.optString("symptomes", ""),
                localisations = o.optString("localisations", ""),
                medicament = if (o.isNull("medicament")) null else o.optString("medicament"),
                efficacite = if (o.isNull("efficacite")) null else o.optString("efficacite"),
                commentaire = o.optString("commentaire", ""),
                creeLe = o.optLong("cree_le", System.currentTimeMillis()),
            )
        }
        return crises
    }

    // ---------------------------------------------------------------- PDF

    fun pdf(context: Context, crises: List<Crise>, bilan: Bilan): FichierExport {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val toile = page.canvas

        val titre = Paint().apply {
            isAntiAlias = true
            textSize = 18f
            isFakeBoldText = true
        }
        val normal = Paint().apply { isAntiAlias = true; textSize = 11f }
        val petit = Paint().apply { isAntiAlias = true; textSize = 9.5f }

        var y = 50f
        toile.drawText("Suivi des maux de tête", 40f, y, titre)
        y += 22f
        toile.drawText(
            "Synthèse au ${maintenant().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} · " +
                "${crises.size} crises enregistrées",
            40f, y, petit,
        )
        y += 26f

        toile.drawText("Chiffres clés", 40f, y, titre.apply { textSize = 13f })
        y += 18f
        normal.textSize = 11f
        listOf(
            "Crises ce mois : ${bilan.crisesMois}",
            "Intensité moyenne (30 j) : ${arrondi(bilan.intensiteMoyenne)} / 10",
            "Durée moyenne (30 j) : ${arrondi(bilan.dureeMoyenneHeures)} h",
            "Jours sans douleur (30 j) : ${bilan.joursSansDouleur} sur 30",
            "Crises ayant dépassé la soirée : ${bilan.crisesLongues}",
            "Jours avec médicament ce mois : ${bilan.medicamentsJours} " +
                "(seuil ${bilan.seuilMedicaments} j, ICHD-3)",
        ).forEach { ligne ->
            toile.drawText(ligne, 48f, y, normal)
            y += 15f
        }

        y += 10f
        toile.drawText("Sommeil", 40f, y, titre)
        y += 18f
        val bon = bilan.sommeilBon
        val mauvais = bilan.sommeilMauvais
        toile.drawText(
            if (bon.concluable && mauvais.concluable) {
                "Après une bonne nuit ${arrondi(bon.moyenne ?: 0.0)} / 10 (n=${bon.effectif}) · " +
                    "après une mauvaise nuit ${arrondi(mauvais.moyenne ?: 0.0)} / 10 (n=${mauvais.effectif})"
            } else {
                "Données insuffisantes pour conclure (bon n=${bon.effectif}, mauvais n=${mauvais.effectif})"
            },
            48f, y, normal,
        )
        y += 26f

        toile.drawText("Déclencheurs les plus fréquents", 40f, y, titre)
        y += 18f
        if (bilan.declencheurs.isEmpty()) {
            toile.drawText("Aucun déclencheur renseigné", 48f, y, normal); y += 15f
        } else {
            bilan.declencheurs.forEach {
                toile.drawText("${it.libelle} : ${it.effectif} crises (${it.pourcentage} %)", 48f, y, normal)
                y += 15f
            }
        }

        y += 10f
        toile.drawText("20 dernières crises", 40f, y, titre)
        y += 18f
        crises.sortedByDescending { it.debutLe }.take(20).forEach { c ->
            if (y > 810f) return@forEach
            val debut = Instant.ofEpochMilli(c.debutLe).atZone(ZoneId.systemDefault())
            val duree = Temps.dureeHeures(c.debutLe, c.finLe, System.currentTimeMillis())
            val fin = c.finLe?.let {
                " → " + Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime().format(HEURE)
            } ?: " → en cours"
            toile.drawText(
                "${debut.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM"))} " +
                    "${debut.toLocalTime().format(HEURE)}$fin · ${duree} h · " +
                    "${c.niveau}/10 · ${Referentiels.libelleSommeil(c.sommeil)}" +
                    (c.medicament?.let { " · $it" } ?: ""),
                48f, y, petit,
            )
            y += 13f
        }

        document.finishPage(page)
        val fichier = File(dossier(context), "synthese_maux-de-tete_${maintenant().format(HORODATAGE)}.pdf")
        fichier.outputStream().use { document.writeTo(it) }
        document.close()
        return FichierExport(fichier.name, fichier, "${crises.size} crises")
    }

    private fun arrondi(valeur: Double): String =
        String.format(java.util.Locale.FRANCE, "%.1f", valeur).replace('.', ',')

    // ------------------------------------------------------------ partage

    fun uriDe(context: Context, fichier: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", fichier)

    fun partager(context: Context, fichier: File, mime: String, titre: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uriDe(context, fichier))
            putExtra(Intent.EXTRA_SUBJECT, titre)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, titre))
    }

    fun mimeDe(nom: String): String = when {
        nom.endsWith(".csv") -> "text/csv"
        nom.endsWith(".json") -> "application/json"
        else -> "application/pdf"
    }
}
