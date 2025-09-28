package ec.net.csed.tv

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val httpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

suspend fun loginIPTV(url: String, username: String, password: String): Boolean {
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
