package ec.net.csed.tv

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit

// --- CONFIGURACIÓN DE DISEÑO ---
val azulEmpresarial = Color(0xFF24669a)
val fondoOscuroInicio = Color(0xFF1A1A24) // azul  oscuro
val fondoOscuroFin = Color(0xFF0A0A10)     // negro   puro
val fondoOscuroEstilizado = Brush.linearGradient(
    colors = listOf(fondoOscuroInicio, fondoOscuroFin),
    start = androidx.compose.ui.geometry.Offset.Zero,
    end = androidx.compose.ui.geometry.Offset.Infinite
)
val colorCristal = Color.White.copy(alpha = 0.08f)
val colorBordeCristal = Color.White.copy(alpha = 0.2f)
val colorBordeEnfocado = azulEmpresarial

@Composable
fun TopHeader(
    user: String,
    onLogoutClicked: () -> Unit // <-- 1. Añadimos un parámetro para la acción de logout
) {
    var currentTime by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) } // <-- 2. Estado para controlar el menú

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .size(120.dp) // Tamaño del círculo contenedor (ajusta según necesites)
            .offset(x = 50.dp)
            .graphicsLayer(alpha = 0.99f) // Esto ayuda a que el shadow funcione bien
            // .shadow(elevation = 16.dp, shape = CircleShape, ambientColor = Color.White.copy(alpha = 0.2f), spotColor = Color.White.copy(alpha = 0.2f)) // Sombra sutil
            .background(Color.White, CircleShape) // Fondo blanco semitransparente con forma de círculo
            .border(
                width = 2.dp, // Grosor del borde
                brush = Brush.radialGradient( // Degradado para el borde
                    colors = listOf(Color.White.copy(alpha = 0.8f), Color.Transparent),
                    center = Offset(0.5f, 0.5f) // Centro del gradiente radial
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    )
    {
        Image(
            painter = painterResource(id = R.raw.logo_csedtv),
            contentDescription = "Logo",
            modifier = Modifier
                .size(120.dp) // Tamaño del logo dentro del círculo (más pequeño que el contenedor)
                .clip(CircleShape), // Recorta la imagen para que también sea circular si es necesario
            contentScale = ContentScale.Fit // Ajusta el logo para que quepa bien
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Spacer(modifier = Modifier.weight(1f))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(text = "24°C", color = Color.White.copy(alpha = 0.7f), fontSize = 18.sp)
            Text(text = currentTime, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

            // --- SECCIÓN DE USUARIO ---
            Box {
                val interactionSource = remember { MutableInteractionSource() }
                val isFocused by interactionSource.collectIsFocusedAsState()

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorCristal)
                        .border( // Borde que cambia con el foco para TV
                            width = if (isFocused) 2.dp else 1.dp,
                            color = if (isFocused) colorBordeEnfocado else colorBordeCristal,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .focusable(interactionSource = interactionSource) // <-- 4. Hacemos que sea focusable
                        .clickable { expanded = true } // <-- 5. Al hacer clic, se expande
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = user, color = Color.White, fontWeight = FontWeight.Bold)
                }

                // --- MENÚ DESPLEGABLE ---
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }, // Se cierra si tocas fuera
                    modifier = Modifier.background(Color(0xFF1F1F2B))
                ) {
                    // 1. Creamos las herramientas para detectar el foco
                    val interactionSource = remember { MutableInteractionSource() }
                    val isFocused by interactionSource.collectIsFocusedAsState()

                    DropdownMenuItem(
                        text = { Text("Cerrar sesión", color = Color.White) },
                        onClick = {
                            expanded = false
                            onLogoutClicked()
                        },
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .border(
                                width = if (isFocused) 2.dp else 0.dp,
                                color = if (isFocused) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun TopHeader_ant(user: String) {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            delay(1000)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(120.dp) // Tamaño del círculo contenedor (ajusta según necesites)
                .offset(x = 50.dp)
                .graphicsLayer(alpha = 0.99f) // Esto ayuda a que el shadow funcione bien
                // .shadow(elevation = 16.dp, shape = CircleShape, ambientColor = Color.White.copy(alpha = 0.2f), spotColor = Color.White.copy(alpha = 0.2f)) // Sombra sutil
                .background(Color.White, CircleShape) // Fondo blanco semitransparente con forma de círculo
                .border(
                    width = 2.dp, // Grosor del borde
                    brush = Brush.radialGradient( // Degradado para el borde
                        colors = listOf(Color.White.copy(alpha = 0.8f), Color.Transparent),
                        center = Offset(0.5f, 0.5f) // Centro del gradiente radial
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        )
        {
            Image(
                painter = painterResource(id = R.raw.logo_csedtv),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(120.dp) // Tamaño del logo dentro del círculo (más pequeño que el contenedor)
                    .clip(CircleShape), // Recorta la imagen para que también sea circular si es necesario
                contentScale = ContentScale.Fit // Ajusta el logo para que quepa bien
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Text(text = "24°C", color = Color.White.copy(alpha = 0.7f), fontSize = 18.sp)
            Text(text = currentTime, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorCristal)
                    .border(1.dp, colorBordeCristal, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = user, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ChannelCard original, adaptado para el nuevo diseño oscuro y premium.
@Composable
fun StyledChannelCard(
    canal: LiveChannel,
    navController: NavHostController,
    focusRequester: FocusRequester,
    onAttached: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .aspectRatio(16f / 10f)
            .clip(RoundedCornerShape(12.dp))
            .focusRequester(focusRequester)
            .onGloballyPositioned { onAttached() }
            .focusable(interactionSource = interactionSource)
            .clickable { navController.navigate("player/${canal.stream_id}") }
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) colorBordeEnfocado else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        AsyncImage(
            model = canal.stream_icon,
            contentDescription = canal.name,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray),
            contentScale = ContentScale.Crop,
            error = painterResource(id = R.drawable.ic_launcher_background)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                        startY = 200f
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = "${canal.stream_id} | ${canal.name}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


// --- PANTALLA PRINCIPAL ---

@Composable
fun ChannelScreen(
    navController: NavHostController,
    userViewModel: UserViewModel,
    channelViewModel: ChannelViewModel
) {
    val categoriasOriginales by channelViewModel.categorias.collectAsState()
    val canales by channelViewModel.canales.collectAsState()
    val categoriaSeleccionada by channelViewModel.categoriaSeleccionada.collectAsState()
    val canalSeleccionado by channelViewModel.canalSeleccionado.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    var inputBuffer by remember { mutableStateOf("") }
    var inputJob by remember { mutableStateOf<Job?>(null) }


    val infiniteTransition = rememberInfiniteTransition(label = "barrido_infinito")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, delayMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "offset_barrido"
    )


    val firstCategoryFocusRequester = remember { FocusRequester() }
    val firstChannelFocusRequester = remember { FocusRequester() }

    val context = LocalContext.current
    val performLogout = {
        coroutineScope.launch {
            // Borra las credenciales de DataStore
            context.dataStore.edit { preferences ->
                preferences.remove(USERNAME_KEY)
                preferences.remove(PASSWORD_KEY)
            }
        }
        // Limpia la sesión global
        UserSession.username = ""
        UserSession.password = ""

        // Navega de vuelta al Login, limpiando el historial de navegación
        navController.navigate(AppScreens.Login.name) {
            popUpTo(0) { inclusive = true }
        }
    }

    // Añadimos "Todos los Canales" a la lista de categorías
    val categoriasConTodos = remember(categoriasOriginales) {
        listOf(LiveCategory("all", "TODOS LOS CANALES")) + categoriasOriginales
    }

    val canalesFiltrados = remember(canales, categoriaSeleccionada) {
        if (categoriaSeleccionada == "all" || categoriaSeleccionada == null) {
            canales
        } else {
            canales.filter { it.category_id == categoriaSeleccionada }
        }
    }

    LaunchedEffect(Unit) {
        if (categoriasOriginales.isEmpty() || canales.isEmpty()) {
            channelViewModel.cargarDatos(
                "http://45.183.142.42:8850",
                UserSession.username,
                UserSession.password
            )
        }
        if (categoriaSeleccionada == null) {
            channelViewModel.seleccionarCategoria("all")
        }
    }

    LaunchedEffect(categoriasOriginales, canales) {
        if (categoriasOriginales.isNotEmpty() && canales.isEmpty()) {
            delay(3000)
            if (canales.isEmpty()) {
                channelViewModel.cargarDatos(
                    UserSession.baseUrl ?: "",
                    UserSession.username ?: "",
                    UserSession.password ?: ""
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(fondoOscuroEstilizado)
            .drawWithContent {
                drawContent()

                // Ahora, usa el valor 'shimmerOffset' que calculamos arriba
                val brushWidth = 500f
                val shimmerBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.0f),
                        Color.White.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.0f),
                    ),
                    start = Offset(size.width * shimmerOffset - brushWidth, 0f),
                    end = Offset(size.width * shimmerOffset, size.height)
                )

                // Finalmente, dibuja el efecto de barrido sobre el contenido.
                drawRect(shimmerBrush)
            }
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    val keyChar = keyEvent.nativeKeyEvent?.displayLabel
                    if (keyChar in '0'..'9') {
                        inputBuffer += keyChar
                        inputJob?.cancel()
                        inputJob = coroutineScope.launch {
                            delay(1500)
                            val channelNum = inputBuffer.toIntOrNull()
                            inputBuffer = ""
                            if (channelNum != null) {
                                val canalEncontrado = canales.firstOrNull { it.stream_id == channelNum }
                                if (canalEncontrado != null) {
                                    navController.navigate("player/${canalEncontrado.stream_id}")
                                }
                            }
                        }
                    }
                }
                false
            }
    ) {
        if (isPortrait) {
            // --- DISEÑO PARA CELULAR VERTICAL ---
            Column(Modifier.fillMaxSize()) {
                //TopHeader(user = UserSession.username ?: "Usuario")
                TopHeader(
                    user = UserSession.username ?: "Usuario",
                    onLogoutClicked = performLogout // <-- Le pasamos la acción
                )
                var expanded by remember { mutableStateOf(false) }
                val categoriaActualNombre = categoriasConTodos.find { it.category_id == categoriaSeleccionada }?.category_name ?: "Seleccionar"

                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colorCristal)
                            .border(1.dp, colorBordeCristal, RoundedCornerShape(12.dp))
                            .clickable { expanded = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(categoriaActualNombre, color = Color.White, modifier = Modifier.weight(1f))
                        Text("▼", color = Color.White)
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF1F1F2B))
                    ) {
                        categoriasConTodos.forEach { categoria ->
                            DropdownMenuItem(
                                text = { Text(categoria.category_name, color = Color.White) },
                                onClick = {
                                    channelViewModel.seleccionarCategoria(categoria.category_id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(canalesFiltrados) { canal ->
                        StyledChannelCard(
                            canal = canal,
                            navController = navController,
                            focusRequester = remember { FocusRequester() },
                            onAttached = { }
                        )
                    }
                }
            }
        } else {
            // --- DISEÑO PARA TV / CELULAR HORIZONTAL ---
            Column(Modifier.fillMaxSize()) {
                //TopHeader(user = UserSession.username ?: "Usuario")
                TopHeader(
                    user = UserSession.username ?: "Usuario",
                    onLogoutClicked = performLogout // <-- Le pasamos la acción
                )
                Row(Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(250.dp)
                            .padding(start = 24.dp, top = 24.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categoriasConTodos) { categoria ->
                            val interactionSource = remember { MutableInteractionSource() }
                            val isFocused by interactionSource.collectIsFocusedAsState()
                            val isSelected = categoria.category_id == categoriaSeleccionada

                            Text(
                                text = categoria.category_name,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                fontSize = 18.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(if (categoria.category_id == categoriasConTodos.first().category_id) firstCategoryFocusRequester else remember { FocusRequester() })
                                    .focusable(interactionSource = interactionSource)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { channelViewModel.seleccionarCategoria(categoria.category_id) }
                                    .background(if (isSelected) colorCristal else Color.Transparent)
                                    .border(
                                        width = if (isFocused) 2.dp else 0.dp,
                                        color = if (isFocused) colorBordeEnfocado else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(vertical = 12.dp, horizontal = 16.dp)
                            )
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(canalesFiltrados) { canal ->
                            var attached by remember { mutableStateOf(false) }
                            StyledChannelCard(
                                canal = canal,
                                navController = navController,
                                focusRequester = if (canal.stream_id == canalSeleccionado) firstChannelFocusRequester else remember { FocusRequester() },
                                onAttached = {
                                    if (!attached) {
                                        // Tu lógica de onAttached, si la necesitas para el re-enfoque
                                        attached = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (inputBuffer.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0xCC111111), shape = RoundedCornerShape(16.dp))
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                Text(inputBuffer, color = Color.White, style = MaterialTheme.typography.headlineLarge, letterSpacing = 4.sp)
            }
        }
    }
}

// NOTA: Tu ChannelCard original ya no se usa, fue reemplazado por StyledChannelCard.
// Puedes eliminar la función ChannelCard que tenías al principio del archivo.