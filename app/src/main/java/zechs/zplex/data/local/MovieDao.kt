package zechs.zplex.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import zechs.zplex.data.model.entities.Movie

@Dao
interface MovieDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMovie(media: Movie): Long

    @Query("SELECT * FROM movies")
    fun getAllMovies(): LiveData<List<Movie>>

    @Query("SELECT EXISTS(SELECT * FROM movies WHERE id = :id)")
    fun getMovie(id: Int): LiveData<Boolean>

    @Query("SELECT * FROM movies WHERE id = :id LIMIT 1")
    suspend fun getMovieById(id: Int): Movie?

    @Delete
    suspend fun deleteMovie(media: Movie)

}