package zechs.zplex.ui.fragment.media

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import zechs.zplex.ThisApp
import zechs.zplex.models.dataclass.ConstantsResult
import zechs.zplex.models.dataclass.Movie
import zechs.zplex.models.dataclass.Show
import zechs.zplex.models.drive.DriveResponse
import zechs.zplex.models.tmdb.collection.CollectionsResponse
import zechs.zplex.models.tmdb.entities.Pair
import zechs.zplex.models.tmdb.media.MediaResponse
import zechs.zplex.models.tmdb.media.MovieResponse
import zechs.zplex.models.tmdb.media.TvResponse
import zechs.zplex.repository.FilesRepository
import zechs.zplex.repository.TmdbRepository
import zechs.zplex.utils.Constants.CLIENT_ID
import zechs.zplex.utils.Constants.CLIENT_SECRET
import zechs.zplex.utils.Constants.REFRESH_TOKEN
import zechs.zplex.utils.Constants.TEMP_TOKEN
import zechs.zplex.utils.Constants.ZPLEX
import zechs.zplex.utils.Constants.ZPLEX_DRIVE_ID
import zechs.zplex.utils.Constants.ZPLEX_MOVIES_ID
import zechs.zplex.utils.Constants.ZPLEX_SHOWS_ID
import zechs.zplex.utils.ConverterUtils
import zechs.zplex.utils.Resource
import zechs.zplex.utils.SessionManager
import java.io.IOException

class MediaViewModel(
    app: Application,
    private val filesRepository: FilesRepository,
    private val tmdbRepository: TmdbRepository
) : AndroidViewModel(app) {

    private val database = Firebase.firestore

    private val accessToken = SessionManager(
        getApplication<Application>().applicationContext
    ).fetchAuthToken()

    val searchList: MutableLiveData<Resource<DriveResponse>> = MutableLiveData()
    val media: MutableLiveData<Resource<MediaResponse>> = MutableLiveData()

    fun saveShow(show: Show) = viewModelScope.launch {
        tmdbRepository.upsertShow(show)
    }

    fun deleteShow(show: Show) = viewModelScope.launch {
        tmdbRepository.deleteShow(show)
    }

    fun getShow(id: Int) = tmdbRepository.fetchShow(id)

    fun saveMovie(movie: Movie) = viewModelScope.launch {
        tmdbRepository.upsertMovie(movie)
    }

    fun deleteMovie(movie: Movie) = viewModelScope.launch {
        tmdbRepository.deleteMovie(movie)
    }

    fun getMovie(id: Int) = tmdbRepository.fetchMovie(id)

    fun getMedia(
        tmdbId: Int,
        mediaType: String
    ) = viewModelScope.launch {
        media.postValue(Resource.Loading())
        try {
            if (hasInternetConnection()) {
                if (mediaType == "movie") {
                    val response = tmdbRepository.getMovie(tmdbId)
                    if (response.body()?.belongs_to_collection != null) {
                        val collectionsResponse = tmdbRepository.getCollection(
                            collectionId = response.body()?.belongs_to_collection!!.id
                        )
                        media.postValue(
                            handleMovieResponseWithCollection(
                                response,
                                collectionsResponse
                            )
                        )
                    } else {
                        media.postValue(handleMovieResponse(response))
                    }
                } else {
                    val response = tmdbRepository.getShow(tmdbId)
                    media.postValue(handleTvResponse(response))
                }
            } else {
                media.postValue(Resource.Error("No internet connection"))
            }
        } catch (t: Throwable) {
            println(t.stackTrace)
            println(t.message)
            media.postValue(
                Resource.Error(
                    if (t is IOException) "Network Failure" else t.message ?: "Something went wrong"
                )
            )
        }
    }

    private fun handleMovieResponseWithCollection(
        response: Response<MovieResponse>,
        collectionsResponse: Response<CollectionsResponse>
    ): Resource<MediaResponse> {
        if (response.isSuccessful && collectionsResponse.isSuccessful) {
            if (response.body() != null && collectionsResponse.body() != null) {
                val resultResponse = response.body()!!
                val collectionsResult = collectionsResponse.body()!!

                val ratingText = "${resultResponse.vote_average}/10"
                val durationText = if (resultResponse.runtime == null) {
                    "Unknown"
                } else ConverterUtils.convertMinutes(resultResponse.runtime)

                val genreResponse = if (resultResponse.genres?.isEmpty() == true) {
                    "Unknown"
                } else resultResponse.genres?.get(0)?.name ?: "Unknown"

                val genreText = if (genreResponse == "Science Fiction") {
                    "Sci-Fi"
                } else genreResponse.substringBefore(" ")

                val pairsArray: MutableList<Pair> = mutableListOf()
                pairsArray.add(Pair("Genre", genreText))
                pairsArray.add(Pair("Rating", ratingText))
                pairsArray.add(Pair("Duration", durationText))

                resultResponse.production_companies?.let { company ->
                    if (company.isNotEmpty()) company[0].name?.let {
                        Pair("Studio", it)
                    }?.let { pairsArray.add(it) }
                }

                resultResponse.release_date?.let { firstAired ->
                    if (firstAired.isNotEmpty()) pairsArray.add(
                        Pair("Released", firstAired.take(4))
                    )
                }

                val videosList = resultResponse.videos?.results?.filter { v ->
                    v.site == "YouTube"
                } ?: listOf()

                val mediaResponse = MediaResponse(
                    id = resultResponse.id,
                    name = resultResponse.title,
                    overview = resultResponse.overview,
                    poster_path = resultResponse.poster_path,
                    related_media = collectionsResult.parts,
                    misc = pairsArray,
                    seasons = listOf(),
                    cast = resultResponse.credits.cast?.toList() ?: listOf(),
                    recommendations = resultResponse.recommendations?.results?.toList() ?: listOf(),
                    similar = resultResponse.similar?.results?.toList() ?: listOf(),
                    videos = videosList,
                    vote_average = "${resultResponse.vote_average}/10"
                )
                return Resource.Success(mediaResponse)
            }
        }
        return Resource.Error(response.message())
    }

    private fun handleTvResponse(
        response: Response<TvResponse>
    ): Resource<MediaResponse> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                val ratingText = "${resultResponse.vote_average}/10"
                val durationText = if (resultResponse.episode_run_time?.isEmpty() == true) {
                    "Unknown"
                } else ConverterUtils.convertMinutes(resultResponse.episode_run_time?.get(0) ?: 0)

                val genreResponse = if (resultResponse.genres?.isEmpty() == true) {
                    "Unknown"
                } else resultResponse.genres?.get(0)?.name ?: "Unknown"

                val genreText = if (genreResponse == "Science Fiction") {
                    "Sci-Fi"
                } else genreResponse.substringBefore(" ")

                val pairsArray: MutableList<Pair> = mutableListOf()
                pairsArray.add(Pair("Genre", genreText))
                pairsArray.add(Pair("Rating", ratingText))
                pairsArray.add(Pair("Duration", durationText))

                resultResponse.networks?.let { network ->
                    if (network.isNotEmpty())
                        network[0].name?.let { Pair("Network", it) }?.let { pairsArray.add(it) }
                }

                resultResponse.first_air_date?.let { firstAired ->
                    if (firstAired.isNotEmpty())
                        pairsArray.add(
                            Pair(
                                "Released",
                                firstAired.take(4)
                            )
                        )
                }

                val videosList = resultResponse.videos?.results?.filter { v ->
                    v.site == "YouTube"
                } ?: listOf()

                val mediaResponse = MediaResponse(
                    id = resultResponse.id,
                    name = resultResponse.name,
                    overview = resultResponse.overview,
                    poster_path = resultResponse.poster_path,
                    related_media = listOf(),
                    misc = pairsArray,
                    seasons = resultResponse.seasons ?: listOf(),
                    cast = resultResponse.credits.cast?.toList() ?: listOf(),
                    recommendations = resultResponse.recommendations?.results?.toList() ?: listOf(),
                    similar = resultResponse.similar?.results?.toList() ?: listOf(),
                    videos = videosList,
                    vote_average = "${resultResponse.vote_average}/10"
                )
                return Resource.Success(mediaResponse)
            }
        }
        return Resource.Error(response.message())
    }


    private fun handleMovieResponse(
        response: Response<MovieResponse>
    ): Resource<MediaResponse> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->

                val ratingText = "${resultResponse.vote_average}/10"
                val durationText = if (resultResponse.runtime == null) {
                    "Unknown"
                } else ConverterUtils.convertMinutes(resultResponse.runtime)

                val genreResponse = if (resultResponse.genres?.isEmpty() == true) {
                    "Unknown"
                } else resultResponse.genres?.get(0)?.name ?: "Unknown"

                val genreText = if (genreResponse == "Science Fiction") {
                    "Sci-Fi"
                } else genreResponse.substringBefore(" ")

                val pairsArray: MutableList<Pair> = mutableListOf()
                pairsArray.add(Pair("Genre", genreText))
                pairsArray.add(Pair("Rating", ratingText))
                pairsArray.add(Pair("Duration", durationText))

                resultResponse.production_companies?.let { company ->
                    if (company.isNotEmpty()) company[0].name?.let {
                        Pair("Studio", it)
                    }?.let { pairsArray.add(it) }
                }

                resultResponse.release_date?.let { firstAired ->
                    if (firstAired.isNotEmpty()) pairsArray.add(
                        Pair("Released", firstAired.take(4))
                    )
                }

                val videosList = resultResponse.videos?.results?.filter { v ->
                    v.site == "YouTube"
                } ?: listOf()

                val mediaResponse = MediaResponse(
                    id = resultResponse.id,
                    name = resultResponse.title,
                    overview = resultResponse.overview,
                    poster_path = resultResponse.poster_path,
                    related_media = listOf(),
                    misc = pairsArray,
                    seasons = listOf(),
                    cast = resultResponse.credits.cast?.toList() ?: listOf(),
                    recommendations = resultResponse.recommendations?.results?.toList() ?: listOf(),
                    similar = resultResponse.similar?.results?.toList() ?: listOf(),
                    videos = videosList,
                    vote_average = "${resultResponse.vote_average}/10"
                )
                return Resource.Success(mediaResponse)
            }
        }
        return Resource.Error(response.message())
    }

    fun doSearchFor(tmdbId: Int) = viewModelScope.launch {
        searchList.postValue(Resource.Loading())
        try {
            if (hasInternetConnection()) {
                getCredentials(tmdbId)
            } else {
                searchList.postValue(Resource.Error("No internet connection"))
            }
        } catch (t: Throwable) {
            println(t.stackTrace)
            println(t.message)
            searchList.postValue(
                Resource.Error(
                    if (t is IOException) {
                        "Network Failure"
                    } else t.message ?: "Something went wrong"
                )
            )
        }
    }

    private fun getCredentials(tmdbId: Int) {
        database.collection("constants")
            .document("eQvhagfVy6IgH2Hcx1Kk")
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val constantsResult = documentSnapshot.toObject<ConstantsResult>()
                constantsResult?.let {
                    ZPLEX = it.zplex
                    ZPLEX_DRIVE_ID = it.zplex_drive_id
                    ZPLEX_MOVIES_ID = it.zplex_movies_id
                    ZPLEX_SHOWS_ID = it.zplex_shows_id
                    CLIENT_ID = it.client_id
                    CLIENT_SECRET = it.client_secret
                    REFRESH_TOKEN = it.refresh_token
                    TEMP_TOKEN = it.temp_token
                    CoroutineScope(Dispatchers.IO).launch {
                        val response = filesRepository.getDriveFiles(
                            pageSize = 1,
                            if (accessToken == "") it.temp_token else accessToken,
                            pageToken = "",
                            driveQuery = searchQuery(tmdbId, it.zplex_movies_id),
                            orderBy = "modifiedTime desc"
                        )
                        searchList.postValue(handleSearchListResponse(response))
                    }
                }
            }
    }

    private fun handleSearchListResponse(
        response: Response<DriveResponse>
    ): Resource<DriveResponse> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                return Resource.Success(resultResponse)
            }
        }
        return Resource.Error(response.message())
    }

    private fun searchQuery(
        tmdbId: Int, movieId: String
    ) = "name contains '${tmdbId}' and parents in '${movieId}' and trashed = false"

    private fun hasInternetConnection(): Boolean {
        val connectivityManager = getApplication<ThisApp>().getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}