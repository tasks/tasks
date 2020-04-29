package org.tasks.data;

import static org.tasks.Strings.isNullOrEmpty;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.todoroo.andlib.utility.AndroidUtilities;
import java.util.Map;
import java.util.Objects;
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
  private Integer color = 0;

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
    return isNullOrEmpty(values) ? null : AndroidUtilities.mapFromSerializedString(values);
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
    return color == null ? 0 : color;
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
    return id == filter.id
        && Objects.equals(title, filter.title)
        && Objects.equals(sql, filter.sql)
        && Objects.equals(values, filter.values)
        && Objects.equals(criterion, filter.criterion)
        && Objects.equals(color, filter.color)
        && Objects.equals(icon, filter.icon);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, title, sql, values, criterion, color, icon);
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
