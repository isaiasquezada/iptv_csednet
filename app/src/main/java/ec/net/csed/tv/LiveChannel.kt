package ec.net.csed.tv

import kotlinx.serialization.Serializable

@Serializable
data class LiveChannel(
    val stream_id: Int,
    val name: String,
    val stream_icon: String?,
    val category_id: String
)
