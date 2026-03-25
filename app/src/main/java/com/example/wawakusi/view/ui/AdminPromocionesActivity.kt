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
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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
import com.example.wawakusi.data.api.request.CrearDescuentoRequest
import com.example.wawakusi.data.api.response.CatalogoProductoResponse
import com.example.wawakusi.data.api.response.PromocionAdminResponse
import com.example.wawakusi.data.api.response.VistasResponse
import com.example.wawakusi.databinding.ActivityAdminPromocionesBinding
import com.example.wawakusi.util.AppMensaje
import com.example.wawakusi.util.MenuDinamico
import com.example.wawakusi.util.SharedPreferencesManager
import com.example.wawakusi.util.TipoMensaje
import com.example.wawakusi.viewmodel.DescuentoViewModel
import com.example.wawakusi.viewmodel.ProductViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AdminPromocionesActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var binding: ActivityAdminPromocionesBinding
    private lateinit var productViewModel: ProductViewModel
    private lateinit var descuentoViewModel: DescuentoViewModel

    private var productosCatalogo: List<CatalogoProductoResponse> = emptyList()
    private var idPromocionEliminando: Int? = null
    private var estadoObjetivoPromocion: Int? = null

    private val sesionExpiradaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            AppMensaje.enviarMensaje(binding.root, "Tu sesión expiró. Inicia sesión nuevamente.", TipoMensaje.ADVERTENCIA)
            irALogin()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SharedPreferencesManager.obtenerRol() != "ADMIN") {
            irALogin()
            return
        }

        binding = ActivityAdminPromocionesBinding.inflate(layoutInflater)
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

        productViewModel = ViewModelProvider(this).get(ProductViewModel::class.java)
        descuentoViewModel = ViewModelProvider(this).get(DescuentoViewModel::class.java)
        productViewModel.catalogoProductosResponse.observe(this, Observer { list ->
            productosCatalogo = list ?: emptyList()
            actualizarAutoCompleteProductos(productosCatalogo)
        })
        descuentoViewModel.promocionesAdminResponse.observe(this, Observer { list ->
            renderPromocionesAdmin(list ?: emptyList())
        })
        descuentoViewModel.crearPromocionAdminResponse.observe(this, Observer { resp ->
            binding.btnGuardarPromocion.isEnabled = true
            val msg = resp?.message ?: resp?.mensaje ?: "Operación realizada."
            if (resp?.rpta == true || (resp?.message != null && !msg.contains("No se pudo"))) {
                AppMensaje.enviarMensaje(binding.root, msg, TipoMensaje.CORRECTO)
                limpiarFormulario()
                mostrarFormulario(false)
                cargarPromociones()
            } else {
                AppMensaje.enviarMensaje(binding.root, msg, TipoMensaje.ERROR)
            }
        })
        descuentoViewModel.eliminarPromocionAdminResponse.observe(this, Observer { resp ->
            val msg = resp?.message ?: resp?.mensaje ?: "Operación realizada."
            if (resp?.rpta == true || (resp?.message != null && !msg.contains("No se pudo"))) {
                AppMensaje.enviarMensaje(binding.root, msg, TipoMensaje.CORRECTO)
                idPromocionEliminando = null
                cargarPromociones()
            } else {
                AppMensaje.enviarMensaje(binding.root, msg, TipoMensaje.ERROR)
            }
        })
        descuentoViewModel.actualizarEstadoPromocionAdminResponse.observe(this, Observer { resp ->
            val msg = resp?.message ?: resp?.mensaje ?: "Operación realizada."
            if (resp?.rpta == true || (resp?.message != null && !msg.contains("No se pudo"))) {
                AppMensaje.enviarMensaje(binding.root, msg, TipoMensaje.CORRECTO)
                idPromocionEliminando = null
                estadoObjetivoPromocion = null
                cargarPromociones()
            } else {
                AppMensaje.enviarMensaje(binding.root, msg, TipoMensaje.ERROR)
            }
        })

        binding.btnNuevaPromocion.setOnClickListener {
            val visible = binding.cardFormularioPromocion.visibility == View.VISIBLE
            if (visible) limpiarFormulario()
            mostrarFormulario(!visible)
        }
        binding.btnCancelarPromocion.setOnClickListener {
            limpiarFormulario()
            mostrarFormulario(false)
        }
        binding.btnGuardarPromocion.setOnClickListener {
            registrarPromocion()
        }
        binding.etPromoInicio.setOnClickListener {
            mostrarSelectorFecha("fechaInicio") { selected ->
                binding.etPromoInicio.setText(selected)
            }
        }
        binding.etPromoFin.setOnClickListener {
            mostrarSelectorFecha("fechaFin") { selected ->
                binding.etPromoFin.setText(selected)
            }
        }
        binding.actProducto.setOnClickListener {
            binding.actProducto.requestFocus()
            binding.actProducto.showDropDown()
        }

        mostrarFormulario(false)
        setLoadingPromos(true)
        productViewModel.listarCatalogoProductos()
        cargarPromociones()
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
                val body = response.body()
                if (body != null) MenuDinamico.aplicarMenuSesion(navigationView, body)
            }

            override fun onFailure(call: Call<VistasResponse>, t: Throwable) {}
        })
    }

    private fun cargarPromociones() {
        setLoadingPromos(true)
        descuentoViewModel.listarPromocionesAdmin()
    }

    private fun mostrarFormulario(mostrar: Boolean) {
        binding.cardFormularioPromocion.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.cardListaPromos.visibility = if (mostrar) View.GONE else View.VISIBLE
        binding.btnNuevaPromocion.text = if (mostrar) "Ocultar formulario" else "Nueva promoción"
    }

    private fun setLoadingPromos(cargando: Boolean) {
        binding.pbPromosAdmin.visibility = if (cargando) View.VISIBLE else View.GONE
        if (cargando) {
            binding.tvPromosAdminVacio.visibility = View.GONE
            binding.tvPromosCount.text = ""
        }
    }

    private fun actualizarAutoCompleteProductos(productos: List<CatalogoProductoResponse>) {
        val items = productos.map { "${it.id} · ${it.nombre}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        binding.actProducto.setAdapter(adapter)
    }

    private fun mostrarSelectorFecha(tag: String, onSelected: (String) -> Unit) {
        val picker = MaterialDatePicker.Builder.datePicker().build()
        picker.addOnPositiveButtonClickListener { selection ->
            onSelected(formatearFecha(selection))
        }
        picker.show(supportFragmentManager, tag)
    }

    private fun formatearFecha(selectionUtcMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(selectionUtcMs))
    }

    private fun registrarPromocion() {
        val nombre = binding.etPromoNombre.text?.toString()?.trim().orEmpty()
        val porcentajeTxt = binding.etPromoPorcentaje.text?.toString()?.trim().orEmpty()
        val inicio = binding.etPromoInicio.text?.toString()?.trim().orEmpty()
        val fin = binding.etPromoFin.text?.toString()?.trim().orEmpty()
        val productoTxt = binding.actProducto.text?.toString()?.trim().orEmpty()
        val descripcion = binding.etPromoDescripcion.text?.toString()?.trim().orEmpty()

        val porcentaje = porcentajeTxt.replace(",", ".").toDoubleOrNull()
        val productoId = productoTxt.substringBefore("·").trim().toIntOrNull()

        if (nombre.isBlank() || porcentaje == null || inicio.isBlank() || fin.isBlank() || productoId == null) {
            AppMensaje.enviarMensaje(binding.root, "Complete nombre, porcentaje, fechas y producto.", TipoMensaje.ADVERTENCIA)
            return
        }
        if (porcentaje <= 0.0 || porcentaje > 100.0) {
            AppMensaje.enviarMensaje(binding.root, "El porcentaje debe ser mayor a 0 y menor o igual a 100.", TipoMensaje.ADVERTENCIA)
            return
        }

        binding.btnGuardarPromocion.isEnabled = false
        descuentoViewModel.crearPromocionAdmin(
            CrearDescuentoRequest(
                nombre = nombre,
                descripcion = descripcion.ifBlank { null },
                porcentaje = porcentaje,
                fechaInicio = inicio,
                fechaFin = fin,
                productoId = productoId
            )
        )
    }

    private fun renderPromocionesAdmin(promos: List<PromocionAdminResponse>) {
        setLoadingPromos(false)
        val activas = promos.filter { it.estado == 1 }
        val inactivas = promos.filter { it.estado != 1 }

        binding.promosContainer.removeAllViews()
        binding.promosInactivasContainer.removeAllViews()

        binding.tvPromosCount.text = "(${activas.size})"
        binding.tvPromosAdminVacio.visibility = if (activas.isEmpty()) View.VISIBLE else View.GONE
        for (p in activas) binding.promosContainer.addView(crearTarjetaPromocion(p))

        binding.tvPromosInactivasCount.text = "(${inactivas.size})"
        binding.tvPromosInactivasVacio.visibility = if (inactivas.isEmpty()) View.VISIBLE else View.GONE
        for (p in inactivas) binding.promosInactivasContainer.addView(crearTarjetaPromocion(p))
    }

    private fun crearTarjetaPromocion(p: PromocionAdminResponse): MaterialCardView {
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

        val tvTitulo = TextView(this)
        tvTitulo.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        tvTitulo.text = "${p.nombre} · ${p.porcentaje}%"
        tvTitulo.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvTitulo.setTextSize(16f)
        tvTitulo.setTypeface(tvTitulo.typeface, android.graphics.Typeface.BOLD)

        val tvProducto = TextView(this)
        tvProducto.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        tvProducto.text = "Producto: ${p.productoNombre ?: "Sin asignar"}"
        tvProducto.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvProducto.alpha = 0.85f
        tvProducto.setTextSize(13f)

        val tvFecha = TextView(this)
        tvFecha.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        tvFecha.text = "Vigencia: ${p.fechaInicio} → ${p.fechaFin}"
        tvFecha.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvFecha.alpha = 0.75f
        tvFecha.setTextSize(13f)

        val btnAccion = MaterialButton(this)
        btnAccion.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        btnAccion.isAllCaps = false
        val activar = p.estado != 1
        btnAccion.text = if (activar) "Activar" else "Desactivar"
        btnAccion.setBackgroundColor(
            ContextCompat.getColor(this, if (activar) R.color.lavender else R.color.naranja)
        )
        btnAccion.setTextColor(
            ContextCompat.getColor(this, if (activar) R.color.black else R.color.blanco)
        )
        btnAccion.setOnClickListener {
            confirmarCambioEstado(p, if (activar) 1 else 0)
        }

        content.addView(tvTitulo)
        content.addView(tvProducto)
        content.addView(tvFecha)
        content.addView(btnAccion)
        card.addView(content)
        return card
    }

    private fun confirmarCambioEstado(p: PromocionAdminResponse, estadoObjetivo: Int) {
        val accion = if (estadoObjetivo == 1) "Activar" else "Desactivar"
        val titulo = if (estadoObjetivo == 1) "Activar promoción" else "Desactivar promoción"
        val mensaje = if (estadoObjetivo == 1) {
            "¿Deseas activar la promoción “${p.nombre}”?"
        } else {
            "¿Deseas desactivar la promoción “${p.nombre}”?"
        }
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton(accion) { _, _ ->
                idPromocionEliminando = p.id
                estadoObjetivoPromocion = estadoObjetivo
                setLoadingPromos(true)
                descuentoViewModel.actualizarEstadoPromocionAdmin(p.id, estadoObjetivo)
            }
            .show()
    }

    private fun limpiarFormulario() {
        binding.etPromoNombre.setText("")
        binding.etPromoPorcentaje.setText("")
        binding.etPromoInicio.setText("")
        binding.etPromoFin.setText("")
        binding.actProducto.setText("")
        binding.etPromoDescripcion.setText("")
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            MenuDinamico.ITEM_ADMIN_DASHBOARD -> startActivity(Intent(this, AdminPanelActivity::class.java))
            MenuDinamico.ITEM_ADMIN_PRODUCTOS -> startActivity(Intent(this, AdminProductosActivity::class.java))
            MenuDinamico.ITEM_ADMIN_PEDIDOS -> startActivity(Intent(this, AdminPedidosActivity::class.java))
            MenuDinamico.ITEM_ADMIN_REPORTES -> startActivity(Intent(this, AdminReportesActivity::class.java))
            MenuDinamico.ITEM_ADMIN_PROMOCIONES -> {}
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

