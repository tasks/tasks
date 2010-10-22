package com.todoroo.astrid.widget;

import java.util.Date;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.service.TaskService;

public class EncouragementService {

    @Autowired private TaskService taskService;

    private final EncouragementProvider[] providers = new EncouragementProvider[] {
            new ResourceEncouragementProvider(),
            new TimeOfDayEncouragementProvider(),
            new CompletionCountEncouragementProvider()
    };

    public EncouragementService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    public String getEncouragement() {
        int index = (int)Math.floor(Math.random() * providers.length);
        return providers[index].getEncouragement();
    }


    public interface EncouragementProvider {
        public String getEncouragement();
    }

    public static class ResourceEncouragementProvider implements EncouragementProvider {
        @Override
        public String getEncouragement() {
            String[] encouragements =  ContextManager.getResources().getStringArray(R.array.PPW_encouragements);
            int encouragementIdx = (int)Math.floor(Math.random() * encouragements.length);
            return encouragements[encouragementIdx];
        }
    }

    public static class TimeOfDayEncouragementProvider implements EncouragementProvider {
        private static int GOOD_MORNING = 0;
        private static int GOOD_AFTERNOON = 1;
        private static int GOOD_EVENING = 2;
        private static int LATE_NIGHT = 3;
        @Override
        public String getEncouragement() {
            String[] encouragements =  ContextManager.getResources().
                getStringArray(R.array.PPW_encouragements_tod);

            int index = (int)Math.floor(Math.random() * (encouragements.length / 4));

            int hour = new Date().getHours();
            if(hour >= 6 && hour < 12)
                return encouragements[4*index + GOOD_MORNING];
            else if(hour >= 12 && hour < 18)
                return encouragements[4*index + GOOD_AFTERNOON];
            else if(hour >= 18 && hour < 23)
                return encouragements[4*index + GOOD_EVENING];

            return encouragements[4*index + LATE_NIGHT];
        }
    }

    public class CompletionCountEncouragementProvider implements EncouragementProvider {
        @Override
        public String getEncouragement() {
            Filter filter = new Filter(null, null, new QueryTemplate().where(TaskCriteria.completed()), null);
            int completed = taskService.countTasks(filter);

            String[] encouragements =  ContextManager.getResources().
                getStringArray(R.array.PPW_encouragements_completed);
            int index = (int)Math.floor(Math.random() * (encouragements.length));

            return String.format(encouragements[index], completed);
        }
    }
}
