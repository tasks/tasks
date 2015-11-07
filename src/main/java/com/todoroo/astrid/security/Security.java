package com.todoroo.astrid.security;

/**
 * Created by Jiayu Hu.
 * This class handle basic AES encryption functions
 */

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;


public class Security {

    private static SecretKeySpec secretKey ;
    private static byte[] key ;

    public static String strToEncrypt;
    public static String strPassword;
    private static String encryptedStr;
    private static String decryptedStr;

    //public static void main(String args[]) {

        //final String strToEncrypt = "String to Encrypt";
        //final String strPssword = "123456";
        //Security.setKey(strPssword);

        //Security.encrypt(strToEncrypt.trim());

        //System.out.println("String to Encrypt: " + strToEncrypt);
        //System.out.println("Encrypted: " + Security.getEncryptedStr());

        //final String strToDecrypt =  Security.getEncryptedStr();
        //Security.decrypt(strToDecrypt.trim());

        //System.out.println("String To Decrypt : " + strToDecrypt);
        //System.out.println("Decrypted : " + Security.getDecryptedStr());

    //}

    public static void setKey(String myKey) {

        MessageDigest sha = null;

        try {
            key = myKey.getBytes("UTF-8");
            //System.out.println(key.length);
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // use only first 128 bit
            //System.out.println(key.length);
            //System.out.println(key);
            //System.out.println(new String(key,"UTF-8"));

            secretKey = new SecretKeySpec(key, "AES");

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    public static void setStrToEncrypt(String str) {

        Security.strToEncrypt = str.trim();

    }

    public static void setPassword(String password) {

        Security.strPassword = password;

    }

    public static void encrypt(String strToEncrypt) {

        try {

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            setEncryptedStr(Base64.encodeBase64String(cipher.doFinal(strToEncrypt.getBytes("UTF-8"))));

        } catch (Exception e) {

            System.out.println("Error while encrypting: "+e.toString());
        }

    }

    public static void decrypt(String strToDecrypt) {

        try {

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");

            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            setDecryptedStr(new String(cipher.doFinal(Base64.decodeBase64(strToDecrypt))));

        } catch (Exception e) {

            System.out.println("Error while decrypting: "+e.toString());

        }

    }

    public static void setEncryptedStr(String encryptedStr) {

        Security.encryptedStr = encryptedStr;

    }

    public static String getEncryptedStr() {

        return encryptedStr;

    }

    public static void setDecryptedStr(String decryptedString) {

        Security.decryptedStr = decryptedString;

    }

    public static String getDecryptedString() {

        return decryptedStr;

    }

}