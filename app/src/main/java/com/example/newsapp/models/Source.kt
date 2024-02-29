package com.example.newsapp.models

data class Source(
    val id: String,
    val name: String
){
    override fun hashCode(): Int {
        var result = id.hashCode()

        if (id.isNullOrEmpty()) {
            result = 31 * result + id.hashCode()
        }
        if (name.isNullOrEmpty()) {
            result = 31 * result + name.hashCode()
        }
        return result
    }
}