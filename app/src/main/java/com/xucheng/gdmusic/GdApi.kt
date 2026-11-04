package com.xucheng.gdmusic

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Track(
    val id: String,
    val name: String,
    val artist: String,
    val album: String,
    val picId: String,
    val lyricId: String,
    val source: String
)

data class SongUrl(val url: String, val br: Int)

val SOURCES = listOf(
    "netease" to "网易云",
    "kuwo" to "酷我",
    "joox" to "JOOX",
    "bilibili" to "B站"
)

val BITRATES = listOf(128, 192, 320, 999)

/**
 * GD 音乐台 API 客户端
 * 出处：GD音乐台(music.gdstudio.xyz) —— 仅限学习使用，严禁商用
 */
object GdApi {
    private const val BASE = "https://music-api.gdstudio.xyz/api.php"

    private fun get(query: String): String {
        val conn = URL("$BASE?$query").openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("User-Agent", "GDMusic/1.0 (Android)")
        try {
            val code = conn.responseCode
            if (code == 503) throw RuntimeException("503")
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            return stream?.bufferedReader()?.use { it.readText() } ?: ""
        } finally {
            conn.disconnect()
        }
    }

    fun search(source: String, keyword: String, count: Int = 30, page: Int = 1): List<Track> {
        val q = "types=search&source=${URLEncoder.encode(source, "UTF-8")}" +
                "&name=${URLEncoder.encode(keyword, "UTF-8")}&count=$count&pages=$page"
        val arr = JSONArray(get(q))
        val list = mutableListOf<Track>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val artists = o.optJSONArray("artist")
            val artistStr = buildString {
                if (artists != null) {
                    for (j in 0 until artists.length()) {
                        if (j > 0) append(" / ")
                        append(artists.optString(j))
                    }
                }
            }
            list.add(
                Track(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    artist = artistStr.ifBlank { "未知歌手" },
                    album = o.optString("album").ifBlank { "未知专辑" },
                    picId = o.optString("pic_id"),
                    lyricId = o.optString("lyric_id").ifBlank { o.optString("id") },
                    source = o.optString("source").ifBlank { source }
                )
            )
        }
        return list
    }

    fun songUrl(source: String, id: String, br: Int): SongUrl {
        val q = "types=url&source=${URLEncoder.encode(source, "UTF-8")}" +
                "&id=${URLEncoder.encode(id, "UTF-8")}&br=$br"
        val obj = JSONObject(get(q))
        val url = obj.optString("url")
        if (url.isBlank()) throw RuntimeException("empty url")
        return SongUrl(url, obj.optInt("br", br))
    }

    fun picUrl(source: String, picId: String, size: Int = 300): String? {
        if (picId.isBlank()) return null
        return try {
            val q = "types=pic&source=${URLEncoder.encode(source, "UTF-8")}" +
                    "&id=${URLEncoder.encode(picId, "UTF-8")}&size=$size"
            JSONObject(get(q)).optString("url").ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    fun lyric(source: String, lyricId: String): Pair<String, String> {
        return try {
            val q = "types=lyric&source=${URLEncoder.encode(source, "UTF-8")}" +
                    "&id=${URLEncoder.encode(lyricId, "UTF-8")}"
            val obj = JSONObject(get(q))
            Pair(obj.optString("lyric"), obj.optString("tlyric"))
        } catch (_: Exception) {
            Pair("", "")
        }
    }
}
