package com.treasuredata.android;

import io.keen.client.java.http.Request;
import io.keen.client.java.http.Response;
import io.keen.client.java.http.UrlConnectionHttpHandler;
import org.komamitsu.android.util.Log;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

class TDHttpHandler extends UrlConnectionHttpHandler {
    private static final int DEFAULT_CONNECT_TIMEOUT = 30000;
    private static final int DEFAULT_READ_TIMEOUT = 30000;
    private static final String TAG = TDHttpHandler.class.getSimpleName();
    private static final String DEFAULT_API_ENDPOINT = "https://in.treasuredata.com/android/v3/event";

    private final SSLContext sslContext;
    private final String apiKey;
    private final String apiEndpoint;

    public TDHttpHandler(String apiKey, String apiEndpoint) {
        if (apiKey == null) {
            throw new IllegalArgumentException("apiKey is null");
        }
        if (apiEndpoint == null) {
            apiEndpoint = DEFAULT_API_ENDPOINT;
        }
        this.apiKey = apiKey;
        this.apiEndpoint = apiEndpoint;

        SSLContext sslContext = null;
        String errorLabel = "createSSLContext error: ";
        try {
            sslContext = createSSLContext();
        } catch (CertificateException e) {
            Log.e(TAG, errorLabel + e.getMessage());
        } catch (KeyStoreException e) {
            Log.e(TAG, errorLabel + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, errorLabel + e.getMessage());
        } catch (KeyManagementException e) {
            Log.e(TAG, errorLabel + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, errorLabel + e.getMessage());
        }
        this.sslContext = sslContext;
    }

    protected HttpURLConnection openConnection(Request request) throws IOException {
        URL url = new URL(this.apiEndpoint);
        HttpURLConnection result = (HttpURLConnection) url.openConnection();
        if (url.getProtocol().equals("https")) {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) result;
            httpsURLConnection.setSSLSocketFactory(sslContext.getSocketFactory());
        }
        result.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
        result.setReadTimeout(DEFAULT_READ_TIMEOUT);
        return result;
    }

    protected void sendRequest(HttpURLConnection connection, Request request) throws IOException {
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-TD-Data-Type", "k");
        connection.setRequestProperty("X-TD-Write-Key", apiKey);
        /*
        for (Map.Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
            boolean needDelimiter = false;
            StringBuilder buf = new StringBuilder();
            for (String v : header.getValue()) {
                if (needDelimiter) {
                    buf.append(", ");
                }
                needDelimiter = true;
                buf.append(v);
            }
            Log.d(TAG, "sendRequest(header): k=[" + header.getKey() + "], v=[" + buf.toString() + "]");
        }
        */
        connection.setDoOutput(true);
        request.body.writeTo(connection.getOutputStream());
    }

    @Override
    protected Response readResponse(HttpURLConnection connection) throws IOException {
        Response response = super.readResponse(connection);
        return response;
    }

    private SSLContext createSSLContext() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, KeyManagementException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream caInput = TDHttpHandler.class.getResourceAsStream("/gd_bundle.der");
        java.security.cert.Certificate ca;
        try {
            ca = cf.generateCertificate(caInput);
            // System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
        } finally {
            caInput.close();
        }
        // Create a KeyStore containing our trusted CAs
        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);

        // Create a TrustManager that trusts the CAs in our KeyStore
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);

        // Create an SSLContext that uses our TrustManager
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, tmf.getTrustManagers(), null);

        return context;
    }
}
