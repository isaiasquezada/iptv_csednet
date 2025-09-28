    package ec.net.csed.tv

    import android.os.Bundle
    import androidx.activity.compose.setContent
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.lazy.LazyColumn
    import androidx.compose.foundation.lazy.items
    import androidx.compose.foundation.lazy.grid.GridCells
    import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.unit.dp
    import androidx.navigation.NavHostController
    import androidx.navigation.NavType
    import androidx.navigation.navArgument
    import androidx.navigation.compose.*
    import coil.compose.AsyncImage
    import kotlinx.coroutines.delay
    import ec.net.csed.tv.ui.theme.IptvTheme
    import androidx.compose.runtime.rememberCoroutineScope
    import kotlinx.coroutines.launch
    import androidx.compose.ui.res.painterResource
    import android.net.Uri
    import android.widget.VideoView
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.viewinterop.AndroidView
    import androidx.compose.foundation.background
    import androidx.compose.foundation.Image
    import androidx.compose.foundation.border
    import androidx.compose.foundation.focusable
    import androidx.compose.foundation.interaction.MutableInteractionSource
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.foundation.lazy.grid.itemsIndexed
    import androidx.compose.ui.input.key.onKeyEvent
    import androidx.compose.ui.input.key.KeyEventType
    import androidx.compose.runtime.saveable.rememberSaveable
    import androidx.compose.ui.input.key.type
    import kotlinx.coroutines.Job
    import androidx.compose.ui.platform.LocalConfiguration
    import android.content.res.Configuration
    import androidx.compose.foundation.interaction.collectIsFocusedAsState
    // import androidx.tv.foundation.lazy.list.collectIsFocusedAsState
    import androidx.compose.ui.focus.FocusRequester
    import androidx.compose.ui.focus.focusRequester
    import androidx.compose.ui.focus.onFocusChanged
    import androidx.datastore.preferences.core.edit
    import kotlinx.coroutines.flow.firstOrNull
    import androidx.compose.material3.OutlinedTextFieldDefaults
    import androidx.compose.ui.layout.onGloballyPositioned
    import androidx.fragment.app.FragmentActivity
    import androidx.compose.ui.graphics.Brush
    import androidx.compose.ui.geometry.Offset
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.foundation.layout.fillMaxWidth
    import androidx.compose.foundation.text.KeyboardOptions
    import androidx.compose.ui.focus.FocusDirection
    import androidx.compose.ui.platform.LocalFocusManager
    import androidx.compose.ui.text.input.ImeAction
    import androidx.compose.foundation.text.KeyboardActions
    import androidx.compose.runtime.*
    import androidx.compose.ui.draw.alpha
    import android.media.MediaPlayer
    import androidx.core.view.WindowCompat // <-- AÑADE ESTE IMPORT
    // Para el componente Dialog
    import androidx.compose.ui.window.Dialog
    import androidx.compose.foundation.rememberScrollState
    import androidx.compose.foundation.verticalScroll

// Para los íconos de Material Design
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.Warning

    //class MainActivity : ComponentActivity() {
    class MainActivity : FragmentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val userViewModel = UserViewModel() // ← instancia
            val channelViewModel = ChannelViewModel()

            WindowCompat.setDecorFitsSystemWindows(window, false)

            setContent {
                IptvTheme {
                    val navController = rememberNavController()
                    NavigationManager(navController, userViewModel, channelViewModel)
                }
            }
        }
    }


    // Enum para las pantallas
    enum class AppScreens {
        Splash,
        Login,
        Channels
    }

    @Composable
    fun NavigationManager(
        navController: NavHostController,
        userViewModel: UserViewModel,
        channelViewModel: ChannelViewModel
    ) {
        NavHost(navController = navController, startDestination = AppScreens.Splash.name) {
            composable(AppScreens.Splash.name) {
                SplashScreen(navController)
            }
            composable(AppScreens.Login.name) {
                LoginScreen(navController, userViewModel, channelViewModel)
            }
            composable(AppScreens.Channels.name) {
                ChannelScreen(navController, userViewModel, channelViewModel)
            }
            composable(
                route = "player/{streamId}",
                arguments = listOf(navArgument("streamId") { type = NavType.IntType })
            ) { backStackEntry ->
                val streamId = backStackEntry.arguments?.getInt("streamId") ?: 0
                PlayerScreen(
                    streamId = streamId,
                    username = UserSession.username, //userViewModel.username.collectAsState().value,
                    password = UserSession.password, //userViewModel.password.collectAsState().value,
                    baseUrl = "http://45.183.142.42:8850",
                    navController = navController
                )
            }
        }
    }

    @Composable
    fun SplashScreen(navController: NavHostController) {
        val context = LocalContext.current
        // 1. Creamos el estado para controlar la visibilidad del video.
        var isVideoVisible by remember { mutableStateOf(false) }

        // Tu lógica de navegación y audio se mantiene igual.
        LaunchedEffect(Unit) {
            val activarAudioSplash = false // ← cámbialo a true si quieres que suene
            val mediaPlayer = if (activarAudioSplash) {
                android.media.MediaPlayer.create(context, R.raw.splash_audio).apply {
                    setVolume(1f, 1f)
                    start()
                }
            } else null

            delay(4000)
            mediaPlayer?.release()
            // Navega a la pantalla de Login después del delay
            navController.navigate(AppScreens.Login.name) {
                // Limpia el stack para que el usuario no pueda volver al splash
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }

        // El Box con fondo blanco sigue siendo la base
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = {
                    VideoView(it).apply {
                        // Ponemos un fondo transparente al VideoView por si acaso
                        this.setBackgroundColor(android.graphics.Color.TRANSPARENT)

                        setVideoURI(Uri.parse("android.resource://${context.packageName}/raw/intro_csedtv_white"))

                        // ✨ LA MAGIA ESTÁ AQUÍ ✨
                        // Este listener nos avisa cuando el video REALMENTE empieza a mostrarse
                        setOnInfoListener { _, what, _ ->
                            if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                                // En cuanto el primer fotograma se dibuja, hacemos visible el VideoView
                                isVideoVisible = true
                                return@setOnInfoListener true
                            }
                            false
                        }

                        // OnPrepared ahora solo se encarga de iniciar la reproducción
                        setOnPreparedListener { mp ->
                            mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                            mp.isLooping = true
                            mp.setVolume(1f, 1f) // Activamos el audio del video
                            start()
                            // YA NO cambiamos la visibilidad aquí
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (isVideoVisible) 1f else 0f)
            )
        }
    }

    @Composable
    fun ErrorDialog(onDismiss: () -> Unit) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.White)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error Icon",
                        tint = Color.Red,
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Error de Autenticación",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "El usuario o la contraseña son incorrectos. Por favor, inténtalo de nuevo.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onDismiss, // Al hacer clic, se cierra el diálogo
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24669a))
                    ) {
                        Text("CERRAR", color = Color.White)
                    }
                }
            }
        }
    }

    @Composable
    private fun LoginForm(
        user: String,
        onUserChange: (String) -> Unit,
        pass: String,
        onPassChange: (String) -> Unit,
        onLoginClick: () -> Unit,
        gradienteBoton: Brush
    ) {
        val focusManager = LocalFocusManager.current
        val azulEmpresarial = Color(0xFF24669a)
        val azulEnFoco = Color(0xFF3C87C8)

        // CAMPO DE USUARIO
        OutlinedTextField(
            value = user,
            onValueChange = onUserChange,
            label = { Text("Usuario", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = azulEnFoco,
                unfocusedBorderColor = azulEmpresarial,
                cursorColor = azulEmpresarial
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // CAMPO DE CONTRASEÑA
        OutlinedTextField(
            value = pass,
            onValueChange = onPassChange,
            label = { Text("Contraseña", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            // Puedes agregar keyboardActions aquí también para ejecutar el login al presionar "Done"
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onLoginClick() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = azulEnFoco,
                unfocusedBorderColor = azulEmpresarial,
                cursorColor = azulEmpresarial
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // BOTÓN DE INICIO DE SESIÓN
        Button(
            onClick = onLoginClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = gradienteBoton, shape = RoundedCornerShape(50))
                .clip(RoundedCornerShape(50))
        ) {
            Text("Iniciar Sesión", fontWeight = FontWeight.Bold)
        }
    }


    @Composable
    fun LoginScreen(
        navController: NavHostController,
        userViewModel: UserViewModel,
        channelViewModel: ChannelViewModel
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        val azulEmpresarial = Color(0xFF24669a)
        val rojoEmpresarial = Color(0xFFda3a2c)
        val azulEnFoco = Color(0xFF3C87C8)

        // Creamos un gradiente diagonal
        val gradienteBoton = Brush.linearGradient(
            colors = listOf(azulEmpresarial, rojoEmpresarial),
            start = Offset(0f, 0f), // Esquina superior izquierda
            end = Offset.Infinite   // Esquina inferior derecha
        )

        var user by remember { mutableStateOf("") }
        var pass by remember { mutableStateOf("") }
        var error by remember { mutableStateOf(false) }
        var isLoggedIn by remember { mutableStateOf(false) }

        // Leer desde DataStore
        LaunchedEffect(Unit) {
            context.dataStore.data.firstOrNull()?.let { prefs ->
                val storedUser = prefs[USERNAME_KEY]
                val storedPass = prefs[PASSWORD_KEY]
                if (!storedUser.isNullOrEmpty() && !storedPass.isNullOrEmpty()) {
                    user = storedUser
                    pass = storedPass
                    isLoggedIn = true

                    // ✅ Guardar en sesión global
                    UserSession.username = user
                    UserSession.password = pass
                }
            }
        }

        // Obtenemos la configuración actual de la pantalla
        val configuration = LocalConfiguration.current
        // Decidimos si la altura es "compacta" (típico de un celular horizontal)
        val isCompactHeight = configuration.screenHeightDp.dp < 480.dp

        val performLogin : () -> Unit = {
            scope.launch {
                val success = loginIPTV("http://45.183.142.42:8850", user, pass)
                if (success) {
                    // Guardar en DataStore
                    context.dataStore.edit { prefs ->
                        prefs[USERNAME_KEY] = user
                        prefs[PASSWORD_KEY] = pass
                    }

                    // Guardar en el ViewModel
                    userViewModel.setCredentials(user, pass)

                    // ✅ Guardar en sesión global
                    UserSession.username = user
                    UserSession.password = pass

                    channelViewModel.cargarDatos(
                        "http://45.183.142.42:8850",
                        user,
                        pass
                    )
                    delay(1500) // dejar un tiempo para precarga
                    navController.navigate(AppScreens.Channels.name)
                } else {
                    error = true
                }
            }
        }


        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(rememberScrollState()) // Mantenemos el scroll por si acaso
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val focusManager = LocalFocusManager.current
            /*
            Image(
                painter = painterResource(id = R.raw.logo_csedtv),
                contentDescription = "Logo IPTV",
                modifier = Modifier
                    .height(240.dp)
                    .padding(bottom = 24.dp)
            )

             */

            if (isLoggedIn) {
                Text("Usuario: $user (conectado)", color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    isLoggedIn = false
                    user = ""
                    pass = ""
                },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,       // Fondo del botón
                        contentColor = Color.Black         // Texto dentro del botón
                    )
                ) {
                    Text("Cambiar usuario")
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    navController.navigate(AppScreens.Channels.name)
                },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Cyan,       // Fondo del botón
                        contentColor = Color.Black         // Texto dentro del botón
                    )
                ) {
                    Text("Ingresar")
                }
            } else {

                if (isCompactHeight) {
                    // --- DISEÑO PARA CELULAR HORIZONTAL (POCA ALTURA) ---
                    Row(
                        modifier = Modifier.widthIn(max = 700.dp), // Un max-width más generoso para el Row
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.raw.logo_csedtv),
                            contentDescription = "Logo IPTV",
                            modifier = Modifier
                                .weight(1f) // El logo ocupa una parte
                                .height(180.dp) // Un poco más pequeño
                        )
                        Spacer(modifier = Modifier.width(32.dp))
                        Column(
                            modifier = Modifier.weight(1f) // El formulario ocupa la otra parte
                        ) {
                            LoginForm(user, { user = it }, pass, { pass = it }, onLoginClick = performLogin, gradienteBoton)
                        }
                    }
                } else {
                    // --- DISEÑO PARA TV Y CELULAR VERTICAL (MUCHA ALTURA) ---
                    Column(
                        modifier = Modifier.widthIn(max = 480.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.raw.logo_csedtv),
                            contentDescription = "Logo IPTV",
                            modifier = Modifier
                                .height(240.dp)
                                .padding(bottom = 24.dp)
                        )
                        LoginForm(user, { user = it }, pass, { pass = it }, onLoginClick = performLogin, gradienteBoton)
                    }
                }

                if (error) {
                    ErrorDialog(onDismiss = { error = false })
                }


                /*
                Column(
                    modifier = Modifier.widthIn(max = 480.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // CAMPO DE USUARIO
                    OutlinedTextField(
                        value = user,
                        onValueChange = { user = it },
                        label = {
                            Text(
                                text = "Usuario",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                        // 2. Configura las opciones del teclado.
                        keyboardOptions = KeyboardOptions(
                            // Esto cambia el botón 'Enter' por un botón 'Siguiente' (una flecha →).
                            imeAction = ImeAction.Next
                        ),
                        // 3. Define la acción a ejecutar.
                        keyboardActions = KeyboardActions(
                            onNext = {
                                // Mueve el foco al siguiente elemento focusable.
                                focusManager.moveFocus(FocusDirection.Next)
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = azulEnFoco,
                            unfocusedBorderColor = azulEmpresarial,
                            cursorColor = azulEmpresarial
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )


                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = {
                            Text(
                                text = "Contraseña",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = azulEnFoco,
                            unfocusedBorderColor = azulEmpresarial,
                            cursorColor = azulEmpresarial
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )


                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                val success = loginIPTV("http://45.183.142.42:8850", user, pass)
                                if (success) {
                                    // Guardar en DataStore
                                    context.dataStore.edit { prefs ->
                                        prefs[USERNAME_KEY] = user
                                        prefs[PASSWORD_KEY] = pass
                                    }

                                    // Guardar en el ViewModel
                                    userViewModel.setCredentials(user, pass)

                                    // ✅ Guardar en sesión global
                                    UserSession.username = user
                                    UserSession.password = pass

                                    channelViewModel.cargarDatos(
                                        "http://45.183.142.42:8850",
                                        user,
                                        pass
                                    )
                                    delay(1500) // dejar un tiempo para precarga
                                    navController.navigate(AppScreens.Channels.name)
                                } else {
                                    error = true
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent, // Hacemos el contenedor transparente
                            contentColor = Color.White          // Color del texto
                        ),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                        modifier = Modifier
                            .background(
                                brush = gradienteBoton,
                                shape = RoundedCornerShape(50)
                            ) // Aplicamos el gradiente y la forma
                            .clip(RoundedCornerShape(50))
                    ) {
                        Text("Iniciar Sesión", fontWeight = FontWeight.Bold)
                    }

                    if (error) {
                        ErrorDialog(onDismiss = { error = false })
                    }
                }
                */
            }
        }
    }


    @Composable
    fun ChannelScreen_old(navController: NavHostController) {
        val coroutineScope = rememberCoroutineScope()
        var categorias by remember { mutableStateOf(listOf<LiveCategory>()) }
        var canales by remember { mutableStateOf(listOf<LiveChannel>()) }
        var categoriaSeleccionada by rememberSaveable { mutableStateOf<String?>(null) }
        var inputBuffer by remember { mutableStateOf("") }
        var inputJob by remember { mutableStateOf<Job?>(null) }

        val configuration = LocalConfiguration.current
        val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

        val url = "http://45.183.142.42:8850"
        val usuario = "fzambonino"
        val password = "csednet.2024"

        LaunchedEffect(Unit) {
            categorias = getLiveCategories(url, usuario, password)
            canales = getLiveChannels(url, usuario, password)
            categoriaSeleccionada = categorias.firstOrNull()?.category_id
        }

        val canalesFiltrados = canales.filter { it.category_id == categoriaSeleccionada }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        val keyChar = keyEvent.nativeKeyEvent?.displayLabel
                        if (keyChar in '0'..'9') {
                            inputBuffer += keyChar

                            inputJob?.cancel()
                            inputJob = coroutineScope.launch {
                                delay(3000)
                                val id = inputBuffer.toIntOrNull()
                                inputBuffer = ""
                                if (id != null) {
                                    val canalEncontrado = canales.firstOrNull { it.stream_id == id }
                                    if (canalEncontrado != null) {
                                        navController.navigate("player/${canalEncontrado.stream_id}")
                                    }
                                }
                            }
                        }
                    }
                    true
                }
        ) {
            Row(Modifier.fillMaxSize()) {

                // Panel izquierdo (logo + categorías)
                Column(
                    modifier = Modifier
                        .width(250.dp)
                        .fillMaxHeight()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo
                    Image(
                        painter = painterResource(id = R.raw.logo_iptv),
                        contentDescription = "Logo IPTV",
                        modifier = Modifier
                            .height(80.dp)
                            .padding(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Categorías como botones
                    LazyColumn {
                        items(categorias) { categoria ->
                            val seleccionada = categoria.category_id == categoriaSeleccionada
                            Button(
                                onClick = { categoriaSeleccionada = categoria.category_id },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (seleccionada) MaterialTheme.colorScheme.primary else Color.DarkGray
                                )
                            ) {
                                Text(
                                    text = categoria.category_name,
                                    textAlign = TextAlign.Center,
                                    color = Color.White,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Panel derecho: canales organizados en cuadros celestes
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(canalesFiltrados) { index: Int, canal: LiveChannel ->
                        val interactionSource = remember { MutableInteractionSource() }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFBBDEFB))
                                .border(2.dp, Color.LightGray, RoundedCornerShape(12.dp))
                                .focusable(interactionSource = interactionSource)
                                .clickable {
                                    navController.navigate("player/${canal.stream_id}")
                                }
                                .padding(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "#${canal.stream_id}",
                                        color = Color.DarkGray,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }

                                AsyncImage(
                                    model = canal.stream_icon,
                                    contentDescription = canal.name,
                                    modifier = Modifier
                                        .height(100.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = canal.name,
                                    textAlign = TextAlign.Center,
                                    color = Color.Black,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
            // 🔢 Mostrar número digitado
            if (inputBuffer.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color(0xAA333333), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = inputBuffer,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
            }
        }

    }


    @Composable
    fun ChannelCard(canal: LiveChannel, navController: NavHostController, focusRequester: FocusRequester? = null, onAttached: (() -> Unit)? = null) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused by interactionSource.collectIsFocusedAsState()

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isFocused) Color(0xFF42A5F5) else Color(0xFFBBDEFB))
                .border(2.dp, Color.LightGray, RoundedCornerShape(12.dp))
                // .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .then(
                    if (focusRequester != null) {
                        Modifier
                            .focusRequester(focusRequester)
                            .onGloballyPositioned { onAttached?.invoke() }
                    } else Modifier
                )
                .onFocusChanged { }
                .focusable(interactionSource = interactionSource)
                .clickable {
                    navController.navigate("player/${canal.stream_id}")
                }
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "#${canal.stream_id}",
                        color = Color.DarkGray,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                AsyncImage(
                    model = canal.stream_icon,
                    contentDescription = canal.name,
                    modifier = Modifier
                        .height(100.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = canal.name,
                    textAlign = TextAlign.Center,
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }