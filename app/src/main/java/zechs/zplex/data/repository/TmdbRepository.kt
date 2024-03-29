package zechs.zplex.data.repository

import android.util.Log
import retrofit2.Response
import zechs.zplex.data.local.MovieDao
import zechs.zplex.data.local.ShowDao
import zechs.zplex.data.model.MediaType
import zechs.zplex.data.model.Order
import zechs.zplex.data.model.SortBy
import zechs.zplex.data.model.entities.Movie
import zechs.zplex.data.model.entities.Show
import zechs.zplex.data.model.tmdb.keyword.TmdbKeyword
import zechs.zplex.data.model.tmdb.search.SearchResponse
import zechs.zplex.data.remote.TmdbApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbRepository @Inject constructor(
    private val tmdbApi: TmdbApi,
    private val movieDao: MovieDao,
    private val showDao: ShowDao
) {

    companion object {
        private const val TAG = "TmdbRepository"
    }

    suspend fun upsertMovie(
        movie: Movie
    ) = movieDao.upsertMovie(movie)

    suspend fun fetchMovieById(
        id: Int
    ) = movieDao.getMovieById(id)

    fun fetchMovie(
        id: Int
    ) = movieDao.getMovie(id)

    suspend fun deleteMovie(
        tmdbId: Int
    ) = movieDao.deleteMovieById(tmdbId)

    fun getSavedMovies() = movieDao.getAllMovies()

    suspend fun upsertShow(
        show: Show
    ) = showDao.upsertShow(show)

    suspend fun fetchShowById(
        id: Int
    ) = showDao.getShowById(id)

    fun fetchShow(
        id: Int
    ) = showDao.getShow(id)

    suspend fun deleteShow(
        tmdbId: Int
    ) = showDao.deleteShowById(tmdbId)

    fun getSavedShows() = showDao.getAllShows()

    suspend fun getShow(
        tvId: Int,
        appendToQuery: String? = "credits,recommendations,videos"
    ) = tmdbApi.getShow(tvId, append_to_response = appendToQuery)

    suspend fun getMovie(
        movieId: Int,
        appendToQuery: String? = "credits,recommendations,videos"
    ) = tmdbApi.getMovie(movieId, append_to_response = appendToQuery)

    suspend fun getSeason(
        tvId: Int,
        seasonNumber: Int
    ) = tmdbApi.getSeason(tvId, seasonNumber)

    suspend fun getEpisode(
        tvId: Int,
        seasonNumber: Int,
        episodeNumber: Int
    ) = tmdbApi.getEpisode(tvId, seasonNumber, episodeNumber)

    suspend fun getSearch(
        query: String,
        page: Int
    ) = tmdbApi.getSearch(query = query, page = page)

    suspend fun getCollection(
        collectionId: Int
    ) = tmdbApi.getCollection(collection_id = collectionId)

    suspend fun getTrending(
        timeWindow: String
    ) = tmdbApi.getTrending(timeWindow)

    suspend fun getUpcoming(page: Int) = tmdbApi.getUpcoming(page = page)

    suspend fun getBrowse(
        mediaType: MediaType,
        sortBy: SortBy,
        order: Order,
        page: Int,
        withKeyword: List<TmdbKeyword>?,
        withGenres: Int?,
    ): Response<SearchResponse> {
        val keywords = try {
            withKeyword
                ?.map { keyword -> keyword.id }
                ?.joinToString(separator = ",")
        } catch (e: Exception) {
            Log.d(TAG, e.message ?: "Unable to parse keywords")
            null
        }
        return tmdbApi.getBrowse(
            media_type = mediaType,
            sort_by = "${sortBy.name}.${order.name}",
            page = page,
            with_keywords = keywords,
            with_genres = withGenres,
        )
    }

    suspend fun getInTheatres(
        dateStart: String,
        dateEnd: String
    ) = tmdbApi.getInTheatres(
        release_date_start = dateStart,
        release_date_end = dateEnd
    )

    suspend fun getPopularOnStreaming() = tmdbApi.getPopularOnStreaming()

    suspend fun getShowsFromCompany(
        companyId: Int,
        page: Int
    ) = tmdbApi.getFromCompany(
        media_type = MediaType.tv,
        with_companies = companyId,
        page = page
    )

    suspend fun getMoviesFromCompany(
        companyId: Int,
        page: Int
    ) = tmdbApi.getFromCompany(
        media_type = MediaType.movie,
        with_companies = companyId,
        page = page
    )

    suspend fun getPerson(
        personId: Int
    ) = tmdbApi.getPerson(personId)

    suspend fun searchKeyword(
        query: String
    ) = tmdbApi.searchKeyword(
        query = query
    )

}