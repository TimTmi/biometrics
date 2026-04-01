package com.example.wallpapers

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WallpaperRepository {

    private val apiKey = "55108816-5a07d9822a8d4153ed04a9691"

    private val service: WallpaperApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://pixabay.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WallpaperApiService::class.java)
    }

    suspend fun getWallpapers(page: Int, query: String? = null): List<WallpaperImage> {
        return try {
            val response = service.getWallpapers(apiKey, page, query)
            response.hits
        } catch (e: Exception) {
            emptyList()
        }
    }

}