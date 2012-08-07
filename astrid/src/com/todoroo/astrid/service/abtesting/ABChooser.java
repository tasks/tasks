/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service.abtesting;

import java.util.Random;
import java.util.Set;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.dao.ABTestEventDao;

/**
 * Helper class to facilitate A/B testing by randomly choosing an option
 * based on probabilities that can be supplied from local defaults
 * @author Sam Bosley <sam@astrid.com>
 *
 */
public class ABChooser {

    public static final int NO_OPTION = -1;

    @Autowired
    private ABTests abTests;

    @Autowired
    private ABTestEventDao abTestEventDao;

    private final Random random;

    public ABChooser() {
        DependencyInjectionService.getInstance().inject(this);
        random = new Random();
    }

    /**
     * Iterates through the list of all available tests and makes sure that a choice
     * is made for each of them
     */
    public void makeChoicesForAllTests(boolean newUser, boolean activatedUser) {
        Set<String> tests = abTests.getAllTestKeys();
        for (String test : tests) {
            makeChoiceForTest(test, newUser, activatedUser);
        }
    }

    /**
     * If a choice/variant has not yet been selected for the specified test,
     * make one and create the initial +0 analytics data point
     *
     * @param testKey - the preference key string of the option (defined in ABTests)
     */
    private void makeChoiceForTest(String testKey, boolean newUser, boolean activatedUser) {
        int pref = readChoiceForTest(testKey);
        if (pref > NO_OPTION) return;

        int chosen = NO_OPTION;
        if (abTests.isValidTestKey(testKey)) {
            int[] optionProbs = abTests.getProbsForTestKey(testKey, newUser);
            String[] optionDescriptions = abTests.getDescriptionsForTestKey(testKey);
            chosen = chooseOption(optionProbs);
            setChoiceForTest(testKey, chosen);

            String desc = optionDescriptions[chosen];
            abTestEventDao.createInitialTestEvent(testKey, desc, newUser, activatedUser);
        }
        return;
    }

    /**
     * Returns the chosen option if set or NO_OPTION if unset
     * @param testKey
     * @return
     */
    public static int readChoiceForTest(String testKey) {
        return Preferences.getInt(testKey, NO_OPTION);
    }

    /**
     * Changes the choice of an A/B feature in the preferences. Useful for
     * the feature flipper (can manually override previous choice)
     * @param testKey
     * @param choiceIndex
     */
    public void setChoiceForTest(String testKey, int choiceIndex) {
        if (abTests.isValidTestKey(testKey))
            Preferences.setInt(testKey, choiceIndex);
    }

    /*
     * Helper method to choose an option from an int[] corresponding to the
     * relative weights of each option. Returns the index of the chosen option.
     */
    private int chooseOption(int[] optionProbs) {
        int sum = 0;
        for (int opt : optionProbs) // Compute sum
            sum += opt;

        double rand = random.nextDouble() * sum; // Get uniformly distributed double between [0, sum)
        sum = 0;
        for (int i = 0; i < optionProbs.length; i++) {
            sum += optionProbs[i];
            if (rand <= sum) return i;
        }
        return optionProbs.length - 1;
    }
}
