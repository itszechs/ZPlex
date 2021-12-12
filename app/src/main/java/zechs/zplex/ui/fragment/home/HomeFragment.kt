package zechs.zplex.ui.fragment.home

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionManager
import com.google.android.material.transition.MaterialFade
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import zechs.zplex.R
import zechs.zplex.adapter.FilesAdapter
import zechs.zplex.adapter.LogsAdapter
import zechs.zplex.databinding.FragmentHomeBinding
import zechs.zplex.models.Args
import zechs.zplex.models.drive.File
import zechs.zplex.ui.activity.ZPlexActivity
import zechs.zplex.ui.fragment.ArgsViewModel
import zechs.zplex.utils.Constants.ZPLEX
import zechs.zplex.utils.Resource
import java.net.IDN
import java.net.URI
import java.net.URL


class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel
    private val argsModel: ArgsViewModel by activityViewModels()

    private lateinit var filesAdapter: FilesAdapter
    private lateinit var filesAdapter2: FilesAdapter
    private lateinit var logsAdapter: LogsAdapter

    private val thisTAG = "HomeFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialFade()
        exitTransition = MaterialSharedAxis(
            MaterialSharedAxis.Y, true
        ).apply {
            duration = 500L
        }
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        homeViewModel = (activity as ZPlexActivity).homeViewModel
        setupRecyclerView()

        homeViewModel.homeList.observe(viewLifecycleOwner, { response ->
            when (response) {
                is Resource.Success -> {
                    homeView(true)
                    response.data?.let { filesResponse ->
                        filesAdapter.differ.submitList(filesResponse.files.toList())
                    }
                }
                is Resource.Error -> {
                    homeView(false)
                    response.message?.let { message ->
                        Toast.makeText(
                            context,
                            "An error occurred: $message",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e(thisTAG, "An error occurred: $message")
                    }
                }
                is Resource.Loading -> {
                }
            }
        })

        homeViewModel.logsList.observe(viewLifecycleOwner, { response ->
            TransitionManager.beginDelayedTransition(binding.root)
            when (response) {
                is Resource.Success -> {
                    binding.apply {
                        recentlyAdded.visibility = View.VISIBLE
                        rvNewEpisodes.visibility = View.VISIBLE
                    }
                    response.data?.let { logsResponse ->
                        val newEpisodeList = logsResponse.releasesLog.toList()
                        if (newEpisodeList.isEmpty()) {
                            binding.apply {
                                recentlyAdded.visibility = View.GONE
                                rvNewEpisodes.visibility = View.GONE
                            }
                        }
                        logsAdapter.differ.submitList(newEpisodeList)
                    }
                }
                is Resource.Error -> {
                    binding.apply {
                        recentlyAdded.visibility = View.GONE
                        rvNewEpisodes.visibility = View.GONE
                    }
                    response.message?.let { message ->
                        Toast.makeText(
                            context,
                            "An error occurred: $message",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e(thisTAG, "An error occurred: $message")
                    }
                }
                is Resource.Loading -> {
                    binding.apply {
                        recentlyAdded.visibility = View.VISIBLE
                        rvNewEpisodes.visibility = View.INVISIBLE
                    }
                }
            }
        })

        homeViewModel.getSavedShows().observe(viewLifecycleOwner, { files ->
            if (files.toList().isNotEmpty()) {
                binding.myShows.visibility = View.VISIBLE
                binding.rvMyShowsHome.visibility = View.VISIBLE
                filesAdapter2.differ.submitList(files)
            } else {
                binding.myShows.visibility = View.GONE
                binding.rvMyShowsHome.visibility = View.GONE
            }
        })
    }

    private fun homeView(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        TransitionManager.beginDelayedTransition(binding.root, MaterialFadeThrough())
        binding.rvHome.visibility = visibility
        binding.recentlyAdded.visibility = visibility
    }

    private fun setupRecyclerView() {
        filesAdapter = FilesAdapter()
        filesAdapter2 = FilesAdapter()

        binding.rvHome.apply {
            adapter = filesAdapter
            layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        }

        filesAdapter.setOnItemClickListener {
            getDetails(it)
        }

        logsAdapter = LogsAdapter()
        binding.rvNewEpisodes.apply {
            adapter = logsAdapter
            layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        }

        logsAdapter.setOnItemClickListener {
            val playUrl = URL("${ZPLEX}${it.folder}/${it.file}")
            val playURI = URI(
                playUrl.protocol,
                playUrl.userInfo,
                IDN.toASCII(playUrl.host),
                playUrl.port,
                playUrl.path,
                playUrl.query,
                playUrl.ref
            ).toASCIIString().replace("?", "%3F")

            try {
                val vlcIntent = Intent(Intent.ACTION_VIEW)
                vlcIntent.setPackage("org.videolan.vlc")
                vlcIntent.component = ComponentName(
                    "org.videolan.vlc",
                    "org.videolan.vlc.gui.video.VideoPlayerActivity"
                )
                vlcIntent.setDataAndTypeAndNormalize(Uri.parse(playURI), "video/*")
                vlcIntent.putExtra("title", it.file.dropLast(4))
                vlcIntent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                startActivity(vlcIntent)
            } catch (notFoundException: ActivityNotFoundException) {
                notFoundException.printStackTrace()
                Toast.makeText(
                    context,
                    "VLC not found, Install VLC from Play Store",
                    LENGTH_LONG
                ).show()
            }
        }

        binding.rvMyShowsHome.apply {
            adapter = filesAdapter2
            layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
            filesAdapter2.setOnItemClickListener {
                getDetails(it)
            }
        }
    }

    private fun getDetails(it: File) {
        val regex = "^(.*[0-9])( - )(.*)( - )(TV|Movie)".toRegex()
        val nameSplit = regex.find(it.name)?.destructured?.toList()

        if (nameSplit != null) {
            val mediaId = nameSplit[0]
            val mediaName = nameSplit[2]
            val mediaType = nameSplit[4]

            argsModel.setArg(
                Args(
                    file = it,
                    mediaId = mediaId.toInt(),
                    type = mediaType,
                    name = mediaName
                )
            )
            findNavController().navigate(R.id.action_homeFragment_to_aboutFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.apply {
            rvHome.adapter = null
            rvNewEpisodes.adapter = null
            rvMyShowsHome.adapter = null
        }
        _binding = null
    }

}