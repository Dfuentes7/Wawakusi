package com.example.wawakusi.view.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.wawakusi.R
import com.example.wawakusi.data.api.WawakusiApiClient
import com.example.wawakusi.data.api.response.CatalogoProductoResponse
import com.example.wawakusi.data.api.response.VistasResponse
import com.example.wawakusi.databinding.ActivityDetalleProductoBinding
import com.example.wawakusi.util.AppMensaje
import com.example.wawakusi.util.MenuDinamico
import com.example.wawakusi.util.SharedPreferencesManager
import com.example.wawakusi.util.TipoMensaje
import com.example.wawakusi.viewmodel.CarritoViewModel
import com.example.wawakusi.workers.RecordatorioWorker
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import java.util.Locale
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DetalleProductoActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var binding: ActivityDetalleProductoBinding

    private lateinit var carritoViewModel: CarritoViewModel

    private var producto: CatalogoProductoResponse? = null
    private var varianteIdSeleccionada: Int? = null
    private var stockSeleccionado: Int? = null
    private var cantidad: Int = 1

    private val sesionExpiradaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            AppMensaje.enviarMensaje(navigationView, "Tu sesión expiró. Inicia sesión nuevamente.", TipoMensaje.ADVERTENCIA)
            irALogin()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalleProductoBinding.inflate(layoutInflater)
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
        carritoViewModel.agregarItemResponse.observe(this, Observer { resp ->
            binding.btnAgregarCarrito.isEnabled = true
            val msg = resp?.message ?: resp?.mensaje ?: "Operación realizada."
            if (resp?.rpta == true || (resp?.message != null && !msg.contains("No se pudo"))) {
                AppMensaje.enviarMensaje(binding.root, msg, TipoMensaje.CORRECTO)
                try {
                    solicitarPermisoNotificaciones()
                    SharedPreferencesManager.guardarCartCount(maxOf(SharedPreferencesManager.obtenerCartCount(), 1))
                    RecordatorioWorker.notifyCartNow(this@DetalleProductoActivity)
                    RecordatorioWorker.scheduleCartReminder(this@DetalleProductoActivity, 1)
                    carritoViewModel.obtenerMiCarrito()
                    carritoViewModel.carritoResponse.observe(this, Observer { c ->
                        if (c != null && c.rpta) {
                            val count = c.items.sumOf { it.cantidad }
                            MenuDinamico.actualizarBadgeCarrito(navigationView, count)
                        }
                    })
                } catch (_: Exception) {}
            } else {
                AppMensaje.enviarMensaje(binding.root, msg, TipoMensaje.ERROR)
            }
        })

        val json = intent.getStringExtra("producto_json")
        producto = if (json.isNullOrBlank()) null else Gson().fromJson(json, CatalogoProductoResponse::class.java)
        if (producto == null) {
            AppMensaje.enviarMensaje(binding.root, "No se pudo cargar el producto.", TipoMensaje.ERROR)
            finish()
            return
        }

        renderProducto(producto!!)
        if (producto?.variantes.isNullOrEmpty()) {
            cargarProductoCompleto(producto!!.id)
        }

        binding.btnMas.setOnClickListener {
            val max = stockSeleccionado
            val next = cantidad + 1
            if (max != null && next > max) return@setOnClickListener
            cantidad = next
            binding.tvCantidad.text = cantidad.toString()
        }
        binding.btnMenos.setOnClickListener {
            val next = cantidad - 1
            if (next < 1) return@setOnClickListener
            cantidad = next
            binding.tvCantidad.text = cantidad.toString()
        }
        // La variante se mostrará como texto; sin desplegable
        binding.btnAgregarCarrito.setOnClickListener {
            agregarAlCarrito()
        }
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
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

    private fun cargarProductoCompleto(idProducto: Int) {
        WawakusiApiClient.retrofitService.catalogoProductos().enqueue(object : Callback<List<CatalogoProductoResponse>> {
            override fun onResponse(
                call: Call<List<CatalogoProductoResponse>>,
                response: Response<List<CatalogoProductoResponse>>
            ) {
                if (!response.isSuccessful) return
                val p = response.body()?.firstOrNull { it.id == idProducto } ?: return
                producto = p
                renderProducto(p)
            }

            override fun onFailure(call: Call<List<CatalogoProductoResponse>>, t: Throwable) {}
        })
    }

    private fun renderProducto(p: CatalogoProductoResponse) {
        binding.tvNombre.text = p.nombre
        val descripcion = p.descripcion?.trim().orEmpty()
        binding.tvDescripcion.text = if (descripcion.isNotBlank()) descripcion else "Sin descripción."

        val precioFinal = p.precioFinal
        val precioBase = p.precioBase
        val porcentaje = p.descuento?.porcentaje
        binding.tvPrecioFinal.text = when {
            precioFinal != null -> "$${String.format(Locale.getDefault(), "%.2f", precioFinal)}"
            precioBase != null -> "$${String.format(Locale.getDefault(), "%.2f", precioBase)}"
            else -> "Precio no disponible"
        }
        try {
            val badge = binding.tvBadgePrecio
            val txt = when {
                precioFinal != null -> "$${String.format(Locale.getDefault(), "%.2f", precioFinal)}"
                precioBase != null -> "$${String.format(Locale.getDefault(), "%.2f", precioBase)}"
                else -> null
            }
            if (txt != null) {
                badge.text = txt
                badge.visibility = View.VISIBLE
            } else {
                badge.visibility = View.GONE
            }
        } catch (_: Exception) {}
        if (precioFinal != null && precioBase != null && porcentaje != null) {
            binding.tvPrecioAntes.visibility = View.VISIBLE
            binding.tvPrecioAntes.text = "Antes $${String.format(Locale.getDefault(), "%.2f", precioBase)}"
            binding.tvPrecioAntes.paintFlags =
                binding.tvPrecioAntes.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            binding.tvChipDescuento.visibility = View.VISIBLE
            binding.tvChipDescuento.text = "-${String.format(Locale.getDefault(), "%.0f", porcentaje)}%"
        } else {
            binding.tvPrecioAntes.visibility = View.GONE
            binding.tvChipDescuento.visibility = View.GONE
        }

        Glide.with(this)
            .load(p.imagen)
            .placeholder(R.drawable.logo)
            .error(R.drawable.logo)
            .into(binding.ivProducto)

        val variantes = p.variantes
        if (variantes.isEmpty()) {
            binding.cardVariante.visibility = View.GONE
            varianteIdSeleccionada = null
            stockSeleccionado = null
            return
        }
        val v = variantes.first()
        val talla = v.talla?.trim().orEmpty().ifBlank { "Única" }
        val color = v.color?.trim().orEmpty().ifBlank { "Sin color" }
        val precio = v.precio ?: p.precioFinal ?: p.precioBase
        val precioTxt = if (precio != null) "$${String.format(Locale.getDefault(), "%.2f", precio)}" else "-"
        val stockTxt = v.stock?.toString() ?: "-"
        binding.tvVarLinea1.text = "$talla · $color"
        binding.tvVarLinea2.text = "Precio: $precioTxt · Stock: $stockTxt"
        varianteIdSeleccionada = v.idVariante
        stockSeleccionado = v.stock
        actualizarStockUI(v.stock)

        // Variante mostrada como información (no editable)
    }

    private fun agregarAlCarrito() {
        if (!SharedPreferencesManager.estaAutenticado()) {
            AppMensaje.enviarMensaje(binding.root, "Inicia sesión para agregar al carrito.", TipoMensaje.ADVERTENCIA)
            irALogin()
            return
        }

        val varianteId = varianteIdSeleccionada
        if (varianteId == null) {
            AppMensaje.enviarMensaje(binding.root, "No hay variante disponible para este producto.", TipoMensaje.ADVERTENCIA)
            return
        }

        val max = stockSeleccionado
        if (max != null && cantidad > max) {
            AppMensaje.enviarMensaje(binding.root, "Stock insuficiente.", TipoMensaje.ADVERTENCIA)
            return
        }

        binding.btnAgregarCarrito.isEnabled = false
        carritoViewModel.agregarItem(varianteId, cantidad)
    }

    private fun actualizarStockUI(stock: Int?) {
        if (stock == null) {
            binding.tvStock.visibility = View.GONE
            return
        }
        binding.tvStock.visibility = View.VISIBLE
        binding.tvStock.text = "Stock disponible: $stock"
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
}
