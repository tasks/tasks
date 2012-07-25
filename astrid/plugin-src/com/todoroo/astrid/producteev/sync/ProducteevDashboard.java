/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.producteev.sync;

import java.util.ArrayList;
import java.util.StringTokenizer;

import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.data.StoreObject;

/**
 * {@link StoreObject} entries for a Producteev Dashboard
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ProducteevDashboard {

    /** type*/
    public static final String TYPE = "pdv-dash"; //$NON-NLS-1$

    /** dashboard id in producteev */
    public static final LongProperty REMOTE_ID = new LongProperty(StoreObject.TABLE,
            StoreObject.ITEM.name);

    /** dashboard name */
    public static final StringProperty NAME = new StringProperty(StoreObject.TABLE,
            StoreObject.VALUE1.name);

    /** users (list in the format "id_user,name;id_user,name;") */
    public static final StringProperty USERS = new StringProperty(StoreObject.TABLE,
            StoreObject.VALUE2.name);

    // data class-part
    private final long id;

    private final String name;

    private ArrayList<ProducteevUser> users = null;

    public ProducteevDashboard (StoreObject dashboardData) {
        this(dashboardData.getValue(REMOTE_ID),dashboardData.getValue(NAME),(dashboardData.containsValue(USERS)?dashboardData.getValue(USERS):null));
    }

    /**
     * Constructor for a dashboard.
     *
     * @param id id of the remote dashboard
     * @param name name of the remote dashboard
     * @param usercsv csv-userstring as returned by a StoreObject-dashboard with property ProducteevDashboard.USERS
     */
    @SuppressWarnings("nls")
    public ProducteevDashboard(long id, String name, String usercsv) {
        this.id = id;
        this.name = name;

        if (usercsv == null)
            return;

        StringTokenizer tokenizer = new StringTokenizer(usercsv, ";");
        int usercount = tokenizer.countTokens();

        while (tokenizer.hasMoreTokens()) {
            String userdata = tokenizer.nextToken();
            int delim_index = userdata.indexOf(",");
            String userid = userdata.substring(0, delim_index);
            String username = userdata.substring(delim_index+1);
            int name_gap = username.indexOf(" ");
            String firstname = (name_gap == -1 ? username : username.substring(0,name_gap));
            String lastname = (name_gap == -1 ? null : username.substring(name_gap+1));
            if (users == null) {
                users = new ArrayList<ProducteevUser>(usercount);
            }
            users.add(new ProducteevUser(Long.parseLong(userid),null,firstname,lastname));
        }
    }

    /**
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * return the name of this dashboard
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * @return the users
     */
    public ArrayList<ProducteevUser> getUsers() {
        if (users == null) {
            return new ArrayList<ProducteevUser>(); // Don't return null
        }
        return users;
    }

    /** Try and find user in the dashboard. return null if un-findable */
    public static String getUserFromDashboard(StoreObject dashboard, long userId) {
        String users = ";" + dashboard.getValue(USERS); //$NON-NLS-1$
        int index = users.indexOf(";" + userId + ","); //$NON-NLS-1$ //$NON-NLS-2$
        if(index > -1)
            return users.substring(users.indexOf(',', index) + 1,
                    users.indexOf(';', index + 1));
        return null;
    }
}
