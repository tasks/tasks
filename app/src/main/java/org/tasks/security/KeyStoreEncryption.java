package org.tasks.security;

import android.annotation.SuppressLint;
import android.os.Build.VERSION_CODES;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import android.util.Base64;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.inject.Inject;
import timber.log.Timber;

@RequiresApi(api = VERSION_CODES.M)
public class KeyStoreEncryption implements Encryption {

  private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
  private static final String ALIAS = "passwords";
  private static final Charset ENCODING = StandardCharsets.UTF_8;
  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 16;

  private KeyStore keyStore;

  @Inject
  public KeyStoreEncryption() {
    try {
      keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
      keyStore.load(null);
    } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
      throw new IllegalStateException();
    }
  }

  @Override
  public String encrypt(String text) {
    byte[] iv = new byte[GCM_IV_LENGTH];
    new SecureRandom().nextBytes(iv);
    Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, iv);
    try {
      byte[] output = cipher.doFinal(text.getBytes(ENCODING));
      byte[] result = new byte[iv.length + output.length];
      System.arraycopy(iv, 0, result, 0, iv.length);
      System.arraycopy(output, 0, result, iv.length, output.length);
      return Base64.encodeToString(result, Base64.DEFAULT);
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      Timber.e(e);
      return null;
    }
  }

  @Override
  public String decrypt(String text) {
    byte[] decoded = Base64.decode(text, Base64.DEFAULT);
    byte[] iv = Arrays.copyOfRange(decoded, 0, GCM_IV_LENGTH);
    Cipher cipher = getCipher(Cipher.DECRYPT_MODE, iv);
    try {
      byte[] decrypted = cipher.doFinal(decoded, GCM_IV_LENGTH, decoded.length - GCM_IV_LENGTH);
      return new String(decrypted, ENCODING);
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      Timber.e(e);
      return null;
    }
  }

  private Cipher getCipher(int cipherMode, byte[] iv) {
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(cipherMode, getSecretKey(), new GCMParameterSpec(GCM_TAG_LENGTH * Byte.SIZE, iv));
      return cipher;
    } catch (NoSuchAlgorithmException
        | NoSuchPaddingException
        | InvalidAlgorithmParameterException
        | InvalidKeyException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private SecretKey getSecretKey() {
    try {
      Entry entry = keyStore.getEntry(ALIAS, null);
      return entry == null ? generateNewKey() : ((KeyStore.SecretKeyEntry) entry).getSecretKey();
    } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableEntryException e) {
      throw new IllegalStateException();
    }
  }

  @SuppressLint("TrulyRandom")
  private SecretKey generateNewKey() {
    try {
      final KeyGenerator keyGenerator =
          KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);

      keyGenerator.init(
          new KeyGenParameterSpec.Builder(
                  ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
              .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
              .setRandomizedEncryptionRequired(false)
              .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
              .build());
      return keyGenerator.generateKey();
    } catch (NoSuchAlgorithmException
        | InvalidAlgorithmParameterException
        | NoSuchProviderException e) {
      throw new IllegalStateException();
    }
  }
}
