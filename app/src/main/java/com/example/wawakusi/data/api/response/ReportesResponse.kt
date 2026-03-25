package com.example.wawakusi.data.api.response

data class ReporteVentasResponse(
    val rpta: Boolean,
    val mensaje: String? = null,
    val resumen: ReporteResumenResponse? = null,
    val porEstado: List<ReportePorEstadoResponse> = emptyList(),
    val porDia: List<ReportePorDiaResponse> = emptyList(),
    val topProductos: List<ReporteTopProductoResponse> = emptyList()
)

data class ReporteResumenResponse(
    val totalVentas: Int? = null,
    val totalIngresos: Double? = null,
    val ticketPromedio: Double? = null
)

data class ReportePorEstadoResponse(
    val estado: Int? = null,
    val cantidad: Int? = null,
    val total: Double? = null
)

data class ReportePorDiaResponse(
    val dia: String? = null,
    val cantidad: Int? = null,
    val total: Double? = null
)

data class ReporteTopProductoResponse(
    val productoId: Int? = null,
    val productoNombre: String? = null,
    val cantidadVendida: Int? = null,
    val totalVendido: Double? = null
)

data class DashboardResponse(
    val rpta: Boolean,
    val mensaje: String? = null,
    val productosActivos: Int? = null,
    val clientesActivos: Int? = null,
    val ventasPagadas: Int? = null,
    val ingresosTotales: Double? = null,
    val porDia: List<ReportePorDiaResponse> = emptyList(),
    val porEstado: List<ReportePorEstadoResponse> = emptyList()
)
