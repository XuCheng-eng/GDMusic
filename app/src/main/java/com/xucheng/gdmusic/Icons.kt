package com.xucheng.gdmusic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 播放/暂停/切歌图标：用 Canvas 手绘，避免引入 10MB 的 material-icons-extended
 */
@Composable
fun PlayIcon(tint: Color, size: Dp = 24.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.12f, 0f)
            lineTo(w, h / 2f)
            lineTo(w * 0.12f, h)
            close()
        }
        drawPath(path, tint)
    }
}

@Composable
fun PauseIcon(tint: Color, size: Dp = 24.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val barW = w * 0.22f
        val r = CornerRadius(barW / 3f)
        drawRoundRect(tint, topLeft = Offset(w * 0.20f, 0f), size = Size(barW, w), cornerRadius = r)
        drawRoundRect(tint, topLeft = Offset(w * 0.58f, 0f), size = Size(barW, w), cornerRadius = r)
    }
}

@Composable
fun NextIcon(tint: Color, size: Dp = 24.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(w * 0.62f, h / 2f)
            lineTo(0f, h)
            close()
        }
        drawPath(path, tint)
        drawRoundRect(
            tint,
            topLeft = Offset(w * 0.74f, 0f),
            size = Size(w * 0.16f, h),
            cornerRadius = CornerRadius(w * 0.05f)
        )
    }
}

@Composable
fun PrevIcon(tint: Color, size: Dp = 24.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w, 0f)
            lineTo(w * 0.38f, h / 2f)
            lineTo(w, h)
            close()
        }
        drawPath(path, tint)
        drawRoundRect(
            tint,
            topLeft = Offset(w * 0.10f, 0f),
            size = Size(w * 0.16f, h),
            cornerRadius = CornerRadius(w * 0.05f)
        )
    }
}

@Composable
fun MusicNoteIcon(tint: Color, size: Dp = 24.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stemX = w * 0.68f
        val stemTop = h * 0.12f
        drawLine(
            tint,
            Offset(stemX, stemTop),
            Offset(stemX, h * 0.72f),
            strokeWidth = w * 0.09f
        )
        drawLine(
            tint,
            Offset(w * 0.30f, stemTop + h * 0.05f),
            Offset(stemX, stemTop),
            strokeWidth = w * 0.09f
        )
        drawCircle(tint, radius = w * 0.17f, center = Offset(w * 0.48f, h * 0.74f))
    }
}
