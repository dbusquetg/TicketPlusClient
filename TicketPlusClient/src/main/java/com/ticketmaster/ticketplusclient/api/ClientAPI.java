/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.api;

import com.ticketmaster.ticketplusclient.session.SessionManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Fábrica y configurador del cliente HTTP Retrofit para la comunicación con el backend TicketPlus.
 *
 * <p>Esta clase implementa el patrón <b>Singleton</b> sobre la instancia de {@link Retrofit}
 * y el patrón <b>Factory Method</b> a través de {@link #createService(Class)} para crear
 * implementaciones de las interfaces API declaradas (e.g., {@link AuthAPI}).</p>
 *
 * <p>Configura un interceptor OkHttp que añade automáticamente la cabecera
 * {@code Authorization: Bearer <token>} en todas las peticiones cuando el usuario
 * tiene una sesión activa en {@link SessionManager}, liberando así al resto de
 * componentes de gestionar la autenticación manualmente.</p>
 *
 * <p>Las comunicaciones se realizan sobre HTTPS (TLS) usando el certificado
 * autofirmado del servidor ({@code server.crt}) almacenado en los recursos
 * del proyecto. El cliente confía únicamente en ese certificado.</p>
 *
 * <p>La URL base del servidor se define en la constante {@link #BASE_URL}.</p>
 *
 * @author Christian
 */
public class ClientAPI {

    /**
     * URL base del backend — HTTPS puerto 8443
     */
    private static final String BASE_URL = System.getProperty("ticketplus.base.url",
            "https://10.2.99.25:8443/");

    /**
     * Instancia singleton de {@link Retrofit}. Se inicializa de forma diferida
     * (lazy) en la primera llamada a {@link #getClient()}.
     */
    private static Retrofit retrofit;

    /**
     * Devuelve la instancia singleton de {@link Retrofit} configurada con soporte
     * SSL y el interceptor de autenticación.
     *
     * <p>Carga el certificado autofirmado del servidor desde {@code /server.crt}
     * (en resources) y construye un {@link SSLContext} que confía únicamente
     * en ese certificado — evitando confiar en cualquier CA del sistema.</p>
     *
     * @return la instancia singleton de {@link Retrofit} lista para ser usada
     * @throws RuntimeException si la configuración SSL falla
     */
    public static Retrofit getClient() {

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        if (retrofit == null) {
            try {
                // Carga el certificado público del servidor desde resources
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                InputStream certStream = ClientAPI.class.getResourceAsStream("/server.crt");
                Certificate serverCert = cf.generateCertificate(certStream);
                certStream.close();

                // Construye un KeyStore que confía ÚNICAMENTE en nuestro certificado
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, null);
                keyStore.setCertificateEntry("ticketplus-server", serverCert);

                TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(keyStore);

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);

                X509TrustManager trustManager =
                        (X509TrustManager) tmf.getTrustManagers()[0];

                OkHttpClient httpClient = new OkHttpClient.Builder()
                        .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                        // Acepta el servidor por IP (certificado autofirmado usa IP, no dominio)
                        .hostnameVerifier((hostname, session) ->
                                hostname.equals("10.2.99.25") || hostname.equals("localhost"))
                        .addInterceptor(chain -> {
                            Request original = chain.request();
                            String token = SessionManager.getInstance().getToken();

                            Request request;

                            if (token != null) {
                                request = original.newBuilder()
                                        .header("Authorization", "Bearer " + token)
                                        .header("Accept", "application/json")
                                        .header("Content-Type", "application/json")
                                        .method(original.method(), original.body())
                                        .build();
                            } else {
                                request = original.newBuilder()
                                        .header("Accept", "application/json")
                                        .header("Content-Type", "application/json")
                                        .method(original.method(), original.body())
                                        .build();
                            }

                            return chain.proceed(request);
                        })
                        .addInterceptor(loggingInterceptor)
                        .build();

                retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(httpClient)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();

            } catch (Exception e) {
                throw new RuntimeException("SSL configuration failed: " + e.getMessage(), e);
            }
        }
        return retrofit;
    }

    /**
     * Crea e instancia una implementación concreta de la interfaz API especificada.
     *
     * @param <T>          tipo de la interfaz API a instanciar
     * @param serviceClass clase de la interfaz API (e.g., {@code AuthAPI.class})
     * @return implementación generada por Retrofit de la interfaz indicada
     */
    public static <T> T createService(Class<T> serviceClass) {
        return getClient().create(serviceClass);
    }

    /**
     * Destruye la instancia singleton de {@link Retrofit}, forzando su
     * recreación en la siguiente llamada a {@link #getClient()}.
     *
     * <p>Debe invocarse tras un logout para asegurar que las peticiones
     * posteriores no reutilicen configuraciones de sesión antiguas.</p>
     */
    public static void reset() {
        retrofit = null;
    }
}