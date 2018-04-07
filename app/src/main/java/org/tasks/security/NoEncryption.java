package org.tasks.security;

public class NoEncryption implements Encryption {

  @Override
  public String encrypt(String text) {
    return text;
  }

  @Override
  public String decrypt(String text) {
    return text;
  }
}
