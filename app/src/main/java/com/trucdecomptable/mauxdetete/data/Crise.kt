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
import kotlinx.coroutines.flow.Flow

/**
 * Une crise de maux de tête. Les listes (déclencheurs, repas, symptômes,
 * localisations) sont stockées en texte séparé par « | » : pas de table de
 * jointure, sauvegarde JSON et export CSV directs.
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
) {
    val enCours: Boolean get() = finLe == null

    fun liste(champ: String): List<String> = when (champ) {
        "declencheurs" -> decouper(declencheurs)
        "repas" -> decouper(repas)
        "symptomes" -> decouper(symptomes)
        "localisations" -> decouper(localisations)
        else -> emptyList()
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

@Database(entities = [Crise::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {

    abstract fun dao(): CriseDao

    companion object {
        @Volatile
        private var instance: AppDb? = null

        fun get(context: Context): AppDb =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDb::class.java,
                    "maux-de-tete.db",
                ).build().also { instance = it }
            }

        /** Ferme la base avant un remplacement de fichier (restauration). */
        @Synchronized
        fun fermer() {
            instance?.close()
            instance = null
        }
    }
}
