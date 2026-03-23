package com.example.wawakusi.view.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.wawakusi.R
import com.example.wawakusi.data.api.WawakusiApiClient
import com.example.wawakusi.data.api.response.VistaItem
import com.example.wawakusi.data.api.response.VistasResponse
import com.example.wawakusi.databinding.ActivityAdminPanelBinding
import com.example.wawakusi.util.AppMensaje
import com.example.wawakusi.util.MenuDinamico
import com.example.wawakusi.util.SharedPreferencesManager
import com.example.wawakusi.util.TipoMensaje
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminPanelActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var binding: ActivityAdminPanelBinding

    private val sesionExpiradaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            AppMensaje.enviarMensaje(binding.root, "Tu sesión expiró. Inicia sesión nuevamente.", TipoMensaje.ADVERTENCIA)
            irALogin()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminPanelBinding.inflate(layoutInflater)
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

        cargarPanel()
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
        cargarPanel()
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

    private fun cargarPanel() {
        WawakusiApiClient.retrofitService.vistas().enqueue(object : Callback<VistasResponse> {
            override fun onResponse(call: Call<VistasResponse>, response: Response<VistasResponse>) {
                val body = response.body()
                if (body == null || body.publico || body.rol != "ADMIN") {
                    AppMensaje.enviarMensaje(binding.root, "No autorizado.", TipoMensaje.ERROR)
                    irALogin()
                    return
                }
                renderOpciones(body.vistas)
            }

            override fun onFailure(call: Call<VistasResponse>, t: Throwable) {
                AppMensaje.enviarMensaje(binding.root, "No se pudo cargar el panel.", TipoMensaje.ERROR)
            }
        })
    }

    private fun renderOpciones(vistas: List<VistaItem>) {
        val container = binding.adminContainer
        val keep = 1
        while (container.childCount > keep) {
            container.removeViewAt(keep)
        }

        val opciones = vistas.filter { it.codigo in setOf("CUS10", "CUS11", "CUS12", "CUS13") }
        if (opciones.isEmpty()) {
            AppMensaje.enviarMensaje(binding.root, "No hay opciones de admin asignadas.", TipoMensaje.ADVERTENCIA)
            val btnUsuarios = MaterialButton(this)
            btnUsuarios.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            btnUsuarios.text = "USUARIOS · Usuarios y roles"
            btnUsuarios.isAllCaps = false
            btnUsuarios.setOnClickListener {
                startActivity(Intent(this, AdminUsuariosRolesActivity::class.java))
            }
            container.addView(btnUsuarios)
            return
        }

        for (op in opciones) {
            val btn = MaterialButton(this)
            btn.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            btn.text = "${op.codigo} · ${op.nombre}"
            btn.isAllCaps = false
            btn.setOnClickListener {
                AppMensaje.enviarMensaje(binding.root, "Función en construcción: ${op.nombre}", TipoMensaje.INFORMACION)
            }
            container.addView(btn)
        }

        val btnPromo = MaterialButton(this)
        btnPromo.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        btnPromo.text = "PROMO · Registrar promociones"
        btnPromo.isAllCaps = false
        btnPromo.setOnClickListener {
            startActivity(Intent(this, AdminPromocionesActivity::class.java))
        }
        container.addView(btnPromo)

        val btnUsuarios = MaterialButton(this)
        btnUsuarios.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        btnUsuarios.text = "USUARIOS · Usuarios y roles"
        btnUsuarios.isAllCaps = false
        btnUsuarios.setOnClickListener {
            startActivity(Intent(this, AdminUsuariosRolesActivity::class.java))
        }
        container.addView(btnUsuarios)
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
            MenuDinamico.ITEM_ADMIN -> {}
            MenuDinamico.ITEM_ADMIN_DASHBOARD -> {}
            MenuDinamico.ITEM_ADMIN_PRODUCTOS -> startActivity(Intent(this, AdminProductosActivity::class.java))
            MenuDinamico.ITEM_ADMIN_PEDIDOS -> AppMensaje.enviarMensaje(binding.root, "Función en construcción: Gestionar pedidos", TipoMensaje.INFORMACION)
            MenuDinamico.ITEM_ADMIN_REPORTES -> AppMensaje.enviarMensaje(binding.root, "Función en construcción: Visualizar reportes", TipoMensaje.INFORMACION)
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
