import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.util.Base64Utils;

public class DataEncryption {
    private static final String ALGORITHM = "AES";
    private static final byte[] keyValue =
        new byte[] { 'T', 'h', 'e', 'B', 'e', 's', 't',
                      'S', 'e', 'c', 'r','e', 't', 'K', 'e', 'y' };

    public static String encryptData(String data) throws Exception {
        SecretKeySpec key = new SecretKeySpec(keyValue, ALGORITHM);
        Cipher c = Cipher.getInstance(ALGORITHM);
        c.init(Cipher.ENCRYPT_MODE, key);
        byte[] encVal = c.doFinal(data.getBytes());
        return Base64Utils.encodeToString(encVal);
    }

    public static String decryptData(String encryptedData) throws Exception {
        SecretKeySpec key = new SecretKeySpec(keyValue, ALGORITHM);
        Cipher c = Cipher.getInstance(ALGORITHM);
        c.init(Cipher.DECRYPT_MODE, key);
        byte[] decordedValue = Base64Utils.decodeFromString(encryptedData);
        byte[] decValue = c.doFinal(decordedValue);
        return new String(decValue);
    }
}
