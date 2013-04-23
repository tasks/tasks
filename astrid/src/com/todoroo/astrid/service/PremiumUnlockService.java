package com.todoroo.astrid.service;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.RestClient;
import com.todoroo.andlib.utility.Preferences;

public class PremiumUnlockService {

    public static final String PREF_KILL_SWITCH = "p_premium_kill_switch"; //$NON-NLS-1$

    private static final String PREM_SWITCH_URL = "http://astrid.com/home/premium_check"; //$NON-NLS-1$

    @Autowired
    private RestClient restClient;

    public PremiumUnlockService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    public void checkForPremium() {
        if (Preferences.getBoolean(PREF_KILL_SWITCH, false))
            return;

        try {
            String response = restClient.get(PREM_SWITCH_URL).trim();
            if ("OFF".equals(response)) //$NON-NLS-1$
                Preferences.setBoolean(PREF_KILL_SWITCH, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
