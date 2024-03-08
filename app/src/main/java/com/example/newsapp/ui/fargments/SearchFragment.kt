package com.example.newsapp.ui.fargments

import android.app.Fragment
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newsapp.R
import com.example.newsapp.adapters.NewsAdapter
import com.example.newsapp.databinding.FragmentSearchBinding
import com.example.newsapp.ui.NewsActivity
import com.example.newsapp.utils.Constants
import com.example.newsapp.utils.Resource
import com.example.newsapp.viewmodels.NewsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private lateinit var newsViewModel: NewsViewModel
    private val searchAdapter by lazy { NewsAdapter() }

    // For error layout variables
    private lateinit var retryButton: Button
    private lateinit var errorText: TextView
    private lateinit var itemSearchError: CardView

    var isError = false
    var isLoading = false
    var isLastPage = false
    var isScrolling = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSearchBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        newsViewModel = (activity as NewsActivity).newsViewModel

        // Inflating error layout (which uses item_error layout) inside fragment layout
        itemSearchError = view.findViewById(R.id.itemSearchError)

        val inflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = inflater.inflate(R.layout.item_error, null)

        errorText = view.findViewById(R.id.errorText)
        retryButton = view.findViewById(R.id.retryButton)

        setUpSearchRV()


        // Clicks on the searched article item to open the web url
        searchAdapter.setOnItemClickListener {
            val b = Bundle().apply {
                putSerializable("article", it)
            }
            findNavController().navigate(R.id.action_searchFragment2_to_articleFragment, b)
        }

        // Handling the search text behaviour and suspending the search for few ms using Job on mainScope
        var job: Job? = null
        binding.searchEdit.addTextChangedListener {
            job?.cancel()
            job = MainScope().launch {
                delay(Constants.SEARCH_NEWS_TIME_DELAY)
                it?.let {
                    if (it.toString().isNotEmpty()) {
                        newsViewModel.searchNews(it.toString())
                    }
                }
            }
        }

        // Updating UI based on the livedata updation of NewsResponses search
        observeSearchNews()

        // Retries the fetching of search responses
        retryButton.setOnClickListener {
            if (binding.searchEdit.text.toString().isNotEmpty()) {
                newsViewModel.searchNews(binding.searchEdit.text.toString())
            } else {
                hideErrorMessage()
            }
        }
    }

    private fun observeSearchNews() {
        newsViewModel.searchNews.observe(viewLifecycleOwner, Observer {
            when (it) {
                is Resource.Success -> {
                    hideProgressBarLoading()
                    it.data?.let {
                        searchAdapter.differ.submitList(it.articles.toList())
                        val totalPages = it.totalResults / Constants.QUERY_PAGE_SIZE + 2

                        //Compares the current page (newsViewModel.headlinesPage) with the calculated total pages to determine if it is the last page.
                        isLastPage = newsViewModel.searchNewsPage == totalPages

                        // If it is the last page, it adjusts the padding of the recyclerHeadlines view to remove any bottom padding.
                        if (isLastPage) {
                            binding.recyclerSearch.setPadding(0, 0, 0, 0)
                        }
                    }
                }

                is Resource.Error -> {
                    hideProgressBarLoading()
                    it.message?.let { message ->
                        Toast.makeText(activity, "Sorry error : $message", Toast.LENGTH_SHORT)
                            .show()
                        showErrorMessage(message)
                    }

                }

                is Resource.Loading -> {
                    showProgressBarLoading()
                }

                else -> {
                    Unit
                }
            }
        })
    }

    // For Pagination of news articles when new data comes and user scrolls to view it
    val scrollListener = object : RecyclerView.OnScrollListener() {

        // After Scrolling
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
            val visibleItemCount = layoutManager.childCount
            val totalItemCount = layoutManager.itemCount

            val isNoError = !isError
            val isNotLoadingAndNotLastPage = !isLoading && !isLastPage
            val isAtLastItem = firstVisibleItemPosition + visibleItemCount >= totalItemCount
            val isNotAtBeginning = firstVisibleItemPosition >= 0
            val isTotalMoreThanVisible = totalItemCount >= Constants.QUERY_PAGE_SIZE
            val shouldPaginate = isNoError && isNotLoadingAndNotLastPage && isAtLastItem
                    && isNotAtBeginning && isTotalMoreThanVisible
            if (shouldPaginate) {
                newsViewModel.searchNews(binding.searchEdit.text.toString())
                isScrolling = false
            }
        }

        // When state of scrolling changes ( user is scrolling )
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                isScrolling = true
            }
        }
    }

    private fun hideProgressBarLoading() {
        binding.paginationProgressBar.visibility = View.GONE
        isLoading = false
    }

    private fun showProgressBarLoading() {
        binding.paginationProgressBar.visibility = View.VISIBLE
        isLoading = true
    }

    private fun showErrorMessage(message: String) {
        itemSearchError.visibility = View.VISIBLE
        errorText.text = message
        isError = true
    }

    private fun hideErrorMessage() {
        itemSearchError.visibility = View.GONE
        isError = false
    }

    private fun setUpSearchRV() {
        binding.recyclerSearch.apply {
            adapter = searchAdapter
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            addOnScrollListener(this@SearchFragment.scrollListener)
        }
    }

}