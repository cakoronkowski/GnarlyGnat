package helpers;

import lombok.Synchronized;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

/**
 * Created by cakor on 12/3/2016.
 */
public class CryptoHelper {

    public static String generate(byte[] convertme){
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return byteArray2Hex(md.digest(convertme));
        }catch (Exception e)
        {
            System.err.println(e);
            return null;
        }
    }

    public static boolean check(byte[] b, String hash)
    {
        boolean result;
        try{
            result= generate(b).equals(hash);
        }catch (Exception e)
        {
            result=false;
            System.err.println(e);
        }
        return result;
    }

    private static String byteArray2Hex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

}
