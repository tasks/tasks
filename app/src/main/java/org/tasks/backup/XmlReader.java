package org.tasks.backup;

import org.xmlpull.v1.XmlPullParser;

public class XmlReader {

  private static final String XML_NULL = "null"; // $NON-NLS-1$
  private final XmlPullParser xpp;

  public XmlReader(XmlPullParser xpp) {
    this.xpp = xpp;
  }

  public Long readLong(String name) {
    final String value = xpp.getAttributeValue(null, name);
    return value == null || XML_NULL.equals(value) ? null : Long.parseLong(value);
  }

  public void readLong(String name, ValueWriter<Long> writer) {
    final Long value = readLong(name);
    if (value != null) {
      writer.write(value);
    }
  }

  public Integer readInteger(String name) {
    final String value = xpp.getAttributeValue(null, name);
    return value == null || XML_NULL.equals(value) ? null : Integer.parseInt(value);
  }

  public void readInteger(String name, ValueWriter<Integer> writer) {
    final Integer value = readInteger(name);
    if (value != null) {
      writer.write(value);
    }
  }

  public String readString(String name) {
    return xpp.getAttributeValue(null, name);
  }

  public void readString(String name, ValueWriter<String> writer) {
    final String value = readString(name);
    if (value != null) {
      writer.write(value);
    }
  }

  public Double readDouble(String name) {
    final String value = xpp.getAttributeValue(null, name);
    return value == null || XML_NULL.equals(value) ? null : Double.parseDouble(value);
  }

  public interface ValueWriter<T> {

    void write(T value);
  }
}
