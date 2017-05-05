package com.treasuredata.android;

import android.util.Base64;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.jr.ob.JSON;
import io.keen.client.java.KeenJsonHandler;
import org.komamitsu.android.util.Log;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.util.Iterator;
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
                return json.mapFrom(reader);
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
                return json.mapFrom(new String(decryptedBytes, UTF8));
            } catch (Exception e) {
                Log.w(TAG, "Decryption failed. Trying to handle this event as a plain", e);
                try {
                    return json.mapFrom(data);
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
            try {
                JsonFactory factory = new JsonFactory();
                JsonGenerator generator = factory.createGenerator(writer);
                generator.writeStartObject();
                Iterator<? extends Map.Entry<String, ?>> it = value.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, ?> entry = it.next();
                    generator.writeObjectField(entry.getKey(), entry.getValue());
                }
                generator.writeEndObject();
                generator.close();
            } catch (JsonGenerationException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            bufferedWriter.append(json.asString(value));
            bufferedWriter.close();
            try {
                byte[] encryptedBytes = encrypt(byteArrayOutputStream.toByteArray());
                String base64 = base64Encoder.encode(encryptedBytes);
                writer.write(base64);
            } catch (Exception e) {
                Log.w(TAG, "Encryption failed. Storing this event as a plain", e);
                secretKeySpec = null;
                writer.append(json.asString(value));
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

        json = new CustomizedJSON();
    }

    TDJsonHandler(String encryptionKeyword) {
        this(encryptionKeyword, null);
    }

    ///// PRIVATE CONSTANTS /////

    ///// PRIVATE FIELDS /////

    private final JSON json;
}
