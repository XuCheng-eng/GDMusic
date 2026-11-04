package com.xucheng.gdmusic

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GDMusicTheme {
                App()
            }
        }
    }
}

class RecentStore(context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences("gd_music", Context.MODE_PRIVATE)

    fun load(): List<String> =
        sp.getString("recent", "")?.split("\u0001")?.filter { it.isNotBlank() } ?: emptyList()

    fun add(keyword: String) {
        val list = (listOf(keyword) + load().filter { it != keyword }).take(8)
        sp.edit().putString("recent", list.joinToString("\u0001")).apply()
    }
}

/**
 * 全局 UI 状态：Compose 可观察
 */
class AppStore {
    var controller by mutableStateOf<MediaController?>(null)
    var isPlaying by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var errorMsg by mutableStateOf<String?>(null)

    var keyword by mutableStateOf("")
    var source by mutableStateOf("netease")
    var results by mutableStateOf<List<Track>>(emptyList())
    var isSearching by mutableStateOf(false)

    var queue by mutableStateOf<List<Track>>(emptyList())
    var currentIndex by mutableIntStateOf(-1)
    var currentBr by mutableIntStateOf(0)
    var bitrate by mutableIntStateOf(320)
    var showPlayer by mutableStateOf(false)
}

@Composable
fun App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { AppStore() }
    val recent = remember { mutableStateListOf<String>() }
    val recentStore = remember { RecentStore(context) }

    LaunchedEffect(Unit) { recent.addAll(recentStore.load()) }

    // 连接后台播放服务
    DisposableEffect(context) {
        val token = SessionToken(context, ComponentName(context, PlayerService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            store.controller = try {
                future.get()
            } catch (_: Exception) {
                null
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            MediaController.releaseFuture(future)
            store.controller?.release()
            store.controller = null
        }
    }

    fun search() {
        val kw = store.keyword.trim()
        if (kw.isEmpty()) return
        recentStore.add(kw)
        recent.clear()
        recent.addAll(recentStore.load())
        store.isSearching = true
        store.errorMsg = null
        scope.launch {
            try {
                store.results = withContext(Dispatchers.IO) {
                    GdApi.search(store.source, kw)
                }
                if (store.results.isEmpty()) store.errorMsg = "没有找到相关歌曲，换个关键词或音源试试"
            } catch (e: Exception) {
                store.results = emptyList()
                store.errorMsg = if (e.message == "503") {
                    "请求太频繁了（5分钟限50次），休息一下再试"
                } else {
                    "搜索失败，请检查网络后重试"
                }
            }
            store.isSearching = false
        }
    }

    fun playTrack(index: Int) {
        val track = store.queue.getOrNull(index) ?: return
        val c = store.controller ?: return
        store.isLoading = true
        store.errorMsg = null
        scope.launch {
            try {
                val song = withContext(Dispatchers.IO) {
                    GdApi.songUrl(track.source, track.id, store.bitrate)
                }
                val pic = withContext(Dispatchers.IO) {
                    GdApi.picUrl(track.source, track.picId)
                }
                store.currentIndex = index
                store.currentBr = song.br
                c.setMediaItem(
                    MediaItem.Builder()
                        .setUri(song.url)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(track.name)
                                .setArtist(track.artist)
                                .setAlbumTitle(track.album)
                                .setArtworkUri(pic?.let { Uri.parse(it) })
                                .build()
                        )
                        .build()
                )
                c.prepare()
                c.play()
            } catch (e: Exception) {
                store.errorMsg = if (e.message == "503") {
                    "请求太频繁了（5分钟限50次），休息一下再试"
                } else {
                    "获取播放链接失败，请稍后重试"
                }
            }
            store.isLoading = false
        }
    }

    fun playNext() {
        if (store.queue.isNotEmpty()) playTrack((store.currentIndex + 1).mod(store.queue.size))
    }

    fun playPrev() {
        if (store.queue.isNotEmpty()) playTrack((store.currentIndex - 1).mod(store.queue.size))
    }

    fun setBitrate(br: Int) {
        store.bitrate = br
        if (store.currentIndex >= 0) playTrack(store.currentIndex)
    }

    // 监听播放器状态
    DisposableEffect(store.controller) {
        val c = store.controller ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) {
                store.isPlaying = value
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) playNext()
            }

            override fun onPlayerError(error: PlaybackException) {
                store.errorMsg = "播放出错，试试其他歌曲或音源"
                store.isLoading = false
            }
        }
        c.addListener(listener)
        onDispose { c.removeListener(listener) }
    }

    val onPlay: (Int) -> Unit = { index ->
        store.queue = store.results
        store.showPlayer = true
        playTrack(index)
    }

    if (store.showPlayer) {
        PlayerScreen(
            store = store,
            onClose = { store.showPlayer = false },
            onTogglePlay = {
                val c = store.controller
                if (c != null) {
                    if (c.isPlaying) c.pause() else c.play()
                }
            },
            onNext = { playNext() },
            onPrev = { playPrev() },
            onBitrateChange = { setBitrate(it) }
        )
    } else {
        SearchScreen(
            store = store,
            recent = recent.toList(),
            onSearch = { search() },
            onRecentClick = { kw ->
                store.keyword = kw
                search()
            },
            onPlay = onPlay,
            onTogglePlay = {
                val c = store.controller
                if (c != null) {
                    if (c.isPlaying) c.pause() else c.play()
                }
            },
            onNext = { playNext() },
            onOpenPlayer = { if (store.currentIndex >= 0) store.showPlayer = true }
        )
    }
}
