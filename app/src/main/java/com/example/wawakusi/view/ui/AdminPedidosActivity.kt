package com.example.wawakusi.view.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.wawakusi.R
import com.example.wawakusi.data.api.WawakusiApiClient
import com.example.wawakusi.data.api.response.MessageResponse
import com.example.wawakusi.data.api.response.VentaItemResponse
import com.example.wawakusi.data.api.response.VentaListResponse
import com.example.wawakusi.data.api.response.VistasResponse
import com.example.wawakusi.databinding.ActivityAdminPedidosBinding
import com.example.wawakusi.util.AppMensaje
import com.example.wawakusi.util.MenuDinamico
import com.example.wawakusi.util.SharedPreferencesManager
import com.example.wawakusi.util.TipoMensaje
import com.example.wawakusi.viewmodel.VentaViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminPedidosActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var binding: ActivityAdminPedidosBinding
    private lateinit var ventaViewModel: VentaViewModel

    private val sesionExpiradaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            AppMensaje.enviarMensaje(navigationView, "Tu sesión expiró. Inicia sesión nuevamente.", TipoMensaje.ADVERTENCIA)
            irALogin()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminPedidosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener(this)
        MenuDinamico.aplicarHeader(navigationView)
        actualizarMenuDesdeApi()

        if (!SharedPreferencesManager.estaAutenticado()) {
            AppMensaje.enviarMensaje(binding.root, "Inicia sesión.", TipoMensaje.ADVERTENCIA)
            irALogin()
            return
        }

        if (SharedPreferencesManager.obtenerRol() != "ADMIN") {
            AppMensaje.enviarMensaje(binding.root, "No autorizado.", TipoMensaje.ERROR)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        ventaViewModel = ViewModelProvider(this).get(VentaViewModel::class.java)
        ventaViewModel.ventasAdminResponse.observe(this, Observer { resp ->
            renderAdminPedidos(resp)
        })
        ventaViewModel.actualizarEstadoResponse.observe(this, Observer { resp ->
            manejarActualizarEstado(resp)
        })

        cargarVentas()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(SharedPreferencesManager.ACTION_SESION_EXPIRADA)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(sesionExpiradaReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(sesionExpiradaReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(sesionExpiradaReceiver)
    }

    override fun onResume() {
        super.onResume()
        if (SharedPreferencesManager.obtenerToken() != null && SharedPreferencesManager.tokenExpirado()) {
            SharedPreferencesManager.limpiarSesion()
            AppMensaje.enviarMensaje(binding.root, "Tu sesión expiró. Inicia sesión nuevamente.", TipoMensaje.ADVERTENCIA)
        }
        MenuDinamico.aplicarHeader(navigationView)
        actualizarMenuDesdeApi()
    }

    private fun actualizarMenuDesdeApi() {
        WawakusiApiClient.retrofitService.vistas().enqueue(object : Callback<VistasResponse> {
            override fun onResponse(call: Call<VistasResponse>, response: Response<VistasResponse>) {
                val body = response.body() ?: VistasResponse(rpta = false, publico = !SharedPreferencesManager.estaAutenticado())
                MenuDinamico.aplicarMenuSesion(navigationView, body)
            }

            override fun onFailure(call: Call<VistasResponse>, t: Throwable) {}
        })
    }

    private fun cargarVentas() {
        binding.pbAdminPedidos.visibility = View.VISIBLE
        binding.tvAdminPedidosVacio.visibility = View.GONE
        ventaViewModel.listarVentasAdmin()
    }

    private fun renderAdminPedidos(resp: VentaListResponse?) {
        binding.pbAdminPedidos.visibility = View.GONE
        binding.ordersContainer.removeAllViews()

        if (resp == null || !resp.rpta) {
            binding.tvAdminPedidosVacio.visibility = View.VISIBLE
            AppMensaje.enviarMensaje(binding.root, resp?.mensaje ?: "No se pudieron cargar las ventas.", TipoMensaje.ERROR)
            return
        }

        binding.tvAdminPedidosVacio.visibility = if (resp.ventas.isEmpty()) View.VISIBLE else View.GONE
        for (v in resp.ventas) {
            binding.ordersContainer.addView(crearTarjetaAdminPedido(v))
        }
    }

    private fun manejarActualizarEstado(resp: MessageResponse?) {
        if (resp == null) return
        val msg = resp.mensaje ?: resp.message ?: "Operación realizada."
        if (resp.rpta == true) {
            AppMensaje.enviarMensaje(binding.root, msg, TipoMensaje.CORRECTO)
            cargarVentas()
        } else {
            AppMensaje.enviarMensaje(binding.root, msg, TipoMensaje.ERROR)
        }
    }

    private fun crearTarjetaAdminPedido(item: VentaItemResponse): MaterialCardView {
        val card = MaterialCardView(this)
        card.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(10)
        }
        card.radius = dp(16).toFloat()
        card.cardElevation = dp(2).toFloat()
        card.strokeWidth = dp(1)
        card.strokeColor = ContextCompat.getColor(this, R.color.lavender)
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.blanco))
        card.isClickable = true
        card.isFocusable = true
        card.setOnClickListener { mostrarDetallePedido(item) }

        val content = LinearLayout(this)
        content.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(16), dp(14), dp(16), dp(14))

        val tvCodigo = TextView(this)
        tvCodigo.text = item.codigo ?: "Venta #${item.idVenta}"
        tvCodigo.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvCodigo.setTextSize(15f)
        tvCodigo.setTypeface(tvCodigo.typeface, android.graphics.Typeface.BOLD)

        val cliente = item.clienteNombre?.trim().orEmpty().ifBlank { "Cliente" }
        val tvCliente = TextView(this)
        tvCliente.text = "Cliente: $cliente · ${item.clienteEmail ?: "-"}"
        tvCliente.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvCliente.alpha = 0.8f
        tvCliente.setTextSize(13f)

        val total = item.total ?: 0.0
        val tvMeta = TextView(this)
        tvMeta.text = "Total: $ ${String.format("%.2f", total)} · Pago: ${estadoPagoTexto(item.pagoEstado)} · ${item.metodoPago ?: "-"}"
        tvMeta.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvMeta.alpha = 0.85f
        tvMeta.setTextSize(13f)

        val tvEstado = TextView(this)
        tvEstado.text = "Estado: ${estadoVentaTexto(item.estado)} · Envío: ${estadoEnvioTexto(item.envioEstado)}"
        tvEstado.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvEstado.alpha = 0.8f
        tvEstado.setTextSize(13f)

        content.addView(tvCodigo)
        content.addView(tvCliente)
        content.addView(tvMeta)
        content.addView(tvEstado)

        val acciones = construirAccionesEstado(item)
        if (acciones != null) {
            content.addView(acciones)
        }

        card.addView(content)
        return card
    }

    private fun construirAccionesEstado(item: VentaItemResponse): View? {
        val estadoActual = item.estado
        val row = LinearLayout(this)
        row.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(0, dp(12), 0, 0)

        val btnDetalle = MaterialButton(this)
        btnDetalle.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(8)
        }
        btnDetalle.text = "Ver detalles"
        btnDetalle.isAllCaps = false
        btnDetalle.setBackgroundColor(ContextCompat.getColor(this, R.color.lavender))
        btnDetalle.setTextColor(ContextCompat.getColor(this, R.color.black))
        btnDetalle.setOnClickListener { mostrarDetallePedido(item) }

        val btnCambiar = MaterialButton(this)
        btnCambiar.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(8)
        }
        btnCambiar.text = "Cambiar estado"
        btnCambiar.isAllCaps = false
        btnCambiar.setBackgroundColor(ContextCompat.getColor(this, R.color.naranja))
        btnCambiar.setTextColor(ContextCompat.getColor(this, R.color.blanco))
        btnCambiar.isEnabled = estadoActual != 0 && estadoActual != 5
        btnCambiar.setOnClickListener { mostrarDialogoCambiarEstado(item) }

        row.addView(btnDetalle)
        row.addView(btnCambiar)
        return row
    }

    private fun mostrarDialogoCambiarEstado(item: VentaItemResponse) {
        val estadoActual = item.estado ?: -1
        if (estadoActual == 0 || estadoActual == 5) {
            AppMensaje.enviarMensaje(binding.root, "No se puede cambiar el estado en la situación actual.", TipoMensaje.ADVERTENCIA)
            return
        }

        val opciones = listOf(
            2 to "Enviado",
            3 to "En camino",
            4 to "Finalizado",
            5 to "Cancelado"
        )
        val labels = opciones.map { it.second }.toTypedArray()
        var selected = opciones.indexOfFirst { it.first == estadoActual }
        if (selected < 0) selected = 0

        AlertDialog.Builder(this)
            .setTitle(item.codigo ?: "Cambiar estado")
            .setSingleChoiceItems(labels, selected) { _, which ->
                selected = which
            }
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoEstado = opciones.getOrNull(selected)?.first ?: return@setPositiveButton
                ventaViewModel.actualizarEstadoVenta(item.idVenta, nuevoEstado)
            }
            .show()
    }

    private fun mostrarDetallePedido(item: VentaItemResponse) {
        val root = LinearLayout(this)
        root.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(16), dp(14), dp(16), dp(14))

        fun textLine(text: String, bold: Boolean = false, alpha: Float = 0.85f): TextView {
            val tv = TextView(this)
            tv.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            tv.text = text
            tv.setTextColor(ContextCompat.getColor(this, R.color.black))
            tv.alpha = alpha
            tv.textSize = 13f
            if (bold) tv.setTypeface(tv.typeface, android.graphics.Typeface.BOLD)
            tv.setPadding(0, dp(4), 0, dp(4))
            return tv
        }

        val codigo = item.codigo ?: "Venta #${item.idVenta}"
        root.addView(textLine(codigo, bold = true, alpha = 1f).apply { textSize = 16f })

        val cliente = item.clienteNombre?.trim().orEmpty().ifBlank { "Cliente" }
        root.addView(textLine("Cliente: $cliente"))
        root.addView(textLine("Email: ${item.clienteEmail ?: "-"} · Tel: ${item.clienteTelefono ?: "-"}"))

        val total = item.total ?: 0.0
        root.addView(textLine("Total: $ ${String.format("%.2f", total)} · Ítems: ${item.totalItems ?: item.detalles.sumOf { it.cantidad ?: 0 }}"))
        root.addView(textLine("Pago: ${estadoPagoTexto(item.pagoEstado)} · ${item.metodoPago ?: "-"}"))
        root.addView(textLine("Estado: ${estadoVentaTexto(item.estado)} · Envío: ${estadoEnvioTexto(item.envioEstado)}"))

        val direccion = item.direccionEnvio?.trim().orEmpty()
        if (direccion.isNotBlank()) root.addView(textLine("Dirección: $direccion", alpha = 0.8f))

        if (item.detalles.isNotEmpty()) {
            root.addView(textLine("Productos:", bold = true, alpha = 0.95f).apply { setPadding(0, dp(10), 0, dp(6)) })
            for (d in item.detalles) {
                val talla = d.talla?.trim().orEmpty().ifBlank { "Única" }
                val color = d.color?.trim().orEmpty().ifBlank { "Sin color" }
                val qty = d.cantidad ?: 0
                val nombre = d.productoNombre?.trim().orEmpty().ifBlank { "Producto" }
                val precio = d.precioUnitario ?: 0.0
                root.addView(textLine("• $qty × $nombre ($talla · $color) · $ ${String.format("%.2f", precio)}", alpha = 0.78f))
            }
        }

        val scroll = ScrollView(this)
        scroll.addView(root)

        AlertDialog.Builder(this)
            .setTitle("Detalle del pedido")
            .setView(scroll)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun estadoVentaTexto(estado: Int?): String {
        return when (estado) {
            0 -> "Pendiente de pago"
            1 -> "Pagado"
            2 -> "Enviado"
            3 -> "En camino"
            4 -> "Finalizado"
            5 -> "Cancelado"
            else -> "-"
        }
    }

    private fun estadoEnvioTexto(estado: Int?): String {
        return when (estado) {
            0 -> "Pendiente"
            1 -> "Enviado"
            2 -> "En camino"
            3 -> "Entregado"
            else -> "-"
        }
    }

    private fun estadoPagoTexto(estado: Int?): String {
        return when (estado) {
            0 -> "Pendiente"
            1 -> "Pagado"
            2 -> "Fallido"
            else -> "-"
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            MenuDinamico.ITEM_ADMIN_DASHBOARD -> startActivity(Intent(this, AdminPanelActivity::class.java))
            MenuDinamico.ITEM_ADMIN_PRODUCTOS -> startActivity(Intent(this, AdminProductosActivity::class.java))
            MenuDinamico.ITEM_ADMIN_PEDIDOS -> {}
            MenuDinamico.ITEM_ADMIN_REPORTES -> startActivity(Intent(this, AdminReportesActivity::class.java))
            MenuDinamico.ITEM_ADMIN_PROMOCIONES -> startActivity(Intent(this, AdminPromocionesActivity::class.java))
            MenuDinamico.ITEM_ADMIN_USUARIOS -> startActivity(Intent(this, AdminUsuariosRolesActivity::class.java))
            MenuDinamico.ITEM_PERFIL -> startActivity(Intent(this, PerfilActivity::class.java))
            MenuDinamico.ITEM_CERRAR_SESION -> {
                SharedPreferencesManager.limpiarSesion()
                irALogin()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun irALogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
