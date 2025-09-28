package ec.net.csed.tv

import kotlinx.serialization.Serializable

@Serializable
data class LiveCategory(
    val category_id: String,
    val category_name: String
)
