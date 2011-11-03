package com.todoroo.astrid.service.abtesting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Helper class to define options with their probabilities and descriptions
 * @author Sam Bosley <sam@astrid.com>
 *
 */
@SuppressWarnings("nls")
public class ABOptions {

    public ABOptions() {
        bundles = new HashMap<String, ABOptionBundle>();
        events = new HashMap<String, List<String>>();
        initialize();
    }

    /**
     * Gets the integer array of weighted probabilities for an option key
     * @param key
     * @return
     */
    public synchronized int[] getProbsForKey(String key) {
        if (bundles.containsKey(key)) {
            ABOptionBundle bundle = bundles.get(key);
            return bundle.weightedProbs;
        } else {
            return null;
        }
    }

    /**
     * Updates the weighted probability array for a given key. Returns true
     * on success, false if they key doesn't exist or if the array is the wrong
     * length.
     * @param key
     * @param newProbs
     * @return
     */
    public synchronized boolean setProbsForKey(String key, int[] newProbs) {
        if (bundles.containsKey(newProbs)) {
            ABOptionBundle bundle = bundles.get(key);
            if (bundle.descriptions == null || newProbs.length == bundle.descriptions.length) {
                bundle.weightedProbs = newProbs;
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the string array of option descriptions for an option key
     * @param key
     * @return
     */
    public String[] getDescriptionsForKey(String key) {
        if (bundles.containsKey(key)) {
            ABOptionBundle bundle = bundles.get(key);
            return bundle.descriptions;
        } else {
            return null;
        }
    }

    /**
     * Returns the description for a particular choice of the given option
     * @param key
     * @param optionIndex
     * @return
     */
    public String getDescriptionForOption(String key, int optionIndex) {
        if (bundles.containsKey(key)) {
            ABOptionBundle bundle = bundles.get(key);
            if (bundle.descriptions != null && optionIndex < bundle.descriptions.length) {
                return bundle.descriptions[optionIndex];
            }
        }
        return null;
    }

    /**
     * Maps keys (i.e. preference key identifiers) to feature weights and descriptions
     */
    private final HashMap<String, ABOptionBundle> bundles;
    private final HashMap<String, List<String>> events; // maps events to lists of interested keys

    private static class ABOptionBundle {
        public int[] weightedProbs;
        public String[] descriptions;

        public ABOptionBundle(int[] weightedProbs, String[] descriptions) {
            this.weightedProbs = weightedProbs;
            this.descriptions = descriptions;
        }
    }

    public boolean isValidKey(String key) {
        return bundles.containsKey(key);
    }

    /**
     * Gets a localytics attribute array for the specified event.
     * @param event
     * @return
     */
    public String[] getLocalyticsAttributeArrayForEvent(String event) {
        ArrayList<String> attributes = new ArrayList<String>();
        List<String> interestedKeys = events.get(event);
        if (interestedKeys != null)
            for (String key : interestedKeys) {
                // Get choice if exists and add to array
                if (isValidKey(key)) {
                    ABOptionBundle bundle = bundles.get(key);
                    int choice = ABChooser.readChoiceForOption(key);
                    if (choice != ABChooser.NO_OPTION &&
                            bundle.descriptions != null && choice < bundle.descriptions.length) {
                        attributes.add(key);
                        attributes.add(getDescriptionForOption(key, choice));
                    }
                }
            }
        return attributes.toArray(new String[attributes.size()]);
    }


    /**
     * A/B testing options are defined below according to the following spec:
     *
     * @param optionKey = "<key>"
     * --This key is used to identify the option in the application and in the preferences
     *
     * @param probs = { int, int, ... }
     * --The different choices in an option correspond to an index in the probability array.
     * Probabilities are expressed as integers to easily define relative weights. For example,
     * the array { 1, 2 } would mean option 0 would happen one time for every two occurrences of option 1
     *
     * (optional)
     * @param descriptions = { "...", "...", ... }
     * --A string description of each option. Useful for tagging events. The index of
     * each description should correspond to the events location in the probability array
     * (i.e. the arrays should be the same length if this one exists)
     *
     * (optional)
     * @param relevantEvents = { "...", "...", ... }
     * --An arbitrary length list of relevant localytics events. When events are
     * tagged from StatisticsService, they will be appended with attributes
     * that have that event in this array
     */
    private void addOption(String optionKey, int[] probs, String[] descriptions, String[] relevantEvents) {
        ABOptionBundle bundle = new ABOptionBundle(probs, descriptions);
        bundles.put(optionKey, bundle);

        if (relevantEvents != null) {
            for (String event : relevantEvents) {
                List<String> interestedKeys = events.get(event);
                if (interestedKeys == null) {
                    interestedKeys = new ArrayList<String>();
                    events.put(event, interestedKeys);
                }
                interestedKeys.add(optionKey);
            }
        }
    }

    private void initialize() { // Set up
        //Calls to addOption go here
    }

}
