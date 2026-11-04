package com.xucheng.gdmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class LyricLine(val timeMs: Long, val text: String)

fun parseLrc(lrc: String): List<LyricLine> {
    val regex = Regex("""\[(\d{1,2}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    val lines = mutableListOf<LyricLine>()
    lrc.lines().forEach { raw ->
        val matches = regex.findAll(raw).toList()
        if (matches.isNotEmpty()) {
            val text = raw.replace(regex, "").trim()
            if (text.isNotEmpty()) {
                matches.forEach { m ->
                    val min = m.groupValues[1].toLong()
                    val sec = m.groupValues[2].toLong()
                    val fracStr = m.groupValues[3]
                    val frac = when (fracStr.length) {
                        0 -> 0L
                        1 -> fracStr.toLong() * 100
                        2 -> fracStr.toLong() * 10
                        else -> fracStr.take(3).toLong()
                    }
                    lines.add(LyricLine(min * 60_000 + sec * 1_000 + frac, text))
                }
            }
        }
    }
    return lines.sortedBy { it.timeMs }
}

fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

@Composable
fun PlayerScreen(
    store: AppStore,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onBitrateChange: (Int) -> Unit
) {
    val track = store.queue.getOrNull(store.currentIndex)
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    var showLyric by remember { mutableStateOf(false) }
    var lyricLines by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var tLyricLines by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var coverUrl by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    val controller = store.controller

    // 轮询播放进度
    LaunchedEffect(controller, store.isPlaying, store.currentIndex) {
        while (true) {
            if (controller != null) {
                positionMs = controller.currentPosition.coerceAtLeast(0L)
                durationMs = controller.duration.coerceAtLeast(0L)
            }
            delay(500)
        }
    }

    // 加载封面与歌词
    LaunchedEffect(track?.id, track?.source) {
        if (track != null) {
            coverUrl = coverCache[track.picId]
            if (coverUrl == null) {
                coverUrl = withContext(Dispatchers.IO) {
                    GdApi.picUrl(track.source, track.picId, 500)
                }?.also { if (it != null) coverCache[track.picId] = it }
            }
            val (lrc, tLrc) = withContext(Dispatchers.IO) {
                GdApi.lyric(track.source, track.lyricId)
            }
            lyricLines = parseLrc(lrc)
            tLyricLines = parseLrc(tLrc)
        }
    }

    // 当前歌词行
    val currentLineIndex = if (showLyric && lyricLines.isNotEmpty()) {
        lyricLines.indexOfLast { it.timeMs <= positionMs }
    } else -1

    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 3) {
            listState.animateScrollToItem(currentLineIndex - 3)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 顶栏
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "收起",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.weight(1f))
                if (store.currentBr > 0) {
                    Text(
                        "${if (store.currentBr >= 740) "无损" else "${store.currentBr}K"}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                }
            }

            if (showLyric) {
                // 歌词模式
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (lyricLines.isEmpty()) {
                        Text(
                            "暂无歌词",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 40.dp)
                        ) {
                            items(lyricLines.size) { i ->
                                val isCurrent = i == currentLineIndex
                                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                    Text(
                                        lyricLines[i].text,
                                        fontSize = if (isCurrent) 15.sp else 13.sp,
                                        fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                                    )
                                    val tLine = tLyricLines.firstOrNull {
                                        it.timeMs == lyricLines[i].timeMs
                                    }
                                    if (tLine != null) {
                                        Text(
                                            tLine.text,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // 封面模式
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(230.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (coverUrl != null) {
                                AsyncImage(
                                    model = coverUrl,
                                    contentDescription = "封面",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    MusicNoteIcon(
                                        tint = MaterialTheme.colorScheme.outline,
                                        size = 64.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 歌名/歌手
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            ) {
                Text(
                    track?.name ?: "",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    track?.let { "${it.artist} · ${it.album}" } ?: "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(10.dp))

            // 进度条
            Column(Modifier.padding(horizontal = 24.dp)) {
                Slider(
                    value = when {
                        durationMs <= 0 -> 0f
                        dragging -> dragValue
                        else -> positionMs.toFloat() / durationMs
                    },
                    onValueChange = {
                        dragging = true
                        dragValue = it
                    },
                    onValueChangeFinished = {
                        if (durationMs > 0) {
                            controller?.seekTo((dragValue * durationMs).toLong())
                        }
                        dragging = false
                    },
                    enabled = durationMs > 0
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(formatTime(if (dragging) (dragValue * durationMs).toLong() else positionMs), fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.weight(1f))
                    Text(formatTime(durationMs), fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                }
            }

            // 控制按钮
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
            ) {
                IconButton(onClick = onPrev, modifier = Modifier.size(52.dp)) {
                    PrevIcon(tint = MaterialTheme.colorScheme.onSurface, size = 26.dp)
                }
                Spacer(Modifier.width(14.dp))
                Box(
                    Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onTogglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    if (store.isLoading) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else if (store.isPlaying) {
                        PauseIcon(tint = MaterialTheme.colorScheme.onPrimary, size = 26.dp)
                    } else {
                        PlayIcon(tint = MaterialTheme.colorScheme.onPrimary, size = 26.dp)
                    }
                }
                Spacer(Modifier.width(14.dp))
                IconButton(onClick = onNext, modifier = Modifier.size(52.dp)) {
                    NextIcon(tint = MaterialTheme.colorScheme.onSurface, size = 26.dp)
                }
            }

            // 音质选择 + 歌词开关
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                BITRATES.forEach { br ->
                    FilterChip(
                        selected = store.bitrate == br,
                        onClick = { onBitrateChange(br) },
                        label = { Text(if (br >= 740) "无损" else "${br}K", fontSize = 11.sp) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (showLyric) "封面" else "歌词",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showLyric = !showLyric }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
