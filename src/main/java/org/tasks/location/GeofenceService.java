package org.tasks.location;

import android.content.ContentValues;

import com.google.common.base.Function;
import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.SynchronizeMetadataCallback;

import org.tasks.R;
import org.tasks.preferences.Preferences;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;

@Singleton
public class GeofenceService {

    private final MetadataDao metadataDao;
    private final GeofenceApi geofenceApi;

    @Inject
    public GeofenceService(MetadataDao metadataDao, GeofenceApi geofenceApi) {
        this.metadataDao = metadataDao;
        this.geofenceApi = geofenceApi;
    }

    public List<Geofence> getGeofences(long taskId) {
        return toGeofences(metadataDao.toList(Query.select(
                Metadata.PROPERTIES).where(MetadataCriteria.byTaskAndwithKey(
                taskId, GeofenceFields.METADATA_KEY)).orderBy(Order.asc(GeofenceFields.PLACE))));
    }

    public void setupGeofences() {
        geofenceApi.register(getActiveGeofences());
    }

    public void setupGeofences(long taskId) {
        geofenceApi.register(getGeofencesForTask(taskId));
    }

    public void cancelGeofences() {
        geofenceApi.cancel(getActiveGeofences());
    }

    public void cancelGeofences(long taskId) {
        for (Geofence geofence : getGeofences(taskId)) {
            geofenceApi.cancel(geofence);
        }
    }

    public boolean synchronizeGeofences(final long taskId, Set<Geofence> geofences) {
        List<Metadata> metadata = newArrayList(transform(geofences, new Function<Geofence, Metadata>() {
            @Override
            public Metadata apply(final Geofence geofence) {
                return new Metadata() {{
                    setKey(GeofenceFields.METADATA_KEY);
                    setValue(GeofenceFields.PLACE, geofence.getName());
                    setValue(GeofenceFields.LATITUDE, geofence.getLatitude());
                    setValue(GeofenceFields.LONGITUDE, geofence.getLongitude());
                    setValue(GeofenceFields.RADIUS, geofence.getRadius());
                }};
            }
        }));

        boolean changed = synchronizeMetadata(taskId, metadata, new SynchronizeMetadataCallback() {
            @Override
            public void beforeDeleteMetadata(Metadata m) {
                geofenceApi.cancel(new Geofence(m));
            }
        });

        if(changed) {
            setupGeofences(taskId);
        }
        return changed;
    }

    private List<Geofence> toGeofences(List<Metadata> geofences) {
        return newArrayList(transform(geofences, new Function<Metadata, Geofence>() {
            @Override
            public Geofence apply(Metadata metadata) {
                return new Geofence(metadata);
            }
        }));
    }

    private List<Geofence> getActiveGeofences() {
        return toGeofences(metadataDao.toList(Query.select(Metadata.ID, Metadata.TASK, GeofenceFields.PLACE, GeofenceFields.LATITUDE, GeofenceFields.LONGITUDE, GeofenceFields.RADIUS).
                join(Join.inner(Task.TABLE, Metadata.TASK.eq(Task.ID))).
                where(Criterion.and(TaskCriteria.isActive(), MetadataCriteria.withKey(GeofenceFields.METADATA_KEY)))));
    }

    private List<Geofence> getGeofencesForTask(long taskId) {
        return toGeofences(metadataDao.toList(Query.select(Metadata.ID, Metadata.TASK, GeofenceFields.PLACE, GeofenceFields.LATITUDE, GeofenceFields.LONGITUDE, GeofenceFields.RADIUS).
                join(Join.inner(Task.TABLE, Metadata.TASK.eq(Task.ID))).
                where(Criterion.and(TaskCriteria.isActive(),
                        MetadataCriteria.byTaskAndwithKey(taskId, GeofenceFields.METADATA_KEY)))));
    }

    private boolean synchronizeMetadata(long taskId, List<Metadata> metadata, final SynchronizeMetadataCallback callback) {
        final boolean[] dirty = new boolean[1];
        final Set<ContentValues> newMetadataValues = new HashSet<>();
        for(Metadata metadatum : metadata) {
            metadatum.setTask(taskId);
            metadatum.clearValue(Metadata.CREATION_DATE);
            metadatum.clearValue(Metadata.ID);

            ContentValues values = metadatum.getMergedValues();
            for(Map.Entry<String, Object> entry : values.valueSet()) {
                if(entry.getKey().startsWith("value")) //$NON-NLS-1$
                {
                    values.put(entry.getKey(), entry.getValue().toString());
                }
            }
            newMetadataValues.add(values);
        }

        metadataDao.byTaskAndKey(taskId, GeofenceFields.METADATA_KEY, new Callback<Metadata>() {
            @Override
            public void apply(Metadata item) {
                long id = item.getId();

                // clear item id when matching with incoming values
                item.clearValue(Metadata.ID);
                item.clearValue(Metadata.CREATION_DATE);
                ContentValues itemMergedValues = item.getMergedValues();

                if(newMetadataValues.contains(itemMergedValues)) {
                    newMetadataValues.remove(itemMergedValues);
                } else {
                    // not matched. cut it
                    item.setId(id);
                    if (callback != null) {
                        callback.beforeDeleteMetadata(item);
                    }
                    metadataDao.delete(id);
                    dirty[0] = true;
                }
            }
        });

        // everything that remains shall be written
        for(ContentValues values : newMetadataValues) {
            Metadata item = new Metadata();
            item.setCreationDate(DateUtilities.now());
            item.mergeWith(values);
            metadataDao.persist(item);
            dirty[0] = true;
        }

        return dirty[0];
    }
}
