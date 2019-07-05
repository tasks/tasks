package org.tasks;

public class Objects {
  private Objects() {}

  public static boolean equals(Object a, Object b) {
    return (a == b) || (a != null && a.equals(b));
  }
}
