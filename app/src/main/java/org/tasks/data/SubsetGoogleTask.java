package org.tasks.data;

import java.util.Objects;

public class SubsetGoogleTask {

  public long gt_id;
  public long gt_parent;
  public String gt_list_id;
  public long gt_order;

  public long getId() {
    return gt_id;
  }

  public String getListId() {
    return gt_list_id;
  }

  public long getParent() {
    return gt_parent;
  }

  public void setParent(long parent) {
    gt_parent = parent;
  }

  public long getOrder() {
    return gt_order;
  }

  public void setOrder(long order) {
    gt_order = order;
  }

  public int getIndent() {
    return gt_parent > 0 ? 1 : 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SubsetGoogleTask)) {
      return false;
    }
    SubsetGoogleTask that = (SubsetGoogleTask) o;
    return gt_id == that.gt_id
        && gt_parent == that.gt_parent
        && gt_order == that.gt_order
        && Objects.equals(gt_list_id, that.gt_list_id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(gt_id, gt_parent, gt_list_id, gt_order);
  }

  @Override
  public String toString() {
    return "SubsetGoogleTask{"
        + "gt_id="
        + gt_id
        + ", gt_parent="
        + gt_parent
        + ", gt_list_id='"
        + gt_list_id
        + '\''
        + ", gt_order="
        + gt_order
        + '}';
  }
}
