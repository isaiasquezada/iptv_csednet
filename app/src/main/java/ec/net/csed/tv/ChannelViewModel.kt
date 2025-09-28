// ChannelViewModel.kt
package ec.net.csed.tv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChannelViewModel : ViewModel() {
    private val _categorias = MutableStateFlow<List<LiveCategory>>(emptyList())
    private val _canales = MutableStateFlow<List<LiveChannel>>(emptyList())
    private val _categoriaSeleccionada = MutableStateFlow<String?>(null)
    private val _canalSeleccionado = MutableStateFlow<Int?>(null)

    val categorias: StateFlow<List<LiveCategory>> = _categorias
    val canales: StateFlow<List<LiveChannel>> = _canales
    val categoriaSeleccionada: StateFlow<String?> = _categoriaSeleccionada
    val canalSeleccionado: StateFlow<Int?> = _canalSeleccionado

    fun cargarDatos(baseUrl: String, username: String, password: String) {
        viewModelScope.launch {
            val cats = getLiveCategories(baseUrl, username, password)
            val chans = getLiveChannels(baseUrl, username, password)
            _categorias.value = cats
            _canales.value = chans

            // Preseleccionar primera categoría solo si no hay una elegida aún
            if (_categoriaSeleccionada.value == null && cats.isNotEmpty()) {
                _categoriaSeleccionada.value = cats.first().category_id
            }
        }
    }

    fun seleccionarCategoria(id: String) {
        _categoriaSeleccionada.value = id
    }

    fun seleccionarCanal(id: Int) {
        _canalSeleccionado.value = id
    }

    fun limpiar() {
        _categorias.value = emptyList()
        _canales.value = emptyList()
        _categoriaSeleccionada.value = null
        _canalSeleccionado.value = null
    }
}


/*
class ChannelViewModel : ViewModel() {
    private val _categorias = MutableStateFlow<List<LiveCategory>>(emptyList())
    private val _canales = MutableStateFlow<List<LiveChannel>>(emptyList())

    val categorias: StateFlow<List<LiveCategory>> = _categorias
    val canales: StateFlow<List<LiveChannel>> = _canales

    fun cargarDatos(baseUrl: String, username: String, password: String) {
        viewModelScope.launch {
            val cats = getLiveCategories(baseUrl, username, password)
            val chans = getLiveChannels(baseUrl, username, password)
            _categorias.value = cats
            _canales.value = chans
        }
    }

    fun limpiar() {
        _categorias.value = emptyList()
        _canales.value = emptyList()
    }
}*/
