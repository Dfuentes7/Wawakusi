package com.example.wawakusi.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.wawakusi.data.api.WawakusiApiClient
import com.example.wawakusi.data.api.response.CatalogoProductoResponse
import com.example.wawakusi.data.api.response.MessageResponse
import com.example.wawakusi.data.api.response.ProductResponse
import com.google.gson.Gson
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProductRepository {

    var productosResponse = MutableLiveData<List<ProductResponse>>()
    var catalogoProductosResponse = MutableLiveData<List<CatalogoProductoResponse>>()
    var promocionesResponse = MutableLiveData<List<CatalogoProductoResponse>>()
    var crearProductoResponse = MutableLiveData<MessageResponse>()
    var actualizarProductoResponse = MutableLiveData<MessageResponse>()
    var eliminarProductoResponse = MutableLiveData<MessageResponse>()

    private val gson = Gson()

    fun listarProductos(): MutableLiveData<List<ProductResponse>> {
        val call: Call<List<ProductResponse>> = WawakusiApiClient.retrofitService.productos()
        call.enqueue(object : Callback<List<ProductResponse>> {
            override fun onResponse(call: Call<List<ProductResponse>>, response: Response<List<ProductResponse>>) {
                if (response.isSuccessful) {
                    productosResponse.value = response.body() ?: emptyList()
                    return
                }
                productosResponse.value = emptyList()
            }

            override fun onFailure(call: Call<List<ProductResponse>>, t: Throwable) {
                Log.e("ErrorProductos", t.message.toString())
                productosResponse.value = emptyList()
            }
        })
        return productosResponse
    }

    fun listarCatalogoProductos(): MutableLiveData<List<CatalogoProductoResponse>> {
        val call: Call<List<CatalogoProductoResponse>> = WawakusiApiClient.retrofitService.catalogoProductos()
        call.enqueue(object : Callback<List<CatalogoProductoResponse>> {
            override fun onResponse(call: Call<List<CatalogoProductoResponse>>, response: Response<List<CatalogoProductoResponse>>) {
                if (response.isSuccessful) {
                    catalogoProductosResponse.value = response.body() ?: emptyList()
                    return
                }
                catalogoProductosResponse.value = emptyList()
            }

            override fun onFailure(call: Call<List<CatalogoProductoResponse>>, t: Throwable) {
                Log.e("ErrorCatalogoProductos", t.message.toString())
                catalogoProductosResponse.value = emptyList()
            }
        })
        return catalogoProductosResponse
    }

    fun listarPromociones(): MutableLiveData<List<CatalogoProductoResponse>> {
        val call: Call<List<CatalogoProductoResponse>> = WawakusiApiClient.retrofitService.promocionesProductos()
        call.enqueue(object : Callback<List<CatalogoProductoResponse>> {
            override fun onResponse(call: Call<List<CatalogoProductoResponse>>, response: Response<List<CatalogoProductoResponse>>) {
                if (response.isSuccessful) {
                    promocionesResponse.value = response.body() ?: emptyList()
                    return
                }
                promocionesResponse.value = emptyList()
            }

            override fun onFailure(call: Call<List<CatalogoProductoResponse>>, t: Throwable) {
                Log.e("ErrorPromociones", t.message.toString())
                promocionesResponse.value = emptyList()
            }
        })
        return promocionesResponse
    }

    fun crearProducto(
        nombre: String,
        precio: String,
        cantidad: String,
        descripcion: String,
        imagenBytes: ByteArray,
        mimeType: String
    ): MutableLiveData<MessageResponse> {
        val bodyImage = RequestBody.create(MediaType.parse(mimeType), imagenBytes)
        val partImage = MultipartBody.Part.createFormData("Imagen", "imagen.jpg", bodyImage)

        val textPlain = MediaType.parse("text/plain")
        val rbNombre = RequestBody.create(textPlain, nombre)
        val rbPrecio = RequestBody.create(textPlain, precio)
        val rbCantidad = RequestBody.create(textPlain, cantidad)
        val rbDescripcion = RequestBody.create(textPlain, descripcion)

        val call: Call<MessageResponse> =
            WawakusiApiClient.retrofitService.crearProducto(rbNombre, rbPrecio, rbCantidad, rbDescripcion, partImage)

        call.enqueue(object : Callback<MessageResponse> {
            override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                if (response.isSuccessful) {
                    crearProductoResponse.value = response.body() ?: MessageResponse(message = "Producto registrado.")
                    return
                }

                val errorJson = response.errorBody()?.string()
                val parsed = try {
                    if (errorJson.isNullOrBlank()) null else gson.fromJson(errorJson, MessageResponse::class.java)
                } catch (_: Exception) {
                    null
                }

                crearProductoResponse.value = parsed
                    ?: MessageResponse(message = "No se pudo registrar producto (${response.code()}).")
            }

            override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                Log.e("ErrorCrearProducto", t.message.toString())
                crearProductoResponse.value = MessageResponse(message = "No se pudo conectar con el servidor.")
            }
        })

        return crearProductoResponse
    }

    fun actualizarProducto(
        id: Int,
        nombre: String,
        precio: String,
        cantidad: String,
        descripcion: String,
        imagenBytes: ByteArray?,
        mimeType: String?
    ): MutableLiveData<MessageResponse> {
        val textPlain = MediaType.parse("text/plain")
        val rbNombre = RequestBody.create(textPlain, nombre)
        val rbPrecio = RequestBody.create(textPlain, precio)
        val rbCantidad = RequestBody.create(textPlain, cantidad)
        val rbDescripcion = RequestBody.create(textPlain, descripcion)

        val partImage = if (imagenBytes != null && !mimeType.isNullOrBlank()) {
            val bodyImage = RequestBody.create(MediaType.parse(mimeType), imagenBytes)
            MultipartBody.Part.createFormData("Imagen", "imagen.jpg", bodyImage)
        } else {
            null
        }

        val call: Call<MessageResponse> = WawakusiApiClient.retrofitService.actualizarProducto(
            id,
            rbNombre,
            rbPrecio,
            rbCantidad,
            rbDescripcion,
            partImage
        )

        call.enqueue(object : Callback<MessageResponse> {
            override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                if (response.isSuccessful) {
                    actualizarProductoResponse.value =
                        response.body() ?: MessageResponse(message = "Producto actualizado.")
                    return
                }

                val errorJson = response.errorBody()?.string()
                val parsed = try {
                    if (errorJson.isNullOrBlank()) null else gson.fromJson(errorJson, MessageResponse::class.java)
                } catch (_: Exception) {
                    null
                }

                actualizarProductoResponse.value = parsed
                    ?: MessageResponse(message = "No se pudo actualizar producto (${response.code()}).")
            }

            override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                Log.e("ErrorActualizarProducto", t.message.toString())
                actualizarProductoResponse.value = MessageResponse(message = "No se pudo conectar con el servidor.")
            }
        })

        return actualizarProductoResponse
    }

    fun eliminarProducto(id: Int): MutableLiveData<MessageResponse> {
        val call: Call<MessageResponse> = WawakusiApiClient.retrofitService.eliminarProducto(id)
        call.enqueue(object : Callback<MessageResponse> {
            override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                if (response.isSuccessful) {
                    eliminarProductoResponse.value =
                        response.body() ?: MessageResponse(message = "Producto eliminado.")
                    return
                }

                val errorJson = response.errorBody()?.string()
                val parsed = try {
                    if (errorJson.isNullOrBlank()) null else gson.fromJson(errorJson, MessageResponse::class.java)
                } catch (_: Exception) {
                    null
                }

                eliminarProductoResponse.value = parsed
                    ?: MessageResponse(message = "No se pudo eliminar producto (${response.code()}).")
            }

            override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                Log.e("ErrorEliminarProducto", t.message.toString())
                eliminarProductoResponse.value = MessageResponse(message = "No se pudo conectar con el servidor.")
            }
        })
        return eliminarProductoResponse
    }
}
