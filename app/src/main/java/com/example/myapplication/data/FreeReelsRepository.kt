package com.example.myapplication.data

import com.example.myapplication.model.DramaItem
import com.example.myapplication.network.FreeReelsApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

enum class ContentTab {
    FOR_YOU,
    HOMEPAGE,
    ANIME,
}

class FreeReelsRepository(private val api: FreeReelsApi) {
    suspend fun fetchByTab(tab: ContentTab): List<DramaItem> {
        val payload = when (tab) {
            ContentTab.FOR_YOU -> api.getForYou()
            ContentTab.HOMEPAGE -> api.getHomepage()
            ContentTab.ANIME -> api.getAnimePage()
        }
        return normalizeItems(payload)
    }

    suspend fun search(query: String): List<DramaItem> {
        return normalizeItems(api.search(query))
    }

    private fun normalizeItems(payload: JsonElement): List<DramaItem> {
        val seen = mutableSetOf<String>()
        val items = mutableListOf<DramaItem>()

        fun walk(node: JsonElement?) {
            when (node) {
                is JsonArray -> node.forEach { walk(it) }
                is JsonObject -> {
                    val maybe = toItem(node)
                    if (maybe != null && seen.add(maybe.id)) {
                        items += maybe
                    }
                    node.values.forEach { walk(it) }
                }
                else -> Unit
            }
        }

        walk(payload)
        return items
    }

    private fun toItem(raw: JsonObject): DramaItem? {
        val id = raw.stringValue("key") ?: raw.stringValue("id") ?: return null
        val title = raw.stringValue("title") ?: return null
        val cover = raw.stringValue("cover") ?: return null
        if (id.isBlank() || title.isBlank() || cover.isBlank()) return null

        val episodeInfo = raw["episode_info"]?.asObjectOrNull()
        val subtitleCount = episodeInfo
            ?.get("subtitle_list")
            ?.asArrayOrNull()
            ?.size ?: 0

        val videoUrl = listOf(
            "external_audio_h264_m3u8",
            "external_audio_h265_m3u8",
            "m3u8_url",
            "video_url"
        ).firstNotNullOfOrNull { key -> episodeInfo?.stringValue(key) }.orEmpty()

        val tags = raw["series_tag"]
            ?.asArrayOrNull()
            ?.mapNotNull { it.asPrimitiveOrNull()?.contentOrNull?.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

        return DramaItem(
            id = id,
            title = title,
            description = raw.stringValue("desc") ?: "Deskripsi belum tersedia.",
            cover = cover,
            episodes = raw.intValue("episode_count") ?: 0,
            followers = raw.longValue("follow_count") ?: 0,
            subtitleCount = subtitleCount,
            tags = tags,
            videoUrl = videoUrl,
        )
    }
}

fun provideRepository(): FreeReelsRepository {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.sansekai.my.id/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    return FreeReelsRepository(retrofit.create(FreeReelsApi::class.java))
}

private fun JsonObject.stringValue(key: String): String? {
    return this[key]?.asPrimitiveOrNull()?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
}

private fun JsonObject.intValue(key: String): Int? {
    return this[key]?.asPrimitiveOrNull()?.intOrNull
}

private fun JsonObject.longValue(key: String): Long? {
    return this[key]?.asPrimitiveOrNull()?.longOrNull
}

private fun JsonElement.asObjectOrNull(): JsonObject? = this as? JsonObject
private fun JsonElement.asArrayOrNull(): JsonArray? = this as? JsonArray
private fun JsonElement.asPrimitiveOrNull(): JsonPrimitive? = this as? JsonPrimitive