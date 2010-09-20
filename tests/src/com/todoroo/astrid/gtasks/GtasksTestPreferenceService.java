package com.todoroo.astrid.gtasks;

public class GtasksTestPreferenceService extends GtasksPreferenceService {

    private boolean loggedIn = false;

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    @Override
    public boolean isLoggedIn() {
        return loggedIn;
    }

}
