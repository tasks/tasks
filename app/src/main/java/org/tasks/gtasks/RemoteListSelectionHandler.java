package org.tasks.gtasks;

import com.todoroo.astrid.api.Filter;

public interface RemoteListSelectionHandler {

  void addAccount();

  void selectedList(Filter list);
}
