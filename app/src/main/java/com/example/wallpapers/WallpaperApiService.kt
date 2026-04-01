package com.example.wallpapers

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WallpaperApiService {
    @GET("api/")
    suspend fun getWallpapers(
        @Query("key") apiKey: String,
        @Query("page") page: Int,
        @Query("q") query: String? = null,
        @Query("image_type") type: String = "photo"
    ): PixabayResponse
}