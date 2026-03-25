package com.example.wawakusi.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.wawakusi.data.api.response.CheckoutPaypalCreateResponse
import com.example.wawakusi.data.api.response.CheckoutPaypalCaptureResponse
import com.example.wawakusi.repository.CheckoutRepository

class CheckoutViewModel : ViewModel() {
    var checkoutCreateResponse: LiveData<CheckoutPaypalCreateResponse>
    var checkoutCaptureResponse: LiveData<CheckoutPaypalCaptureResponse>

    private var repository = CheckoutRepository()

    init {
        checkoutCreateResponse = repository.checkoutCreateResponse
        checkoutCaptureResponse = repository.checkoutCaptureResponse
    }

    fun paypalCreate(direccionEnvio: String) {
        checkoutCreateResponse = repository.paypalCreate(direccionEnvio)
    }

    fun paypalCapture(paypalOrderId: String, checkoutContext: String?) {
        checkoutCaptureResponse = repository.paypalCapture(paypalOrderId, checkoutContext)
    }
}
