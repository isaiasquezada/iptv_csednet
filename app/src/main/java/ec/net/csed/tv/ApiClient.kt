package ec.net.csed.tv

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import android.util.Log
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.decodeFromString

val httpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

private const val TAG = "LoginIPTV"

suspend fun loginIPTV2(url: String, username: String, password: String): Boolean {
    // Revision de los logs para ver cómo se comporta el nuevo server de CSED -- esto será temporal
    Log.d(TAG, "Iniciando login con URL base: '$url', Usuario: '$username'")
    val fullUrl = "$url/player_api.php?username=$username&password=$password"
    Log.d(TAG, "URL completa de la petición: $fullUrl")

    return try {
        val response: LoginResponse = httpClient.get("$url/player_api.php") {
            url {
                parameters.append("username", username)
                parameters.append("password", password)
            }
        }.body()

        response.user_info.auth == 1
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

suspend fun loginIPTV(url: String, username: String, password: String): Boolean {
    Log.d(TAG, "Iniciando login con URL base: '$url', Usuario: '$username'")

    val fullUrl = "$url/player_api.php?username=$username&password=$password"
    Log.d(TAG, "URL completa de la petición: $fullUrl")

    return try {
        val httpResponse: HttpResponse = httpClient.get("$url/player_api.php") {
            url {
                parameters.append("username", username)
                parameters.append("password", password)
            }
        }

        // Esta línea ahora funcionará gracias a la importación
        val responseBodyString = httpResponse.bodyAsText()

        Log.d(TAG, "Respuesta del servidor - Estado: ${httpResponse.status}")
        Log.d(TAG, "Respuesta del servidor - Cuerpo: $responseBodyString")

        // --- INICIO DE LA CORRECCIÓN ---
        // 1. Creamos nuestra propia instancia de Json para decodificar.
        //    'ignoreUnknownKeys = true' es muy útil para evitar que la app falle si el servidor añade nuevos campos.
        val json = Json { ignoreUnknownKeys = true }

        // 2. Usamos nuestra instancia 'json' para decodificar el texto.
        val response: LoginResponse = json.decodeFromString(responseBodyString)
        // --- FIN DE LA CORRECCIÓN ---

        val isAuthenticated = response.user_info.auth == 1
        Log.d(TAG, "Resultado de la autenticación (auth == 1): $isAuthenticated")

        isAuthenticated
    } catch (e: Exception) {
        Log.e(TAG, "Error durante el login: ${e.message}", e)
        false
    }
}

suspend fun getLiveCategories(url: String, username: String, password: String): List<LiveCategory> {
    return try {
        httpClient.get("$url/player_api.php") {
            url {
                parameters.append("username", username)
                parameters.append("password", password)
                parameters.append("action", "get_live_categories")
            }
        }.body()
    } catch (e: Exception) {
        emptyList()
    }
}

suspend fun getLiveChannels(url: String, username: String, password: String): List<LiveChannel> {
    return try {
        httpClient.get("$url/player_api.php") {
            url {
                parameters.append("username", username)
                parameters.append("password", password)
                parameters.append("action", "get_live_streams")
            }
        }.body()
    } catch (e: Exception) {
        emptyList()
    }
}

@Serializable
data class LoginResponse(
    val user_info: UserInfo
)

@Serializable
data class UserInfo(
    val auth: Int,
    val username: String
)
