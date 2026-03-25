package com.example.wawakusi.view.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.wawakusi.databinding.ActivityTiendaBinding

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.example.wawakusi.R
import com.example.wawakusi.data.api.WawakusiApiClient
import com.example.wawakusi.data.api.response.CatalogoProductoResponse
import com.example.wawakusi.data.api.response.VistasResponse
import com.example.wawakusi.util.AppMensaje
import com.example.wawakusi.util.MenuDinamico
import com.example.wawakusi.util.SharedPreferencesManager
import com.example.wawakusi.util.TipoMensaje
import com.example.wawakusi.viewmodel.ProductViewModel
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class TiendaActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var binding: ActivityTiendaBinding
    private lateinit var productViewModel: ProductViewModel

    private val sesionExpiradaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            AppMensaje.enviarMensaje(navigationView, "Tu sesión expiró. Inicia sesión nuevamente.", TipoMensaje.ADVERTENCIA)
            irALogin()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SharedPreferencesManager.obtenerRol() == "ADMIN") {
            val intent = Intent(this, AdminPanelActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
            return
        }
        binding = ActivityTiendaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializamos el DrawerLayout y NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Configuración del ActionBarDrawerToggle
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Configuración del NavigationView
        navigationView.setNavigationItemSelectedListener(this)
        navigationView.setCheckedItem(R.id.nav_tienda)
        MenuDinamico.aplicarHeader(navigationView)
        actualizarMenuDesdeApi()

        productViewModel = ViewModelProvider(this).get(ProductViewModel::class.java)
        productViewModel.catalogoProductosResponse.observe(this, Observer { list ->
            renderProductosTienda(list ?: emptyList())
        })
        binding.gridProducts.removeAllViews()
        setLoadingProductos(true)
        productViewModel.listarCatalogoProductos()
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Aquí manejas las opciones del menú
        val id = item.itemId
        when (id) {
            R.id.nav_home -> {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_nosotros -> {
                val intent = Intent(this, NosotrosActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_tienda -> { /* Ya estamos aquí */ }
            R.id.nav_promociones -> {
                val intent = Intent(this, PromocionesActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_contacto -> {
                val intent = Intent(this, ContactoActivity::class.java)
                startActivity(intent)
            }
            MenuDinamico.ITEM_LOGIN -> {
                irALogin()
            }
            MenuDinamico.ITEM_REGISTRO -> {
                startActivity(Intent(this, RegistroActivity::class.java))
            }
            MenuDinamico.ITEM_CERRAR_SESION -> {
                SharedPreferencesManager.limpiarSesion()
                irALogin()
            }
            MenuDinamico.ITEM_PERFIL -> {
                startActivity(Intent(this, PerfilActivity::class.java))
            }
            MenuDinamico.ITEM_CARRITO -> {
                startActivity(Intent(this, CarritoActivity::class.java))
            }
            MenuDinamico.ITEM_MIS_PEDIDOS -> {
                startActivity(Intent(this, MisPedidosActivity::class.java))
            }
            MenuDinamico.ITEM_ADMIN -> {
                startActivity(Intent(this, AdminPanelActivity::class.java))
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_carrito, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_carrito -> {
                startActivity(Intent(this, CarritoActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        if (SharedPreferencesManager.obtenerToken() != null && SharedPreferencesManager.tokenExpirado()) {
            SharedPreferencesManager.limpiarSesion()
            AppMensaje.enviarMensaje(navigationView, "Tu sesión expiró. Inicia sesión nuevamente.", TipoMensaje.ADVERTENCIA)
        }
        navigationView.setCheckedItem(R.id.nav_tienda)
        MenuDinamico.aplicarHeader(navigationView)
        actualizarMenuDesdeApi()
        setLoadingProductos(true)
        productViewModel.listarCatalogoProductos()
    }

    private fun irALogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun actualizarMenuDesdeApi() {
        WawakusiApiClient.retrofitService.vistas().enqueue(object : Callback<VistasResponse> {
            override fun onResponse(call: Call<VistasResponse>, response: Response<VistasResponse>) {
                val body = response.body() ?: VistasResponse(rpta = false, publico = !SharedPreferencesManager.estaAutenticado())
                MenuDinamico.aplicarMenuSesion(navigationView, body)
            }

            override fun onFailure(call: Call<VistasResponse>, t: Throwable) {
                val body = VistasResponse(rpta = false, publico = !SharedPreferencesManager.estaAutenticado())
                MenuDinamico.aplicarMenuSesion(navigationView, body)
            }
        })
    }

    private fun setLoadingProductos(cargando: Boolean) {
        binding.pbTiendaProductos.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.gridProducts.visibility = if (cargando) View.GONE else View.VISIBLE
        if (cargando) {
            binding.tvTiendaVacio.visibility = View.GONE
        }
    }

    private fun renderProductosTienda(productos: List<CatalogoProductoResponse>) {
        setLoadingProductos(false)
        binding.gridProducts.removeAllViews()

        binding.tvTiendaVacio.visibility = if (productos.isEmpty()) View.VISIBLE else View.GONE
        if (productos.isEmpty()) return

        for (p in productos) {
            binding.gridProducts.addView(crearTarjetaTienda(p))
        }
    }

    private fun crearTarjetaTienda(p: CatalogoProductoResponse): MaterialCardView {
        val card = MaterialCardView(this)
        card.layoutParams = GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED, 1f),
            GridLayout.spec(GridLayout.UNDEFINED, 1f)
        ).apply {
            width = 0
            setMargins(dp(6), dp(6), dp(6), dp(6))
        }
        card.radius = dp(12).toFloat()
        card.cardElevation = dp(4).toFloat()
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.blanco))
        card.isClickable = true
        card.isFocusable = true
        card.setOnClickListener {
            val descripcion = p.descripcion?.trim().orEmpty()
            val intent = Intent(this, DetalleProductoActivity::class.java)
            intent.putExtra("producto_json", Gson().toJson(p))
            startActivity(intent)
        }
        card.setOnLongClickListener {
            val descripcion = p.descripcion?.trim().orEmpty()
            if (descripcion.isNotBlank()) {
                AppMensaje.enviarMensaje(binding.root, descripcion, TipoMensaje.INFORMACION)
            }
            true
        }

        val content = LinearLayout(this)
        content.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(12), dp(12), dp(12), dp(12))

        val img = ImageView(this)
        img.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(140)
        )
        img.scaleType = ImageView.ScaleType.CENTER_CROP
        Glide.with(this)
            .load(p.imagen)
            .placeholder(R.drawable.logo)
            .error(R.drawable.logo)
            .into(img)

        val tvNombre = TextView(this)
        tvNombre.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        tvNombre.text = p.nombre
        tvNombre.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvNombre.setTextSize(14f)
        tvNombre.setTypeface(tvNombre.typeface, android.graphics.Typeface.BOLD)
        tvNombre.setPadding(0, dp(10), 0, 0)

        val tvPrecio = TextView(this)
        tvPrecio.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val precioFinal = p.precioFinal
        val precioBase = p.precioBase
        val porcentaje = p.descuento?.porcentaje
        tvPrecio.text = if (precioFinal != null && precioBase != null && porcentaje != null) {
            "Oferta: $${precioFinal} (Antes $${precioBase})"
        } else if (precioFinal != null) {
            "$${precioFinal}"
        } else if (precioBase != null) {
            "$${precioBase}"
        } else {
            "Precio no disponible"
        }
        tvPrecio.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvPrecio.alpha = 0.85f
        tvPrecio.setTextSize(13f)
        tvPrecio.setPadding(0, dp(4), 0, 0)

        content.addView(img)
        content.addView(tvNombre)
        content.addView(tvPrecio)
        card.addView(content)
        return card
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
