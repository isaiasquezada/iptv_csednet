package ec.net.csed.tv

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState


@Composable
fun ChannelScreen(
    navController: NavHostController,
    userViewModel: UserViewModel,
    channelViewModel: ChannelViewModel
) {
    val categorias by channelViewModel.categorias.collectAsState()
    val canales by channelViewModel.canales.collectAsState()
    val categoriaSeleccionada by channelViewModel.categoriaSeleccionada.collectAsState()
    val canalSeleccionado by channelViewModel.canalSeleccionado.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    var inputBuffer by remember { mutableStateOf("") }
    var inputJob by remember { mutableStateOf<Job?>(null) }

    val firstCategoryFocusRequester = remember { FocusRequester() }
    val firstChannelFocusRequester = remember { FocusRequester() }
    var canRequestFocus by remember { mutableStateOf(false) }

    val focusRequesterList = remember { mutableStateListOf<FocusRequester>() }

    val url = "http://45.183.142.42:8850"
    val usuario = UserSession.username
    val password = UserSession.password

    // Solo cargar si está vacío (esto evita recarga innecesaria)
    /*LaunchedEffect(Unit) {
        if (categorias.isEmpty() || canales.isEmpty()) {
            channelViewModel.cargarDatos(url, usuario, password)
        }
        if (categoriaSeleccionada == null) {
            channelViewModel.seleccionarCategoria(categorias.firstOrNull()?.category_id ?: "")
        }
    }*/

    // Cargar datos si es necesario
    LaunchedEffect(Unit) {
        if (categorias.isEmpty() || canales.isEmpty()) {
            channelViewModel.cargarDatos(url, usuario, password)
        }
    }

// Establecer categoría una vez cargadas
    LaunchedEffect(categorias) {
        if (categoriaSeleccionada == null && categorias.isNotEmpty()) {
            channelViewModel.seleccionarCategoria(categorias.first().category_id)
        }
    }


    val canalesFiltrados = canales.filter { it.category_id == categoriaSeleccionada }

    // Reenfocar al canal anterior
    LaunchedEffect(canalesFiltrados) {
        if (!isPortrait && canalesFiltrados.isNotEmpty() && canRequestFocus) {
            delay(300)
            try {
                firstChannelFocusRequester.requestFocus()
            } catch (e: Exception) {
                println("⚠️ Error al pedir focus: ${e.message}")
            }
        }
    }

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
                                    channelViewModel.seleccionarCanal(id)
                                    navController.navigate("player/$id")
                                }
                            }
                        }
                    }
                }
                false
            }
    ) {
        if (isPortrait) {
            // Versión retrato
            Column(Modifier.fillMaxSize().padding(8.dp)) {
                var expanded by remember { mutableStateOf(false) }

                Box {
                    Button(onClick = { expanded = true }) {
                        Text(categorias.find { it.category_id == categoriaSeleccionada }?.category_name ?: "Seleccionar")
                    }

                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categorias.forEach { categoria ->
                            DropdownMenuItem(
                                text = { Text(categoria.category_name) },
                                onClick = {
                                    channelViewModel.seleccionarCategoria(categoria.category_id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(canalesFiltrados) { index, canal ->
                        val focusRequester = remember { FocusRequester() }
                        if (index >= focusRequesterList.size) focusRequesterList.add(focusRequester)

                        ChannelCard(
                            canal = canal,
                            navController = navController,
                            focusRequester = if (canal.stream_id == canalSeleccionado) firstChannelFocusRequester else focusRequester,
                            onAttached = { if (canal.stream_id == canalSeleccionado) canRequestFocus = true }
                        )
                    }
                }
            }
        } else {
            // Versión horizontal (TV)
            Row(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.width(250.dp).fillMaxHeight().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.raw.logo_iptv),
                        contentDescription = "Logo IPTV",
                        modifier = Modifier.height(80.dp).padding(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn {
                        itemsIndexed(categorias) { index, categoria ->
                            val seleccionada = categoria.category_id == categoriaSeleccionada
                            val interactionSource = remember { MutableInteractionSource() }
                            val isFocused by interactionSource.collectIsFocusedAsState()
                            val focusRequester = remember { FocusRequester() }

                            Button(
                                onClick = { channelViewModel.seleccionarCategoria(categoria.category_id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .focusRequester(if (index == 0) firstCategoryFocusRequester else focusRequester)
                                    .focusable(interactionSource = interactionSource),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        isFocused -> Color(0xFF42A5F5)
                                        seleccionada -> MaterialTheme.colorScheme.primary
                                        else -> Color.DarkGray
                                    }
                                )
                            ) {
                                Text(
                                    text = categoria.category_name,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(canalesFiltrados) { index, canal ->
                        val focusRequester = remember { FocusRequester() }
                        if (index >= focusRequesterList.size) focusRequesterList.add(focusRequester)

                        ChannelCard(
                            canal = canal,
                            navController = navController,
                            focusRequester = if (canal.stream_id == canalSeleccionado) firstChannelFocusRequester else null,
                            onAttached = { if (canal.stream_id == canalSeleccionado) canRequestFocus = true }
                        )
                    }
                }
            }
        }

        if (inputBuffer.isNotEmpty()) {
            Box(
                modifier = Modifier.align(Alignment.Center)
                    .background(Color(0xAA333333), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                Text(inputBuffer, color = Color.White, style = MaterialTheme.typography.headlineLarge)
            }
        }
    }
}

/*fun ChannelScreen(
    navController: NavHostController,
    userViewModel: UserViewModel,
    channelViewModel: ChannelViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    //var categorias by remember { mutableStateOf(listOf<LiveCategory>()) }
    //var canales by remember { mutableStateOf(listOf<LiveChannel>()) }
    val categorias by channelViewModel.categorias.collectAsState()
    val canales by channelViewModel.canales.collectAsState()

    // var categoriaSeleccionada by rememberSaveable { mutableStateOf<String?>(null) }
    val categoriaSeleccionada by channelViewModel.categoriaSeleccionada.collectAsState()

    var inputBuffer by remember { mutableStateOf("") }
    var inputJob by remember { mutableStateOf<Job?>(null) }

    val url = "http://45.183.142.42:8850"
    val usuario = UserSession.username
    val password = UserSession.password

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val firstCategoryFocusRequester = remember { FocusRequester() }
    // val firstChannelFocusRequester = remember { FocusRequester() }
    val firstChannelFocusRequester = remember { FocusRequester() }
    var canRequestFocus by remember { mutableStateOf(false) }

    val focusRequesterList = remember { mutableStateListOf<FocusRequester>() }

    LaunchedEffect(Unit) {
        // categorias = getLiveCategories(url, usuario, password)
        // canales = getLiveChannels(url, usuario, password)
        channelViewModel.cargarDatos(url, usuario, password)
        if (categoriaSeleccionada == null) {
            categoriaSeleccionada = categorias.firstOrNull()?.category_id
        }
    }

    val canalesFiltrados = canales.filter { it.category_id == categoriaSeleccionada }

    // Solicitar foco solo si no estamos en modo retrato y hay canales
    LaunchedEffect(canalesFiltrados) {
        if (!isPortrait && canalesFiltrados.isNotEmpty()) {
            delay(300)
            if (canRequestFocus) {
                try {
                    firstChannelFocusRequester.requestFocus()
                } catch (e: Exception) {
                    println("⚠️ Error al pedir focus: ${e.message}")
                }
            }        }
    }

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
                false
            }
    ) {
        if (isPortrait) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(8.dp)) {
                var expanded by remember { mutableStateOf(false) }

                Box {
                    Button(onClick = { expanded = true }) {
                        Text(categorias.find { it.category_id == categoriaSeleccionada }?.category_name ?: "Seleccionar")
                    }

                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categorias.forEach { categoria ->
                            DropdownMenuItem(
                                text = { Text(categoria.category_name) },
                                onClick = {
                                    categoriaSeleccionada = categoria.category_id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(canalesFiltrados.toList()) { index, canal ->
                        val focusRequester = remember { FocusRequester() }
                        if (index >= focusRequesterList.size) {
                            focusRequesterList.add(focusRequester)
                        }
                        ChannelCard(
                            canal = canal,
                            navController = navController,
                            focusRequester = if (index == 0) firstChannelFocusRequester else focusRequester
                        )
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                // CATEGORÍAS
                Column(
                    modifier = Modifier
                        .width(250.dp)
                        .fillMaxHeight()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.raw.logo_iptv),
                        contentDescription = "Logo IPTV",
                        modifier = Modifier
                            .height(80.dp)
                            .padding(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn {
                        itemsIndexed(categorias.toList()) { index, categoria ->
                            val seleccionada = categoria.category_id == categoriaSeleccionada
                            val interactionSource = remember { MutableInteractionSource() }
                            val isFocused by interactionSource.collectIsFocusedAsState()
                            val focusRequester = remember { FocusRequester() }

                            Button(
                                onClick = { categoriaSeleccionada = categoria.category_id },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .focusRequester(if (index == 0) firstCategoryFocusRequester else focusRequester)
                                    .onFocusChanged { }
                                    .focusable(interactionSource = interactionSource),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        isFocused -> Color(0xFF42A5F5)
                                        seleccionada -> MaterialTheme.colorScheme.primary
                                        else -> Color.DarkGray
                                    }
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

                // CANALES
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(canalesFiltrados.toList()) { index, canal ->
                        val focusRequester = remember { FocusRequester() }
                        if (index >= focusRequesterList.size) {
                            focusRequesterList.add(focusRequester)
                        }

                        ChannelCard(
                            canal = canal,
                            navController = navController,
                            focusRequester = if (index == 0) firstChannelFocusRequester else null,
                            onAttached = {
                                if (index == 0) canRequestFocus = true
                            }
                        )
                    }
                }
            }
        }

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
*/