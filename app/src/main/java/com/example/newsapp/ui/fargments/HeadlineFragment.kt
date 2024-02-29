package com.example.newsapp.ui.fargments


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
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newsapp.R
import com.example.newsapp.adapters.NewsAdapter
import com.example.newsapp.databinding.FragmentHeadlineBinding
import com.example.newsapp.ui.NewsActivity
import com.example.newsapp.utils.Constants
import com.example.newsapp.utils.Resource
import com.example.newsapp.viewmodels.NewsViewModel


class HeadlineFragment : Fragment() {

    private lateinit var binding: FragmentHeadlineBinding
    private lateinit var newsViewModel: NewsViewModel
    private val newsArticleAdapter by lazy { NewsAdapter() }
    private lateinit var retryButton: Button
    private lateinit var errorText: TextView
    private lateinit var itemHeadLinesError: CardView

    var isError = false
    var isLoading = false
    var isLastPage = false
    var isScrolling = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHeadlineBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        newsViewModel = (activity as NewsActivity).newsViewModel
        itemHeadLinesError = view.findViewById(R.id.itemHeadlinesError)


        // Inflate error_layout in fragment layout
        val inflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = inflater.inflate(R.layout.item_error, null)

        // Uses item_error view
        retryButton = view.findViewById(R.id.retryButton)
        errorText = view.findViewById(R.id.errorText)

        setUpNewsArticleRV()

        // Opens WebView for an article
        newsArticleAdapter.setOnItemClickListener {
            val b = Bundle().apply {
                putSerializable("article", it)
            }
            findNavController().navigate(R.id.action_headlineFragment_to_articleFragment, b)
        }

        // Updating UI based on the livedata updation of NewsResponses
        observeHeadlines()


        retryButton.setOnClickListener {
            newsViewModel.getHeadlines("in")
        }
    }

    private fun observeHeadlines() {
        newsViewModel.headlines.observe(viewLifecycleOwner, Observer { response ->
            when (response) {
                is Resource.Loading -> {
                    showProgressBar()
                }

                is Resource.Success -> {
                    hideErrorMessage()
                    hideProgressBar()
                    response.data?.let {
                        newsArticleAdapter.differ.submitList(it.articles.toList())

                        //Calculates the total number of pages based on totalResults and a constant QUERY_PAGE_SIZE
                        val totalPages = it.totalResults / Constants.QUERY_PAGE_SIZE + 2

                        //Compares the current page (newsViewModel.headlinesPage) with the calculated total pages to determine if it is the last page.
                        isLastPage = newsViewModel.headlinesPage == totalPages

                        // If it is the last page, it adjusts the padding of the recyclerHeadlines view to remove any bottom padding.
                        if (isLastPage) {
                            binding.recyclerHeadlines.setPadding(0, 0, 0, 0)
                        }
                    }
                }

                is Resource.Error -> {
                    hideProgressBar()
                    response.message?.let { message ->
                        Toast.makeText(activity, "Sorry error : $message", Toast.LENGTH_SHORT)
                            .show()
                        showErrorMessage(message)
                    }
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
                newsViewModel.getHeadlines("in")
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

    private fun showErrorMessage(message: String) {
        itemHeadLinesError.visibility = View.VISIBLE
        errorText.text = message
        isError = true
    }

    private fun hideErrorMessage() {
        itemHeadLinesError.visibility = View.GONE
        isError = false
    }

    private fun showProgressBar() {
        binding.paginationProgressBar.visibility = View.VISIBLE
        isLoading = true
    }

    private fun hideProgressBar() {
        binding.paginationProgressBar.visibility = View.GONE
        isLoading = false
    }

    private fun setUpNewsArticleRV() {
        binding.recyclerHeadlines.apply {
            adapter = newsArticleAdapter
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            addOnScrollListener(this@HeadlineFragment.scrollListener)
        }
    }


}