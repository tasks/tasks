package org.tasks.security;

public interface Encryption {

  String encrypt(String text);

  String decrypt(String text);
}
