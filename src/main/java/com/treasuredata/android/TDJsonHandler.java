package com.treasuredata.android;

import android.util.Base64;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.keen.client.java.KeenJsonHandler;
import org.komamitsu.android.util.Log;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.util.Map;

class TDJsonHandler implements KeenJsonHandler {
    private static final String TAG = TDJsonHandler.class.getSimpleName();
    private static final Base64Encoder DEFAULT_BASE64_ENCODER = new Base64Encoder() {
        @Override
        public String encode(byte[] data)
        {
            return Base64.encodeToString(data, Base64.DEFAULT);
        }

        @Override
        public byte[] decode(String encoded)
        {
            return Base64.decode(encoded, Base64.DEFAULT);
        }
    };
    private SecretKeySpec secretKeySpec;
    private final Cipher cipher;
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private final Base64Encoder base64Encoder;

    public interface Base64Encoder {
        String encode(byte[] data);

        byte[] decode(String encoded);
    }

    @Override
    public Map<String, Object> readJson(Reader reader) throws IOException {
        return readJson(reader, false);
    }

    @Override
    public Map<String, Object> readJsonWithoutDecryption(Reader reader) throws IOException {
        return readJson(reader, true);
    }

    private Map<String, Object> readJson(Reader reader, boolean withoutDecryption) throws IOException {
        if (withoutDecryption || secretKeySpec == null) {
            try {
                return gson.fromJson(reader, MAP_TYPE);
            } catch (Exception e) {
                Log.w(TAG, "This event can't be handled as a plain", e);
                return null;
            }
        }
        else {
            BufferedReader bufferedReader = new BufferedReader(reader);
            StringBuilder buf = new StringBuilder();
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null)
                    break;

                buf.append(line).append("\n");
            }

            String data = buf.toString();
            try {
                byte[] decryptedBytes = decrypt(base64Encoder.decode(data));
                return gson.fromJson(new String(decryptedBytes, UTF8), MAP_TYPE);
            } catch (Exception e) {
                Log.w(TAG, "Decryption failed. Trying to handle this event as a plain", e);
                try {
                    return gson.fromJson(data, MAP_TYPE);
                } catch (Exception ee) {
                    Log.w(TAG, "This event can't be handled as a plain", ee);
                    return null;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeJson(Writer writer, Map<String, ?> value) throws IOException {
        writeJson(writer, value, false);
    }

    @Override
    public void writeJsonWithoutEncryption(Writer writer, Map<String, ?> value) throws IOException {
        writeJson(writer, value, true);
    }

    private void writeJson(Writer writer, Map<String, ?> value, boolean withoutEncryption) throws IOException {
        if (withoutEncryption || secretKeySpec == null) {
            gson.toJson(value, writer);
        }
        else {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            gson.toJson(value, bufferedWriter);
            bufferedWriter.close();
            try {
                byte[] encryptedBytes = encrypt(byteArrayOutputStream.toByteArray());
                writer.write(base64Encoder.encode(encryptedBytes));
            } catch (Exception e) {
                Log.w(TAG, "Encryption failed. Storing this event as a plain", e);
                secretKeySpec = null;
                gson.toJson(value, writer);
            }
        }
        writer.close();
    }

    private byte[] encrypt(byte[] data) throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        return cipher.doFinal(data);
    }

    private byte[] decrypt(byte[] encData) throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        return cipher.doFinal(encData);
    }

    ///// DEFAULT ACCESS CONSTRUCTORS /////

    /**
     * Constructs a new Jackson JSON handler.
     */
    TDJsonHandler() {
        this(null);
    }

    // Exposing this API for testing
    TDJsonHandler(String encryptionKeyword, Base64Encoder base64Encoder) {
        SecretKeySpec secretKeySpec = null;
        Cipher cipher = null;
        if (encryptionKeyword != null) {
            try {
                MessageDigest digester = MessageDigest.getInstance("MD5");
                digester.update(encryptionKeyword.getBytes(), 0, encryptionKeyword.getBytes().length);
                secretKeySpec = new SecretKeySpec(digester.digest(), "AES");
                cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.secretKeySpec = secretKeySpec;
        this.cipher = cipher;
        if (base64Encoder == null) {
            this.base64Encoder = DEFAULT_BASE64_ENCODER;
        }
        else {
            this.base64Encoder = base64Encoder;
        }

        gson = createGson();
    }

    TDJsonHandler(String encryptionKeyword) {
        this(encryptionKeyword, null);
    }

    private Gson createGson() {
        TypeAdapter<Number> doubleAdapter = new TypeAdapter<Number>()
        {
            private TypeAdapter<Number> delegate = TypeAdapters.DOUBLE;

            @Override
            public Number read(JsonReader in)
                    throws IOException
            {
                return delegate.read(in);
            }

            @Override
            public void write(JsonWriter out, Number value)
                    throws IOException
            {
                if (value instanceof Double) {
                    if (value.equals(Double.NaN) || value.equals(Double.POSITIVE_INFINITY) || value.equals(Double.NEGATIVE_INFINITY)) {
                        out.value(value.toString());
                        return;
                    }
                }
                delegate.write(out, value);
            }
        };

        TypeAdapter<Number> floatAdapter = new TypeAdapter<Number>()
        {
            private TypeAdapter<Number> delegate = TypeAdapters.FLOAT;

            @Override
            public Number read(JsonReader in)
                    throws IOException
            {
                return delegate.read(in);
            }

            @Override
            public void write(JsonWriter out, Number value)
                    throws IOException
            {
                if (value instanceof Float) {
                    if (value.equals(Float.NaN) || value.equals(Float.POSITIVE_INFINITY) || value.equals(Float.NEGATIVE_INFINITY)) {
                        out.value(value.toString());
                        return;
                    }
                }
                delegate.write(out, value);
            }
        };

        return new GsonBuilder().
                registerTypeAdapterFactory(TypeAdapters.newFactory(float.class, Float.class, floatAdapter)).
                registerTypeAdapterFactory(TypeAdapters.newFactory(double.class, Double.class, doubleAdapter)).
                setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").
                disableHtmlEscaping().
                serializeNulls().create();
    }

    ///// PRIVATE CONSTANTS /////

    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>(){}.getType();

    ///// PRIVATE FIELDS /////

    private final Gson gson;

}

