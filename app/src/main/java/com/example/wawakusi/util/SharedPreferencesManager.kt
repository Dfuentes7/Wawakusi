package com.example.wawakusi.util

import android.content.Context
import android.content.Intent
import android.util.Base64
import android.view.Menu
import android.widget.TextView
import com.example.wawakusi.R
import com.example.wawakusi.data.api.response.VistasResponse
import com.google.android.material.navigation.NavigationView
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import kotlin.math.max

object SharedPreferencesManager {

    const val ACTION_SESION_EXPIRADA = "com.example.wawakusi.ACTION_SESION_EXPIRADA"

    private const val PREFS_NAME = "wawakusi_prefs"
    private const val KEY_TOKEN = "token"
    private const val KEY_TOKEN_EXP_AT = "token_exp_at"
    private const val KEY_USUARIO = "usuario"
    private const val KEY_ROL = "rol"

    private fun prefs() = MiApp.instancia.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun guardarSesion(token: String, usuario: String?, rol: String?) {
        val expAt = extraerExpiracionMillis(token) ?: 0L
        prefs().edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_TOKEN_EXP_AT, expAt)
            .putString(KEY_USUARIO, usuario)
            .putString(KEY_ROL, rol)
            .apply()
    }

    fun obtenerUsuario(): String? = prefs().getString(KEY_USUARIO, null)

    fun obtenerRol(): String? = prefs().getString(KEY_ROL, null)

    fun obtenerToken(): String? = prefs().getString(KEY_TOKEN, null)

    fun obtenerTokenValido(): String? {
        val token = obtenerToken() ?: return null
        if (tokenExpirado()) return null
        return token
    }

    fun tokenExpirado(): Boolean {
        val token = obtenerToken() ?: return true
        val expAt = prefs().getLong(KEY_TOKEN_EXP_AT, 0L)
        if (expAt <= 0L) return false
        val now = System.currentTimeMillis()
        val skew = 10_000L
        return now + skew >= expAt
    }

    fun estaAutenticado(): Boolean = obtenerTokenValido() != null

    fun limpiarSesion() {
        prefs().edit()
            .remove(KEY_TOKEN)
            .remove(KEY_TOKEN_EXP_AT)
            .remove(KEY_USUARIO)
            .remove(KEY_ROL)
            .apply()
    }

    fun limpiarSesionYNotificar() {
        limpiarSesion()
        val intent = Intent(ACTION_SESION_EXPIRADA)
        MiApp.instancia.sendBroadcast(intent)
    }

    private fun extraerExpiracionMillis(token: String): Long? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null

            val payloadBytes = Base64.decode(
                parts[1],
                Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
            )
            val payload = String(payloadBytes, StandardCharsets.UTF_8)
            val json = JSONObject(payload)
            if (!json.has("exp")) return null
            max(0L, json.getLong("exp")) * 1000L
        } catch (_: Exception) {
            null
        }
    }
}

object MenuDinamico {
    private const val GROUP_SESION = 2000
    private const val GROUP_ADMIN = 2100

    const val ITEM_LOGIN = 2001
    const val ITEM_REGISTRO = 2002
    const val ITEM_PERFIL = 2003
    const val ITEM_ADMIN = 2004
    const val ITEM_CERRAR_SESION = 2005
    const val ITEM_CARRITO = 2006
    const val ITEM_MIS_PEDIDOS = 2007

    const val ITEM_ADMIN_DASHBOARD = 2101
    const val ITEM_ADMIN_PRODUCTOS = 2102
    const val ITEM_ADMIN_PEDIDOS = 2103
    const val ITEM_ADMIN_REPORTES = 2104
    const val ITEM_ADMIN_PROMOCIONES = 2105
    const val ITEM_ADMIN_USUARIOS = 2106

    fun aplicarHeader(navigationView: NavigationView) {
        val header = navigationView.getHeaderView(0)
        val tvSubtitle = header.findViewById<TextView>(R.id.tvHeaderSubtitle)

        val usuario = SharedPreferencesManager.obtenerUsuario()
        val rol = SharedPreferencesManager.obtenerRol()

        tvSubtitle.text = when {
            !usuario.isNullOrBlank() && !rol.isNullOrBlank() -> "$usuario ($rol)"
            !usuario.isNullOrBlank() -> usuario
            else -> "Invitado"
        }
    }

    fun aplicarMenuSesion(navigationView: NavigationView, vistasResponse: VistasResponse) {
        val menu = navigationView.menu
        menu.removeGroup(GROUP_SESION)
        menu.removeGroup(GROUP_ADMIN)

        val codigos = vistasResponse.vistas.map { it.codigo }.toSet()
        val tienePerfil = codigos.contains("CUS09")

        val isAdmin = !vistasResponse.publico && vistasResponse.rol == "ADMIN"

        listOf(
            R.id.nav_home,
            R.id.nav_nosotros,
            R.id.nav_tienda,
            R.id.nav_promociones,
            R.id.nav_contacto
        ).forEach { id ->
            val item = menu.findItem(id)
            if (item != null) item.isVisible = !isAdmin
        }

        if (isAdmin) {
            menu.add(GROUP_ADMIN, ITEM_ADMIN_DASHBOARD, Menu.NONE, "Dashboard")
                .setIcon(R.drawable.ic_admin_dashboard_24)

            if (codigos.contains("CUS11")) {
                menu.add(GROUP_ADMIN, ITEM_ADMIN_PRODUCTOS, Menu.NONE, "Gestionar productos")
                    .setIcon(R.drawable.ic_admin_productos_24)
            }

            if (codigos.contains("CUS12")) {
                menu.add(GROUP_ADMIN, ITEM_ADMIN_PEDIDOS, Menu.NONE, "Gestionar pedidos")
                    .setIcon(R.drawable.ic_admin_pedidos_24)
            }

            if (codigos.contains("CUS13")) {
                menu.add(GROUP_ADMIN, ITEM_ADMIN_REPORTES, Menu.NONE, "Visualizar reportes")
                    .setIcon(R.drawable.ic_admin_reportes_24)
            }

            menu.add(GROUP_ADMIN, ITEM_ADMIN_PROMOCIONES, Menu.NONE, "Registrar promociones")
                .setIcon(R.drawable.ic_admin_promociones_24)

            menu.add(GROUP_ADMIN, ITEM_ADMIN_USUARIOS, Menu.NONE, "Usuarios y roles")
                .setIcon(R.drawable.ic_admin_usuarios_24)
        }

        if (vistasResponse.publico) {
            if (codigos.contains("CUS02")) {
                menu.add(GROUP_SESION, ITEM_LOGIN, Menu.NONE, "Iniciar sesión")
                    .setIcon(R.drawable.outline_add_home_24)
            }

            if (codigos.contains("CUS01")) {
                menu.add(GROUP_SESION, ITEM_REGISTRO, Menu.NONE, "Registrarse")
                    .setIcon(R.drawable.outline_add_home_24)
            }
        }

        if (tienePerfil) {
            menu.add(GROUP_SESION, ITEM_PERFIL, Menu.NONE, "Mi perfil")
                .setIcon(R.drawable.outline_add_home_24)
        }

        if (!vistasResponse.publico && !isAdmin) {
            val puedeCarrito = codigos.contains("CUS06") || codigos.contains("CUS05") || codigos.contains("CUS07")
            if (puedeCarrito) {
                val item = menu.add(GROUP_SESION, ITEM_CARRITO, Menu.NONE, "Carrito")
                    .setIcon(R.drawable.ic_cart_nav_24)
                try {
                    item.setActionView(R.layout.menu_badge)
                } catch (_: Exception) {}
            }

            if (codigos.contains("CUS08")) {
                menu.add(GROUP_SESION, ITEM_MIS_PEDIDOS, Menu.NONE, "Mis pedidos")
                    .setIcon(R.drawable.ic_admin_pedidos_24)
            }
        }

        if (!vistasResponse.publico) {
            menu.add(GROUP_SESION, ITEM_CERRAR_SESION, Menu.NONE, "Cerrar sesión")
                .setIcon(R.drawable.outline_add_home_24)
        }
    }

    fun actualizarBadgeCarrito(navigationView: NavigationView, count: Int) {
        val item = navigationView.menu.findItem(ITEM_CARRITO) ?: return
        if (item.actionView == null) {
            try {
                item.setActionView(R.layout.menu_badge)
            } catch (_: Exception) {}
        }
        val badgeView = item.actionView?.findViewById<TextView>(R.id.tvBadge) ?: return
        if (count > 0) {
            badgeView.text = if (count > 99) "99+" else count.toString()
            badgeView.visibility = android.view.View.VISIBLE
        } else {
            badgeView.visibility = android.view.View.GONE
        }
    }
}
