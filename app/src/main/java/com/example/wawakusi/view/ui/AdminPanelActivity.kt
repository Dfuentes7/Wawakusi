package com.example.wawakusi.view.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
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
import com.example.wawakusi.data.api.response.DashboardResponse
import com.example.wawakusi.data.api.response.ReportePorDiaResponse
import com.example.wawakusi.data.api.response.VistasResponse
import com.example.wawakusi.databinding.ActivityAdminPanelBinding
import com.example.wawakusi.util.AppMensaje
import com.example.wawakusi.util.MenuDinamico
import com.example.wawakusi.util.SharedPreferencesManager
import com.example.wawakusi.util.TipoMensaje
import com.example.wawakusi.viewmodel.ReportesViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminPanelActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var binding: ActivityAdminPanelBinding
    private lateinit var reportesViewModel: ReportesViewModel

    private val sesionExpiradaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            AppMensaje.enviarMensaje(binding.root, "Tu sesión expiró. Inicia sesión nuevamente.", TipoMensaje.ADVERTENCIA)
            irALogin()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminPanelBinding.inflate(layoutInflater)
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

        reportesViewModel = ViewModelProvider(this).get(ReportesViewModel::class.java)
        reportesViewModel.dashboardResponse.observe(this, Observer { resp ->
            renderDashboard(resp)
        })

        cargarPanel()
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
        cargarPanel()
    }

    private fun actualizarMenuDesdeApi() {
        WawakusiApiClient.retrofitService.vistas().enqueue(object : Callback<VistasResponse> {
            override fun onResponse(call: Call<VistasResponse>, response: Response<VistasResponse>) {
                val body = response.body()
                if (body != null) {
                    MenuDinamico.aplicarMenuSesion(navigationView, body)
                    return
                }
                aplicarMenuFallback()
            }

            override fun onFailure(call: Call<VistasResponse>, t: Throwable) {
                aplicarMenuFallback()
            }
        })
    }

    private fun aplicarMenuFallback() {
        val rol = SharedPreferencesManager.obtenerRol()
        val publico = !SharedPreferencesManager.estaAutenticado()
        val vistasResponse = if (rol == "ADMIN" && !publico) {
            VistasResponse(rpta = false, publico = false, vistas = emptyList(), rol = "ADMIN", permisos = emptyList())
        } else {
            VistasResponse(rpta = false, publico = publico, vistas = emptyList(), rol = rol, permisos = emptyList())
        }
        MenuDinamico.aplicarMenuSesion(navigationView, vistasResponse)
    }

    private fun cargarPanel() {
        WawakusiApiClient.retrofitService.vistas().enqueue(object : Callback<VistasResponse> {
            override fun onResponse(call: Call<VistasResponse>, response: Response<VistasResponse>) {
                val body = response.body()
                if (body == null || body.publico || body.rol != "ADMIN") {
                    AppMensaje.enviarMensaje(binding.root, "No autorizado.", TipoMensaje.ERROR)
                    irALogin()
                    return
                }
                reportesViewModel.obtenerDashboard(7)
            }

            override fun onFailure(call: Call<VistasResponse>, t: Throwable) {
                AppMensaje.enviarMensaje(binding.root, "No se pudo cargar el panel.", TipoMensaje.ERROR)
            }
        })
    }

    private fun renderDashboard(resp: DashboardResponse?) {
        if (resp == null || !resp.rpta) {
            binding.tvResumenAdmin.text = resp?.mensaje ?: "No se pudo cargar el dashboard."
            binding.containerBarras.removeAllViews()
            binding.tvProductosValue.text = "—"
            binding.tvClientesValue.text = "—"
            binding.tvVentasValue.text = "—"
            binding.tvIngresosValue.text = "—"
            binding.chipGroupEstadosDashboard.removeAllViews()
            binding.tvEstadosVacio.visibility = View.VISIBLE
            return
        }

        val productos = resp.productosActivos ?: 0
        val clientes = resp.clientesActivos ?: 0
        val ventas = resp.ventasPagadas ?: 0
        val ingresos = resp.ingresosTotales ?: 0.0

        val headerMsg = resp.mensaje?.trim().orEmpty()
        binding.tvResumenAdmin.visibility = if (headerMsg.isBlank()) View.GONE else View.VISIBLE
        binding.tvResumenAdmin.text = headerMsg

        binding.tvProductosValue.text = productos.toString()
        binding.tvClientesValue.text = clientes.toString()
        binding.tvVentasValue.text = ventas.toString()
        binding.tvIngresosValue.text = money(ingresos)

        renderBarras(resp.porDia)
        renderEstados(resp.porEstado)
    }

    private fun renderBarras(items: List<ReportePorDiaResponse>) {
        val container = binding.containerBarras
        container.removeAllViews()

        if (items.isEmpty()) {
            val tv = TextView(this)
            tv.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            tv.text = "Sin datos para mostrar."
            tv.setTextColor(ContextCompat.getColor(this, R.color.black))
            tv.alpha = 0.75f
            container.addView(tv)
            return
        }

        val max = items.maxOfOrNull { it.total ?: 0.0 } ?: 0.0
        val maxH = dp(90)
        val barColor = ContextCompat.getColor(this, R.color.naranja)
        for (i in items) {
            val col = LinearLayout(this)
            col.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
            col.orientation = LinearLayout.VERTICAL
            col.gravity = android.view.Gravity.BOTTOM

            val bar = View(this)
            val total = i.total ?: 0.0
            val h = if (max <= 0.0) dp(6) else Math.max(dp(6), (total / max * maxH).toInt())
            bar.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h)
            val bg = GradientDrawable()
            bg.setColor(barColor)
            bg.cornerRadius = dp(10).toFloat()
            bar.background = bg

            val label = TextView(this)
            label.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            label.text = (i.dia ?: "-").takeLast(5)
            label.setTextColor(ContextCompat.getColor(this, R.color.black))
            label.textSize = 10f
            label.alpha = 0.75f
            label.gravity = android.view.Gravity.CENTER_HORIZONTAL
            label.setPadding(0, dp(6), 0, 0)

            col.addView(bar)
            col.addView(label)
            container.addView(col)
        }
    }

    private fun renderEstados(items: List<com.example.wawakusi.data.api.response.ReportePorEstadoResponse>) {
        val group = binding.chipGroupEstadosDashboard
        group.removeAllViews()

        if (items.isEmpty()) {
            binding.tvEstadosVacio.visibility = View.VISIBLE
            return
        }
        binding.tvEstadosVacio.visibility = View.GONE

        for (e in items) {
            val estado = e.estado ?: -1
            val cantidad = e.cantidad ?: 0
            val total = e.total ?: 0.0

            val chip = Chip(this)
            chip.text = "${estadoVentaTexto(estado)} · $cantidad · ${money(total)}"
            chip.isClickable = false
            chip.isCheckable = false
            chip.setTextColor(ContextCompat.getColor(this, R.color.black))
            chip.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.lavender))
            chip.chipStrokeWidth = dp(1).toFloat()
            chip.chipStrokeColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.lavender))
            group.addView(chip)
        }
    }

    private fun estadoVentaTexto(estado: Int): String {
        return when (estado) {
            1 -> "PAGADO"
            2 -> "ENVIADO"
            3 -> "EN CAMINO"
            4 -> "FINALIZADO"
            else -> "OTRO"
        }
    }

    private fun money(amount: Double): String {
        return "$ ${String.format("%.2f", amount)}"
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        when (id) {
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
            MenuDinamico.ITEM_ADMIN -> {}
            MenuDinamico.ITEM_ADMIN_DASHBOARD -> {}
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
