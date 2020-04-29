package org.tasks.data;

import java.util.Objects;

public class SubsetCaldav {
  public long cd_id;
  public String cd_calendar;
  public long cd_parent;
  public String cd_remote_parent;

  public long getId() {
    return cd_id;
  }

  public String getCalendar() {
    return cd_calendar;
  }

  public String getRemoteParent() {
    return cd_remote_parent;
  }

  public void setRemoteParent(String remoteId) {
    cd_remote_parent = remoteId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SubsetCaldav)) {
      return false;
    }
    SubsetCaldav that = (SubsetCaldav) o;
    return cd_id == that.cd_id
        && cd_parent == that.cd_parent
        && Objects.equals(cd_calendar, that.cd_calendar)
        && Objects.equals(cd_remote_parent, that.cd_remote_parent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cd_id, cd_calendar, cd_parent, cd_remote_parent);
  }

  @Override
  public String toString() {
    return "SubsetCaldav{"
        + "cd_id="
        + cd_id
        + ", cd_calendar='"
        + cd_calendar
        + '\''
        + ", cd_parent="
        + cd_parent
        + ", cd_remote_parent='"
        + cd_remote_parent
        + '\''
        + '}';
  }
}
