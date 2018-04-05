package org.tasks.security;

import android.annotation.SuppressLint;
import android.os.Build.VERSION_CODES;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.inject.Inject;

@RequiresApi(api = VERSION_CODES.M)
public class KeyStoreEncryption implements Encryption {

  private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
  private static final String ALIAS = "passwords";
  private static final String ENCODING = "UTF-8";

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
  public EncryptedString encrypt(String text)
      throws UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException {
    Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, null);
    return new EncryptedString(
        new String(cipher.doFinal(text.getBytes(ENCODING)), ENCODING), cipher.getIV());
  }

  @Override
  public String decrypt(EncryptedString encryptedString)
      throws IOException, BadPaddingException, IllegalBlockSizeException {
    Cipher cipher = getCipher(Cipher.DECRYPT_MODE, encryptedString.getIv());
    return new String(cipher.doFinal(encryptedString.getValue().getBytes(ENCODING)), ENCODING);
  }

  private Cipher getCipher(int cipherMode, byte[] iv) {
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      if (cipherMode == Cipher.ENCRYPT_MODE) {
        cipher.init(cipherMode, getSecretKey());
      } else {
        cipher.init(cipherMode, getSecretKey(), new GCMParameterSpec(128, iv));
      }
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
