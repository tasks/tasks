package org.tasks.security;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

public interface Encryption {

  EncryptedString encrypt(String text)
      throws UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException;

  String decrypt(EncryptedString encryptedString)
      throws IOException, BadPaddingException, IllegalBlockSizeException;
}
