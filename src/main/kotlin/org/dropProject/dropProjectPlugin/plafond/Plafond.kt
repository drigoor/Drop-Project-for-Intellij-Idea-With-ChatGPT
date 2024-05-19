package org.dropProject.dropProjectPlugin.plafond


import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException


object Plafond {
    private val client = OkHttpClient()
    private val moshi = Moshi.Builder().build()
    private val plafondDataJsonAdapter = moshi.adapter(PlafondData::class.java)


    fun isBellowN(url: String, threshold: Int): Boolean {
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val body = response.body?.string()
            val plafondData = plafondDataJsonAdapter.fromJson(body ?: "")
            val value = plafondData?.percentage ?: 0

            return value < threshold
        }
    }


    fun hasEnoughPlafond(threshold: Int): Boolean {
        return !isBellowN("https://raw.githubusercontent.com/brunompc/my-repo/master/stuff.json", threshold)
    }

}
