package com.trucdecomptable.mauxdetete

import com.trucdecomptable.mauxdetete.update.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun comparaison_numerique_et_non_lexicale() {
        assertTrue(UpdateChecker.compareVersions("1.10.0", "1.9.0") > 0)
        assertTrue(UpdateChecker.compareVersions("1.0.0", "1.0.0") == 0)
        assertTrue(UpdateChecker.compareVersions("1.0.0", "1.0.1") < 0)
        assertTrue(UpdateChecker.compareVersions("2", "1.99.99") > 0)
    }

    @Test
    fun manifeste_version_plus_recente() {
        val corps = """{"version":"1.2.0","notes":"corrections"}"""
        val resultat = UpdateChecker.parseManifest(corps, installed = "1.1.0")
        assertTrue(resultat is UpdateChecker.CheckResult.Available)
        val dispo = resultat as UpdateChecker.CheckResult.Available
        assertEquals("1.2.0", dispo.info.version)
        assertEquals("corrections", dispo.info.notes)
        assertEquals(UpdateChecker.APK_URL, dispo.info.apkUrl)
    }

    @Test
    fun manifeste_a_jour() {
        val resultat = UpdateChecker.parseManifest("""{"version":"1.0.0"}""", installed = "1.0.0")
        assertEquals(UpdateChecker.CheckResult.UpToDate, resultat)
    }

    @Test
    fun manifeste_illisible_renvoie_null() {
        assertNull(UpdateChecker.parseManifest("pas du json", "1.0.0"))
        assertNull(UpdateChecker.parseManifest("""{"notes":"sans version"}""", "1.0.0"))
    }

    @Test
    fun api_ignore_les_brouillons_et_exige_un_apk() {
        val corps = """
        [
          {"draft": true, "tag_name": "v9.9.9", "name": "v9.9.9",
           "assets": [{"name": "app-release.apk", "browser_download_url": "https://exemple/brouillon.apk"}]},
          {"draft": false, "tag_name": "latest", "name": "Maux de tete v1.3.0",
           "body": "nouveautes",
           "assets": [{"name": "version.json", "browser_download_url": "https://exemple/version.json"}]},
          {"draft": false, "tag_name": "latest", "name": "Maux de tete v1.4.0",
           "assets": [{"name": "app-release.apk", "browser_download_url": "https://exemple/app-release.apk"}]}
        ]
        """.trimIndent()
        val resultat = UpdateChecker.parseApi(corps, installed = "1.0.0")
        assertTrue(resultat is UpdateChecker.CheckResult.Available)
        val dispo = resultat as UpdateChecker.CheckResult.Available
        assertEquals("1.4.0", dispo.info.version)
        assertEquals("https://exemple/app-release.apk", dispo.info.apkUrl)
    }

    @Test
    fun api_a_jour() {
        val corps = """[{"draft":false,"tag_name":"latest","name":"v1.0.0",
            "assets":[{"name":"app-release.apk","browser_download_url":"https://exemple/a.apk"}]}]"""
        assertEquals(UpdateChecker.CheckResult.UpToDate, UpdateChecker.parseApi(corps, "1.0.0"))
    }

    @Test
    fun extraction_de_version_dans_un_titre() {
        assertEquals("1.14.0", UpdateChecker.extractVersion("Maux de tete v1.14.0"))
        assertNull(UpdateChecker.extractVersion("latest"))
    }
}
