package com.example.wawakusi.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.wawakusi.data.api.response.CatalogoProductoResponse
import com.example.wawakusi.data.api.response.MessageResponse
import com.example.wawakusi.data.api.response.ProductResponse
import com.example.wawakusi.repository.ProductRepository

class ProductViewModel : ViewModel() {
    var productosResponse: LiveData<List<ProductResponse>>
    var catalogoProductosResponse: LiveData<List<CatalogoProductoResponse>>
    var promocionesResponse: LiveData<List<CatalogoProductoResponse>>
    var crearProductoResponse: LiveData<MessageResponse>
    var actualizarProductoResponse: LiveData<MessageResponse>
    var eliminarProductoResponse: LiveData<MessageResponse>

    private var repository = ProductRepository()

    init {
        productosResponse = repository.productosResponse
        catalogoProductosResponse = repository.catalogoProductosResponse
        promocionesResponse = repository.promocionesResponse
        crearProductoResponse = repository.crearProductoResponse
        actualizarProductoResponse = repository.actualizarProductoResponse
        eliminarProductoResponse = repository.eliminarProductoResponse
    }

    fun listarProductos() {
        productosResponse = repository.listarProductos()
    }

    fun listarCatalogoProductos() {
        catalogoProductosResponse = repository.listarCatalogoProductos()
    }

    fun listarPromociones() {
        promocionesResponse = repository.listarPromociones()
    }

    fun crearProducto(
        nombre: String,
        precio: String,
        talla: String?,
        color: String?,
        cantidad: String,
        descripcion: String,
        imagenBytes: ByteArray,
        mimeType: String
    ) {
        crearProductoResponse = repository.crearProducto(nombre, precio, talla, color, cantidad, descripcion, imagenBytes, mimeType)
    }

    fun actualizarProducto(
        id: Int,
        nombre: String,
        precio: String,
        talla: String?,
        color: String?,
        cantidad: String,
        descripcion: String,
        imagenBytes: ByteArray?,
        mimeType: String?
    ) {
        actualizarProductoResponse =
            repository.actualizarProducto(id, nombre, precio, talla, color, cantidad, descripcion, imagenBytes, mimeType)
    }

    fun eliminarProducto(id: Int) {
        eliminarProductoResponse = repository.eliminarProducto(id)
    }
}


