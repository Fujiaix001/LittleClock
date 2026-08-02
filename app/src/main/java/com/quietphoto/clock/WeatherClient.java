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
import java.text.SimpleDateFormat;
import java.text.Normalizer;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class WeatherClient {
    private static final int TIMEOUT_MS = 8000;
    private static final int MAX_RESPONSE_BYTES = 128 * 1024;

    public static String removeAccents(String text) {
        if (text == null || text.length() == 0) return text;
        String nfd = Normalizer.normalize(text, Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    /** Returns the first, most local part of a comma-separated location label. */
    public static String minimalLocationName(String locationName) {
        if (locationName == null) return "";
        String value = locationName.trim();
        int separator = value.indexOf(',');
        if (separator >= 0) value = value.substring(0, separator).trim();
        return value;
    }

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

    public static final class HourlyWeather {
        public final String localTime;
        public final int temperatureCelsius;
        public final int precipitationProbability;
        public final int weatherCode;

        HourlyWeather(String localTime, int temperatureCelsius,
                int precipitationProbability, int weatherCode) {
            this.localTime = localTime;
            this.temperatureCelsius = temperatureCelsius;
            this.precipitationProbability = precipitationProbability;
            this.weatherCode = weatherCode;
        }
    }

    public static final class ExtendedWeather {
        public final CurrentWeather current;
        public final List<HourlyWeather> nextHours;
        public final long sunriseAtMs;
        public final long sunsetAtMs;

        ExtendedWeather(CurrentWeather current, List<HourlyWeather> nextHours,
                long sunriseAtMs, long sunsetAtMs) {
            this.current = current;
            this.nextHours = nextHours;
            this.sunriseAtMs = sunriseAtMs;
            this.sunsetAtMs = sunsetAtMs;
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
            if (admin.length() > 0 && !admin.equalsIgnoreCase(name) && !admin.equalsIgnoreCase(country)) {
                display.append(", ").append(admin);
            }
            if (country.length() > 0 && !country.equalsIgnoreCase(name)) {
                display.append(", ").append(country);
            }
            locations.add(new LocationResult(
                    removeAccents(display.toString()),
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

    /**
     * Fetches the optional display data in one request. The normal current-weather request is
     * intentionally kept separate so the feature can remain completely dormant when disabled.
     */
    public static ExtendedWeather fetchExtended(double latitude, double longitude) throws Exception {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude
                + "&longitude=" + longitude
                + "&current=temperature_2m,weather_code,is_day"
                + "&hourly=temperature_2m,precipitation_probability,weather_code"
                + "&daily=sunrise,sunset&forecast_days=2"
                + "&temperature_unit=celsius&timezone=auto";
        JSONObject root = new JSONObject(request(url));
        JSONObject currentJson = root.getJSONObject("current");
        CurrentWeather current = new CurrentWeather(
                (int) Math.round(currentJson.getDouble("temperature_2m")),
                currentJson.getInt("weather_code"),
                currentJson.optInt("is_day", 1) == 1);

        JSONObject hourly = root.optJSONObject("hourly");
        JSONArray times = hourly == null ? null : hourly.optJSONArray("time");
        JSONArray temperatures = hourly == null ? null : hourly.optJSONArray("temperature_2m");
        JSONArray precipitation = hourly == null
                ? null : hourly.optJSONArray("precipitation_probability");
        JSONArray codes = hourly == null ? null : hourly.optJSONArray("weather_code");
        List<HourlyWeather> nextHours = new ArrayList<HourlyWeather>();
        String currentTime = currentJson.optString("time", "");
        int firstFuture = 0;
        if (times != null && currentTime.length() > 0) {
            firstFuture = -1;
            for (int i = 0; i < times.length(); i++) {
                String candidate = times.optString(i, "");
                if (candidate.compareTo(currentTime) > 0) {
                    firstFuture = i;
                    break;
                }
            }
            if (firstFuture < 0) firstFuture = 0;
        }
        if (times != null && temperatures != null && codes != null) {
            int end = Math.min(times.length(), Math.min(temperatures.length(), codes.length()));
            for (int i = firstFuture; i < end && nextHours.size() < 3; i++) {
                String time = times.optString(i, "");
                if (time.length() == 0) continue;
                int probability = precipitation == null || i >= precipitation.length()
                        ? 0 : precipitation.optInt(i, 0);
                nextHours.add(new HourlyWeather(
                        time.length() >= 16 ? time.substring(11, 16) : time,
                        (int) Math.round(temperatures.optDouble(i, 0.0)),
                        Math.max(0, Math.min(100, probability)),
                        codes.optInt(i, 0)));
            }
        }

        String timezone = root.optString("timezone", "UTC");
        JSONObject daily = root.optJSONObject("daily");
        JSONArray sunrise = daily == null ? null : daily.optJSONArray("sunrise");
        JSONArray sunset = daily == null ? null : daily.optJSONArray("sunset");
        long sunriseAtMs = sunrise == null || sunrise.length() == 0
                ? -1L : parseLocalDateTime(sunrise.optString(0, ""), timezone);
        long sunsetAtMs = sunset == null || sunset.length() == 0
                ? -1L : parseLocalDateTime(sunset.optString(0, ""), timezone);
        return new ExtendedWeather(current, nextHours, sunriseAtMs, sunsetAtMs);
    }

    private static long parseLocalDateTime(String value, String timezone) throws Exception {
        if (value == null || value.length() == 0) return -1L;
        String normalized = value.length() > 16 ? value.substring(0, 16) : value;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
        format.setLenient(false);
        format.setTimeZone(TimeZone.getTimeZone(timezone));
        Date parsed = format.parse(normalized);
        return parsed == null ? -1L : parsed.getTime();
    }

    private static String request(String url) throws Exception {
        java.net.HttpURLConnection connection = null;
        BufferedInputStream input = null;
        try {
            if (Build.VERSION.SDK_INT <= 19) {
                url = url.replaceFirst("^https://", "http://");
            }
            connection = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
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
