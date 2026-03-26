package com.example.wawakusi.util
import android.app.Application
import com.example.wawakusi.workers.RecordatorioWorker

class MiApp : Application() {
    companion object{
        lateinit var instancia: MiApp
    }

    override fun onCreate() {
        super.onCreate()
        instancia = this
        try {
            RecordatorioWorker.schedulePromoCheckTwiceDaily(this)
        } catch (_: Exception) {}
    }
}
