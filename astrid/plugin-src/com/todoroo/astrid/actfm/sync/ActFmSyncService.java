/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.ConditionVariable;
import android.text.TextUtils;
import android.util.Log;

import com.timsu.astrid.GCMIntentService;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.billing.BillingConstants;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.UserDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.service.abtesting.ABTestEventReportingService;
import com.todoroo.astrid.subtasks.SubtasksHelper;
import com.todoroo.astrid.tags.reusable.FeaturedListFilterExposer;

/**
 * Service for synchronizing data on Astrid.com server with local.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public final class ActFmSyncService {

    // --- instance variables

    @Autowired TagDataService tagDataService;
    @Autowired MetadataService metadataService;
    @Autowired TaskService taskService;
    @Autowired ActFmPreferenceService actFmPreferenceService;
    @Autowired GtasksPreferenceService gtasksPreferenceService;
    @Autowired ActFmInvoker actFmInvoker;
    @Autowired TaskDao taskDao;
    @Autowired TagDataDao tagDataDao;
    @Autowired UserDao userDao;
    @Autowired MetadataDao metadataDao;
    @Autowired ABTestEventReportingService abTestEventReportingService;

    private String token;

    public ActFmSyncService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    private Thread pushOrderThread = null;
    private Runnable pushTagOrderRunnable;
    private final List<Object> pushOrderQueue = Collections.synchronizedList(new LinkedList<Object>());

    private final AtomicInteger taskPushThreads = new AtomicInteger(0);
    private final ConditionVariable waitUntilEmpty = new ConditionVariable(true);

    private static final long WAIT_BEFORE_PUSH_ORDER = 15 * 1000;
    private void initializeTagOrderRunnable() {
        pushTagOrderRunnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if(pushOrderQueue.isEmpty()) {
                        synchronized(ActFmSyncService.this) {
                            pushOrderThread = null;
                            return;
                        }
                    }
                    if (pushOrderQueue.size() > 0) {
                        AndroidUtilities.sleepDeep(WAIT_BEFORE_PUSH_ORDER);
                        try {
                            Object id = pushOrderQueue.remove(0);
                            if (id instanceof Long) {
                                Long tagDataId = (Long) id;
                                TagData td = tagDataService.fetchById(tagDataId, TagData.ID, TagData.UUID, TagData.TAG_ORDERING);
                                if (td != null) {
                                    pushTagOrdering(td);
                                }
                            } else if (id instanceof String) {
                                String filterId = (String) id;
                                pushFilterOrdering(filterId);
                            }
                        } catch (IndexOutOfBoundsException e) {
                            // In case element was removed
                        }
                    }
                }
            }
        };
    }

    public void waitUntilEmpty() {
        waitUntilEmpty.block();
    }

    private void addAbTestEventInfo(List<Object> params) {
        JSONArray abTestInfo = abTestEventReportingService.getTestsWithVariantsArray();
        try {
            for (int i = 0; i < abTestInfo.length(); i++) {
                params.add("ab_variants[]"); params.add(abTestInfo.getString(i));
            }
        } catch (JSONException e) {
            Log.e("Error parsing AB test info", abTestInfo.toString(), e);
        }
    }

    //----------------- Push ordering
    public void pushTagOrderingOnSave(long tagDataId) {
        pushOrderingOnSave(tagDataId);
    }

    public void pushFilterOrderingOnSave(String filterId) {
        pushOrderingOnSave(filterId);
    }

    private void pushOrderingOnSave(Object id) {
        if (!pushOrderQueue.contains(id)) {
            pushOrderQueue.add(id);
            synchronized(this) {
                if(pushOrderThread == null) {
                    pushOrderThread = new Thread(pushTagOrderRunnable);
                    pushOrderThread.start();
                }
            }
        }
    }

    public void pushTagOrderingImmediately(TagData tagData) {
        if (pushOrderQueue.contains(tagData.getId())) {
            pushOrderQueue.remove(tagData.getId());
        }
        pushTagOrdering(tagData);
    }

    public void pushFilterOrderingImmediately(String filterId) {
        if (pushOrderQueue.contains(filterId)) {
            pushOrderQueue.remove(filterId);
        }
        pushFilterOrdering(filterId);
    }

    public boolean cancelTagOrderingPush(long tagDataId) {
        if (pushOrderQueue.contains(tagDataId)) {
            pushOrderQueue.remove(tagDataId);
            return true;
        }
        return false;
    }

    public boolean cancelFilterOrderingPush(String filterId) {
        if (pushOrderQueue.contains(filterId)) {
            pushOrderQueue.remove(filterId);
            return true;
        }
        return false;
    }

    private void pushTagOrdering(TagData tagData) {
        if (!checkForToken())
            return;

        String remoteId = tagData.getValue(TagData.UUID);
        if (!RemoteModel.isValidUuid(remoteId))
            return;

        // Make sure that all tasks are pushed before attempting to sync tag ordering
        waitUntilEmpty();

        ArrayList<Object> params = new ArrayList<Object>();

        params.add("tag_id"); params.add(remoteId);
        params.add("order");
        params.add(SubtasksHelper.convertTreeToRemoteIds(tagData.getValue(TagData.TAG_ORDERING)));
        params.add("token"); params.add(token);

        try {
            actFmInvoker.invoke("list_order", params.toArray(new Object[params.size()]));
        } catch (IOException e) {
            handleException("push-tag-order", e);
        }
    }

    private void pushFilterOrdering(String filterLocalId) {
        if (!checkForToken())
            return;

        String filterId = SubtasksHelper.serverFilterOrderId(filterLocalId);
        if (filterId == null)
            return;

        // Make sure that all tasks are pushed before attempting to sync filter ordering
        waitUntilEmpty();

        ArrayList<Object> params = new ArrayList<Object>();
        String order = Preferences.getStringValue(filterLocalId);
        if (order == null || "null".equals(order))
            order = "[]";

        params.add("filter"); params.add(filterId);
        params.add("order"); params.add(SubtasksHelper.convertTreeToRemoteIds(order));
        params.add("token"); params.add(token);

        try {
            actFmInvoker.invoke("list_order", params.toArray(new Object[params.size()]));
        } catch (IOException e) {
            handleException("push-filter-order", e);
        }
    }

    // --- data fetch methods
    public int fetchFeaturedLists(int serverTime) throws JSONException, IOException {
        if (!checkForToken())
            return 0;
        JSONObject result = actFmInvoker.invoke("featured_lists",
                "token", token, "modified_after", serverTime);
        JSONArray featuredLists = result.getJSONArray("list");
        if (featuredLists.length() > 0)
            Preferences.setBoolean(FeaturedListFilterExposer.PREF_SHOULD_SHOW_FEATURED_LISTS, true);

        for (int i = 0; i < featuredLists.length(); i++) {
            JSONObject featObject = featuredLists.getJSONObject(i);
            tagDataService.saveFeaturedList(featObject);
        }

        return result.optInt("time", 0);
    }

    public void updateUserSubscriptionStatus(Runnable onSuccess, Runnable onRecoverableError, Runnable onInvalidToken) {
        String purchaseToken = Preferences.getStringValue(BillingConstants.PREF_PURCHASE_TOKEN);
        String productId = Preferences.getStringValue(BillingConstants.PREF_PRODUCT_ID);
        try {
            if (!checkForToken())
                throw new ActFmServiceException("Not logged in", null);

            ArrayList<Object> params = new ArrayList<Object>();
            params.add("purchase_token"); params.add(purchaseToken);
            params.add("product_id"); params.add(productId);
            addAbTestEventInfo(params);
            params.add("token"); params.add(token);

            actFmInvoker.invoke("premium_update_android", params.toArray(new Object[params.size()]));
            Preferences.setBoolean(BillingConstants.PREF_NEEDS_SERVER_UPDATE, false);
            if (onSuccess != null)
                onSuccess.run();
        } catch (Exception e) {
            if (e instanceof ActFmServiceException) {
                ActFmServiceException ae = (ActFmServiceException)e;
                if (ae.result != null && ae.result.optString("status").equals("error")) {
                    if (ae.result.optString("code").equals("invalid_purchase_token")) { // Not a valid purchase--expired or duolicate
                        Preferences.setBoolean(ActFmPreferenceService.PREF_LOCAL_PREMIUM, false);
                        Preferences.setBoolean(BillingConstants.PREF_NEEDS_SERVER_UPDATE, false);
                        if (onInvalidToken != null)
                            onInvalidToken.run();
                        return;
                    }
                }
            }
            Preferences.setBoolean(BillingConstants.PREF_NEEDS_SERVER_UPDATE, true);
            if (onRecoverableError != null)
                onRecoverableError.run();
        }
    }

    public void setGCMRegistration(String regId) {
        try {
            String deviceId = GCMIntentService.getDeviceID();
            String existingC2DM = Preferences.getStringValue(GCMIntentService.PREF_C2DM_REGISTRATION);

            ArrayList<Object> params = new ArrayList<Object>();
            params.add("gcm"); params.add(regId);
            if (!TextUtils.isEmpty(deviceId)) {
                params.add("device_id"); params.add(deviceId);
            }
            if (!TextUtils.isEmpty(existingC2DM)) { // Unregisters C2DM with the server for migration purposes
                params.add("c2dm"); params.add(existingC2DM);
            }

            invoke("user_set_gcm", params.toArray(new Object[params.size()]));

            Preferences.setString(GCMIntentService.PREF_REGISTRATION, regId);
            Preferences.setString(GCMIntentService.PREF_C2DM_REGISTRATION, null);
            Preferences.setString(GCMIntentService.PREF_NEEDS_REGISTRATION, null);
        } catch (IOException e) {
            Preferences.setString(GCMIntentService.PREF_NEEDS_REGISTRATION, regId);
            Log.e("gcm", "error-gcm-register", e);
        }
    }

    // --- generic invokation

    /** invoke authenticated method against the server */
    public JSONObject invoke(String method, Object... getParameters) throws IOException,
    ActFmServiceException {
        if(!checkForToken())
            throw new ActFmServiceException("not logged in", null);
        Object[] parameters = new Object[getParameters.length + 2];
        parameters[0] = "token";
        parameters[1] = token;
        for(int i = 0; i < getParameters.length; i++)
            parameters[i+2] = getParameters[i];
        return actFmInvoker.invoke(method, parameters);
    }
//
//    // --- helpers
//    private void synchronizeAttachments(JSONObject item, Task model) {
//        TodorooCursor<Metadata> attachments = metadataService.query(Query.select(Metadata.PROPERTIES)
//                .where(Criterion.and(MetadataCriteria.byTaskAndwithKey(model.getId(),
//                        FileMetadata.METADATA_KEY), FileMetadata.REMOTE_ID.gt(0))));
//        try {
//            HashMap<Long, Metadata> currentFiles = new HashMap<Long, Metadata>();
//            for (attachments.moveToFirst(); !attachments.isAfterLast(); attachments.moveToNext()) {
//                Metadata m = new Metadata(attachments);
//                currentFiles.put(m.getValue(FileMetadata.REMOTE_ID), m);
//            }
//
//            JSONArray remoteFiles = item.getJSONArray("attachments");
//            for (int i = 0; i < remoteFiles.length(); i++) {
//                JSONObject file = remoteFiles.getJSONObject(i);
//
//                long id = file.optLong("id");
//                if (currentFiles.containsKey(id)) {
//                    // Match, make sure name and url are up to date, then remove from map
//                    Metadata fileMetadata = currentFiles.get(id);
//                    fileMetadata.setValue(FileMetadata.URL, file.getString("url"));
//                    fileMetadata.setValue(FileMetadata.NAME, file.getString("name"));
//                    metadataService.save(fileMetadata);
//                    currentFiles.remove(id);
//                } else {
//                    // Create new file attachment
//                    Metadata newAttachment = FileMetadata.createNewFileMetadata(model.getId(), "",
//                            file.getString("name"), file.getString("content_type"));
//                    String url = file.getString("url");
//                    newAttachment.setValue(FileMetadata.URL, url);
//                    newAttachment.setValue(FileMetadata.REMOTE_ID, id);
//                    metadataService.save(newAttachment);
//                }
//            }
//
//            // Remove all the leftovers
//            Set<Long> attachmentsToDelete = currentFiles.keySet();
//            for (Long remoteId : attachmentsToDelete) {
//                Metadata toDelete = currentFiles.get(remoteId);
//                String path = toDelete.getValue(FileMetadata.FILE_PATH);
//                if (TextUtils.isEmpty(path))
//                    metadataService.delete(toDelete);
//                else {
//                    File f = new File(toDelete.getValue(FileMetadata.FILE_PATH));
//                    if (!f.exists() || f.delete()) {
//                        metadataService.delete(toDelete);
//                    }
//
//                }
//            }
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//        } finally {
//            attachments.close();
//        }
//    }


    protected void handleException(String message, Exception exception) {
        Log.w("actfm-sync", message, exception);
    }

    private boolean checkForToken() {
        if(!actFmPreferenceService.isLoggedIn())
            return false;
        token = actFmPreferenceService.getToken();
        return true;
    }

    // --- json reader helper

    /**
     * Read data models from JSON
     */
    public static class JsonHelper {

        protected static long readDate(JSONObject item, String key) {
            return item.optLong(key, 0) * 1000L;
        }

        public static void jsonFromUser(JSONObject json, User model) throws JSONException {
            json.put("id", model.getValue(User.UUID));
            json.put("name", model.getDisplayName());
            json.put("email", model.getValue(User.EMAIL));
            json.put("picture", model.getPictureUrl(User.PICTURE, RemoteModel.PICTURE_THUMB));
        }

        public static void featuredListFromJson(JSONObject json, TagData model) throws JSONException {
            parseTagDataFromJson(json, model, true);
        }

        private static void parseTagDataFromJson(JSONObject json, TagData model, boolean featuredList) throws JSONException {
            model.clearValue(TagData.UUID);
            model.setValue(TagData.UUID, Long.toString(json.getLong("id")));
            model.setValue(TagData.NAME, json.getString("name"));

            if (featuredList)
                model.setFlag(TagData.FLAGS, TagData.FLAG_FEATURED, true);

            if(json.has("picture"))
                model.setValue(TagData.PICTURE, json.optString("picture", ""));
            if(json.has("thumb"))
                model.setValue(TagData.THUMB, json.optString("thumb", ""));

            if(json.has("is_silent"))
                model.setFlag(TagData.FLAGS, TagData.FLAG_SILENT,json.getBoolean("is_silent"));

            if(!json.isNull("description"))
                model.setValue(TagData.TAG_DESCRIPTION, json.getString("description"));

            if(json.has("members")) {
                JSONArray members = json.getJSONArray("members");
                model.setValue(TagData.MEMBERS, members.toString());
                model.setValue(TagData.MEMBER_COUNT, members.length());
            }

            if (json.has("deleted_at"))
                model.setValue(TagData.DELETION_DATE, readDate(json, "deleted_at"));

            if(json.has("tasks"))
                model.setValue(TagData.TASK_COUNT, json.getInt("tasks"));
        }
    }

}
