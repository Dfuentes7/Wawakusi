package com.example.wawakusi.data.api

import com.example.wawakusi.data.api.request.LoginRequest
import com.example.wawakusi.data.api.request.RegistroRequest
import com.example.wawakusi.data.api.request.CrearDescuentoRequest
import com.example.wawakusi.data.api.request.ActualizarEstadoDescuentoRequest
import com.example.wawakusi.data.api.request.CrearUsuarioAdminRequest
import com.example.wawakusi.data.api.request.ActualizarUsuarioAdminRequest
import com.example.wawakusi.data.api.request.AgregarCarritoItemRequest
import com.example.wawakusi.data.api.request.ActualizarCarritoItemRequest
import com.example.wawakusi.data.api.request.CheckoutPaypalCreateRequest
import com.example.wawakusi.data.api.request.CheckoutPaypalCaptureRequest
import com.example.wawakusi.data.api.request.ActualizarEstadoVentaRequest
import com.example.wawakusi.data.api.request.UpdateMeRequest
import com.example.wawakusi.data.api.response.LoginResponse
import com.example.wawakusi.data.api.response.CatalogoProductoResponse
import com.example.wawakusi.data.api.response.MessageResponse
import com.example.wawakusi.data.api.response.MeResponse
import com.example.wawakusi.data.api.response.ProductResponse
import com.example.wawakusi.data.api.response.PromocionAdminResponse
import com.example.wawakusi.data.api.response.RegistroResponse
import com.example.wawakusi.data.api.response.UpdateMeResponse
import com.example.wawakusi.data.api.response.UsuarioRolResponse
import com.example.wawakusi.data.api.response.RolResponse
import com.example.wawakusi.data.api.response.CarritoResponse
import com.example.wawakusi.data.api.response.CheckoutPaypalCreateResponse
import com.example.wawakusi.data.api.response.CheckoutPaypalCaptureResponse
import com.example.wawakusi.data.api.response.ConsultarPedidoResponse
import com.example.wawakusi.data.api.response.DashboardResponse
import com.example.wawakusi.data.api.response.ReporteVentasResponse
import com.example.wawakusi.data.api.response.VentaListResponse
import com.example.wawakusi.data.api.response.VistasResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.Query
import retrofit2.http.Multipart
import retrofit2.http.Part

interface WawakusiApiService {

    //Aqui iran todos los recursos de la API realizado en Node
    @POST("login")
    fun login(@Body loginRequest: LoginRequest): Call<LoginResponse>
    @POST("login/register")
    fun registrar(@Body registroRequest: RegistroRequest): Call<RegistroResponse>

    @GET("login/views")
    fun vistas(): Call<VistasResponse>

    @GET("usuario/me")
    fun me(): Call<MeResponse>

    @PUT("usuario/me")
    fun updateMe(@Body request: UpdateMeRequest): Call<UpdateMeResponse>

    @GET("usuario")
    fun listarUsuariosAdmin(): Call<List<UsuarioRolResponse>>

    @POST("usuario")
    fun crearUsuarioAdmin(@Body request: CrearUsuarioAdminRequest): Call<MessageResponse>

    @PUT("usuario/{id}")
    fun actualizarUsuarioAdmin(
        @Path("id") id: Int,
        @Body request: ActualizarUsuarioAdminRequest
    ): Call<MessageResponse>

    @GET("rol")
    fun listarRolesAdmin(): Call<List<RolResponse>>

    @GET("carrito")
    fun miCarrito(): Call<CarritoResponse>

    @POST("carrito/items")
    fun agregarCarritoItem(@Body request: AgregarCarritoItemRequest): Call<MessageResponse>

    @PUT("carrito/items/{idDetalle}")
    fun actualizarCarritoItem(
        @Path("idDetalle") idDetalle: Int,
        @Body request: ActualizarCarritoItemRequest
    ): Call<MessageResponse>

    @DELETE("carrito/items/{idDetalle}")
    fun eliminarCarritoItem(@Path("idDetalle") idDetalle: Int): Call<MessageResponse>

    @POST("checkout/paypal/create")
    fun checkoutPaypalCreate(@Body request: CheckoutPaypalCreateRequest): Call<CheckoutPaypalCreateResponse>

    @POST("checkout/paypal/capture")
    fun checkoutPaypalCapture(@Body request: CheckoutPaypalCaptureRequest): Call<CheckoutPaypalCaptureResponse>

    @GET("venta/mias")
    fun misVentas(): Call<VentaListResponse>

    @GET("venta/consultar")
    fun consultarPedido(@Query("codigo") codigo: String): Call<ConsultarPedidoResponse>

    @GET("venta")
    fun ventasAdmin(): Call<VentaListResponse>

    @PUT("venta/{id}/estado")
    fun actualizarEstadoVenta(
        @Path("id") id: Int,
        @Body request: ActualizarEstadoVentaRequest
    ): Call<MessageResponse>

    @GET("reportes/ventas")
    fun reporteVentas(
        @Query("desde") desde: String? = null,
        @Query("hasta") hasta: String? = null
    ): Call<ReporteVentasResponse>

    @GET("reportes/dashboard")
    fun dashboard(@Query("dias") dias: Int? = 7): Call<DashboardResponse>

    @GET("producto")
    fun productos(): Call<List<ProductResponse>>

    @GET("producto/catalogo")
    fun catalogoProductos(): Call<List<CatalogoProductoResponse>>

    @GET("producto/promociones")
    fun promocionesProductos(): Call<List<CatalogoProductoResponse>>

    @GET("descuento")
    fun listarPromocionesAdmin(): Call<List<PromocionAdminResponse>>

    @POST("descuento")
    fun crearPromocionAdmin(@Body request: CrearDescuentoRequest): Call<MessageResponse>

    @DELETE("descuento/{id}")
    fun eliminarPromocionAdmin(@Path("id") id: Int): Call<MessageResponse>

    @PUT("descuento/{id}/estado")
    fun actualizarEstadoPromocionAdmin(
        @Path("id") id: Int,
        @Body request: ActualizarEstadoDescuentoRequest
    ): Call<MessageResponse>

    @Multipart
    @POST("producto")
    fun crearProducto(
        @Part("Nombre") nombre: RequestBody,
        @Part("Precio") precio: RequestBody,
        @Part("Talla") talla: RequestBody,
        @Part("Color") color: RequestBody,
        @Part("Cantidad") cantidad: RequestBody,
        @Part("Descripcion") descripcion: RequestBody,
        @Part imagen: MultipartBody.Part
    ): Call<MessageResponse>

    @Multipart
    @PUT("producto/{id}")
    fun actualizarProducto(
        @Path("id") id: Int,
        @Part("Nombre") nombre: RequestBody,
        @Part("Precio") precio: RequestBody,
        @Part("Talla") talla: RequestBody,
        @Part("Color") color: RequestBody,
        @Part("Cantidad") cantidad: RequestBody,
        @Part("Descripcion") descripcion: RequestBody,
        @Part imagen: MultipartBody.Part?
    ): Call<MessageResponse>

    @DELETE("producto/{id}")
    fun eliminarProducto(@Path("id") id: Int): Call<MessageResponse>
}
