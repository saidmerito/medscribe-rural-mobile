package dj.medscriberural.standalone.data

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<RegisterEntry>): List<Long>

    @Update
    suspend fun update(entry: RegisterEntry)

    // Tri par page (photoPath) puis par ordre de ligne dans la page, pour que
    // les fiches d'une même photo restent groupées et dans l'ordre du registre.
    @Query("SELECT * FROM register_entries ORDER BY createdAtMillis DESC, rowIndex ASC")
    fun observeAll(): Flow<List<RegisterEntry>>

    @Query("SELECT * FROM register_entries WHERE id = :id")
    suspend fun getById(id: Long): RegisterEntry?

    @Query("SELECT * FROM register_entries WHERE photoPath = :photoPath ORDER BY rowIndex ASC")
    suspend fun getByPhotoPath(photoPath: String): List<RegisterEntry>
}
