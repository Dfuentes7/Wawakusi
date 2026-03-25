package com.example.wawakusi.data.api.request

data class CheckoutPaypalCreateRequest(
    val direccionEnvio: String
)

data class CheckoutPaypalCaptureRequest(
    val paypalOrderId: String,
    val checkoutContext: String? = null
)
