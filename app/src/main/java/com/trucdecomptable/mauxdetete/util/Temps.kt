package com.trucdecomptable.mauxdetete.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Calculs de durée et de dépassement de soirée, sans dépendance Android :
 * testables en JVM pure. Une crise « dépasse la soirée » quand elle franchit
 * minuit, dure six heures ou plus, ou n'est pas encore terminée.
 */
object Temps {

    /** Heure à partir de laquelle on considère que la crise a tenu la soirée. */
    const val HEURE_SOIREE = 23

    /** Durée à partir de laquelle une crise est dite longue (statistique). */
    const val DUREE_LONGUE = 6

    fun dureeHeures(debutMs: Long, finMs: Long?, maintenantMs: Long): Long {
        val fin = finMs ?: maintenantMs
        val heures = (fin - debutMs) / 3_600_000L
        return if (heures < 0) 0 else heures
    }

    fun jourLocal(ms: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    fun franchitMinuit(debutMs: Long, finMs: Long?, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        if (finMs == null) return false
        return jourLocal(debutMs, zone) != jourLocal(finMs, zone)
    }

    fun heureLocale(ms: Long, zone: ZoneId = ZoneId.systemDefault()): Int =
        Instant.ofEpochMilli(ms).atZone(zone).hour

    /** Crise de six heures ou plus. C'est une durée, pas un horaire. */
    fun estLongue(debutMs: Long, finMs: Long?, maintenantMs: Long): Boolean =
        dureeHeures(debutMs, finMs, maintenantMs) >= DUREE_LONGUE

    /**
     * « A dépassé la soirée » ne parle que de l'horaire : la crise a franchi
     * minuit, ou s'est terminée à 23h ou plus tard. Une durée de six heures en
     * pleine matinée (6h → 12h) n'a rien à voir avec la soirée et ne doit pas
     * porter ce badge.
     */
    fun depasseSoiree(
        debutMs: Long,
        finMs: Long?,
        @Suppress("UNUSED_PARAMETER") maintenantMs: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        if (finMs == null) return false
        if (franchitMinuit(debutMs, finMs, zone)) return true
        return heureLocale(finMs, zone) >= HEURE_SOIREE
    }

    /** « 13 h », « moins d'1 h », et pour les crises en cours « 3 h (en cours) ». */
    fun libelleDuree(heures: Long, enCours: Boolean = false): String {
        val base = if (heures <= 0) "moins d'1 h" else "$heures h"
        return if (enCours) "$base (en cours)" else base
    }

    /**
     * Report de la fin au lendemain quand l'heure de fin précède l'heure de
     * début : 20h → 9h signifie que la crise a tenu toute la soirée et la nuit.
     * Renvoie la fin corrigée, décalée de [decalageJours] jours.
     */
    fun finReportee(
        debutMs: Long,
        heureFin: Int,
        heureDebut: Int,
        decalageJours: Int,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val debut = Instant.ofEpochMilli(debutMs).atZone(zone)
        var fin = debut.withHour(heureFin).withMinute(0).withSecond(0).withNano(0)
        if (heureFin < heureDebut) fin = fin.plusDays(decalageJours.toLong().coerceAtLeast(1))
        return fin.toInstant().toEpochMilli()
    }
}
