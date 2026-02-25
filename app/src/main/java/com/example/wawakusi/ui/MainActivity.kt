package com.example.wawakusi.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.navigation.NavigationView
import com.example.wawakusi.R

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var viewPager: ViewPager2
    private lateinit var tvContent: TextView

    private val handler = Handler()
    private var currentItem = 0 // Mantener el índice actual de la imagen mostrada

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializamos el toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Inicializamos el DrawerLayout y NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        // Configuración del ActionBarDrawerToggle
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Configuramos el listener de los items del NavigationView
        navigationView.setNavigationItemSelectedListener(this)

        // Seleccionamos el Home por defecto al abrir la app
        navigationView.setCheckedItem(R.id.nav_home)

        // Inicializamos el ViewPager2 para el carrusel de imágenes
        viewPager = findViewById(R.id.viewPager)

        // Array de imágenes que se mostrarán en el carrusel
        val images = intArrayOf(R.drawable.img1, R.drawable.img2, R.drawable.img3)  // Asegúrate de tener estas imágenes en res/drawable

        // Creamos el adaptador y lo asignamos al ViewPager2
        val adapter = ImageSliderAdapter(images)
        viewPager.adapter = adapter

        // Deslizar imágenes automáticamente cada 3 segundos
        val runnable = object : Runnable {
            override fun run() {
                // Incrementamos el índice de la imagen
                currentItem = (currentItem + 1) % images.size  // Si llega al final, se reinicia a 0
                viewPager.setCurrentItem(currentItem, true)  // Cambiar la imagen automáticamente
                handler.postDelayed(this, 3000)  // Repetir el cambio de imagen cada 3 segundos
            }
        }

        // Comenzar el deslizamiento automático
        handler.postDelayed(runnable, 3000)  // Inicia el deslizamiento después de 3 segundos
    }

    // Cuando la Activity existente es traída al frente con FLAG_ACTIVITY_CLEAR_TOP | SINGLE_TOP
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Asegurar que el menú muestre Inicio como seleccionado y actualizar UI
        navigationView.setCheckedItem(R.id.nav_home)
    }

    // Refuerzo: cada vez que resume, asegurarse que el home está correcto si estamos en MainActivity
    override fun onResume() {
        super.onResume()
        if (::navigationView.isInitialized && ::tvContent.isInitialized) {
            navigationView.setCheckedItem(R.id.nav_home)
        }
    }

    // Función para manejar la selección de los ítems del Navigation Drawer
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        drawerLayout.closeDrawers()

        when (id) {
            R.id.nav_home -> {
                // Actualizar UI localmente en vez de crear intents innecesarios
                navigationView.setCheckedItem(R.id.nav_home)
                return true
            }
            R.id.nav_nosotros -> {
                val iNosotros = Intent(this, NosotrosActivity::class.java)
                iNosotros.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)  // Opcional
                startActivity(iNosotros)
                return true
            }
            R.id.nav_tienda -> {
                val iTienda = Intent(this, TiendaActivity::class.java)
                iTienda.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(iTienda)
                return true
            }
            R.id.nav_promociones -> {
                val iPromociones = Intent(this, PromocionesActivity::class.java)
                iPromociones.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(iPromociones)
                return true
            }
            R.id.nav_contacto -> {
                val iContacto = Intent(this, ContactoActivity::class.java)
                iContacto.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(iContacto)
                return true
            }
            else -> return false
        }
    }
}