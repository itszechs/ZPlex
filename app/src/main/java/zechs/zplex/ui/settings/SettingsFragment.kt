package zechs.zplex.ui.settings


import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.constraintlayout.widget.Constraints
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.launch
import zechs.zplex.R
import zechs.zplex.databinding.FragmentSettingsBinding
import zechs.zplex.service.RemoteLibraryIndexingService
import zechs.zplex.service.ServiceState
import zechs.zplex.ui.settings.dialog.LoadingDialog
import zechs.zplex.ui.settings.dialog.StatsDialog
import zechs.zplex.utils.FolderPickerResultContract
import zechs.zplex.utils.FolderType
import zechs.zplex.utils.SelectedFolder
import zechs.zplex.utils.StartFolderPicker
import zechs.zplex.utils.ext.navigateSafe


class SettingsFragment : Fragment() {

    companion object {
        const val TAG = "SettingsFragment"
    }

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel by activityViewModels<SettingsViewModel>()

    private var loadingDialog: LoadingDialog? = null
    private val statsDialog by lazy { StatsDialog(requireContext()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(
            /* axis */ MaterialSharedAxis.Y,
            /* forward */ true
        ).apply {
            interpolator = LinearInterpolator()
            duration = 300
        }

        returnTransition = MaterialSharedAxis(
            /* axis */ MaterialSharedAxis.Y,
            /* forward */ false
        ).apply {
            interpolator = LinearInterpolator()
            duration = 300
        }

        exitTransition = MaterialSharedAxis(
            /* axis */ MaterialSharedAxis.Y,
            /* forward */ true
        ).apply {
            interpolator = LinearInterpolator()
            duration = 250
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(
            inflater, container, /* attachToParent */false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.settingConfigureClient.setOnClickListener {
            findNavController().navigateSafe(R.id.action_settingsFragment_to_signInFragment)
        }

        binding.btnSelectMovies.setOnClickListener {
            showFolderPickerDialog(
                binding.btnSelectMovies.text.toString(),
                getString(R.string.change_movies_folder_title),
                getString(R.string.remove_movies_message),
                StartFolderPicker(getString(R.string.choose_movies_folder), FolderType.MOVIES)
            )
        }

        binding.btnSelectShows.setOnClickListener {
            showFolderPickerDialog(
                binding.btnSelectShows.text.toString(),
                getString(R.string.change_shows_folder_title),
                getString(R.string.remove_shows_message),
                StartFolderPicker(getString(R.string.choose_shows_folder), FolderType.SHOWS)
            )
        }

        binding.settingLogOut.setOnClickListener { logoutDialog() }

        observerBothFolders()
        loadingObserver()
        loginStatusObserver()
        indexingServiceObserver()
        setupIndexingResultObserver()
    }

    private fun showFolderPickerDialog(
        buttonText: String,
        title: String,
        message: String,
        folderPicker: StartFolderPicker
    ) {
        if (buttonText == getString(R.string.selected)) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    // Note: No explicit need to drop the table as the IndexingService will
                    // remove saved entries that are missing in remote folder.
                    launchPicker.launch(folderPicker)
                }
                .setNegativeButton(getString(R.string.no)) { _, _ -> }
                .show()
        } else {
            launchPicker.launch(folderPicker)
        }
    }

    private val launchPicker = registerForActivityResult(FolderPickerResultContract()) {
        if (it != null) {
            handleSelectedFolder(it)
        }
    }

    private fun handleSelectedFolder(folder: SelectedFolder) {
        Log.d(TAG, "handleSelectedFolder: $folder")
        when (folder.type) {
            FolderType.MOVIES -> handleMoviesFolder(folder.name, folder.id)
            FolderType.SHOWS -> handleShowsFolder(folder.name, folder.id)
        }
    }

    private fun handleMoviesFolder(name: String, id: String) {
        binding.btnSelectMovies.text = getString(R.string.selected)
        viewModel.saveMoviesFolder(id)
        Log.d(TAG, "handleMoviesFolder: $name, $id")
    }

    private fun handleShowsFolder(name: String, id: String) {
        binding.btnSelectShows.text = getString(R.string.selected)
        viewModel.saveShowsFolder(id)
        Log.d(TAG, "handleShowsFolder: $name, $id")
    }

    private fun observerBothFolders() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.hasMovieFolder.collect { folder ->
                    binding.btnSelectMovies.apply {
                        text = if (folder == null) {
                            getString(R.string.select)
                        } else {
                            getString(R.string.selected)
                        }
                    }

                    Log.d(TAG, "hasMovieFolder: $folder")
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.hasShowsFolder.collect { folder ->
                    binding.btnSelectShows.apply {
                        text = if (folder == null) {
                            getString(R.string.select)
                        } else {
                            getString(R.string.selected)
                        }
                    }

                    Log.d(TAG, "hasShowsFolder: $folder")
                }
            }
        }
    }

    private fun logoutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.logout_title))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                viewModel.logOut()
            }
            .setNegativeButton(getString(R.string.no)) { _, _ -> }
            .show()
    }

    private fun loadingObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loading.collect { loading ->
                    if (loading) {
                        showLoadingDialog()
                    } else {
                        loadingDialog?.dismiss()
                    }
                }
            }
        }
    }

    private fun showLoadingDialog() {
        if (loadingDialog == null) {
            loadingDialog = LoadingDialog(context = requireContext())
        }

        loadingDialog?.show()

        loadingDialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                Constraints.LayoutParams.MATCH_PARENT,
                Constraints.LayoutParams.WRAP_CONTENT
            )
        }

        loadingDialog?.setOnDismissListener {
            loadingDialog = null
        }
    }

    private fun loginStatusObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoggedIn.collect { loggedIn ->
                    TransitionManager.beginDelayedTransition(binding.root, null)
                    binding.settingSelectMoviesFolder.isGone = !loggedIn
                    binding.settingsSelectShowsFolder.isGone = !loggedIn
                    binding.settingLogOut.isGone = !loggedIn
                }
            }
        }
    }

    private fun indexingServiceObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.indexingServiceStatus.collect { state ->
                    when (state) {
                        ServiceState.Checking -> {
                            Log.d(TAG, "indexingServiceObserver: Checking")
                        }

                        is ServiceState.Running -> {
                            handleServiceRunning()
                        }

                        is ServiceState.Stopped -> {
                            handleServiceStopped(state.lastRun)
                        }
                    }
                }
            }
        }
    }

    private fun resetAnimation() {
        binding.progressImageStatus.rotation = 0f
        binding.progressImageStatus.animate().cancel()
    }

    private fun handleServiceStopped(lastRun: String?) {
        binding.apply {
            progressImageStatus.setImageDrawable(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_sync_done_24)
            )
            progressImageStatus.isVisible = true
            progressImageStatus.animate()
                .rotationBy(360f)
                .setInterpolator(OvershootInterpolator())
                .withEndAction { resetAnimation() }
                .start()
            lastScannedLabel.text = getString(
                R.string.last_scanned,
                lastRun ?: getString(R.string.never)
            )
            settingScanMedia.setOnClickListener {
                requireActivity().startService(
                    Intent(requireContext(), RemoteLibraryIndexingService::class.java)
                )
            }
            btnShowDetails.setOnClickListener { statsDialog.show() }
        }
    }

    private fun handleServiceRunning() {
        binding.apply {
            progressImageStatus.setImageDrawable(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_sync_24)
            )
            progressImageStatus.isVisible = true
            progressImageStatus.animate()
                .rotationBy(360f)
                .setInterpolator(OvershootInterpolator())
                .withEndAction { resetAnimation() }
                .start()
            lastScannedLabel.text = getString(R.string.scanning)
            settingScanMedia.setOnClickListener {
                Snackbar.make(
                    binding.root,
                    getString(R.string.scanning_in_progress),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            btnShowDetails.setOnClickListener {
                Snackbar.make(
                    binding.root,
                    getString(R.string.scanning_in_progress),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupIndexingResultObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.combinedIndexingResult.collect { (movies, shows) ->
                    statsDialog.updateStats(movies, shows)
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}