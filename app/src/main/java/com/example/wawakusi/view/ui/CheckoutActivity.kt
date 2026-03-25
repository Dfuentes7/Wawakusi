package com.example.wawakusi.view.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
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
import com.example.wawakusi.data.api.response.CarritoItemResponse
import com.example.wawakusi.data.api.response.CarritoResponse
import com.example.wawakusi.data.api.response.VistasResponse
import com.example.wawakusi.databinding.ActivityCheckoutBinding
import com.example.wawakusi.util.AppMensaje
import com.example.wawakusi.util.MenuDinamico
import com.example.wawakusi.util.SharedPreferencesManager
import com.example.wawakusi.util.TipoMensaje
import com.example.wawakusi.viewmodel.CarritoViewModel
import com.example.wawakusi.viewmodel.CheckoutViewModel
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CheckoutActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var binding: ActivityCheckoutBinding

    private lateinit var carritoViewModel: CarritoViewModel
    private lateinit var checkoutViewModel: CheckoutViewModel

    private var direccionEnvio: String = ""

    private val sesionExpiradaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            AppMensaje.enviarMensaje(navigationView, "Tu sesión expiró. Inicia sesión nuevamente.", TipoMensaje.ADVERTENCIA)
            irALogin()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SharedPreferencesManager.estaAutenticado()) {
            irALogin()
            return
        }

        binding = ActivityCheckoutBinding.inflate(layoutInflater)
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

        carritoViewModel = ViewModelProvider(this).get(CarritoViewModel::class.java)
        checkoutViewModel = ViewModelProvider(this).get(CheckoutViewModel::class.java)

        carritoViewModel.carritoResponse.observe(this, Observer { resp ->
            renderResumen(resp)
        })
        checkoutViewModel.checkoutCreateResponse.observe(this, Observer { resp ->
            binding.btnPagarPaypal.isEnabled = true
            if (resp == null || !resp.rpta) {
                AppMensaje.enviarMensaje(binding.root, resp?.mensaje ?: "No se pudo iniciar el pago.", TipoMensaje.ERROR)
                return@Observer
            }
            val url = resp.approvalUrl
            if (url.isNullOrBlank()) {
                AppMensaje.enviarMensaje(binding.root, "No se pudo obtener la URL de PayPal.", TipoMensaje.ERROR)
                return@Observer
            }
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        })

        binding.btnContinuarInfo.setOnClickListener {
            val dir = binding.etDireccion.text?.toString()?.trim().orEmpty()
            if (dir.isBlank()) {
                AppMensaje.enviarMensaje(binding.root, "Ingrese una dirección de envío.", TipoMensaje.ADVERTENCIA)
                return@setOnClickListener
            }
            direccionEnvio = dir
            mostrarPaso(2)
            cargarResumen()
        }

        binding.btnContinuarPago.setOnClickListener {
            mostrarPaso(3)
        }

        binding.btnPagarPaypal.setOnClickListener {
            if (direccionEnvio.isBlank()) {
                AppMensaje.enviarMensaje(binding.root, "Ingrese una dirección de envío.", TipoMensaje.ADVERTENCIA)
                mostrarPaso(1)
                return@setOnClickListener
            }
            binding.btnPagarPaypal.isEnabled = false
            checkoutViewModel.paypalCreate(direccionEnvio)
        }

        mostrarPaso(1)
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

    private fun mostrarPaso(paso: Int) {
        binding.cardInfo.visibility = if (paso == 1) View.VISIBLE else View.GONE
        binding.cardResumen.visibility = if (paso == 2) View.VISIBLE else View.GONE
        binding.cardPago.visibility = if (paso == 3) View.VISIBLE else View.GONE

        binding.tvPaso.text = when (paso) {
            1 -> "Paso 1/3 · Información"
            2 -> "Paso 2/3 · Detalles"
            else -> "Paso 3/3 · Pago"
        }
    }

    private fun cargarResumen() {
        binding.pbResumen.visibility = View.VISIBLE
        carritoViewModel.obtenerMiCarrito()
    }

    private fun renderResumen(resp: CarritoResponse?) {
        binding.pbResumen.visibility = View.GONE
        val container = binding.resumenItems
        container.removeAllViews()

        if (resp == null || !resp.rpta) {
            AppMensaje.enviarMensaje(binding.root, resp?.mensaje ?: "No se pudo cargar el carrito.", TipoMensaje.ERROR)
            return
        }
        if (resp.items.isEmpty()) {
            AppMensaje.enviarMensaje(binding.root, "Tu carrito está vacío.", TipoMensaje.ADVERTENCIA)
            startActivity(Intent(this, CarritoActivity::class.java))
            finish()
            return
        }

        for (it in resp.items) {
            container.addView(renderItem(it))
        }
        val total = resp.total ?: 0.0
        binding.tvResumenTotal.text = "Total: $ ${String.format("%.2f", total)}"
    }

    private fun renderItem(item: CarritoItemResponse): View {
        val box = LinearLayout(this)
        box.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(0, dp(6), 0, dp(6))

        val tvNombre = TextView(this)
        tvNombre.text = item.productoNombre
        tvNombre.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvNombre.setTextSize(14f)
        tvNombre.setTypeface(tvNombre.typeface, android.graphics.Typeface.BOLD)

        val precio = item.precioUnitario ?: 0.0
        val tvMeta = TextView(this)
        tvMeta.text = "${item.cantidad} × $ ${String.format("%.2f", precio)}"
        tvMeta.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvMeta.alpha = 0.75f
        tvMeta.setTextSize(13f)

        box.addView(tvNombre)
        box.addView(tvMeta)
        return box
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
            MenuDinamico.ITEM_MIS_PEDIDOS -> startActivity(Intent(this, MisPedidosActivity::class.java))
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
