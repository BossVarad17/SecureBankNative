package com.securebank.app.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;

public class ApiClient {

    public static final String BASE_URL = "https://securebank-ssp4.onrender.com/";

    private static final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();
    private static OkHttpClient httpClient = null;
    private static Retrofit retrofit = null;

    private static OkHttpClient getHttpClient() {
        if (httpClient == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            httpClient = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .cookieJar(new CookieJar() {
                        @Override
                        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                            List<Cookie> existing = cookieStore.containsKey(url.host())
                                    ? cookieStore.get(url.host()) : new ArrayList<>();
                            for (Cookie newCookie : cookies) {
                                existing.removeIf(c -> c.name().equals(newCookie.name()));
                                existing.add(newCookie);
                            }
                            cookieStore.put(url.host(), existing);
                        }

                        @Override
                        public List<Cookie> loadForRequest(HttpUrl url) {
                            List<Cookie> cookies = cookieStore.get(url.host());
                            return cookies != null ? cookies : new ArrayList<>();
                        }
                    })
                    .addInterceptor(logging)
                    .build();
        }
        return httpClient;
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(getHttpClient())
                    // No GsonConverterFactory — we parse JSON manually with org.json
                    .build();
        }
        return retrofit;
    }

    public static ApiService getApiService() {
        return getClient().create(ApiService.class);
    }

    public static void clearCookies() {
        cookieStore.clear();
        httpClient = null;
        retrofit   = null;
    }
}
