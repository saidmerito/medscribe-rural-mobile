package dj.medscriberural.companion.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RegisterEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: RegisterEntry): Long

    @Update
    suspend fun update(entry: RegisterEntry)

    @Query("SELECT * FROM register_entries ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<RegisterEntry>>

    @Query("SELECT * FROM register_entries WHERE id = :id")
    suspend fun getById(id: Long): RegisterEntry?

    // Utilisé par le serveur d'ingestion : retrouve la dernière fiche
    // "pending" pour lui rattacher le résultat renvoyé par le skill Gallery.
    @Query("SELECT * FROM register_entries WHERE status = :status ORDER BY createdAtMillis DESC LIMIT 1")
    suspend fun getLatestByStatus(status: String = RegisterEntry.STATUS_PENDING): RegisterEntry?

    @Query("SELECT * FROM register_entries WHERE photoPath = :photoPath LIMIT 1")
    suspend fun getByPhotoPath(photoPath: String): RegisterEntry?
}
