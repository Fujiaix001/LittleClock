package com.quietphoto.clock;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public final class WeatherClient {
    private static final int TIMEOUT_MS = 8000;
    private static final int MAX_RESPONSE_BYTES = 128 * 1024;

    public static final class LocationResult {
        public final String displayName;
        public final double latitude;
        public final double longitude;
        public final String timezone;

        LocationResult(String displayName, double latitude, double longitude, String timezone) {
            this.displayName = displayName;
            this.latitude = latitude;
            this.longitude = longitude;
            this.timezone = timezone;
        }
    }

    public static final class CurrentWeather {
        public final int temperatureCelsius;
        public final int weatherCode;
        public final boolean daytime;

        CurrentWeather(int temperatureCelsius, int weatherCode, boolean daytime) {
            this.temperatureCelsius = temperatureCelsius;
            this.weatherCode = weatherCode;
            this.daytime = daytime;
        }
    }

    private WeatherClient() {
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager manager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) return false;
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                return Api23Network.isWifiConnected(manager);
            }
            @SuppressWarnings("deprecation")
            NetworkInfo info = manager.getActiveNetworkInfo();
            return info != null && info.isConnected()
                    && info.getType() == ConnectivityManager.TYPE_WIFI;
        } catch (SecurityException ignored) {
            return false;
        }
    }

    @android.annotation.TargetApi(23)
    private static final class Api23Network {
        private Api23Network() {
        }

        static boolean isWifiConnected(ConnectivityManager manager) {
            Network network = manager.getActiveNetwork();
            NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
            return capabilities != null
                    && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
    }

    public static List<LocationResult> searchLocations(String query) throws Exception {
        String encoded = URLEncoder.encode(query, "UTF-8");
        JSONObject root = new JSONObject(request(
                "https://geocoding-api.open-meteo.com/v1/search?name=" + encoded
                        + "&count=8&language=en&format=json"));
        JSONArray results = root.optJSONArray("results");
        List<LocationResult> locations = new ArrayList<LocationResult>();
        if (results == null) return locations;

        for (int i = 0; i < results.length(); i++) {
            JSONObject item = results.optJSONObject(i);
            if (item == null || !item.has("latitude") || !item.has("longitude")) continue;
            String name = item.optString("name", "").trim();
            String admin = item.optString("admin1", "").trim();
            String country = item.optString("country", "").trim();
            StringBuilder display = new StringBuilder(name);
            if (admin.length() > 0 && !admin.equalsIgnoreCase(name)) display.append(", ").append(admin);
            if (country.length() > 0) display.append(", ").append(country);
            locations.add(new LocationResult(
                    display.toString(),
                    item.getDouble("latitude"),
                    item.getDouble("longitude"),
                    item.optString("timezone", "auto")));
        }
        return locations;
    }

    public static CurrentWeather fetchCurrent(double latitude, double longitude) throws Exception {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude
                + "&longitude=" + longitude
                + "&current=temperature_2m,weather_code,is_day&temperature_unit=celsius&timezone=auto";
        JSONObject current = new JSONObject(request(url)).getJSONObject("current");
        return new CurrentWeather(
                (int) Math.round(current.getDouble("temperature_2m")),
                current.getInt("weather_code"),
                current.optInt("is_day", 1) == 1);
    }

    private static String request(String url) throws Exception {
        HttpsURLConnection connection = null;
        BufferedInputStream input = null;
        try {
            connection = (HttpsURLConnection) new java.net.URL(url).openConnection();
            if (Build.VERSION.SDK_INT <= 19) {
                javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[] {
                    new javax.net.ssl.X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                    }
                };
                SSLContext tls12 = SSLContext.getInstance("TLSv1.2");
                tls12.init(null, trustAllCerts, new java.security.SecureRandom());
                connection.setSSLSocketFactory(tls12.getSocketFactory());
                connection.setHostnameVerifier(new javax.net.ssl.HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, javax.net.ssl.SSLSession session) {
                        return true;
                    }
                });
            }
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "LittleClock/2");
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) throw new java.io.IOException("HTTP " + status);

            input = new BufferedInputStream(connection.getInputStream());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int total = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > MAX_RESPONSE_BYTES) throw new java.io.IOException("Response too large");
                output.write(buffer, 0, count);
            }
            return output.toString("UTF-8");
        } finally {
            if (input != null) try { input.close(); } catch (Exception ignored) { }
            if (connection != null) connection.disconnect();
        }
    }
}
