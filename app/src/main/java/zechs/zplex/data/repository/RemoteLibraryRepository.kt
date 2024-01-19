package zechs.zplex.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import zechs.zplex.data.model.drive.DriveFile
import zechs.zplex.data.model.entities.Movie
import zechs.zplex.data.model.entities.Show
import zechs.zplex.data.remote.RemoteLibrary
import zechs.zplex.service.IndexingResult
import zechs.zplex.service.IndexingStateFlow
import zechs.zplex.utils.SessionManager
import zechs.zplex.utils.state.Resource
import zechs.zplex.utils.util.DriveApiQueryBuilder
import javax.inject.Inject

class RemoteLibraryRepository @Inject constructor(
    private val driveRepository: DriveRepository,
    private val tmdbRepository: TmdbRepository,
    private val sessionManager: SessionManager,
    private val indexingStateFlow: IndexingStateFlow
) : RemoteLibrary {

    companion object {
        const val TAG = "RemoteLibraryRepository"
    }

    data class Info(
        val name: String,
        val year: Int,
        val tmdbId: Int,
        val fileId: String
    )

    private val moviesResult = IndexingResult()
    private val showsResult = IndexingResult()

    override suspend fun indexMovies() {
        Log.d(TAG, "Beginning indexing movies...")


        if (!doesMoviesFolderExist()) {
            Log.d(TAG, "Movies folder does not exist, skipping movies processing.")
            indexingStateFlow.emitErrorIndexingMovies("Movies folder does not exist, skipping movies processing.")
            return
        }

        val driveFiles = mutableListOf<DriveFile>()

        val driveFilesResult = driveRepository.getAllFilesInFolder(
            queryBuilder = DriveApiQueryBuilder()
                .inParents(sessionManager.fetchMovieFolder()!!)
                .mimeTypeNotEquals("application/vnd.google-apps.folder")
                .trashed(false)
        )

        if (driveFilesResult is Resource.Success) {
            driveFilesResult.data!!.map { it.toDriveFile() }
                .filter { it.isVideoFile }
                .toTypedArray()
                .let { driveFiles.addAll(it) }

            processMovies(driveFiles)
        } else {
            indexingStateFlow.emitErrorIndexingMovies(driveFilesResult.message ?: "Unknown error")
            Log.d(TAG, "Error getting files: ${driveFilesResult.message ?: "Unknown error"}")
        }

        Log.d(TAG, "Movies indexing result: $moviesResult")
        indexingStateFlow.emitIndexingResultMovies(moviesResult)
        Log.d(TAG, "Ended indexing movies")
    }

    /**
     * Processes a list of DriveFiles representing movies by extracting relevant information
     * such as video details. Each movie is asynchronously processed using the 'processSingleMovie' method,
     * which checks and updates the local database accordingly.
     *
     * If a movie already exists in the local database, only the fileId field is updated.
     * If a movie doesn't exist, it fetches details from TMDB and inserts it into the local database.
     *
     * After processing all movies, a sanitization process is performed to remove files that exist
     * in the local database but no longer exist remotely.
     *
     * @param driveFiles A list of DriveFiles representing movies to be processed.
     * @throws Exception If an error occurs during movie processing or sanitization.
     */
    private suspend fun processMovies(driveFiles: List<DriveFile>) {
        coroutineScope {
            driveFiles
                .mapNotNull {
                    val parse = parseFileName(it)
                    if (parse == null) {
                        moviesResult.indexingErrors++
                    }
                    parse
                }
                .map { videoInfo -> async(Dispatchers.IO) { processSingleMovie(videoInfo) } }
                .awaitAll()
            synchronizeLocalMoviesWithRemote(driveFiles)
        }
    }

    private suspend fun processSingleMovie(videoInfo: Info) {
        Log.d(TAG, "Processing movie: ${videoInfo.name}")

        val savedMovie = tmdbRepository.fetchMovieById(videoInfo.tmdbId)

        if (savedMovie != null) {
            updateExistingMovie(videoInfo, savedMovie)
        } else {
            insertNewMovie(videoInfo)
        }

        delay(250L)
    }

    private suspend fun updateExistingMovie(videoFile: Info, existingMovie: Movie) {
        if (existingMovie.fileId != videoFile.fileId) {
            Log.d(TAG, "Movie already exists: ${videoFile.name}, updating videoId.")
            tmdbRepository.upsertMovie(existingMovie.copy(fileId = videoFile.fileId))
        }
        moviesResult.existingItemsSkipped++
    }

    private suspend fun insertNewMovie(videoFile: Info) {
        Log.d(TAG, "New movie: ${videoFile.name}, inserting into the database.")
        moviesResult.newItemsIndexed++

        val movieResponse = tmdbRepository.getMovie(videoFile.tmdbId, appendToQuery = null)

        val movie = movieResponse.body()
        val newMovie = Movie(
            id = videoFile.tmdbId,
            title = movie?.title ?: videoFile.name,
            media_type = "movie",
            poster_path = movie?.poster_path,
            vote_average = movie?.vote_average,
            fileId = videoFile.fileId
        )

        tmdbRepository.upsertMovie(newMovie)
    }

    private suspend fun synchronizeLocalMoviesWithRemote(driveFiles: List<DriveFile>) {
        tmdbRepository.getSavedMovies().value?.forEach { savedMovie ->
            if (driveFiles.none { driveFile -> driveFile.id == savedMovie.fileId }) {
                Log.d(TAG, "Deleting movie: ${savedMovie.title} from the database.")
                tmdbRepository.deleteMovie(savedMovie.id)
                moviesResult.deleted++
            }
        }
    }


    override suspend fun indexShows() {
        Log.d(TAG, "Beginning indexing shows...")

        if (!doesShowsFolderExist()) {
            indexingStateFlow.emitErrorIndexingShows("Shows folder does not exist, skipping show processing.")
            Log.d(TAG, "Shows folder does not exist, skipping show processing.")
            return
        }

        val driveFiles = mutableListOf<DriveFile>()

        val driveFilesResult = driveRepository.getAllFilesInFolder(
            queryBuilder = DriveApiQueryBuilder()
                .inParents(sessionManager.fetchShowsFolder()!!)
                .mimeTypeEquals("application/vnd.google-apps.folder")
                .trashed(false)
        )

        if (driveFilesResult is Resource.Success) {
            driveFilesResult.data!!.map { it.toDriveFile() }
                .filter { it.isFolder }
                .toTypedArray()
                .let { driveFiles.addAll(it) }

            processShows(driveFiles)
        } else {
            indexingStateFlow.emitErrorIndexingShows(driveFilesResult.message ?: "Unknown error")
            Log.d(TAG, "Error getting files: ${driveFilesResult.message ?: "Unknown error"}")
        }

        Log.d(TAG, "Shows indexing result: $showsResult")
        indexingStateFlow.emitIndexingResultShows(showsResult)
        Log.d(TAG, "Ended indexing shows")
    }

    /**
     * Processes a list of DriveFiles representing TV shows by extracting relevant information
     * such as the show's name and TMDB ID. Each show is asynchronously processed using the
     * 'processSingleShow' method, which checks and updates the local database accordingly.
     *
     * If a show already exists in the local database, only the fileId field is updated.
     * If a show doesn't exist, it fetches details from TMDB and inserts it into the local database.
     *
     * After processing all shows, a sanitization process is performed to remove folders that exist
     * in the local database but no longer exist remotely.
     *
     * @param shows A list of DriveFiles representing TV shows to be processed.
     */
    private suspend fun processShows(shows: List<DriveFile>) {
        coroutineScope {
            shows
                .mapNotNull {
                    val parse = parseFileName(it, extension = false)
                    if (parse == null) { showsResult.indexingErrors++ }
                    parse
                }
                .map { videoInfo -> async(Dispatchers.IO) { processSingleShow(videoInfo) } }
                .awaitAll()

            synchronizeLocalShowsWithRemote(shows)
        }
    }


    private suspend fun processSingleShow(videoInfo: Info) {
        Log.d(TAG, "Processing show: ${videoInfo.name}")

        val savedShow = tmdbRepository.fetchShowById(videoInfo.tmdbId)

        if (savedShow != null) {
            updateExistingShow(videoInfo, savedShow)
        } else {
            insertNewShow(videoInfo)
        }

        delay(250L)
    }

    private suspend fun updateExistingShow(videoInfo: Info, existingShow: Show) {
        if (existingShow.fileId != videoInfo.fileId) {
            Log.d(TAG, "Show already exists: ${videoInfo.name}, updating videoId.")
            tmdbRepository.upsertShow(existingShow.copy(fileId = videoInfo.fileId))
        }
        showsResult.existingItemsSkipped++
    }


    private suspend fun insertNewShow(videoInfo: Info) {
        Log.d(TAG, "New show: ${videoInfo.name}, inserting into the database.")
        showsResult.newItemsIndexed++

        val showResponse = tmdbRepository.getShow(videoInfo.tmdbId, appendToQuery = null)

        val show = showResponse.body()
        val newShow = Show(
            id = videoInfo.tmdbId,
            name = show?.name ?: videoInfo.name,
            media_type = "tv",
            poster_path = show?.poster_path,
            vote_average = show?.vote_average,
            fileId = videoInfo.fileId
        )

        tmdbRepository.upsertShow(newShow)
    }


    private suspend fun synchronizeLocalShowsWithRemote(shows: List<DriveFile>) {
        tmdbRepository.getSavedShows().value?.forEach { savedShow ->
            if (shows.none { show -> show.id == savedShow.fileId }) {
                tmdbRepository.deleteShow(savedShow.id)
                showsResult.deleted++
            }
        }
    }

    private fun parseFileName(
        driveFile: DriveFile,
        extension: Boolean = true
    ): Info? {
        val regex = """^(.+) \((\d{4})\) \[(\d+)]${if (extension) "(\\.mkv|\\.mp4)?" else ""}$"""
            .toRegex()

        return try {
            val matchResult = regex.matchEntire(driveFile.name)
            if (matchResult != null) {
                val (name, year, tmdbId) = matchResult.destructured
                Info(name, year.toInt(), tmdbId.toInt(), driveFile.id)
            } else null
        } catch (e: Exception) {
            Log.d(TAG, "Error parsing file name: ${driveFile.name}")
            null
        }
    }

    private suspend fun doesMoviesFolderExist(): Boolean {
        return sessionManager.fetchMovieFolder() != null
    }

    private suspend fun doesShowsFolderExist(): Boolean {
        return sessionManager.fetchShowsFolder() != null
    }
}