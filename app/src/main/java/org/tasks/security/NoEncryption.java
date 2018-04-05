package org.tasks.security;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

public class NoEncryption implements Encryption {

  @Override
  public EncryptedString encrypt(String text)
      throws UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException {
    return new EncryptedString(text, null);
  }

  @Override
  public String decrypt(EncryptedString encryptedString)
      throws IOException, BadPaddingException, IllegalBlockSizeException {
    return encryptedString.getValue();
  }
}
