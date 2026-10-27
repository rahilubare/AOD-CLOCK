package com.standby.clock

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class Playing(val controller: MediaController, val title: String, val artist: String, val art: Bitmap?, val playing: Boolean)

/**
 * Shows the song that any media app is playing, with play/pause and skip.
 * Uses the same notification access as the glanceable notifications (Android requires it for media sessions).
 */
@Composable
fun NowPlayingWidget() {
    val ctx = LocalContext.current
    var now by remember { mutableStateOf<Playing?>(null) }
    var allowed by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val msm = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val cn = ComponentName(ctx, StandbyNotificationListener::class.java)
        while (true) {
            try {
                val sessions = msm.getActiveSessions(cn)
                allowed = true
                val c = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: sessions.firstOrNull()
                now = c?.let { ctl ->
                    val m = ctl.metadata
                    Playing(
                        ctl,
                        m?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "",
                        m?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "",
                        m?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: m?.getBitmap(MediaMetadata.METADATA_KEY_ART),
                        ctl.playbackState?.state == PlaybackState.STATE_PLAYING,
                    )
                }
            } catch (e: SecurityException) {
                allowed = false
                now = null
            }
            delay(1500L)
        }
    }

    val p = now
    Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        when {
            !allowed -> Text("Allow notification access in the app\nto show what is playing", color = Color(0xFF8E8E93), fontSize = 18.sp)
            p == null -> Text("Nothing playing", color = Color(0xFF8E8E93), fontSize = 20.sp)
            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                val art = remember(p.art) { p.art?.asImageBitmap() }
                Box(Modifier.size(150.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF2C2C2E))) {
                    if (art != null) Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Spacer(Modifier.width(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.title.ifEmpty { "Unknown title" }, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    if (p.artist.isNotEmpty()) Text(p.artist, color = Color(0xFF8E8E93), fontSize = 17.sp, maxLines = 1)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Transport(0) { p.controller.transportControls.skipToPrevious() }
                        Transport(if (p.playing) 2 else 1) {
                            if (p.playing) p.controller.transportControls.pause() else p.controller.transportControls.play()
                        }
                        Transport(3) { p.controller.transportControls.skipToNext() }
                    }
                }
            }
        }
    }
}

/** kind: 0 previous, 1 play, 2 pause, 3 next */
@Composable
private fun Transport(kind: Int, onClick: () -> Unit) {
    Canvas(Modifier.size(52.dp).clickable(onClick = onClick).padding(13.dp)) {
        val w = size.width
        val h = size.height
        fun tri(mirror: Boolean, x0: Float, x1: Float): Path {
            fun fx(x: Float) = if (mirror) w - x * w else x * w
            return Path().apply {
                moveTo(fx(x0), h * 0.1f)
                lineTo(fx(x1), h * 0.5f)
                lineTo(fx(x0), h * 0.9f)
                close()
            }
        }
        when (kind) {
            1 -> drawPath(tri(false, 0.15f, 0.95f), Color.White)
            2 -> {
                drawRect(Color.White, Offset(w * 0.15f, h * 0.1f), Size(w * 0.25f, h * 0.8f))
                drawRect(Color.White, Offset(w * 0.6f, h * 0.1f), Size(w * 0.25f, h * 0.8f))
            }
            3 -> {
                drawPath(tri(false, 0.05f, 0.75f), Color.White)
                drawRect(Color.White, Offset(w * 0.8f, h * 0.1f), Size(w * 0.15f, h * 0.8f))
            }
            else -> {
                drawPath(tri(true, 0.05f, 0.75f), Color.White)
                drawRect(Color.White, Offset(w * 0.05f, h * 0.1f), Size(w * 0.15f, h * 0.8f))
            }
        }
    }
}
