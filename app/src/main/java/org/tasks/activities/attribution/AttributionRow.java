package org.tasks.activities.attribution;

public class AttributionRow {

  private final boolean isHeader;
  private final String license;
  private final String copyrightHolder;
  private final String libraries;

  AttributionRow(String license) {
    this.license = license;
    isHeader = true;
    copyrightHolder = null;
    libraries = null;
  }

  AttributionRow(String copyrightHolder, String libraries) {
    this.copyrightHolder = copyrightHolder;
    this.libraries = libraries;
    isHeader = false;
    license = null;
  }

  boolean isHeader() {
    return isHeader;
  }

  public String getLicense() {
    return license;
  }

  String getCopyrightHolder() {
    return copyrightHolder;
  }

  public String getLibraries() {
    return libraries;
  }
}
