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
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.wawakusi.R
import com.example.wawakusi.data.api.WawakusiApiClient
import com.example.wawakusi.data.api.response.ConsultarPedidoResponse
import com.example.wawakusi.data.api.response.VentaItemResponse
import com.example.wawakusi.data.api.response.VentaListResponse
import com.example.wawakusi.data.api.response.VistasResponse
import com.example.wawakusi.databinding.ActivityMisPedidosBinding
import com.example.wawakusi.util.AppMensaje
import com.example.wawakusi.util.MenuDinamico
import com.example.wawakusi.util.SharedPreferencesManager
import com.example.wawakusi.util.TipoMensaje
import com.example.wawakusi.viewmodel.PedidoViewModel
import com.example.wawakusi.viewmodel.VentaViewModel
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MisPedidosActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object {
        const val EXTRA_FOCUS_BUSCAR = "focusBuscar"
    }

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var binding: ActivityMisPedidosBinding
    private lateinit var ventaViewModel: VentaViewModel
    private lateinit var pedidoViewModel: PedidoViewModel

    private val sesionExpiradaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            AppMensaje.enviarMensaje(navigationView, "Tu sesión expiró. Inicia sesión nuevamente.", TipoMensaje.ADVERTENCIA)
            irALogin()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMisPedidosBinding.inflate(layoutInflater)
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
            AppMensaje.enviarMensaje(binding.root, "Inicia sesión para ver tus pedidos.", TipoMensaje.ADVERTENCIA)
            irALogin()
            return
        }

        ventaViewModel = ViewModelProvider(this).get(VentaViewModel::class.java)
        ventaViewModel.misVentasResponse.observe(this, Observer { resp ->
            renderPedidos(resp)
        })

        pedidoViewModel = ViewModelProvider(this).get(PedidoViewModel::class.java)
        pedidoViewModel.consultarPedidoResponse.observe(this, Observer { resp ->
            renderConsulta(resp)
        })

        binding.btnBuscarCodigo.setOnClickListener {
            val codigo = binding.etBuscarCodigo.text?.toString()?.trim().orEmpty()
            if (codigo.isBlank()) {
                AppMensaje.enviarMensaje(binding.root, "Ingresa el código del pedido.", TipoMensaje.ADVERTENCIA)
                return@setOnClickListener
            }
            binding.ordersContainer.removeAllViews()
            binding.tvPedidosVacio.visibility = View.GONE
            setLoading(true)
            pedidoViewModel.consultarPorCodigo(codigo)
        }

        binding.btnVerTodos.setOnClickListener {
            binding.etBuscarCodigo.setText("")
            cargarPedidos()
        }

        if (intent?.getBooleanExtra(EXTRA_FOCUS_BUSCAR, false) == true) {
            binding.etBuscarCodigo.requestFocus()
        }

        cargarPedidos()
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

    private fun cargarPedidos() {
        binding.pbPedidos.visibility = View.VISIBLE
        binding.tvPedidosVacio.visibility = View.GONE
        binding.btnBuscarCodigo.isEnabled = false
        binding.btnVerTodos.isEnabled = false
        ventaViewModel.listarMisVentas()
    }

    private fun setLoading(loading: Boolean) {
        binding.pbPedidos.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnBuscarCodigo.isEnabled = !loading
        binding.btnVerTodos.isEnabled = !loading
    }

    private fun renderPedidos(resp: VentaListResponse?) {
        setLoading(false)
        binding.ordersContainer.removeAllViews()

        if (resp == null || !resp.rpta) {
            binding.tvPedidosVacio.visibility = View.VISIBLE
            AppMensaje.enviarMensaje(binding.root, resp?.mensaje ?: "No se pudieron cargar tus pedidos.", TipoMensaje.ERROR)
            return
        }

        binding.tvPedidosVacio.visibility = if (resp.ventas.isEmpty()) View.VISIBLE else View.GONE
        for (v in resp.ventas) {
            binding.ordersContainer.addView(crearTarjetaPedido(v))
        }
    }

    private fun renderConsulta(resp: ConsultarPedidoResponse?) {
        setLoading(false)
        binding.ordersContainer.removeAllViews()

        if (resp == null || !resp.rpta || resp.pedido == null) {
            binding.tvPedidosVacio.visibility = View.VISIBLE
            binding.tvPedidosVacio.text = "No se encontró el pedido."
            AppMensaje.enviarMensaje(binding.root, resp?.mensaje ?: "No se pudo consultar el pedido.", TipoMensaje.ERROR)
            return
        }

        binding.tvPedidosVacio.visibility = View.GONE
        binding.ordersContainer.addView(crearTarjetaPedido(resp.pedido))
    }

    private fun crearTarjetaPedido(item: VentaItemResponse): MaterialCardView {
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

        val content = LinearLayout(this)
        content.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(16), dp(14), dp(16), dp(14))

        val tvCodigo = TextView(this)
        tvCodigo.text = item.codigo ?: "Pedido #${item.idVenta}"
        tvCodigo.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvCodigo.setTextSize(15f)
        tvCodigo.setTypeface(tvCodigo.typeface, android.graphics.Typeface.BOLD)

        val tvEstado = TextView(this)
        tvEstado.text = "Estado: ${estadoVentaTexto(item.estado)} · Envío: ${estadoEnvioTexto(item.envioEstado)}"
        tvEstado.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvEstado.alpha = 0.8f
        tvEstado.setTextSize(13f)

        val total = item.total ?: 0.0
        val tvTotal = TextView(this)
        tvTotal.text = "Total: $ ${String.format("%.2f", total)} · Ítems: ${item.totalItems ?: item.detalles.sumOf { it.cantidad ?: 0 }}"
        tvTotal.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvTotal.alpha = 0.85f
        tvTotal.setTextSize(13f)

        val direccion = item.direccionEnvio?.trim().orEmpty()
        val tvDireccion = TextView(this)
        tvDireccion.text = if (direccion.isBlank()) "Dirección: -"
        else "Dirección: $direccion"
        tvDireccion.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvDireccion.alpha = 0.75f
        tvDireccion.setTextSize(13f)

        content.addView(tvCodigo)
        content.addView(tvEstado)
        content.addView(tvTotal)
        content.addView(tvDireccion)

        val detalles = item.detalles.take(3)
        if (detalles.isNotEmpty()) {
            val tvLinea = TextView(this)
            tvLinea.text = "Productos:"
            tvLinea.setTextColor(ContextCompat.getColor(this, R.color.black))
            tvLinea.alpha = 0.9f
            tvLinea.setPadding(0, dp(10), 0, 0)
            tvLinea.setTextSize(13f)
            content.addView(tvLinea)

            for (d in detalles) {
                val talla = d.talla?.trim().orEmpty().ifBlank { "Única" }
                val color = d.color?.trim().orEmpty().ifBlank { "Sin color" }
                val qty = d.cantidad ?: 0
                val nombre = d.productoNombre?.trim().orEmpty().ifBlank { "Producto" }
                val tvDet = TextView(this)
                tvDet.text = "• $qty × $nombre ($talla · $color)"
                tvDet.setTextColor(ContextCompat.getColor(this, R.color.black))
                tvDet.alpha = 0.75f
                tvDet.setTextSize(13f)
                content.addView(tvDet)
            }
        }

        card.addView(content)
        return card
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> startActivity(Intent(this, MainActivity::class.java))
            R.id.nav_nosotros -> startActivity(Intent(this, NosotrosActivity::class.java))
            R.id.nav_tienda -> startActivity(Intent(this, TiendaActivity::class.java))
            R.id.nav_promociones -> startActivity(Intent(this, PromocionesActivity::class.java))
            R.id.nav_contacto -> startActivity(Intent(this, ContactoActivity::class.java))
            MenuDinamico.ITEM_LOGIN -> irALogin()
            MenuDinamico.ITEM_REGISTRO -> startActivity(Intent(this, RegistroActivity::class.java))
            MenuDinamico.ITEM_CERRAR_SESION -> {
                SharedPreferencesManager.limpiarSesion()
                irALogin()
            }
            MenuDinamico.ITEM_PERFIL -> startActivity(Intent(this, PerfilActivity::class.java))
            MenuDinamico.ITEM_CARRITO -> startActivity(Intent(this, CarritoActivity::class.java))
            MenuDinamico.ITEM_MIS_PEDIDOS -> {}
            MenuDinamico.ITEM_ADMIN -> startActivity(Intent(this, AdminPanelActivity::class.java))
            MenuDinamico.ITEM_ADMIN_DASHBOARD -> startActivity(Intent(this, AdminPanelActivity::class.java))
            MenuDinamico.ITEM_ADMIN_PRODUCTOS -> startActivity(Intent(this, AdminProductosActivity::class.java))
            MenuDinamico.ITEM_ADMIN_PEDIDOS -> startActivity(Intent(this, AdminPedidosActivity::class.java))
            MenuDinamico.ITEM_ADMIN_REPORTES -> startActivity(Intent(this, AdminReportesActivity::class.java))
            MenuDinamico.ITEM_ADMIN_PROMOCIONES -> startActivity(Intent(this, AdminPromocionesActivity::class.java))
            MenuDinamico.ITEM_ADMIN_USUARIOS -> startActivity(Intent(this, AdminUsuariosRolesActivity::class.java))
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
