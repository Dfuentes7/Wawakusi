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
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.wawakusi.data.api.response.ProductResponse
import com.example.wawakusi.data.api.response.VistasResponse
import com.example.wawakusi.databinding.ActivityAdminProductosBinding
import com.example.wawakusi.util.AppMensaje
import com.example.wawakusi.util.MenuDinamico
import com.example.wawakusi.util.SharedPreferencesManager
import com.example.wawakusi.util.TipoMensaje
import com.example.wawakusi.viewmodel.ProductViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminProductosActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var binding: ActivityAdminProductosBinding
    private lateinit var productViewModel: ProductViewModel

    private var imagenUri: Uri? = null
    private var imagenBytes: ByteArray? = null
    private var imagenMime: String? = null

    private var textoGuardarOriginal: CharSequence? = null
    private var productoEditando: ProductResponse? = null
    private var idEliminando: Int? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        imagenUri = uri

        imagenMime = contentResolver.getType(uri) ?: "image/*"
        imagenBytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
        binding.tvImagenSeleccionada.text =
            if (imagenBytes != null) "Imagen seleccionada" else "Sin imagen seleccionada"

        binding.ivPreviewImagen.setImageURI(uri)
        binding.ivPreviewImagen.visibility = View.VISIBLE
    }

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

        binding = ActivityAdminProductosBinding.inflate(layoutInflater)
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
        productViewModel.productosResponse.observe(this, Observer { list ->
            renderListaProductos(list ?: emptyList())
        })
        productViewModel.crearProductoResponse.observe(this, Observer { resp ->
            binding.btnGuardarProducto.isEnabled = true
            textoGuardarOriginal?.let { binding.btnGuardarProducto.text = it }
            val msg = resp?.message ?: resp?.mensaje ?: "Operación realizada."
            if (resp?.rpta == true || (resp?.message != null && !msg.contains("No se pudo"))) {
                AppMensaje.enviarMensaje(binding.root, msg, TipoMensaje.CORRECTO)
                salirEdicion()
                setLoadingProductos(true)
                productViewModel.listarProductos()
            } else {
                AppMensaje.enviarMensaje(binding.root, msg, TipoMensaje.ERROR)
            }
        })
        productViewModel.actualizarProductoResponse.observe(this, Observer { resp ->
            binding.btnGuardarProducto.isEnabled = true
            binding.btnGuardarProducto.text = if (productoEditando != null) {
                "Actualizar producto"
            } else {
                textoGuardarOriginal ?: "Registrar producto"
            }
            val msg = resp?.message ?: resp?.mensaje ?: "Operación realizada."
            if (resp?.rpta == true || (resp?.message != null && !msg.contains("No se pudo"))) {
                AppMensaje.enviarMensaje(binding.root, msg, TipoMensaje.CORRECTO)
                salirEdicion()
                setLoadingProductos(true)
                productViewModel.listarProductos()
            } else {
                AppMensaje.enviarMensaje(binding.root, msg, TipoMensaje.ERROR)
            }
        })
        productViewModel.eliminarProductoResponse.observe(this, Observer { resp ->
            val msg = resp?.message ?: resp?.mensaje ?: "Operación realizada."
            if (resp?.rpta == true || (resp?.message != null && !msg.contains("No se pudo"))) {
                AppMensaje.enviarMensaje(binding.root, msg, TipoMensaje.CORRECTO)
                val idEliminado = idEliminando
                if (idEliminado != null && productoEditando?.id == idEliminado) {
                    salirEdicion()
                }
                idEliminando = null
                setLoadingProductos(true)
                productViewModel.listarProductos()
            } else {
                AppMensaje.enviarMensaje(binding.root, msg, TipoMensaje.ERROR)
            }
        })

        textoGuardarOriginal = binding.btnGuardarProducto.text
        setLoadingProductos(true)
        binding.btnNuevoProducto.setOnClickListener {
            toggleFormularioProducto()
        }
        binding.btnSeleccionarImagen.setOnClickListener {
            pickImage.launch("image/*")
        }
        binding.btnGuardarProducto.setOnClickListener {
            registrarProducto()
        }

        productViewModel.listarProductos()
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

    private fun renderListaProductos(productos: List<ProductResponse>) {
        setLoadingProductos(false)

        val container = binding.listaProductosContainer
        container.removeAllViews()

        binding.tvProductosCount.text = "(${productos.size})"
        binding.tvProductosVacio.visibility = if (productos.isEmpty()) View.VISIBLE else View.GONE

        if (productos.isEmpty()) {
            return
        }

        for (p in productos) {
            container.addView(crearTarjetaProducto(p))
        }
    }

    private fun registrarProducto() {
        val nombre = binding.etNombre.text?.toString()?.trim().orEmpty()
        val precio = binding.etPrecio.text?.toString()?.trim().orEmpty()
        val cantidad = binding.etCantidad.text?.toString()?.trim().orEmpty()
        val descripcion = binding.etDescripcion.text?.toString()?.trim().orEmpty()
        val bytes = imagenBytes
        val mime = imagenMime
        val editando = productoEditando

        if (nombre.isBlank() || precio.isBlank() || cantidad.isBlank()) {
            AppMensaje.enviarMensaje(binding.root, "Complete nombre, precio y cantidad.", TipoMensaje.ADVERTENCIA)
            binding.btnGuardarProducto.isEnabled = true
            return
        }

        binding.btnGuardarProducto.isEnabled = false
        if (editando == null) {
            if (bytes == null || mime.isNullOrBlank()) {
                AppMensaje.enviarMensaje(binding.root, "Seleccione una imagen para registrar el producto.", TipoMensaje.ADVERTENCIA)
                binding.btnGuardarProducto.isEnabled = true
                return
            }
            binding.btnGuardarProducto.text = "Registrando..."
            productViewModel.crearProducto(nombre, precio, cantidad, descripcion, bytes, mime)
            return
        }

        binding.btnGuardarProducto.text = "Actualizando..."
        productViewModel.actualizarProducto(editando.id, nombre, precio, cantidad, descripcion, bytes, mime)
    }

    private fun limpiarFormulario() {
        binding.etNombre.setText("")
        binding.etPrecio.setText("")
        binding.etCantidad.setText("")
        binding.etDescripcion.setText("")
        imagenUri = null
        imagenBytes = null
        imagenMime = null
        binding.tvImagenSeleccionada.text = "Sin imagen seleccionada"
        binding.ivPreviewImagen.setImageDrawable(null)
        binding.ivPreviewImagen.visibility = View.GONE
    }

    private fun iniciarEdicion(p: ProductResponse) {
        productoEditando = p
        mostrarFormularioProducto(true)
        binding.etNombre.setText(p.nombre)
        binding.etPrecio.setText(p.precio)
        binding.etCantidad.setText(p.cantidad)
        binding.etDescripcion.setText(p.descripcion)

        imagenUri = null
        imagenBytes = null
        imagenMime = null

        binding.tvImagenSeleccionada.text = if (p.imagen.isNotBlank()) "Imagen actual" else "Sin imagen seleccionada"
        if (p.imagen.isNotBlank()) {
            binding.ivPreviewImagen.visibility = View.VISIBLE
            Glide.with(this).load(p.imagen).into(binding.ivPreviewImagen)
        } else {
            binding.ivPreviewImagen.setImageDrawable(null)
            binding.ivPreviewImagen.visibility = View.GONE
        }

        binding.btnGuardarProducto.text = "Actualizar producto"
        AppMensaje.enviarMensaje(binding.root, "Editando producto #${p.id}", TipoMensaje.INFORMACION)
    }

    private fun salirEdicion() {
        productoEditando = null
        binding.btnGuardarProducto.text = textoGuardarOriginal ?: "Registrar producto"
        limpiarFormulario()
        mostrarFormularioProducto(false)
    }

    private fun toggleFormularioProducto() {
        val visible = binding.cardFormularioProducto.visibility == View.VISIBLE
        if (visible && productoEditando == null) {
            limpiarFormulario()
            mostrarFormularioProducto(false)
            return
        }
        mostrarFormularioProducto(!visible)
    }

    private fun mostrarFormularioProducto(mostrar: Boolean) {
        binding.cardFormularioProducto.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.cardListaProductos.visibility = if (mostrar) View.GONE else View.VISIBLE
        binding.btnNuevoProducto.text = if (mostrar && productoEditando == null) "Ocultar formulario" else "Nuevo producto"
    }

    private fun setLoadingProductos(cargando: Boolean) {
        binding.pbProductos.visibility = if (cargando) View.VISIBLE else View.GONE
        if (cargando) {
            binding.tvProductosVacio.visibility = View.GONE
            binding.tvProductosCount.text = ""
        }
    }

    private fun crearTarjetaProducto(p: ProductResponse): MaterialCardView {
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
        card.setOnClickListener {
            val descripcion = p.descripcion.trim()
            if (descripcion.isNotBlank()) {
                AppMensaje.enviarMensaje(binding.root, descripcion, TipoMensaje.INFORMACION)
            } else {
                AppMensaje.enviarMensaje(binding.root, "Sin descripción.", TipoMensaje.INFORMACION)
            }
        }

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
        tvTitulo.text = p.nombre
        tvTitulo.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvTitulo.setTextSize(16f)
        tvTitulo.setTypeface(tvTitulo.typeface, android.graphics.Typeface.BOLD)

        val tvMeta = TextView(this)
        tvMeta.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        tvMeta.text = "S/ ${p.precio} · Stock: ${p.cantidad} · ID: ${p.id}"
        tvMeta.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvMeta.alpha = 0.75f
        tvMeta.setTextSize(13f)

        val descripcion = p.descripcion.trim()
        val tvDescripcion = TextView(this)
        tvDescripcion.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        tvDescripcion.text = if (descripcion.isNotBlank()) descripcion else "Toca para ver detalles."
        tvDescripcion.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvDescripcion.alpha = if (descripcion.isNotBlank()) 0.85f else 0.6f
        tvDescripcion.setTextSize(13f)
        tvDescripcion.maxLines = 2

        val acciones = LinearLayout(this)
        acciones.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        acciones.orientation = LinearLayout.HORIZONTAL
        acciones.setPadding(0, dp(10), 0, 0)

        val btnEditar = MaterialButton(this)
        btnEditar.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(8)
        }
        btnEditar.text = "Editar"
        btnEditar.isAllCaps = false
        btnEditar.setBackgroundColor(ContextCompat.getColor(this, R.color.lavender))
        btnEditar.setTextColor(ContextCompat.getColor(this, R.color.black))
        btnEditar.setOnClickListener {
            iniciarEdicion(p)
        }

        val btnEliminar = MaterialButton(this)
        btnEliminar.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        btnEliminar.text = "Eliminar"
        btnEliminar.isAllCaps = false
        btnEliminar.setBackgroundColor(ContextCompat.getColor(this, R.color.naranja))
        btnEliminar.setTextColor(ContextCompat.getColor(this, R.color.blanco))
        btnEliminar.setOnClickListener {
            confirmarEliminacion(p)
        }

        acciones.addView(btnEditar)
        acciones.addView(btnEliminar)

        content.addView(tvTitulo)
        content.addView(tvMeta)
        content.addView(tvDescripcion)
        content.addView(acciones)
        card.addView(content)
        return card
    }

    private fun confirmarEliminacion(p: ProductResponse) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar producto")
            .setMessage("¿Deseas eliminar “${p.nombre}”?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                idEliminando = p.id
                setLoadingProductos(true)
                productViewModel.eliminarProducto(p.id)
            }
            .show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            MenuDinamico.ITEM_ADMIN_DASHBOARD -> startActivity(Intent(this, AdminPanelActivity::class.java))
            MenuDinamico.ITEM_ADMIN_PRODUCTOS -> {}
            MenuDinamico.ITEM_ADMIN_PEDIDOS -> AppMensaje.enviarMensaje(binding.root, "Función en construcción: Gestionar pedidos", TipoMensaje.INFORMACION)
            MenuDinamico.ITEM_ADMIN_REPORTES -> AppMensaje.enviarMensaje(binding.root, "Función en construcción: Visualizar reportes", TipoMensaje.INFORMACION)
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
}
