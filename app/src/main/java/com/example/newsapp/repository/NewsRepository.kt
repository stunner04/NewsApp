package com.example.newsapp.repository

import com.example.newsapp.api.RetrofitInstance
import com.example.newsapp.db.ArticleDatabase
import com.example.newsapp.models.Article

class NewsRepository(val db: ArticleDatabase) {

    suspend fun getHeadlines(countryCode: String, pageNumber: Int) =
        RetrofitInstance.api.getHeadlines(countryCode, pageNumber)

    suspend fun getSearchInNews(searchQuery: String, pageNumber: Int) =
        RetrofitInstance.api.searchNews(searchQuery, pageNumber)

    fun getAllFavArticles() = db.getArticleDao().getAllArticles()
    suspend fun addToFav(article: Article) = db.getArticleDao().upsert(article)

    suspend fun deleteFromFav(article: Article) = db.getArticleDao().removeArticle(article)

}