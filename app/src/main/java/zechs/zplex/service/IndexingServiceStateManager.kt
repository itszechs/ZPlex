package zechs.zplex.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class IndexingStateFlow {

    private val _serviceState = MutableStateFlow<ServiceState>(ServiceState.Checking)
    val serviceState = _serviceState.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Default) + SupervisorJob()

    fun serviceStarted() {
        coroutineScope.launch {
            _serviceState.value = ServiceState.Running()
        }
    }

    fun serviceStopped() {
        coroutineScope.launch {
            _serviceState.value = ServiceState.Stopped()
        }
    }

    private val _indexingMoviesState = MutableStateFlow<IndexingState>(IndexingState.Checking)
    val indexingMoviesState = _indexingMoviesState.asStateFlow()

    fun emitErrorIndexingMovies(message: String) {
        coroutineScope.launch {
            _indexingMoviesState.value = IndexingState.Error(message)
        }
    }

    fun emitIndexingResultMovies(result: IndexingResult) {
        coroutineScope.launch {
            _indexingMoviesState.value = IndexingState.Completed(result)
        }
    }

    private val _indexingShowsState = MutableStateFlow<IndexingState>(IndexingState.Checking)
    val indexingShowsState = _indexingShowsState.asStateFlow()


    fun emitIndexingResultShows(result: IndexingResult) {
        coroutineScope.launch {
            _indexingShowsState.value = IndexingState.Completed(result)
        }
    }

    fun emitErrorIndexingShows(message: String) {
        coroutineScope.launch {
            _indexingShowsState.value = IndexingState.Error(message)
        }
    }

}

sealed interface ServiceState {
    data object Checking : ServiceState
    data class Running(val lastRun: String = getTimestamp()) : ServiceState
    data class Stopped(val lastRun: String = getTimestamp()) : ServiceState
}


sealed interface IndexingState {
    data object Checking : IndexingState
    data class Error(val message: String) : IndexingState
    data class Completed(val stats: IndexingResult) : IndexingState
}

private fun getTimestamp() = formatDate(Calendar.getInstance().timeInMillis)

private fun formatDate(timeInMillis: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeInMillis
    // format: 10:25 PM 6th Jan, 2024
    val dateFormat = SimpleDateFormat("hh:mm a d MMM, yyyy", Locale.ENGLISH)
    return dateFormat.format(calendar.time)
}

data class IndexingResult(
    var newItemsIndexed: Int = 0,
    var existingItemsSkipped: Int = 0,
    var indexingErrors: Int = 0,
    var deleted: Int = 0,
)