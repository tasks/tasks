package org.tasks.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "filters")
public class Filter {

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "_id")
  private transient long id;

  @ColumnInfo(name = "title")
  private String title;

  @ColumnInfo(name = "sql")
  private String sql;

  @ColumnInfo(name = "values")
  private String values;

  @ColumnInfo(name = "criterion")
  private String criterion;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getSql() {
    return sql;
  }

  public void setSql(String sql) {
    this.sql = sql;
  }

  public String getValues() {
    return values;
  }

  public void setValues(String values) {
    this.values = values;
  }

  public String getCriterion() {
    return criterion;
  }

  public void setCriterion(String criterion) {
    this.criterion = criterion;
  }

  @Override
  public String toString() {
    return "Filter{"
        + "id="
        + id
        + ", title='"
        + title
        + '\''
        + ", sql='"
        + sql
        + '\''
        + ", values='"
        + values
        + '\''
        + ", criterion='"
        + criterion
        + '\''
        + '}';
  }
}
