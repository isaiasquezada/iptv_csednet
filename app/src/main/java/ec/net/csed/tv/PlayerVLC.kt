package ec.net.csed.tv

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.SessionManager
// import com.google.android.gms.cast.framework.media.MediaLoadRequestData
// Para control Cast
import com.google.android.gms.cast.MediaLoadRequestData
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.activity.ComponentActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext




@Composable
fun PlayerScreen(
    streamId: Int,
    username: String,
    password: String,
    baseUrl: String,
    navController: NavHostController
) {
    val context = LocalContext.current
    val hasCodecLine = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    //val castContext = remember { CastContext.getSharedInstance(context) }
    //val sessionManager = remember { castContext.sessionManager }
    var castContext: CastContext? by remember { mutableStateOf(null) }
    var sessionManager: SessionManager? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        try {
            val instance = CastContext.getSharedInstance(context)
            castContext = instance
            sessionManager = instance.sessionManager
        } catch (e: Exception) {
            println("⚠️ Google Cast no disponible en este dispositivo: ${e.message}")
        }
    }



    // 🔒 Mantener pantalla encendida
    LaunchedEffect(Unit) {
        val activity = context as? android.app.Activity
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    val url = "$baseUrl/live/$username/$password/$streamId.m3u8"
    val hasCodecsInfo = remember { mutableStateOf(false) }

    val isCastCompatible = remember { mutableStateOf(false) } // <- para mostrar el ícono en blanco o gris

    LaunchedEffect(url) {
        try {
            withContext(Dispatchers.IO) {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.setRequestProperty("User-Agent", "VLC/3.0.16 LibVLC/3.0.16")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val lines = connection.inputStream.bufferedReader().readLines().take(50)

                println("🔍 --- Análisis del .m3u8 del canal $streamId ---")

                lines.forEachIndexed { index, line ->
                    println("📄 [$index] $line")
                }

                //val codecsInfo = lines.filter { it.contains("CODECS", ignoreCase = true) }
                val codecsInfo = lines.filter { it.contains("CODECS", ignoreCase = true) }
                hasCodecLine.value = codecsInfo.isNotEmpty()
                val hasHEVC = codecsInfo.any { it.contains("hev1") || it.contains("hvc1") }
                val hasAC3 = codecsInfo.any { it.contains("ac-3") || it.contains("ec-3") }
                val isReallyCompatible = !hasAC3 && !hasHEVC
                withContext(Dispatchers.Main) {
                    isCastCompatible.value = isReallyCompatible
                }
                val hasAudioGroup = lines.any { it.contains("#EXT-X-MEDIA") && it.contains("TYPE=AUDIO") }

                println("🧪 CODECS info: ${codecsInfo.joinToString()}")
                println("🎧 ¿AC3? $hasAC3 | 🎥 ¿HEVC? $hasHEVC | 🎛️ ¿AudioGroup? $hasAudioGroup")

                if (hasHEVC || hasAC3) {
                    println("❌ El canal podría NO ser compatible con Chromecast")
                } else {
                    println("✅ El canal parece compatible con Chromecast")
                }

                // Guardamos en el estado si es compatible
                hasCodecsInfo.value = !(hasHEVC || hasAC3)

            }
        } catch (e: Exception) {
            println("❌ Error al analizar el .m3u8 del canal $streamId: ${e.message}")
        }
    }


    val libVLC = remember {
        LibVLC(context, mutableListOf("--no-drop-late-frames", "--no-skip-frames"))
    }

    val mediaPlayer = remember(libVLC, url) {
        MediaPlayer(libVLC).apply {
            val media = Media(libVLC, Uri.parse(url)).apply {
                setHWDecoderEnabled(true, false)
                addOption(":network-caching=150")
            }
            this.media = media
            setVolume(100)
            play()

            // 🧠 LOG de diagnóstico del canal
            println("🧪 Diagnóstico de canal $streamId:")
            println("🔗 URL: $url")
            println("🎵 ¿AC3? ${url.contains("ac3", ignoreCase = true)}")
            println("🎞️ ¿.m3u8? ${url.endsWith(".m3u8")}")
            println("📡 ¿Cast compatible? ${url.endsWith(".m3u8") && !url.contains("ac3", ignoreCase = true)}")
        }
    }

    val isMuted = remember { mutableStateOf(false) }
    val audioTracks = remember { mutableStateListOf<MediaPlayer.TrackDescription>() }
    val selectedTrackId = remember { mutableStateOf<Int?>(null) }

    // 📡 Si hay sesión de cast activa, detiene reproducción local y lanza cast
    LaunchedEffect(sessionManager?.currentCastSession) {
        sessionManager?.addSessionManagerListener(object : SessionManagerListener<CastSession> {

            override fun onSessionStarted(session: CastSession, sessionId: String) {
                if (session.isConnected) {
                    // Lógica segura para detener mediaPlayer si está listo
                    scope.launch {
                        val maxRetries = 5
                        var retryCount = 0
                        var stopped = false

                        while (!stopped && retryCount < maxRetries) {
                            try {
                                mediaPlayer.stop()
                                stopped = true
                            } catch (e: IllegalStateException) {
                                println("⏳ mediaPlayer aún no listo (intento ${retryCount + 1})")
                                retryCount++
                                delay(  300) // Espera un poco antes de intentar otra vez
                            }
                        }

                        // Si no se logró detener, abortamos cast
                        if (!stopped) {
                            println("❌ No se pudo detener mediaPlayer después de varios intentos")
                            return@launch
                        }

                        // ✅ Enviar al Chromecast
                        val remoteMediaClient = session.remoteMediaClient
                        val mediaMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
                            putString(MediaMetadata.KEY_TITLE, "Canal $streamId")
                        }

                        /*val mediaInfo = MediaInfo.Builder(url)
                            .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                            .setContentType("application/x-mpegURL")
                            .setMetadata(mediaMetadata)
                            .build()*/

                        val mediaInfo = MediaInfo.Builder(url)
                            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED) // antes: STREAM_TYPE_LIVE
                            .setContentType("application/x-mpegURL")
                            .setMetadata(mediaMetadata)
                            .build()


                        val mediaLoadRequestData = MediaLoadRequestData.Builder()
                            .setMediaInfo(mediaInfo)
                            .build()

                        remoteMediaClient?.load(mediaLoadRequestData)
                    }
                }
            }


            override fun onSessionEnded(session: CastSession, error: Int) {
                // Volver a crear el Media para reproducir localmente
                val newMedia = Media(libVLC, Uri.parse(url)).apply {
                    setHWDecoderEnabled(true, false)
                    addOption(":network-caching=150")
                }

                try {
                    mediaPlayer.media = newMedia
                    mediaPlayer.play()
                } catch (e: Exception) {
                    println("⚠️ No se pudo volver a reproducir localmente: ${e.message}")
                }
            }

            override fun onSessionResumeFailed(session: CastSession, error: Int) {}
            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {}
            override fun onSessionStarting(session: CastSession) {}
            override fun onSessionResuming(session: CastSession, sessionId: String) {}
            override fun onSessionEnding(session: CastSession) {}
            override fun onSessionStartFailed(session: CastSession, error: Int) {}
            override fun onSessionSuspended(session: CastSession, reason: Int) {}

        }, CastSession::class.java)

        /*sessionManager.addSessionManagerListener(object : SessionManagerListener<Session> {
            override fun onSessionStarted(session: CastSession, sessionId: String?) {
                if (session.isConnected) {
                    mediaPlayer.stop()

                    val remoteMediaClient = session.remoteMediaClient
                    val mediaMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
                        putString(MediaMetadata.KEY_TITLE, "Canal $streamId")
                    }

                    val mediaInfo = MediaInfo.Builder(url)
                        .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                        .setContentType("application/x-mpegURL")
                        .setMetadata(mediaMetadata)
                        .build()

                    val mediaLoadRequestData = MediaLoadRequestData.Builder()
                        .setMediaInfo(mediaInfo)
                        .build()

                    remoteMediaClient?.load(mediaLoadRequestData)
                }
            }



            override fun onSessionEnded(session: Session?, error: Int) {
                // Podrías volver a reproducir local si quieres
            }

            override fun onSessionResumeFailed(session: Session?, error: Int) {}
            override fun onSessionResumed(session: Session?, wasSuspended: Boolean) {}
            override fun onSessionStarting(session: Session?) {}
            override fun onSessionResuming(session: Session?, sessionId: String?) {}
            override fun onSessionEnding(session: Session?) {}
            override fun onSessionStartFailed(session: Session?, error: Int) {}
            override fun onSessionSuspended(session: Session?, reason: Int) {}
        }, Session::class.java)
    */
    }

    // 🧹 Limpieza
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.stop()
            mediaPlayer.release()
            libVLC.release()
        }
    }

    // 🎧 Cargar pistas de audio
    LaunchedEffect(mediaPlayer) {
        kotlinx.coroutines.delay(1000)
        try {
            mediaPlayer.audioTracks?.forEach { audioTracks.add(it) }
            selectedTrackId.value = mediaPlayer.audioTrack
        } catch (e: Exception) {
            println("⚠️ Error al cargar pistas de audio: ${e.message}")
        }
    }



    // Control de pausa/reanudar según visibilidad
    /*val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    mediaPlayer.stop() // detén completamente
                }
                Lifecycle.Event.ON_RESUME -> {
                    // recarga el stream al volver a primer plano
                    val media = Media(libVLC, Uri.parse(url)).apply {
                        setHWDecoderEnabled(true, false)
                        addOption(":network-caching=150")
                    }
                    mediaPlayer.media = media
                    mediaPlayer.play()
                }
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
*/
    LaunchedEffect(Unit) {
        val contextAsActivity = context as? ComponentActivity
        contextAsActivity?.lifecycle?.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        mediaPlayer.stop()
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        val media = Media(libVLC, Uri.parse(url)).apply {
                            setHWDecoderEnabled(true, false)
                            addOption(":network-caching=150")
                        }
                        mediaPlayer.media = media
                        mediaPlayer.play()
                    }
                    else -> Unit
                }
            }
        })
    }

    // 🎬 UI
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                VLCVideoLayout(it).also { layout ->
                    mediaPlayer.attachViews(layout, null, false, false)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ⬅️ Botón de regreso
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color(0x66000000), shape = RoundedCornerShape(50))
                .clickable { navController.popBackStack() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("⬅️", color = Color.White, style = MaterialTheme.typography.titleLarge)
        }

        // 📡 Botón CAST
        /*
        val isCastCompatible = remember(url) {
            // Muy básico: solo permitimos casting a URLs que terminen en .m3u8
            // y que no contengan palabras clave de incompatibilidad (puedes ajustar)
            // url.endsWith(".m3u8") && !url.contains("ac3", ignoreCase = true)
            url.endsWith(".m3u8") && !url.contains("ac3", ignoreCase = true) && hasCodecsInfo.value
        }*/
        val isCastCompatible = remember(url, hasCodecLine.value) {
            derivedStateOf {
                url.endsWith(".m3u8") &&
                        !url.contains("ac3", ignoreCase = true) &&
                        hasCodecLine.value
            }
        }


        if (castContext != null) {
            val castCompatibleNow = isCastCompatible
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                AndroidView(
                    factory = {
                        MediaRouteButton(it).apply {
                            CastButtonFactory.setUpMediaRouteButton(it, this)
                            if (!castCompatibleNow.value) {
                                this.alpha = 0.3f
                                this.isEnabled = false
                            }
                        }
                    },
                    modifier = Modifier.size(32.dp)
                )
            }
        }


        /*
        if (castContext != null && isCastCompatible) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                AndroidView(
                    factory = {
                        MediaRouteButton(it).apply {
                            CastButtonFactory.setUpMediaRouteButton(it, this)
                        }
                    },
                    modifier = Modifier.size(32.dp)
                )
            }
        }*/

        /* Muestra el boton de CAST, pero lo reemplazo con el de arriba para hver si funka
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            AndroidView(
                factory = {
                    MediaRouteButton(it).apply {
                        CastButtonFactory.setUpMediaRouteButton(it, this)
                    }
                },
                modifier = Modifier.size(32.dp)
            )
        }*/
    }
}

/*
@Composable
fun PlayerScreen(
    streamId: Int,
    username: String,
    password: String,
    baseUrl: String,
    navController: NavHostController
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val activity = context as? android.app.Activity
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    val url = "$baseUrl/live/$username/$password/$streamId.m3u8"

    val libVLC = remember {
        LibVLC(context, mutableListOf("--no-drop-late-frames", "--no-skip-frames"))
    }

    val mediaPlayer = remember(libVLC, url) {
        MediaPlayer(libVLC).apply {
            val media = Media(libVLC, Uri.parse(url)).apply {
                setHWDecoderEnabled(true, false)
                addOption(":network-caching=150")
            }
            this.media = media
            setVolume(100)
            play()
        }
    }

    val isMuted = remember { mutableStateOf(false) }
    val audioTracks = remember { mutableStateListOf<MediaPlayer.TrackDescription>() }
    val selectedTrackId = remember { mutableStateOf<Int?>(null) }

    // 🧹 Limpieza al salir
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.stop()
            mediaPlayer.release()
            libVLC.release()
        }
    }

    // Obtener pistas de audio al cargar
    LaunchedEffect(mediaPlayer) {
        kotlinx.coroutines.delay(1000) // Espera que cargue
        try {
            mediaPlayer.audioTracks?.forEach { track ->
                audioTracks.add(track)
            }
            selectedTrackId.value = mediaPlayer.audioTrack
        } catch (e: Exception) {
            println("⚠️ Error al cargar pistas de audio: ${e.message}")
        }
    }

    // Vista principal
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                VLCVideoLayout(it).also { layout ->
                    mediaPlayer.attachViews(layout, null, false, false)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Boton para regresar
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color(0x66000000), shape = RoundedCornerShape(50))
                .clickable { navController.popBackStack() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = " ⬅️",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )
        }

        //Boton de CAST
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            AndroidView(
                factory = { context ->
                    MediaRouteButton(context).apply {
                        CastButtonFactory.setUpMediaRouteButton(context.applicationContext, this)
                    }
                },
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
*/