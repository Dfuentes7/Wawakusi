package com.example.wawakusi.view.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.wawakusi.databinding.ActivityPaypalReturnBinding
import com.example.wawakusi.util.AppMensaje
import com.example.wawakusi.util.TipoMensaje
import com.example.wawakusi.viewmodel.CheckoutViewModel

class PaypalReturnActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaypalReturnBinding
    private lateinit var checkoutViewModel: CheckoutViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaypalReturnBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkoutViewModel = ViewModelProvider(this).get(CheckoutViewModel::class.java)
        checkoutViewModel.checkoutCaptureResponse.observe(this, Observer { resp ->
            binding.pbPago.visibility = View.GONE
            binding.btnVolver.visibility = View.VISIBLE
            if (resp != null && resp.rpta) {
                binding.tvEstado.text = resp.mensaje ?: "Pago confirmado."
                AppMensaje.enviarMensaje(binding.root, binding.tvEstado.text.toString(), TipoMensaje.CORRECTO)
                val i = Intent(this, MisPedidosActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
                finish()
            } else {
                binding.tvEstado.text = resp?.mensaje ?: "No se pudo confirmar el pago."
                AppMensaje.enviarMensaje(binding.root, binding.tvEstado.text.toString(), TipoMensaje.ERROR)
            }
        })

        binding.btnVolver.setOnClickListener {
            startActivity(Intent(this, CarritoActivity::class.java))
            finish()
        }

        procesarIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        procesarIntent(intent)
    }

    private fun procesarIntent(intent: Intent) {
        val data: Uri = intent.data ?: run {
            binding.pbPago.visibility = View.GONE
            binding.btnVolver.visibility = View.VISIBLE
            binding.tvEstado.text = "No se recibió información de PayPal."
            return
        }

        val paypalOrderId = data.getQueryParameter("token")
        val checkoutContext = data.getQueryParameter("ctx")
        val path = data.path.orEmpty()

        if (paypalOrderId.isNullOrBlank()) {
            binding.pbPago.visibility = View.GONE
            binding.btnVolver.visibility = View.VISIBLE
            binding.tvEstado.text = "No se recibió el token de PayPal."
            return
        }
        if (checkoutContext.isNullOrBlank()) {
            binding.pbPago.visibility = View.GONE
            binding.btnVolver.visibility = View.VISIBLE
            binding.tvEstado.text = "La sesión de pago no es válida. Vuelve a intentarlo."
            return
        }

        if (path.contains("cancel", ignoreCase = true)) {
            binding.pbPago.visibility = View.GONE
            binding.btnVolver.visibility = View.VISIBLE
            binding.tvEstado.text = "Pago cancelado."
            return
        }

        binding.pbPago.visibility = View.VISIBLE
        binding.btnVolver.visibility = View.GONE
        binding.tvEstado.text = "Procesando pago..."
        checkoutViewModel.paypalCapture(paypalOrderId, checkoutContext)
    }
}
