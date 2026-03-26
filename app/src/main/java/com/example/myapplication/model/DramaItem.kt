package com.example.myapplication.model

data class DramaItem(
    val id: String,
    val title: String,
    val description: String,
    val cover: String,
    val episodes: Int,
    val followers: Long,
    val subtitleCount: Int,
    val tags: List<String>,
    val videoUrl: String
)