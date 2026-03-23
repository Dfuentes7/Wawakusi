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
import com.example.wawakusi.data.api.request.ActualizarUsuarioAdminRequest
import com.example.wawakusi.data.api.request.CrearUsuarioAdminRequest
import com.example.wawakusi.data.api.response.RolResponse
import com.example.wawakusi.data.api.response.UsuarioRolResponse
import com.example.wawakusi.data.api.response.VistasResponse
import com.example.wawakusi.databinding.ActivityAdminUsuariosRolesBinding
import com.example.wawakusi.util.AppMensaje
import com.example.wawakusi.util.MenuDinamico
import com.example.wawakusi.util.SharedPreferencesManager
import com.example.wawakusi.util.TipoMensaje
import com.example.wawakusi.viewmodel.RolViewModel
import com.example.wawakusi.viewmodel.UsuarioAdminViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminUsuariosRolesActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var binding: ActivityAdminUsuariosRolesBinding
    private lateinit var usuarioViewModel: UsuarioAdminViewModel
    private lateinit var rolViewModel: RolViewModel

    private var roles: List<RolResponse> = emptyList()
    private var idUsuarioEditando: Int? = null

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

        binding = ActivityAdminUsuariosRolesBinding.inflate(layoutInflater)
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

        usuarioViewModel = ViewModelProvider(this).get(UsuarioAdminViewModel::class.java)
        rolViewModel = ViewModelProvider(this).get(RolViewModel::class.java)

        usuarioViewModel.usuariosRolesResponse.observe(this, Observer { list ->
            renderUsuarios(list ?: emptyList())
        })
        usuarioViewModel.crearUsuarioResponse.observe(this, Observer { resp ->
            binding.btnGuardarUsuario.isEnabled = true
            val msg = resp?.message ?: resp?.mensaje ?: "Operación realizada."
            if (resp?.rpta == true || (resp?.message != null && !msg.contains("No se pudo"))) {
                AppMensaje.enviarMensaje(binding.root, msg, TipoMensaje.CORRECTO)
                limpiarFormulario()
                mostrarFormulario(false)
                setLoadingUsuarios(true)
                usuarioViewModel.listarUsuariosAdmin()
            } else {
                AppMensaje.enviarMensaje(binding.root, msg, TipoMensaje.ERROR)
            }
        })
        usuarioViewModel.actualizarUsuarioResponse.observe(this, Observer { resp ->
            binding.btnGuardarUsuario.isEnabled = true
            val msg = resp?.message ?: resp?.mensaje ?: "Operación realizada."
            if (resp?.rpta == true || (resp?.message != null && !msg.contains("No se pudo"))) {
                AppMensaje.enviarMensaje(binding.root, msg, TipoMensaje.CORRECTO)
                limpiarFormulario()
                mostrarFormulario(false)
                setLoadingUsuarios(true)
                usuarioViewModel.listarUsuariosAdmin()
            } else {
                AppMensaje.enviarMensaje(binding.root, msg, TipoMensaje.ERROR)
            }
        })

        rolViewModel.rolesResponse.observe(this, Observer { list ->
            roles = list ?: emptyList()
            actualizarSelectRoles(roles)
        })

        binding.btnNuevoUsuario.setOnClickListener {
            if (binding.cardFormularioUsuario.visibility == View.VISIBLE) {
                limpiarFormulario()
                mostrarFormulario(false)
            } else {
                iniciarRegistro()
            }
        }
        binding.btnCancelarUsuario.setOnClickListener {
            limpiarFormulario()
            mostrarFormulario(false)
        }
        binding.btnGuardarUsuario.setOnClickListener {
            guardarUsuario()
        }
        binding.actRol.setOnClickListener {
            binding.actRol.requestFocus()
            binding.actRol.showDropDown()
        }

        setLoadingUsuarios(true)
        usuarioViewModel.listarUsuariosAdmin()
        rolViewModel.listarRolesAdmin()
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

    private fun setLoadingUsuarios(cargando: Boolean) {
        binding.pbUsuarios.visibility = if (cargando) View.VISIBLE else View.GONE
        if (cargando) {
            binding.tvUsuariosVacio.visibility = View.GONE
            binding.tvUsuariosCount.text = ""
        }
    }

    private fun mostrarFormulario(mostrar: Boolean) {
        binding.cardFormularioUsuario.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.cardListaUsuarios.visibility = if (mostrar) View.GONE else View.VISIBLE
        binding.btnNuevoUsuario.text = if (mostrar) "Ocultar formulario" else "Nuevo usuario"
    }

    private fun iniciarRegistro() {
        idUsuarioEditando = null
        binding.tvTituloFormulario.text = "Registrar usuario"
        binding.etUsuarioPassword.hint = "Contraseña"
        limpiarFormulario()
        mostrarFormulario(true)
    }

    private fun iniciarEdicion(u: UsuarioRolResponse) {
        idUsuarioEditando = u.idUsuario
        binding.tvTituloFormulario.text = "Editar usuario"
        binding.etUsuarioLogin.setText(u.usuario)
        binding.etUsuarioPassword.setText("")
        binding.etUsuarioPassword.hint = "Contraseña (opcional)"
        binding.swUsuarioActivo.isChecked = u.estado == 1

        val rolSeleccionado = when {
            u.rolId != null -> roles.firstOrNull { it.idRol == u.rolId }
            !u.rolNombre.isNullOrBlank() -> roles.firstOrNull { it.nombre == u.rolNombre }
            else -> null
        }
        binding.actRol.setText(
            if (rolSeleccionado != null) "${rolSeleccionado.idRol} · ${rolSeleccionado.nombre}" else "",
            false
        )

        mostrarFormulario(true)
    }

    private fun actualizarSelectRoles(roles: List<RolResponse>) {
        val items = roles.map { "${it.idRol} · ${it.nombre}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        binding.actRol.setAdapter(adapter)
    }

    private fun obtenerRolIdSeleccionado(): Int? {
        val txt = binding.actRol.text?.toString()?.trim().orEmpty()
        if (txt.isBlank()) return null
        return txt.substringBefore("·").trim().toIntOrNull()
    }

    private fun guardarUsuario() {
        val login = binding.etUsuarioLogin.text?.toString()?.trim()?.lowercase().orEmpty()
        val password = binding.etUsuarioPassword.text?.toString().orEmpty()
        val rolId = obtenerRolIdSeleccionado()
        val estado = if (binding.swUsuarioActivo.isChecked) 1 else 0
        val editando = idUsuarioEditando

        if (login.isBlank() || rolId == null) {
            AppMensaje.enviarMensaje(binding.root, "Complete usuario y seleccione un rol.", TipoMensaje.ADVERTENCIA)
            return
        }

        if (editando == null) {
            if (password.length < 6) {
                AppMensaje.enviarMensaje(binding.root, "La contraseña debe tener al menos 6 caracteres.", TipoMensaje.ADVERTENCIA)
                return
            }
            binding.btnGuardarUsuario.isEnabled = false
            usuarioViewModel.crearUsuarioAdmin(
                CrearUsuarioAdminRequest(
                    usuario = login,
                    password = password,
                    rolId = rolId,
                    estado = estado
                )
            )
            return
        }

        binding.btnGuardarUsuario.isEnabled = false
        usuarioViewModel.actualizarUsuarioAdmin(
            editando,
            ActualizarUsuarioAdminRequest(
                usuario = login,
                password = password.ifBlank { null },
                rolId = rolId,
                estado = estado
            )
        )
    }

    private fun limpiarFormulario() {
        binding.etUsuarioLogin.setText("")
        binding.etUsuarioPassword.setText("")
        binding.actRol.setText("", false)
        binding.swUsuarioActivo.isChecked = true
    }

    private fun renderUsuarios(usuarios: List<UsuarioRolResponse>) {
        setLoadingUsuarios(false)
        val container = binding.usuariosContainer
        container.removeAllViews()

        binding.tvUsuariosCount.text = "(${usuarios.size})"
        binding.tvUsuariosVacio.visibility = if (usuarios.isEmpty()) View.VISIBLE else View.GONE
        if (usuarios.isEmpty()) return

        for (u in usuarios) {
            container.addView(crearTarjetaUsuario(u))
        }
    }

    private fun crearTarjetaUsuario(u: UsuarioRolResponse): MaterialCardView {
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

        val tvUsuario = TextView(this)
        tvUsuario.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        tvUsuario.text = u.usuario
        tvUsuario.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvUsuario.setTextSize(16f)
        tvUsuario.setTypeface(tvUsuario.typeface, android.graphics.Typeface.BOLD)

        val rol = u.rolNombre ?: "SIN ROL"
        val estado = if (u.estado == 1) "Activo" else "Inactivo"

        val tvDetalle = TextView(this)
        tvDetalle.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        tvDetalle.text = "Rol: $rol · Estado: $estado · ID: ${u.idUsuario}"
        tvDetalle.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvDetalle.alpha = 0.75f
        tvDetalle.setTextSize(13f)

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
        btnEditar.setOnClickListener { iniciarEdicion(u) }

        val btnEstado = MaterialButton(this)
        btnEstado.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        val activar = u.estado != 1
        btnEstado.text = if (activar) "Activar" else "Desactivar"
        btnEstado.isAllCaps = false
        btnEstado.setBackgroundColor(ContextCompat.getColor(this, R.color.naranja))
        btnEstado.setTextColor(ContextCompat.getColor(this, R.color.blanco))
        btnEstado.setOnClickListener {
            setLoadingUsuarios(true)
            usuarioViewModel.actualizarUsuarioAdmin(
                u.idUsuario,
                ActualizarUsuarioAdminRequest(estado = if (activar) 1 else 0)
            )
        }

        acciones.addView(btnEditar)
        acciones.addView(btnEstado)

        content.addView(tvUsuario)
        content.addView(tvDetalle)
        content.addView(acciones)
        card.addView(content)
        return card
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            MenuDinamico.ITEM_ADMIN_DASHBOARD -> startActivity(Intent(this, AdminPanelActivity::class.java))
            MenuDinamico.ITEM_ADMIN_PRODUCTOS -> startActivity(Intent(this, AdminProductosActivity::class.java))
            MenuDinamico.ITEM_ADMIN_PROMOCIONES -> startActivity(Intent(this, AdminPromocionesActivity::class.java))
            MenuDinamico.ITEM_ADMIN_USUARIOS -> {}
            MenuDinamico.ITEM_ADMIN_PEDIDOS -> AppMensaje.enviarMensaje(binding.root, "Función en construcción: Gestionar pedidos", TipoMensaje.INFORMACION)
            MenuDinamico.ITEM_ADMIN_REPORTES -> AppMensaje.enviarMensaje(binding.root, "Función en construcción: Visualizar reportes", TipoMensaje.INFORMACION)
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
