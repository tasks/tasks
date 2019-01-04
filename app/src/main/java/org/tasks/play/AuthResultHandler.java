package org.tasks.play;

public interface AuthResultHandler {
  void authenticationSuccessful(String accountName);

  void authenticationFailed(String message);
}
