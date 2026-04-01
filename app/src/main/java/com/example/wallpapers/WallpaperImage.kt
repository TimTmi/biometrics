package com.example.wallpapers

data class WallpaperImage(
    val id: Int,
    val webformatURL: String,
    val largeImageURL: String,
    var height: Int = 0
)