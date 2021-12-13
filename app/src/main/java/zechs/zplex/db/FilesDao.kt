package zechs.zplex.db

import androidx.lifecycle.LiveData
import androidx.room.*
import zechs.zplex.models.drive.File

@Dao
interface FilesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(file: File): Long

    @Query("SELECT * FROM files")
    fun getAllFiles(): LiveData<List<File>>

    @Query("SELECT EXISTS(SELECT * FROM files WHERE id = :id)")
    fun getFile(id: String): LiveData<Boolean>

    @Delete
    suspend fun deleteFile(file: File)

}