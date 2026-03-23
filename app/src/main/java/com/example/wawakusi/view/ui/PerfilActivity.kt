package com.example.wawakusi.view.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.wawakusi.R
import com.example.wawakusi.data.api.request.UpdateMeRequest
import com.example.wawakusi.data.api.response.MeResponse
import com.example.wawakusi.data.api.response.UpdateMeResponse
import com.example.wawakusi.databinding.ActivityPerfilBinding
import com.example.wawakusi.util.AppMensaje
import com.example.wawakusi.util.MenuDinamico
import com.example.wawakusi.util.SharedPreferencesManager
import com.example.wawakusi.util.TipoMensaje
import com.example.wawakusi.viewmodel.UsuarioViewModel
import com.google.android.material.navigation.NavigationView
import com.example.wawakusi.data.api.WawakusiApiClient
import com.example.wawakusi.data.api.response.VistasResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PerfilActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var binding: ActivityPerfilBinding
    private lateinit var usuarioViewModel: UsuarioViewModel

    private val sesionExpiradaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            AppMensaje.enviarMensaje(binding.root, "Tu sesión expiró. Inicia sesión nuevamente.", TipoMensaje.ADVERTENCIA)
            irALogin()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilBinding.inflate(layoutInflater)
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

        usuarioViewModel = ViewModelProvider(this).get(UsuarioViewModel::class.java)
        usuarioViewModel.meResponse.observe(this, Observer { response -> onMe(response) })
        usuarioViewModel.updateMeResponse.observe(this, Observer { response -> onUpdateMe(response) })

        binding.btnGuardar.setOnClickListener {
            guardar()
        }

        usuarioViewModel.cargarPerfil()
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

    private fun onMe(response: MeResponse) {
        if (!response.rpta || response.usuario == null) {
            AppMensaje.enviarMensaje(binding.root, response.mensaje ?: "No se pudo cargar el perfil.", TipoMensaje.ERROR)
            return
        }

        binding.etUsuario.setText(response.usuario.usuario)
        binding.etNombre.setText(
            response.cliente?.nombre ?: if (response.usuario.rolNombre == "ADMIN") "Administrador" else ""
        )
        binding.etTelefono.setText(response.cliente?.telefono ?: "")
        binding.etEmail.setText(response.cliente?.email ?: response.usuario.usuario)
        binding.etDireccion.setText(response.cliente?.direccion ?: "")
        binding.etPassword.setText("")
    }

    private fun onUpdateMe(response: UpdateMeResponse) {
        binding.btnGuardar.isEnabled = true
        if (response.rpta) {
            AppMensaje.enviarMensaje(binding.root, response.mensaje, TipoMensaje.CORRECTO)
            usuarioViewModel.cargarPerfil()
        } else {
            AppMensaje.enviarMensaje(binding.root, response.mensaje, TipoMensaje.ERROR)
        }
    }

    private fun guardar() {
        binding.btnGuardar.isEnabled = false

        val request = UpdateMeRequest(
            usuario = binding.etUsuario.text?.toString()?.trim()?.ifBlank { null },
            nombre = binding.etNombre.text?.toString()?.trim()?.ifBlank { null },
            telefono = binding.etTelefono.text?.toString()?.trim()?.ifBlank { null },
            email = binding.etEmail.text?.toString()?.trim()?.ifBlank { null },
            direccion = binding.etDireccion.text?.toString()?.trim()?.ifBlank { null },
            password = binding.etPassword.text?.toString()?.ifBlank { null }
        )

        usuarioViewModel.actualizarPerfil(request)
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
            MenuDinamico.ITEM_PERFIL -> {}
            MenuDinamico.ITEM_ADMIN -> startActivity(Intent(this, AdminPanelActivity::class.java))
            MenuDinamico.ITEM_ADMIN_DASHBOARD -> startActivity(Intent(this, AdminPanelActivity::class.java))
            MenuDinamico.ITEM_ADMIN_PRODUCTOS -> startActivity(Intent(this, AdminProductosActivity::class.java))
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
