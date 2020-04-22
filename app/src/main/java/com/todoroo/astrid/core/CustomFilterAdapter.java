/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.core;

import static com.google.common.collect.Lists.transform;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.common.base.Joiner;
import com.todoroo.andlib.sql.UnaryCriterion;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.tasks.Callback;
import org.tasks.R;
import org.tasks.locale.Locale;

public class CustomFilterAdapter extends RecyclerView.Adapter<CriterionViewHolder> {

  private final Callback<CriterionInstance> onClick;
  private final List<CriterionInstance> objects;
  private final Locale locale;

  public CustomFilterAdapter(
      List<CriterionInstance> objects,
      Locale locale,
      Callback<CriterionInstance> onClick) {
    this.objects = objects;
    this.locale = locale;
    this.onClick = onClick;
  }

  @NonNull
  @Override
  public CriterionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    return new CriterionViewHolder(
        inflater.inflate(R.layout.custom_filter_row, parent, false), locale, onClick);
  }

  @Override
  public void onBindViewHolder(@NonNull CriterionViewHolder holder, int position) {
    holder.bind(getItem(position));
  }

  public List<CriterionInstance> getItems() {
    return objects;
  }

  @Override
  public int getItemCount() {
    return objects.size();
  }

  private String getValue(CriterionInstance instance) {
    String value = instance.getValueFromCriterion();
    if (value == null && instance.criterion.sql != null && instance.criterion.sql.contains("?")) {
      value = "";
    }
    return value;
  }

  public String getSql() {
    StringBuilder sql = new StringBuilder(" WHERE ");
    for (CriterionInstance instance : objects) {
      String value = getValue(instance);

      switch (instance.type) {
        case CriterionInstance.TYPE_ADD:
          sql.append("OR ");
          break;
        case CriterionInstance.TYPE_SUBTRACT:
          sql.append("AND NOT ");
          break;
        case CriterionInstance.TYPE_INTERSECT:
          sql.append("AND ");
          break;
      }

      // special code for all tasks universe
      if (instance.type == CriterionInstance.TYPE_UNIVERSE || instance.criterion.sql == null) {
        sql.append(TaskCriteria.activeAndVisible()).append(' ');
      } else {
        String subSql = instance.criterion.sql.replace("?", UnaryCriterion.sanitize(value));
        sql.append(Task.ID).append(" IN (").append(subSql).append(") ");
      }
    }
    return sql.toString();
  }

  public Map<String, Object> getValues() {
    Map<String, Object> values = new HashMap<>();
    for (CriterionInstance instance : objects) {
      String value = getValue(instance);

      if (instance.criterion.valuesForNewTasks != null
          && instance.type == CriterionInstance.TYPE_INTERSECT) {
        for (Entry<String, Object> entry : instance.criterion.valuesForNewTasks.entrySet()) {
          values.put(
              entry.getKey().replace("?", value), entry.getValue().toString().replace("?", value));
        }
      }
    }
    return values;
  }

  public String getCriterion() {
    return Joiner.on("\n").join(transform(objects, CriterionInstance::serialize));
  }

  public CriterionInstance getItem(int position) {
    return objects.get(position);
  }

  public void remove(CriterionInstance criterionInstance) {
    objects.remove(criterionInstance);
  }

  public void replace(CriterionInstance replace, CriterionInstance instance) {
    objects.set(objects.indexOf(replace), instance);
  }

  public void add(CriterionInstance instance) {
    objects.add(instance);
  }
}
