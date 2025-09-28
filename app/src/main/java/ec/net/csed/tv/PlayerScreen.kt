package ec.net.csed.tv
/*
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.viewinterop.AndroidView

import androidx.navigation.NavHostController

@Composable
fun PlayerScreen_old(
    streamId: Int,
    username: String,
    password: String,
    baseUrl: String,
    navController: NavHostController
) {
    val context = LocalContext.current

    // Crear y recordar el reproductor
    val player = remember(streamId) {
        buildExoPlayer(context, streamId, username, password, baseUrl)
    }

    // Liberar el reproductor al salir
    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    // Vista de video con botón de volver
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    this.player = player
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clickable { navController.popBackStack() }
        ) {
            Text("⬅️ Volver", color = Color.White)
        }
    }
}

// Función para construir el ExoPlayer
fun buildExoPlayer_old(
    context: Context,
    streamId: Int,
    username: String,
    password: String,
    baseUrl: String
): ExoPlayer {
    val player = ExoPlayer.Builder(context).build()
    val url = "$baseUrl/live/$username/$password/$streamId.m3u8"
    val mediaItem = MediaItem.fromUri(Uri.parse(url))
    player.setMediaItem(mediaItem)
    player.prepare()
    player.playWhenReady = true
    return player
}
*/
