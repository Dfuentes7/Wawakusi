package com.example.wawakusi.view.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.wawakusi.databinding.ActivityNosotrosBinding

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build


import android.view.MenuItem

import androidx.appcompat.app.ActionBarDrawerToggle

import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout

import com.google.android.material.navigation.NavigationView
import com.example.wawakusi.R
import com.example.wawakusi.data.api.WawakusiApiClient
import com.example.wawakusi.data.api.response.VistasResponse
import com.example.wawakusi.util.AppMensaje
import com.example.wawakusi.util.MenuDinamico
import com.example.wawakusi.util.SharedPreferencesManager
import com.example.wawakusi.util.TipoMensaje
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class NosotrosActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var binding: ActivityNosotrosBinding

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
        binding = ActivityNosotrosBinding.inflate(layoutInflater)
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
        navigationView.setCheckedItem(R.id.nav_nosotros)
        MenuDinamico.aplicarHeader(navigationView)
        actualizarMenuDesdeApi()
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
            R.id.nav_nosotros -> { /* Ya estamos aquí */ }
            R.id.nav_tienda -> {
                val intent = Intent(this, TiendaActivity::class.java)
                startActivity(intent)
            }
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

    override fun onResume() {
        super.onResume()
        if (SharedPreferencesManager.obtenerToken() != null && SharedPreferencesManager.tokenExpirado()) {
            SharedPreferencesManager.limpiarSesion()
            AppMensaje.enviarMensaje(navigationView, "Tu sesión expiró. Inicia sesión nuevamente.", TipoMensaje.ADVERTENCIA)
        }
        navigationView.setCheckedItem(R.id.nav_nosotros)
        MenuDinamico.aplicarHeader(navigationView)
        actualizarMenuDesdeApi()
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
}
