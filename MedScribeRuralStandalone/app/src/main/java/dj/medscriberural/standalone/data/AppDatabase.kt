package dj.medscriberural.standalone.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RegisterEntry::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun registerEntryDao(): RegisterEntryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medscribe_rural.db"
                )
                    // Schéma v1 -> v2 : passage d'une fiche/photo à plusieurs
                    // lignes/photo. Base encore en phase de test terrain, pas
                    // de migration de données nécessaire pour l'instant.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
