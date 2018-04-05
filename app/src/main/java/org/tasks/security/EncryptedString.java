package org.tasks.security;

public class EncryptedString {

  private final String value;
  private final byte[] iv;

  public EncryptedString(String value, byte[] iv) {
    this.value = value;
    this.iv = iv;
  }

  public String getValue() {
    return value;
  }

  public byte[] getIv() {
    return iv;
  }
}
