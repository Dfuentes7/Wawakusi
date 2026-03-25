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
import com.example.wawakusi.data.api.response.ReportePorDiaResponse
import com.example.wawakusi.data.api.response.ReportePorEstadoResponse
import com.example.wawakusi.data.api.response.ReporteTopProductoResponse
import com.example.wawakusi.data.api.response.ReporteVentasResponse
import com.example.wawakusi.data.api.response.VistasResponse
import com.example.wawakusi.databinding.ActivityAdminReportesBinding
import com.example.wawakusi.util.AppMensaje
import com.example.wawakusi.util.MenuDinamico
import com.example.wawakusi.util.SharedPreferencesManager
import com.example.wawakusi.util.TipoMensaje
import com.example.wawakusi.viewmodel.ReportesViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AdminReportesActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var binding: ActivityAdminReportesBinding
    private lateinit var reportesViewModel: ReportesViewModel

    private val sesionExpiradaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            AppMensaje.enviarMensaje(navigationView, "Tu sesión expiró. Inicia sesión nuevamente.", TipoMensaje.ADVERTENCIA)
            irALogin()
        }
    }

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminReportesBinding.inflate(layoutInflater)
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

        reportesViewModel = ViewModelProvider(this).get(ReportesViewModel::class.java)
        reportesViewModel.reporteVentasResponse.observe(this, Observer { resp ->
            setLoading(false)
            renderReporte(resp)
        })

        binding.etDesde.setOnClickListener {
            abrirDatePicker { dateStr -> binding.etDesde.setText(dateStr) }
        }
        binding.etHasta.setOnClickListener {
            abrirDatePicker { dateStr -> binding.etHasta.setText(dateStr) }
        }
        binding.btnGenerar.setOnClickListener {
            generar()
        }

        binding.tvResumen.text = "Genera un reporte para ver resultados."
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

    private fun abrirDatePicker(onSelected: (String) -> Unit) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecciona una fecha")
            .build()

        picker.addOnPositiveButtonClickListener { millis ->
            val dateStr = sdf.format(Date(millis))
            onSelected(dateStr)
        }
        picker.show(supportFragmentManager, "datePickerReportes")
    }

    private fun generar() {
        val desde = binding.etDesde.text?.toString()?.trim().orEmpty().ifBlank { null }
        val hasta = binding.etHasta.text?.toString()?.trim().orEmpty().ifBlank { null }
        setLoading(true)
        reportesViewModel.obtenerReporteVentas(desde, hasta)
    }

    private fun setLoading(cargando: Boolean) {
        binding.pbReportes.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.btnGenerar.isEnabled = !cargando
    }

    private fun renderReporte(resp: ReporteVentasResponse?) {
        binding.containerEstados.removeAllViews()
        binding.containerTopProductos.removeAllViews()
        binding.containerPorDia.removeAllViews()

        if (resp == null || !resp.rpta) {
            binding.tvResumen.text = resp?.mensaje ?: "No se pudo obtener el reporte."
            AppMensaje.enviarMensaje(binding.root, binding.tvResumen.text.toString(), TipoMensaje.ERROR)
            return
        }

        val totalVentas = resp.resumen?.totalVentas ?: 0
        val totalIngresos = resp.resumen?.totalIngresos ?: 0.0
        val ticket = resp.resumen?.ticketPromedio ?: 0.0

        binding.tvResumen.text =
            "Ventas: $totalVentas\nIngresos: $ ${String.format("%.2f", totalIngresos)}\nTicket promedio: $ ${String.format("%.2f", ticket)}"

        if (resp.porEstado.isEmpty()) {
            binding.containerEstados.addView(textLine("Sin datos por estado."))
        } else {
            for (e in resp.porEstado) binding.containerEstados.addView(renderEstado(e))
        }

        if (resp.topProductos.isEmpty()) {
            binding.containerTopProductos.addView(textLine("Sin top de productos."))
        } else {
            resp.topProductos.forEachIndexed { index, p ->
                binding.containerTopProductos.addView(renderTopProducto(index + 1, p))
            }
        }

        if (resp.porDia.isEmpty()) {
            binding.containerPorDia.addView(textLine("Sin datos por día."))
        } else {
            for (d in resp.porDia) binding.containerPorDia.addView(renderPorDia(d))
        }
    }

    private fun renderEstado(item: ReportePorEstadoResponse): View {
        val estado = item.estado ?: -1
        val cantidad = item.cantidad ?: 0
        val total = item.total ?: 0.0
        return textLine("${estadoVentaTexto(estado)} · $cantidad ventas · $ ${String.format("%.2f", total)}")
    }

    private fun renderTopProducto(rank: Int, item: ReporteTopProductoResponse): View {
        val nombre = item.productoNombre ?: "Producto"
        val cant = item.cantidadVendida ?: 0
        val total = item.totalVendido ?: 0.0
        return textLine("$rank. $nombre · $cant u · $ ${String.format("%.2f", total)}")
    }

    private fun renderPorDia(item: ReportePorDiaResponse): View {
        val dia = item.dia ?: "-"
        val cant = item.cantidad ?: 0
        val total = item.total ?: 0.0
        return textLine("$dia · $cant ventas · $ ${String.format("%.2f", total)}")
    }

    private fun textLine(text: String): TextView {
        val tv = TextView(this)
        tv.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        tv.text = text
        tv.setTextColor(ContextCompat.getColor(this, R.color.black))
        tv.alpha = 0.85f
        tv.setPadding(0, dp(4), 0, dp(4))
        return tv
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            MenuDinamico.ITEM_ADMIN_DASHBOARD -> startActivity(Intent(this, AdminPanelActivity::class.java))
            MenuDinamico.ITEM_ADMIN_PRODUCTOS -> startActivity(Intent(this, AdminProductosActivity::class.java))
            MenuDinamico.ITEM_ADMIN_PEDIDOS -> startActivity(Intent(this, AdminPedidosActivity::class.java))
            MenuDinamico.ITEM_ADMIN_REPORTES -> {}
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

