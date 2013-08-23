package com.todoroo.astrid.actfm.sync.messages;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.dao.HistoryDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.dao.WaitingOnMeDao;
import com.todoroo.astrid.data.History;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.data.WaitingOnMe;

public class ConvertSelfUserIdsToZero {

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private TagDataDao tagDataDao;

    @Autowired
    private HistoryDao historyDao;

    @Autowired
    private UserActivityDao userActivityDao;

    @Autowired
    private WaitingOnMeDao waitingOnMeDao;

    public ConvertSelfUserIdsToZero() {
        DependencyInjectionService.getInstance().inject(this);
    }

    public synchronized void execute(String selfId) {
        if (RemoteModel.isValidUuid(selfId)) {
            updateDatabase(taskDao, new Task(), Task.CREATOR_ID, selfId);
            updateDatabase(taskDao, new Task(), Task.USER_ID, selfId);
            updateDatabase(tagDataDao, new TagData(), TagData.USER_ID, selfId);
            updateDatabase(historyDao, new History(), History.USER_UUID, selfId);
            updateDatabase(userActivityDao, new UserActivity(), UserActivity.USER_UUID, selfId);
            updateDatabase(waitingOnMeDao, new WaitingOnMe(), WaitingOnMe.WAITING_USER_ID, selfId);
        }
    }

    public synchronized void execute() {
        String selfId = ActFmPreferenceService.userId();
        execute(selfId);
    }

    private <T extends AbstractModel> void updateDatabase(DatabaseDao<T> dao, T instance, StringProperty property, String selfId) {
        instance.setValue(property, Task.USER_ID_SELF);
        instance.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
        dao.update(property.eq(selfId), instance);
    }

}
