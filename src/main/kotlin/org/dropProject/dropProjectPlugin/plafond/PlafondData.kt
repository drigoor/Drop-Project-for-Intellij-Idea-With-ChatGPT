package org.dropProject.dropProjectPlugin.plafond


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class PlafondData(
    @Json(name = "percentage")
    val percentage: Int,
)
