package org.tasks.time;

public class FixedMillisProvider implements MillisProvider {

  private final long millis;

  public FixedMillisProvider(long millis) {
    this.millis = millis;
  }

  @Override
  public long getMillis() {
    return millis;
  }
}
