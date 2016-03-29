package org.tasks;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class AccountManager {

    @Inject
    public AccountManager() {
    }

    public List<String> getAccounts() {
        return Collections.emptyList();
    }

    public boolean hasAccount(String account) {
        return false;
    }
}
