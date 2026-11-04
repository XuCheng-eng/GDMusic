package com.xucheng.gdmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RecommendScreen(
    store: AppStore,
    onRefresh: () -> Unit,
    onPlay: (Int) -> Unit
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
                "为你推荐",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            val seed = store.recSeedName
            Text(
                if (seed != null) "基于《$seed》的同歌手热门 · 网易云曲库" else "先去搜索并播放一首歌，我会根据它推荐相似口味",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(10.dp))
            Row {
                FilterChip(
                    selected = false,
                    onClick = onRefresh,
                    label = { Text("换一批", fontSize = 12.sp) }
                )
            }
            Spacer(Modifier.height(4.dp))
        }

        if (store.recMsg != null) {
            Text(
                store.recMsg!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
            )
        }

        when {
            store.recLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            store.recList.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "播放一首歌后再来试试 🎧",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            else -> {
                LazyColumn(Modifier.weight(1f)) {
                    itemsIndexed(store.recList) { index, track ->
                        val playing = index == store.currentIndex && store.queue === store.recList
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
    }
}
