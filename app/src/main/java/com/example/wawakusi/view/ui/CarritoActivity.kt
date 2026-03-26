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
import com.bumptech.glide.Glide
import com.example.wawakusi.R
import com.example.wawakusi.data.api.WawakusiApiClient
import com.example.wawakusi.data.api.response.CarritoItemResponse
import com.example.wawakusi.data.api.response.CarritoResponse
import com.example.wawakusi.data.api.response.VistasResponse
import com.example.wawakusi.databinding.ActivityCarritoBinding
import com.example.wawakusi.util.AppMensaje
import com.example.wawakusi.util.MenuDinamico
import com.example.wawakusi.util.SharedPreferencesManager
import com.example.wawakusi.util.TipoMensaje
import com.example.wawakusi.viewmodel.CarritoViewModel
import com.example.wawakusi.workers.RecordatorioWorker
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CarritoActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var binding: ActivityCarritoBinding
    private lateinit var carritoViewModel: CarritoViewModel

    private val sesionExpiradaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            AppMensaje.enviarMensaje(navigationView, "Tu sesión expiró. Inicia sesión nuevamente.", TipoMensaje.ADVERTENCIA)
            irALogin()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarritoBinding.inflate(layoutInflater)
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
        carritoViewModel.carritoResponse.observe(this, Observer { resp ->
            renderCarrito(resp)
        })
        carritoViewModel.actualizarItemResponse.observe(this, Observer { resp ->
            val msg = resp?.message ?: resp?.mensaje ?: "Operación realizada."
            if (resp?.rpta == true || (resp?.message != null && !msg.contains("No se pudo"))) {
                cargarCarrito()
            } else {
                AppMensaje.enviarMensaje(binding.root, msg, TipoMensaje.ERROR)
                setLoading(false)
            }
        })
        carritoViewModel.eliminarItemResponse.observe(this, Observer { resp ->
            val msg = resp?.message ?: resp?.mensaje ?: "Operación realizada."
            if (resp?.rpta == true || (resp?.message != null && !msg.contains("No se pudo"))) {
                cargarCarrito()
            } else {
                AppMensaje.enviarMensaje(binding.root, msg, TipoMensaje.ERROR)
                setLoading(false)
            }
        })

        if (!SharedPreferencesManager.estaAutenticado()) {
            AppMensaje.enviarMensaje(binding.root, "Inicia sesión para ver tu carrito.", TipoMensaje.ADVERTENCIA)
            irALogin()
            return
        }
        try {
            RecordatorioWorker.cancelCartReminder(this)
        } catch (_: Exception) {}

        binding.btnExplorar.setOnClickListener {
            startActivity(Intent(this, TiendaActivity::class.java))
        }

        binding.btnIrPago.setOnClickListener {
            startActivity(Intent(this, CheckoutActivity::class.java))
        }

        cargarCarrito()
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

    private fun cargarCarrito() {
        setLoading(true)
        carritoViewModel.obtenerMiCarrito()
    }

    private fun setLoading(cargando: Boolean) {
        binding.pbCarrito.visibility = if (cargando) View.VISIBLE else View.GONE
        if (cargando) {
            binding.tvCarritoVacio.visibility = View.GONE
            binding.btnExplorar.visibility = View.GONE
        }
    }

    private fun renderCarrito(resp: CarritoResponse?) {
        setLoading(false)
        val container = binding.itemsContainer
        container.removeAllViews()

        if (resp == null || resp.rpta.not()) {
            binding.tvCarritoVacio.visibility = View.VISIBLE
            binding.tvCarritoVacio.text = "Tu carrito está vacío."
            binding.btnExplorar.visibility = View.VISIBLE
            binding.tvTotal.text = "Total: $ 0.00"
            binding.btnIrPago.isEnabled = false
            return
        }

        val vacio = resp.items.isEmpty()
        binding.tvCarritoVacio.visibility = if (vacio) View.VISIBLE else View.GONE
        binding.tvCarritoVacio.text = if (vacio) "Tu carrito está vacío. Explora productos en la tienda." else ""
        binding.btnExplorar.visibility = if (vacio) View.VISIBLE else View.GONE
        binding.btnIrPago.isEnabled = !vacio
        for (it in resp.items) {
            container.addView(crearTarjetaItem(it))
        }

        val total = resp.total ?: 0.0
        binding.tvTotal.text = "Total: $ ${String.format("%.2f", total)}"
        try {
            val count = resp.items.sumOf { it.cantidad }
            MenuDinamico.actualizarBadgeCarrito(navigationView, count)
        } catch (_: Exception) {}
    }

    private fun crearTarjetaItem(item: CarritoItemResponse): MaterialCardView {
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
        content.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(16), dp(14), dp(16), dp(14))

        val header = LinearLayout(this)
        header.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        header.orientation = LinearLayout.HORIZONTAL

        val img = androidx.appcompat.widget.AppCompatImageView(this)
        img.layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)).apply { marginEnd = dp(12) }
        img.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
        Glide.with(this).load(item.imagen).placeholder(R.drawable.logo).error(R.drawable.logo).into(img)

        val texts = LinearLayout(this)
        texts.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        texts.orientation = LinearLayout.VERTICAL

        val tvNombre = TextView(this)
        tvNombre.text = item.productoNombre
        tvNombre.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvNombre.setTextSize(15f)
        tvNombre.setTypeface(tvNombre.typeface, android.graphics.Typeface.BOLD)

        val talla = item.talla?.trim().orEmpty().ifBlank { "Única" }
        val color = item.color?.trim().orEmpty().ifBlank { "Sin color" }
        val tvMeta = TextView(this)
        tvMeta.text = "Variante: $talla · $color"
        tvMeta.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvMeta.alpha = 0.75f
        tvMeta.setTextSize(13f)

        texts.addView(tvNombre)
        texts.addView(tvMeta)
        header.addView(img)
        header.addView(texts)

        val precio = item.precioUnitario ?: 0.0
        val tvPrecio = TextView(this)
        tvPrecio.text = "$ ${String.format("%.2f", precio)}"
        tvPrecio.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvPrecio.setTextSize(13f)
        tvPrecio.alpha = 0.85f
        tvPrecio.setPadding(0, dp(10), 0, 0)

        val acciones = LinearLayout(this)
        acciones.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        acciones.orientation = LinearLayout.HORIZONTAL
        acciones.setPadding(0, dp(10), 0, 0)

        val btnMenos = MaterialButton(this)
        btnMenos.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(8) }
        btnMenos.text = "-"
        btnMenos.isAllCaps = false
        btnMenos.setBackgroundColor(ContextCompat.getColor(this, R.color.lavender))
        btnMenos.setTextColor(ContextCompat.getColor(this, R.color.black))
        btnMenos.setOnClickListener {
            setLoading(true)
            carritoViewModel.actualizarItem(item.idDetalle, item.cantidad - 1)
        }

        val btnMas = MaterialButton(this)
        btnMas.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(8) }
        btnMas.text = "+"
        btnMas.isAllCaps = false
        btnMas.setBackgroundColor(ContextCompat.getColor(this, R.color.lavender))
        btnMas.setTextColor(ContextCompat.getColor(this, R.color.black))
        btnMas.setOnClickListener {
            setLoading(true)
            carritoViewModel.actualizarItem(item.idDetalle, item.cantidad + 1)
        }

        val btnEliminar = MaterialButton(this)
        btnEliminar.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        btnEliminar.text = "Eliminar"
        btnEliminar.isAllCaps = false
        btnEliminar.setBackgroundColor(ContextCompat.getColor(this, R.color.naranja))
        btnEliminar.setTextColor(ContextCompat.getColor(this, R.color.blanco))
        btnEliminar.setOnClickListener {
            setLoading(true)
            carritoViewModel.eliminarItem(item.idDetalle)
        }

        acciones.addView(btnMenos)
        acciones.addView(btnMas)
        acciones.addView(btnEliminar)

        val tvCantidad = TextView(this)
        tvCantidad.text = "Cantidad: ${item.cantidad}"
        tvCantidad.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvCantidad.alpha = 0.75f
        tvCantidad.setTextSize(13f)
        tvCantidad.setPadding(0, dp(6), 0, 0)

        content.addView(header)
        content.addView(tvPrecio)
        content.addView(tvCantidad)
        content.addView(acciones)
        card.addView(content)
        return card
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
            MenuDinamico.ITEM_CARRITO -> {}
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
