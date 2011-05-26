package greendroid.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5Util {
    
    private static MessageDigest sMd5MessageDigest;
    private static StringBuilder sStringBuilder;
    
    static {
        try {
            sMd5MessageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // TODO cyril: I'm quite sure about my "MD5" algorithm
            // but this is not a correct way to handle an exception ...
        }
        sStringBuilder = new StringBuilder();
    }
    
    private Md5Util() {
    }
    
    public static String md5(String s) {
        
        sMd5MessageDigest.reset();
        sMd5MessageDigest.update(s.getBytes());
        
        byte digest[] = sMd5MessageDigest.digest();
        
        sStringBuilder.setLength(0);
        for (int i=0; i<digest.length; i++) {
            sStringBuilder.append(Integer.toHexString(0xFF & digest[i]));
        }
                
        return sStringBuilder.toString();
    }
}
