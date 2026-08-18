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

    @Update
    suspend fun update(entry: RegisterEntry)

    @Query("SELECT * FROM register_entries ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<RegisterEntry>>

    @Query("SELECT * FROM register_entries WHERE id = :id")
    suspend fun getById(id: Long): RegisterEntry?
}
