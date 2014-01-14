package com.todoroo.astrid.gtasks;

public class RobolectricGtasksPreferenceService extends GtasksPreferenceService {

    public RobolectricGtasksPreferenceService() {
        setToken("");
    }

    @Override
    public String getIdentifier() {
        return "test";
    }

    public void logout() {
        setToken(null);
    }
}
