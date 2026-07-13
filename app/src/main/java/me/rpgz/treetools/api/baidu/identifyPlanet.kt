package me.rpgz.treetools.api.baidu

import android.graphics.Bitmap
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.rpgz.treetools.ml.PlantIdentificationResponse
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URLEncoder
import java.util.Base64
import java.util.concurrent.TimeUnit
import androidx.core.graphics.scale


object PlantIdentifier {
    private const val API_KEY: String = "DiY0HamnQSa02mHSbykXox6n"
    private const val SECRET_KEY: String = "q4CE6nzHJOHiELpqDrEVadiOm4a2jUIH"
    private const val PLANT_API_URL = "https://aip.baidubce.com/rest/2.0/image-classify/v1/plant"
    private const val TOKEN_API_URL = "https://aip.baidubce.com/oauth/2.0/token"

    private val httpClient: OkHttpClient =
        OkHttpClient.Builder().readTimeout(300, TimeUnit.SECONDS).build()
    private val gson = Gson()

    @Throws(IOException::class)
    suspend fun identifyPlant(bitmap: Bitmap): PlantIdentificationResponse? = withContext(Dispatchers.IO) {
        val imageBase64 = convertBitmapToBase64(bitmap)
        return@withContext identifyPlantFromBase64(imageBase64)
    }

    @Throws(IOException::class)
    fun identifyPlantRaw(bitmap: Bitmap): String? {
        val imageBase64 = convertBitmapToBase64(bitmap)
        return identifyPlantRawFromBase64(imageBase64)
    }

    @Throws(IOException::class)
    private fun identifyPlantFromBase64(imageBase64: String): PlantIdentificationResponse? {
        val accessToken = getAccessToken()
        val mediaType = "application/x-www-form-urlencoded".toMediaType()
        val body = "image=$imageBase64".toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("$PLANT_API_URL?access_token=$accessToken")
            .post(body)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Accept", "application/json")
            .build()
            
        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string()
        Log.d("Infer", "responseBody")
        return responseBody?.let { 
            try {
                gson.fromJson(it, PlantIdentificationResponse::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    @Throws(IOException::class)
    private fun identifyPlantRawFromBase64(imageBase64: String): String? {
        val accessToken = getAccessToken()
        val mediaType = "application/x-www-form-urlencoded".toMediaType()
        val body = "image=$imageBase64".toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("$PLANT_API_URL?access_token=$accessToken")
            .post(body)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Accept", "application/json")
            .build()
            
        val response = httpClient.newCall(request).execute()
        return response.body?.string()
    }

    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        val scaledBitmap = scaleBitmap(bitmap, 500)
        val byteArrayOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val imageBytes = byteArrayOutputStream.toByteArray()
        return convertImageToBase64(imageBytes, urlEncode = true)
    }

    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }
        
        val scaleFactor = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newWidth = (width * scaleFactor).toInt()
        val newHeight = (height * scaleFactor).toInt()
        
        return bitmap.scale(newWidth, newHeight)
    }

    @Throws(IOException::class)
    fun convertImageToBase64(imageBytes: ByteArray, urlEncode: Boolean = true): String {
        var base64 = Base64.getEncoder().encodeToString(imageBytes)
        if (urlEncode) {
            base64 = URLEncoder.encode(base64, "utf-8")
        }
        return base64
    }

    @Throws(IOException::class)
    private fun getAccessToken(): String {
        val mediaType = "application/x-www-form-urlencoded".toMediaType()
        val body = "grant_type=client_credentials&client_id=$API_KEY&client_secret=$SECRET_KEY"
            .toRequestBody(mediaType)
            
        val request = Request.Builder()
            .url(TOKEN_API_URL)
            .post(body)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build()
            
        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string()
            ?: throw IOException("Empty response body")
        return JSONObject(responseBody).getString("access_token")
    }
}