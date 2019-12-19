package org.tasks.data;

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

  public long getParent() {
    return cd_parent;
  }

  public void setParent(long parent) {
    cd_parent = parent;
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

    if (cd_id != that.cd_id) {
      return false;
    }
    if (cd_parent != that.cd_parent) {
      return false;
    }
    if (cd_calendar != null ? !cd_calendar.equals(that.cd_calendar) : that.cd_calendar != null) {
      return false;
    }
    return cd_remote_parent != null
        ? cd_remote_parent.equals(that.cd_remote_parent)
        : that.cd_remote_parent == null;
  }

  @Override
  public int hashCode() {
    int result = (int) (cd_id ^ (cd_id >>> 32));
    result = 31 * result + (cd_calendar != null ? cd_calendar.hashCode() : 0);
    result = 31 * result + (int) (cd_parent ^ (cd_parent >>> 32));
    result = 31 * result + (cd_remote_parent != null ? cd_remote_parent.hashCode() : 0);
    return result;
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
