package com.example.wawakusi.view.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.wawakusi.databinding.ActivityPromocionesBinding

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build
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
import com.example.wawakusi.viewmodel.CarritoViewModel
import com.example.wawakusi.viewmodel.ProductViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response



class PromocionesActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var binding: ActivityPromocionesBinding
    private lateinit var productViewModel: ProductViewModel
    private lateinit var carritoViewModel: CarritoViewModel

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
        binding = ActivityPromocionesBinding.inflate(layoutInflater)
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
        navigationView.setCheckedItem(R.id.nav_promociones)
        MenuDinamico.aplicarHeader(navigationView)
        actualizarMenuDesdeApi()

        productViewModel = ViewModelProvider(this).get(ProductViewModel::class.java)
        productViewModel.promocionesResponse.observe(this, Observer { list ->
            renderPromociones(list ?: emptyList())
        })

        carritoViewModel = ViewModelProvider(this).get(CarritoViewModel::class.java)
        carritoViewModel.agregarItemResponse.observe(this, Observer { resp ->
            val msg = resp?.message ?: resp?.mensaje ?: "Operación realizada."
            if (resp?.rpta == true || (resp?.message != null && !msg.contains("No se pudo"))) {
                AppMensaje.enviarMensaje(binding.root, msg, TipoMensaje.CORRECTO)
                try {
                    carritoViewModel.obtenerMiCarrito()
                } catch (_: Exception) {}
            } else {
                AppMensaje.enviarMensaje(binding.root, msg, TipoMensaje.ERROR)
            }
        })
        carritoViewModel.carritoResponse.observe(this, Observer { c ->
            if (c != null && c.rpta) {
                try {
                    val count = c.items.sumOf { it.cantidad }
                    MenuDinamico.actualizarBadgeCarrito(navigationView, count)
                } catch (_: Exception) {}
            }
        })

        binding.gridPromos.removeAllViews()
        setLoadingPromos(true)
        productViewModel.listarPromociones()
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
            R.id.nav_tienda -> {
                val intent = Intent(this, TiendaActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_promociones -> { /* Ya estamos aquí */ }
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

    override fun onResume() {
        super.onResume()
        if (SharedPreferencesManager.obtenerToken() != null && SharedPreferencesManager.tokenExpirado()) {
            SharedPreferencesManager.limpiarSesion()
            AppMensaje.enviarMensaje(navigationView, "Tu sesión expiró. Inicia sesión nuevamente.", TipoMensaje.ADVERTENCIA)
        }
        navigationView.setCheckedItem(R.id.nav_promociones)
        MenuDinamico.aplicarHeader(navigationView)
        actualizarMenuDesdeApi()
        setLoadingPromos(true)
        productViewModel.listarPromociones()
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

    private fun setLoadingPromos(cargando: Boolean) {
        binding.pbPromosProductos.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.gridPromos.visibility = if (cargando) View.GONE else View.VISIBLE
        if (cargando) {
            binding.tvPromosVacio.visibility = View.GONE
        }
    }

    private fun renderPromociones(promos: List<CatalogoProductoResponse>) {
        setLoadingPromos(false)
        binding.gridPromos.removeAllViews()
        binding.tvPromosVacio.visibility = if (promos.isEmpty()) View.VISIBLE else View.GONE
        if (promos.isEmpty()) return

        for (p in promos) {
            binding.gridPromos.addView(crearTarjetaPromo(p))
        }
    }

    private fun crearTarjetaPromo(p: CatalogoProductoResponse): MaterialCardView {
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

        val descripcion = p.descripcion?.trim().orEmpty()
        val tvDesc = TextView(this)
        tvDesc.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        tvDesc.text = if (descripcion.isNotBlank()) descripcion else "Sin descripción."
        tvDesc.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvDesc.alpha = 0.75f
        tvDesc.setTextSize(12f)
        tvDesc.maxLines = 2
        tvDesc.ellipsize = android.text.TextUtils.TruncateAt.END
        tvDesc.setPadding(0, dp(6), 0, 0)

        val precioBase = p.precioBase
        val precioFinal = p.precioFinal

        val tvOriginal = TextView(this)
        tvOriginal.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        tvOriginal.text = if (precioBase != null) "Antes: $${precioBase}" else ""
        tvOriginal.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvOriginal.alpha = 0.7f
        tvOriginal.setTextSize(12f)
        tvOriginal.paintFlags = tvOriginal.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
        tvOriginal.visibility = if (precioBase != null && precioFinal != null) View.VISIBLE else View.GONE

        val tvOferta = TextView(this)
        tvOferta.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        tvOferta.text = if (precioFinal != null) "Oferta: $${precioFinal}" else "Oferta disponible"
        tvOferta.setTextColor(ContextCompat.getColor(this, R.color.naranja))
        tvOferta.setTextSize(14f)
        tvOferta.setPadding(0, dp(2), 0, 0)

        val v = p.variantes.firstOrNull()
        val talla = v?.talla?.trim().orEmpty().ifBlank { "Única" }
        val color = v?.color?.trim().orEmpty().ifBlank { "Sin color" }
        val stock = v?.stock
        val tvVar = TextView(this)
        tvVar.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        tvVar.text = "Variante: $talla · $color" + if (stock != null) " · Stock: $stock" else ""
        tvVar.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvVar.alpha = 0.75f
        tvVar.setTextSize(12f)
        tvVar.setPadding(0, dp(6), 0, 0)

        val btnAgregar = MaterialButton(this)
        btnAgregar.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        btnAgregar.text = "Agregar al carrito"
        btnAgregar.isAllCaps = false
        btnAgregar.setIconResource(R.drawable.ic_cart_nav_24)
        btnAgregar.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        btnAgregar.iconPadding = dp(8)
        btnAgregar.setBackgroundColor(ContextCompat.getColor(this, R.color.lavender))
        btnAgregar.setTextColor(ContextCompat.getColor(this, R.color.black))
        btnAgregar.setOnClickListener { agregarPromoAlCarrito(p) }

        content.addView(img)
        content.addView(tvNombre)
        content.addView(tvDesc)
        content.addView(tvOriginal)
        content.addView(tvOferta)
        content.addView(tvVar)
        content.addView(btnAgregar)
        card.addView(content)
        card.setOnClickListener {
            val intent = Intent(this, DetalleProductoActivity::class.java)
            intent.putExtra("producto_json", Gson().toJson(p))
            intent.putExtra("producto_id", p.id)
            startActivity(intent)
        }
        return card
    }

    private fun agregarPromoAlCarrito(p: CatalogoProductoResponse) {
        if (!SharedPreferencesManager.estaAutenticado()) {
            AppMensaje.enviarMensaje(binding.root, "Inicia sesión para agregar al carrito.", TipoMensaje.ADVERTENCIA)
            irALogin()
            return
        }
        val variante = p.variantes.firstOrNull { it.stock == null || it.stock > 0 } ?: p.variantes.firstOrNull()
        if (variante?.idVariante != null) {
            if (variante.stock != null && variante.stock <= 0) {
                AppMensaje.enviarMensaje(binding.root, "Producto sin stock.", TipoMensaje.ADVERTENCIA)
                return
            }
            carritoViewModel.agregarItem(variante.idVariante, 1)
            return
        }

        WawakusiApiClient.retrofitService.catalogoProductos().enqueue(object : Callback<List<CatalogoProductoResponse>> {
            override fun onResponse(
                call: Call<List<CatalogoProductoResponse>>,
                response: Response<List<CatalogoProductoResponse>>
            ) {
                if (!response.isSuccessful) {
                    AppMensaje.enviarMensaje(binding.root, "No se pudo obtener el producto.", TipoMensaje.ERROR)
                    return
                }
                val full = response.body()?.firstOrNull { it.id == p.id }
                val v = full?.variantes?.firstOrNull { it.stock == null || it.stock > 0 } ?: full?.variantes?.firstOrNull()
                val idVar = v?.idVariante
                if (idVar == null) {
                    AppMensaje.enviarMensaje(binding.root, "No hay variante disponible para este producto.", TipoMensaje.ADVERTENCIA)
                    return
                }
                if (v.stock != null && v.stock <= 0) {
                    AppMensaje.enviarMensaje(binding.root, "Producto sin stock.", TipoMensaje.ADVERTENCIA)
                    return
                }
                carritoViewModel.agregarItem(idVar, 1)
            }

            override fun onFailure(call: Call<List<CatalogoProductoResponse>>, t: Throwable) {
                AppMensaje.enviarMensaje(binding.root, "No se pudo conectar con el servidor.", TipoMensaje.ERROR)
            }
        })
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
