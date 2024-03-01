package com.example.newsapp.viewmodels

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.models.Article
import com.example.newsapp.models.NewsResponse
import com.example.newsapp.repository.NewsRepository
import com.example.newsapp.utils.Resource
import kotlinx.coroutines.launch
import org.apache.http.params.CoreConnectionPNames
import retrofit2.Response
import java.io.IOException
import java.util.Locale.IsoCountryCode

class NewsViewModel(val app: Application, val newsRepo: NewsRepository) : AndroidViewModel(app) {

    private val _headlines = MutableLiveData<Resource<NewsResponse>>()
    val headlines: LiveData<Resource<NewsResponse>>
        get() = _headlines
    var headlinesPage = 1  // for pagination
    var headlinesResponse: NewsResponse? = null // to store the previous headline responses


    private val _searchNews = MutableLiveData<Resource<NewsResponse>>()
    val searchNews: MutableLiveData<Resource<NewsResponse>>
        get() = _searchNews
    var searchNewsPage = 1 // for pagination
    var searchNewsResponse: NewsResponse? = null // to store the previous search responses
    var newSearchQuery: String? = null //  when new query is searched
    var oldSearchQuery: String? = null //  when previous query is searched

    init {
        getHeadlines("in")
    }

    // 1 -> Getting the handled responses(paginated/updated and internetChecked) of articles for headlines and searching news
    fun getHeadlines(countryCode: String) = viewModelScope.launch {
        checkInternetConnectionHeadlines(countryCode)
    }

    fun searchNews(searchQuery: String) = viewModelScope.launch {
        checkInternetConnectionSearchNews(searchQuery)
    }

    /** 3 ->
     * The fetched responses from the repository passed to the handling functions [handleHeadlinesResponse] &
     * [handleSearchNewsResponse] where these functions will return the paginated and updated articles
     * as Resource() , either success or error
     */
    fun handleHeadlinesResponse(response: Response<NewsResponse>): Resource<NewsResponse> {
        if (response.isSuccessful) {
            response.body()?.let {
                headlinesPage++
                if (headlinesResponse == null) { // first time response is loaded
                    headlinesResponse = it
                } else {  // stores the current list of articles and new list then add the new list to the old articles list
                    val oldArticles = headlinesResponse?.articles
                    val newArticles = it.articles
                    oldArticles?.addAll(newArticles)
                }
                return Resource.Success(headlinesResponse ?: it)
            }
        }
        return Resource.Error(response.message())
    }

    fun handleSearchNewsResponse(response: Response<NewsResponse>): Resource<NewsResponse> {
        if (response.isSuccessful) {
            response.body()?.let {
                if (searchNewsResponse == null || newSearchQuery != oldSearchQuery) { // When search query changed first time
                    searchNewsPage = 1
                    oldSearchQuery = newSearchQuery
                    searchNewsResponse = it
                } else { //Already done searching 1st page and then search more from the next page then add new page response to current response
                    searchNewsPage++
                    val oldArticles = searchNewsResponse?.articles
                    val newArticles = it.articles
                    oldArticles?.addAll(newArticles)
                }
                return Resource.Success(searchNewsResponse ?: it)
            }
        }
        return Resource.Error(response.message())
    }


    /** 2 ->
     * a) Checking the internet connection for headlines and searchNews using [checkInternetConnection]
     * b) Storing the fetched responses from the newsRepo.
     * c) Pass this stored responses to the handling functions [handleHeadlinesResponse] & [handleSearchNewsResponse]
     *    where these functions will return the paginated and updated articles.
     * d) Then, update the headline & searchNews LiveData with the result on the main thread using postValue().
     *    This helps to avoid potential threading issues and ensures that UI-related operations are performed
     *    on the main thread.
     */

    private suspend fun checkInternetConnectionHeadlines(countryCode: String) {
        _headlines.postValue(Resource.Loading())
        try {
            if (checkInternetConnection(this.getApplication())) {
                val response = newsRepo.getHeadlines(countryCode, headlinesPage)
                _headlines.postValue(handleHeadlinesResponse(response))
            } else {
                _headlines.postValue(Resource.Error("No Internet Found"))
            }
        } catch (t: Throwable) {
            when (t) {
                is IOException -> _headlines.postValue(Resource.Error("Unable to connect"))
                else -> _headlines.postValue(Resource.Error("No signal"))
            }
        }
    }

    private suspend fun checkInternetConnectionSearchNews(
        searchQuery: String,
    ) {
        newSearchQuery = searchQuery
        _searchNews.postValue(Resource.Loading())
        try {
            if (checkInternetConnection(this.getApplication())) {
                val response = newsRepo.getSearchInNews(searchQuery,searchNewsPage)
                _searchNews.postValue(handleSearchNewsResponse(response))
            } else {
                _searchNews.postValue(Resource.Error("No Internet Found"))
            }
        } catch (t: Throwable) {
            when (t) {
                is IOException -> _searchNews.postValue(Resource.Error("Unable to connect"))
                else -> _searchNews.postValue(Resource.Error("No signal"))
            }
        }
    }

    // Database related  functions are called in the coroutine lifecycle conscious manner.
    fun addToFav(article: Article) = viewModelScope.launch {
        newsRepo.addToFav(article)
    }

    fun removeFromFav(article: Article) = viewModelScope.launch {
        newsRepo.deleteFromFav(article)
    }

    fun getAllFav() = newsRepo.getAllFavArticles()


    // Connectivity Manager and Network Capabilities are used for internet check
    fun checkInternetConnection(context: Context): Boolean {
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).apply {
            return getNetworkCapabilities(activeNetwork)?.run {
                when {
                    hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
            } ?: false
        }
    }
}