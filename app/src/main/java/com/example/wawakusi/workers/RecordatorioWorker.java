package com.example.wawakusi.workers;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.wawakusi.R;
import com.example.wawakusi.util.SharedPreferencesManager;
import com.example.wawakusi.view.ui.CarritoActivity;
import com.example.wawakusi.view.ui.PromocionesActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RecordatorioWorker extends Worker {

    private static final String UNIQUE_WORK_NAME = "cart_reminder";
    private static final String CHANNEL_ID = "canal_recordatorio";
    private static final int NOTIFICATION_ID = 1001;

    public RecordatorioWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void scheduleCartReminder(@NonNull Context context, long delayMinutes) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(RecordatorioWorker.class)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request);
    }

    public static void cancelCartReminder(@NonNull Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME);
    }

    public static void schedulePromoCheckTwiceDaily(@NonNull Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(PromocionesWorker.class, 12, TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "promo_check_twice_daily",
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }

    public static void notifyCartNow(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 33) {
            int p = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS);
            if (p != PackageManager.PERMISSION_GRANTED) return;
        }

        int count = SharedPreferencesManager.INSTANCE.obtenerCartCount();
        if (count <= 0) count = 1;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Recordatorios",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, CarritoActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                Build.VERSION.SDK_INT >= 23
                        ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        String titulo = "Lista de compras";
        String mensaje = "¡No olvides revisar tu lista de compras hoy!";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(mensaje))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
    }

    @NonNull
    @Override
    public Result doWork() {
        if (Build.VERSION.SDK_INT >= 33) {
            int p = getApplicationContext().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS);
            if (p != PackageManager.PERMISSION_GRANTED) return Result.success();
        }

        int count = SharedPreferencesManager.INSTANCE.obtenerCartCount();
        if (count <= 0) return Result.success();

        crearCanalNotificacion();

        Intent intent = new Intent(getApplicationContext(), CarritoActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                intent,
                Build.VERSION.SDK_INT >= 23
                        ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        String titulo = "Lista de compras";
        String mensaje = "¡No olvides revisar tu lista de compras hoy!";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(mensaje))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(getApplicationContext()).notify(NOTIFICATION_ID, builder.build());
        scheduleCartReminder(getApplicationContext(), 1);
        return Result.success();
    }

    private void crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Recordatorios",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}

class PromocionesWorker extends Worker {

    private static final String CHANNEL_ID = "canal_recordatorio";
    private static final int NOTIFICATION_ID_PROMOS = 2001;
    private static final String PROMOS_URL = "https://wawakusi.vercel.app/api/producto/promociones";

    public PromocionesWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (Build.VERSION.SDK_INT >= 33) {
            int p = getApplicationContext().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS);
            if (p != PackageManager.PERMISSION_GRANTED) return Result.success();
        }

        try {
            String json = fetch(PROMOS_URL);
            if (json == null || json.trim().isEmpty()) return Result.success();

            JSONArray arr = new JSONArray(json);
            String signature = buildSignature(arr);
            String prev = SharedPreferencesManager.INSTANCE.obtenerPromosSignature();
            boolean seeded = SharedPreferencesManager.INSTANCE.promosSeeded();

            SharedPreferencesManager.INSTANCE.guardarPromosSignature(signature);
            if (!seeded) {
                SharedPreferencesManager.INSTANCE.marcarPromosSeeded();
                return Result.success();
            }

            if (prev != null && !prev.equals(signature) && arr.length() > 0) {
                notifyNewPromos();
            }
        } catch (Exception ignored) {
            return Result.success();
        }

        return Result.success();
    }

    private void notifyNewPromos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Recordatorios",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(getApplicationContext(), PromocionesActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                intent,
                Build.VERSION.SDK_INT >= 23
                        ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        String titulo = "Nuevas promociones";
        String mensaje = "¡Hay nuevas promociones disponibles! Revísalas ahora.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(mensaje))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(getApplicationContext()).notify(NOTIFICATION_ID_PROMOS, builder.build());
    }

    private String buildSignature(JSONArray arr) {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            int productoId = o.optInt("id", -1);
            JSONObject d = o.optJSONObject("descuento");
            int descuentoId = d != null ? d.optInt("id", -1) : -1;
            keys.add(productoId + ":" + descuentoId);
        }
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        sb.append("n=").append(arr.length()).append("|");
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(keys.get(i));
        }
        return sb.toString();
    }

    private String fetch(String url) {
        try {
            java.net.URL u = new java.net.URL(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) u.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            java.io.InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) return null;
            byte[] bytes = readAll(is);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] readAll(java.io.InputStream is) throws java.io.IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int r;
        while ((r = is.read(buf)) != -1) baos.write(buf, 0, r);
        return baos.toByteArray();
    }
}
