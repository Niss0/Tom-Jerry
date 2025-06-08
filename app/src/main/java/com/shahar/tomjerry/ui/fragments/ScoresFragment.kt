package com.shahar.tomjerry.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.shahar.tomjerry.databinding.FragmentScoresBinding
import com.shahar.tomjerry.ui.adapter.ScoreAdapter
import com.shahar.tomjerry.viewmodel.ScoreViewModel

class ScoresFragment : Fragment() {

    // View Binding property to safely access views.
    private var _binding: FragmentScoresBinding? = null
    private val binding get() = _binding!!

    // Get a reference to the ScoreViewModel using the by viewModels() delegate.
    // This correctly scopes the ViewModel to the Fragment.
    private val scoreViewModel: ScoreViewModel by activityViewModels()

    // The adapter for our RecyclerView.
    private lateinit var scoreAdapter: ScoreAdapter

    // This is where the fragment's UI is created.
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment using View Binding.
        _binding = FragmentScoresBinding.inflate(inflater, container, false)
        return binding.root
    }

    // This method is called after the view has been created.
    // It's the ideal place to set up the RecyclerView and start observing data.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        // Observe the LiveData from the ViewModel.
        // The observer will be notified whenever the data changes.
        scoreViewModel.topScores.observe(viewLifecycleOwner) { scores ->
            // When the scores list is updated, submit it to the adapter.
            scoreAdapter.submitList(scores)
        }
    }

    private fun setupRecyclerView() {
        scoreAdapter = ScoreAdapter { score ->
            // When an item is clicked, call the ViewModel's onScoreSelected function.
            // This updates the shared LiveData.
            scoreViewModel.onScoreSelected(score)
        }

        binding.scoresRecyclerView.apply {
            adapter = scoreAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
