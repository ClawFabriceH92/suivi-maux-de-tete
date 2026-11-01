package com.trucdecomptable.mauxdetete.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Une crise de maux de tête. Les listes (déclencheurs, repas, symptômes,
 * localisations) sont stockées en texte séparé par « | » : pas de table de
 * jointure, sauvegarde JSON et export CSV directs.
 *
 * Depuis la version 2 : quantité d'Efferalgan et d'ibuprofène prise, en prises
 * et en milligrammes par prise. Migration additive uniquement, les crises déjà
 * saisies gardent leurs valeurs (0 prise).
 */
@Entity(tableName = "crise")
data class Crise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debutLe: Long,
    val finLe: Long? = null,
    val niveau: Int,
    val sommeil: String,
    val reveils: Int = 0,
    val declencheurs: String = "",
    val repas: String = "",
    val symptomes: String = "",
    val localisations: String = "",
    val medicament: String? = null,
    val efficacite: String? = null,
    val commentaire: String = "",
    val creeLe: Long = System.currentTimeMillis(),
    val efferalganPrises: Int = 0,
    val efferalganMg: Int = 1000,
    val ibuprofenePrises: Int = 0,
    val ibuprofeneMg: Int = 400,
) {
    val enCours: Boolean get() = finLe == null

    /** Milligrammes réellement absorbés, toutes molécules confondues. */
    val mgTotal: Int
        get() = efferalganPrises * efferalganMg + ibuprofenePrises * ibuprofeneMg

    val aPrisQuelqueChose: Boolean
        get() = efferalganPrises > 0 || ibuprofenePrises > 0 || !medicament.isNullOrBlank()

    fun liste(champ: String): List<String> = when (champ) {
        "declencheurs" -> decouper(declencheurs)
        "repas" -> decouper(repas)
        "symptomes" -> decouper(symptomes)
        "localisations" -> decouper(localisations)
        else -> emptyList()
    }

    /** « Efferalgan 1000 mg ×2 · Ibuprofène 400 mg ×1 », vide si rien n'a été pris. */
    fun resumeMedicaments(): String {
        val morceaux = mutableListOf<String>()
        if (efferalganPrises > 0) morceaux.add("Efferalgan ${efferalganMg} mg ×$efferalganPrises")
        if (ibuprofenePrises > 0) morceaux.add("Ibuprofène ${ibuprofeneMg} mg ×$ibuprofenePrises")
        if (!medicament.isNullOrBlank()) {
            morceaux.add(medicament + (efficacite?.let { " ($it)" } ?: ""))
        }
        return morceaux.joinToString(" · ")
    }

    companion object {
        const val SEPARATEUR = "|"

        fun joindre(valeurs: Collection<String>): String =
            valeurs.filter { it.isNotBlank() }.joinToString(SEPARATEUR)

        fun decouper(valeur: String?): List<String> =
            valeur?.split(SEPARATEUR)?.filter { it.isNotBlank() } ?: emptyList()
    }
}

@Dao
interface CriseDao {

    @Query("SELECT * FROM crise ORDER BY debutLe DESC")
    fun toutes(): Flow<List<Crise>>

    @Query("SELECT * FROM crise ORDER BY debutLe DESC")
    suspend fun toutesMaintenant(): List<Crise>

    @Query("SELECT * FROM crise WHERE finLe IS NULL ORDER BY debutLe DESC LIMIT 1")
    fun enCours(): Flow<Crise?>

    @Insert
    suspend fun inserer(crise: Crise): Long

    @Update
    suspend fun mettreAJour(crise: Crise)

    @Query("DELETE FROM crise WHERE id = :id")
    suspend fun supprimer(id: Long)

    @Query("DELETE FROM crise")
    suspend fun toutSupprimer()

    @Insert
    suspend fun insererTout(crises: List<Crise>)
}

@Database(entities = [Crise::class], version = 2, exportSchema = false)
abstract class AppDb : RoomDatabase() {

    abstract fun dao(): CriseDao

    companion object {
        @Volatile
        private var instance: AppDb? = null

        /**
         * Ajout des quantités de médicaments. Additif et non destructif : une
         * migration destructive effacerait les crises déjà saisies.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE crise ADD COLUMN efferalganPrises INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE crise ADD COLUMN efferalganMg INTEGER NOT NULL DEFAULT 1000")
                db.execSQL("ALTER TABLE crise ADD COLUMN ibuprofenePrises INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE crise ADD COLUMN ibuprofeneMg INTEGER NOT NULL DEFAULT 400")
            }
        }

        fun get(context: Context): AppDb =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDb::class.java,
                    "maux-de-tete.db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }

        /** Ferme la base avant un remplacement de fichier (restauration). */
        @Synchronized
        fun fermer() {
            instance?.close()
            instance = null
        }
    }
}
