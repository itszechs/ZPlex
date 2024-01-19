package zechs.zplex.ui.settings.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import zechs.zplex.R
import zechs.zplex.service.IndexingState

class StatsDialog(context: Context) : Dialog(context) {

    companion object {
        const val TAG = "StatsDialog"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("Media library stats")
        setContentView(R.layout.dialog_loading)
    }

    fun updateStats(movies: IndexingState, shows: IndexingState) {
        Log.d(TAG, "updateStats: $movies, $shows")
    }

}