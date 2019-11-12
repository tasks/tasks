package org.tasks.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.common.base.Strings;
import com.todoroo.andlib.utility.AndroidUtilities;
import java.util.Map;
import org.tasks.themes.CustomIcons;

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

  @ColumnInfo(name = "f_color")
  private Integer color = -1;

  @ColumnInfo(name = "f_icon")
  private Integer icon = -1;

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
    // TODO: replace dirty hack for missing column
    return sql.replace("tasks.userId=0", "1");
  }

  public void setSql(String sql) {
    this.sql = sql;
  }

  public String getValues() {
    return values;
  }

  public Map<String, Object> getValuesAsMap() {
    return Strings.isNullOrEmpty(values) ? null : AndroidUtilities.mapFromSerializedString(values);
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

  public Integer getColor() {
    return color == null ? -1 : color;
  }

  public void setColor(Integer color) {
    this.color = color;
  }

  public Integer getIcon() {
    return icon == null ? CustomIcons.getFILTER() : icon;
  }

  public void setIcon(Integer icon) {
    this.icon = icon;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Filter)) {
      return false;
    }

    Filter filter = (Filter) o;

    if (id != filter.id) {
      return false;
    }
    if (title != null ? !title.equals(filter.title) : filter.title != null) {
      return false;
    }
    if (sql != null ? !sql.equals(filter.sql) : filter.sql != null) {
      return false;
    }
    if (values != null ? !values.equals(filter.values) : filter.values != null) {
      return false;
    }
    if (criterion != null ? !criterion.equals(filter.criterion) : filter.criterion != null) {
      return false;
    }
    if (color != null ? !color.equals(filter.color) : filter.color != null) {
      return false;
    }
    return icon != null ? icon.equals(filter.icon) : filter.icon == null;
  }

  @Override
  public int hashCode() {
    int result = (int) (id ^ (id >>> 32));
    result = 31 * result + (title != null ? title.hashCode() : 0);
    result = 31 * result + (sql != null ? sql.hashCode() : 0);
    result = 31 * result + (values != null ? values.hashCode() : 0);
    result = 31 * result + (criterion != null ? criterion.hashCode() : 0);
    result = 31 * result + (color != null ? color.hashCode() : 0);
    result = 31 * result + (icon != null ? icon.hashCode() : 0);
    return result;
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
        + ", color="
        + color
        + ", icon="
        + icon
        + '}';
  }
}
