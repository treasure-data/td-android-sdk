package com.treasuredata.android;

import android.util.Base64;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.keen.client.java.KeenJsonHandler;
import org.komamitsu.android.util.Log;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TimeZone;

class TDJsonHandler implements KeenJsonHandler {
    private static final String TAG = TDJsonHandler.class.getSimpleName();
    private SecretKeySpec secretKeySpec;
    private final Cipher cipher;

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
                return mapper.readValue(reader, MAP_TYPE);
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
                byte[] decryptedBytes = decrypt(Base64.decode(data, Base64.DEFAULT));
                return mapper.readValue(decryptedBytes, MAP_TYPE);
            } catch (Exception e) {
                Log.w(TAG, "Decryption failed. Trying to handle this event as a plain", e);
                try {
                    return mapper.readValue(data, MAP_TYPE);
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
            mapper.writeValue(writer, value);
        }
        else {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            mapper.writeValue(bufferedWriter, value);
            try {
                byte[] encryptedBytes = encrypt(byteArrayOutputStream.toByteArray());
                writer.write(Base64.encodeToString(encryptedBytes, Base64.DEFAULT));
            } catch (Exception e) {
                Log.w(TAG, "Encryption failed. Storing this event as a plain", e);
                secretKeySpec = null;
            }
        }
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

    TDJsonHandler(String encryptionKeyword) {
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

        mapper = new ObjectMapper();
        mapper.setDateFormat(SRC_DATA_FORMAT);
    }

    ///// PRIVATE CONSTANTS /////

    private static final MapType MAP_TYPE =
            TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Object.class);

    private static final SimpleDateFormat SRC_DATA_FORMAT;
    private static final SimpleDateFormat DST_DATA_FORMAT;
    static {
        TimeZone timeZone = TimeZone.getTimeZone("GMT");
        SRC_DATA_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        SRC_DATA_FORMAT.setTimeZone(timeZone);
        DST_DATA_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        DST_DATA_FORMAT.setTimeZone(timeZone);
    }

    ///// PRIVATE FIELDS /////

    private final ObjectMapper mapper;

}

