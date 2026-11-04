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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val coverCache = HashMap<String, String>()

@Composable
fun CoverThumb(track: Track, size: Dp, corner: Int = 8) {
    var url by remember(track.picId) { mutableStateOf(coverCache[track.picId]) }
    LaunchedEffect(track.picId) {
        if (url == null) {
            val u = withContext(Dispatchers.IO) {
                GdApi.picUrl(track.source, track.picId, 300)
            }
            if (u != null) {
                coverCache[track.picId] = u
                url = u
            }
        }
    }
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(corner.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MusicNoteIcon(
                    tint = MaterialTheme.colorScheme.outline,
                    size = size / 2
                )
            }
        }
    }
}

@Composable
fun SearchScreen(
    store: AppStore,
    recent: List<String>,
    onSearch: () -> Unit,
    onRecentClick: (String) -> Unit,
    onPlay: (Int) -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(Modifier.padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(10.dp))
            Text(
                "GD 音乐",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = store.keyword,
                onValueChange = { store.keyword = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索歌曲 / 歌手 / 专辑", fontSize = 14.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (store.keyword.isNotEmpty()) {
                        Text(
                            "搜索",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable { onSearch() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SOURCES.forEach { (key, label) ->
                    FilterChip(
                        selected = store.source == key,
                        onClick = {
                            store.source = key
                            if (store.keyword.isNotBlank()) onSearch()
                        },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        if (store.errorMsg != null) {
            Text(
                store.errorMsg!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
            )
        }

        when {
            store.isSearching -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            store.results.isEmpty() && recent.isNotEmpty() -> {
                Column(Modifier.padding(horizontal = 18.dp)) {
                    Spacer(Modifier.height(10.dp))
                    Text("最近搜索", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(8.dp))
                    Row {
                        recent.take(4).forEach { kw ->
                            FilterChip(
                                selected = false,
                                onClick = { onRecentClick(kw) },
                                label = { Text(kw, fontSize = 12.sp) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                    if (recent.size > 4) {
                        Row {
                            recent.drop(4).forEach { kw ->
                                FilterChip(
                                    selected = false,
                                    onClick = { onRecentClick(kw) },
                                    label = { Text(kw, fontSize = 12.sp) },
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            store.results.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "搜一首想听的歌吧 🎵",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            else -> {
                LazyColumn(Modifier.weight(1f)) {
                    itemsIndexed(store.results) { index, track ->
                        val playing = index == store.currentIndex
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (playing) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.background
                                )
                                .clickable { onPlay(index) }
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            CoverThumb(track, 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    track.name,
                                    fontSize = 14.sp,
                                    fontWeight = if (playing) FontWeight.Medium else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${track.artist} · ${track.album}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 底部迷你播放条
        val current = store.queue.getOrNull(store.currentIndex)
        if (current != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onOpenPlayer() }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                CoverThumb(current, 40.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(current.name, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        current.artist,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onTogglePlay) {
                    if (store.isPlaying) {
                        PauseIcon(tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                    } else {
                        PlayIcon(tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                    }
                }
                IconButton(onClick = onNext) {
                    NextIcon(tint = MaterialTheme.colorScheme.onSurface, size = 20.dp)
                }
            }
        }
        Spacer(
            Modifier
                .navigationBarsPadding()
                .height(8.dp)
        )
    }
}
