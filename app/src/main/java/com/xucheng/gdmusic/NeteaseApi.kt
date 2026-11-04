package com.xucheng.gdmusic

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 网易云公开 Legacy 接口（无需登录/Key，国内直连）
 * 仅用于获取「同歌手热门歌曲」做推荐，播放仍走 GD 音乐台 API（netease ID 通用）
 */
object NeteaseApi {
    private const val BASE = "https://music.163.com"

    private fun get(path: String): String {
        val conn = URL("$BASE$path").openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) GDMusic/1.1")
        conn.setRequestProperty("Referer", "https://music.163.com")
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            return stream?.bufferedReader()?.use { it.readText() } ?: ""
        } finally {
            conn.disconnect()
        }
    }

    private fun parseArtists(arr: JSONArray?): String {
        if (arr == null) return "未知歌手"
        return buildString {
            for (i in 0 until arr.length()) {
                if (i > 0) append(" / ")
                append(arr.getJSONObject(i).optString("name"))
            }
        }.ifBlank { "未知歌手" }
    }

    private fun toTrack(song: JSONObject, source: String): Track {
        return Track(
            id = song.optString("id"),
            name = song.optString("name"),
            artist = parseArtists(song.optJSONArray("artists")),
            album = song.optJSONObject("album")?.optString("name")?.ifBlank { "未知专辑" } ?: "未知专辑",
            picId = song.optJSONObject("album")?.optString("picId") ?: "",
            lyricId = song.optString("id"),
            source = source
        )
    }

    /**
     * 搜索定位歌手：返回最佳匹配歌曲的歌手 ID
     */
    fun findArtistId(trackName: String, artistName: String): Long? {
        val q = "/api/search/get/web?s=${URLEncoder.encode("$trackName $artistName", "UTF-8")}&type=1&limit=6"
        val root = JSONObject(get(q))
        val songs = root.optJSONObject("result")?.optJSONArray("songs") ?: return null
        if (songs.length() == 0) return null
        // 优先找歌手名能对上的
        for (i in 0 until songs.length()) {
            val song = songs.getJSONObject(i)
            val artists = song.optJSONArray("artists")
            if (artists != null) {
                for (j in 0 until artists.length()) {
                    val an = artists.getJSONObject(j).optString("name")
                    if (an.isNotEmpty() && (artistName.contains(an) || an.contains(artistName))) {
                        return artists.getJSONObject(j).optLong("id").takeIf { it > 0 }
                    }
                }
            }
        }
        return songs.getJSONObject(0).optJSONArray("artists")?.optJSONObject(0)?.optLong("id")?.takeIf { it > 0 }
    }

    /**
     * 歌手热门歌曲（50 首），剔除与 excludeName 相同的曲目
     */
    fun hotSongs(artistId: Long, excludeName: String): List<Track> {
        val root = JSONObject(get("/api/artist/$artistId"))
        val hot = root.optJSONArray("hotSongs") ?: return emptyList()
        val list = mutableListOf<Track>()
        for (i in 0 until hot.length()) {
            val t = toTrack(hot.getJSONObject(i), "netease")
            if (t.name != excludeName) list.add(t)
        }
        return list
    }
}
