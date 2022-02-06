package com.todoroo.astrid.helper;

import java.util.UUID;

public class UUIDHelper {
  /** @return a newly generated uuid */
  public static String newUUID() {
    return UUID.randomUUID().toString();
  }
}
