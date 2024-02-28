package com.example.newsapp.ui.fargments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newsapp.R
import com.example.newsapp.adapters.NewsAdapter
import com.example.newsapp.databinding.FragmentFavouriteBinding
import com.example.newsapp.ui.NewsActivity
import com.example.newsapp.viewmodels.NewsViewModel
import com.google.android.material.snackbar.Snackbar


class FavouriteFragment : Fragment() {
    private lateinit var binding: FragmentFavouriteBinding
    private lateinit var newsViewModel: NewsViewModel
    private val favAdapter by lazy { NewsAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFavouriteBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        newsViewModel = (activity as NewsActivity).newsViewModel

        setUpFavRV()

        // From fav to the weblink opens
        favAdapter.setOnItemClickListener {
            val b = Bundle().apply {
                putSerializable("article", it)
            }
            findNavController().navigate(R.id.action_favouriteFragment_to_articleFragment, b)
        }

        // Swipe delete or undo
        val itemTouchHelper = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or
                    ItemTouchHelper.RIGHT
        ) {
            // allows the item to move
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return true
            }

            // Actions done when swiped
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val article = favAdapter.differ.currentList[position]
                newsViewModel.removeFromFav(article)
                Snackbar.make(view, "Article removed from favorites", Snackbar.LENGTH_SHORT).apply {
                    setAction("Undo") {
                        newsViewModel.addToFav(article)
                    }
                }.show()
            }
        }

        // Instance of ItemTouchHelper and attach to favRV
        ItemTouchHelper(itemTouchHelper).apply {
            attachToRecyclerView(binding.recyclerFavourites)
        }

        // Update the UI with Livedata
        observeFavArticles()
    }

    private fun observeFavArticles() {
        newsViewModel.getAllFav().observe(viewLifecycleOwner, Observer {
            favAdapter.differ.submitList(it)
        })
    }

    private fun setUpFavRV() {
        binding.recyclerFavourites.apply {
            adapter = favAdapter
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }
    }

}