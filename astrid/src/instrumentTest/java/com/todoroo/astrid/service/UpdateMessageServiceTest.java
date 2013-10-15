/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.RestClient;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.dao.StoreObjectDao.StoreObjectCriteria;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.test.DatabaseTestCase;
import com.todoroo.astrid.utility.Constants;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.json.JSONArray;

import java.io.IOException;

public class UpdateMessageServiceTest extends DatabaseTestCase {

    @Autowired private StoreObjectDao storeObjectDao;
    @Autowired private GtasksPreferenceService gtasksPreferenceService;

    public void testNoUpdates() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(MessageTuple message) {
                fail("should not have displayed updates");
            }

            @Override
            String getUpdates(String url) throws IOException {
                assertTrue(url, url.contains("language=eng"));
                assertTrue(url.contains("version="));
                return "";
            }
        }.processUpdates();
    }

    public void testIOException() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(MessageTuple message) {
                fail("should not have displayed updates");
            }

            @Override
            String getUpdates(String url) throws IOException {
                throw new IOException("yayaya");
            }
        }.processUpdates();
    }

    public void testNewUpdate() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(MessageTuple message) {
                assertTrue(message.message.toString().contains("yo"));
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'yo'}]";
            }
        }.processUpdates();
    }

    public void testMultipleUpdates() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(MessageTuple message) {
                assertTrue(message.message.toString().contains("yo"));
                assertFalse(message.message.toString().contains("cat")); // We only process the first update now
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'yo'},{message:'cat'}]";
            }
        }.processUpdates();
    }

    public void testExistingUpdate() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(MessageTuple message) {
                assertTrue(message.message.toString().contains("yo"));
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'yo'}]";
            }
        }.processUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(MessageTuple message) {
                fail("should have not displayed again");
            }

            @Override
            protected void onEmptyMessage() {
                // expected
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'yo'}]";
            }
        }.processUpdates();
    }

    public void testUpdateWithDate() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(MessageTuple message) {
                assertTrue(message.message.toString().contains("yo"));
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'yo',date:'date'}]";
            }
        }.processUpdates();
    }

    public void testUpdateWithInternalPluginOn() {
        clearLatestUpdates();
        gtasksPreferenceService.setToken("gtasks");

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(MessageTuple message) {
                assertTrue(message.message.toString().contains("gtasks man"));
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'gtasks man',plugin:'gtasks'}]";
            }
        }.processUpdates();
        gtasksPreferenceService.setToken(null);
    }

    public void testUpdateWithInternalPluginOff() {
        clearLatestUpdates();
        gtasksPreferenceService.setToken(null);

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(MessageTuple message) {
                fail("displayed update");
            }

            @Override
            protected void onEmptyMessage() {
                // expected
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'gtasks man',plugin:'gtasks'}]";
            }
        }.processUpdates();
    }

    public void testUpdateWithExternalPluginOn() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(MessageTuple message) {
                assertTrue(message.message.toString().contains("astrid man"));
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'astrid man',plugin:'" + Constants.PACKAGE + "'}]";
            }
        }.processUpdates();
    }

    public void testUpdateWithExternalPluginOff() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(MessageTuple message) {
                fail("displayed update");
            }

            @Override
            protected void onEmptyMessage() {
                // expected
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'astrid man',plugin:'com.bogus.package'}]";
            }
        }.processUpdates();
    }

    public void testUpdateWithScreenFlow() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(MessageTuple message) {
                assertTrue(message.linkText.size() > 0);
                assertTrue(message.click.size() > 0);
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{type:'screen',screens:['com.todoroo.astrid.activity.TaskListActivity'],message:'Screens'}]";
            }
        };
    }

    public void testUpdateWithPrefs() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(MessageTuple message) {
                assertTrue(message.linkText.size() > 0);
                assertTrue(message.click.size() > 0);
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{type:'pref',prefs:[{key:'key', type:'bool', title:'my pref'}],message:'Prefs'}]";
            }
        };
    }

    public void testUpdateWithLinks() {
        clearLatestUpdates();

        new TestUpdateMessageService() {
            @Override
            void verifyMessage(MessageTuple message) {
                assertEquals("Message", message.message);
                assertEquals("link", message.linkText.get(0));
                assertNotNull(message.click.get(0));
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'Message', links:[{title:'link',url:'http://astrid.com'}]]";
            }
        };
    }

    // ---

    private void clearLatestUpdates() {
        storeObjectDao.deleteWhere(StoreObjectCriteria.byType(UpdateMessageService.UpdateMessage.TYPE));
    }

    /** helper test class */
    abstract public class TestUpdateMessageService extends UpdateMessageService {

        public TestUpdateMessageService() {
            super(null);
            restClient = new RestClient() {

                public String post(String url, HttpEntity data, Header... headers) throws IOException {
                    return null;
                }

                public String get(String url) throws IOException {
                    return getUpdates(url);
                }
            };
        }

        abstract void verifyMessage(MessageTuple message);

        abstract String getUpdates(String url) throws IOException;

        protected void onEmptyMessage() {
            fail("empty update message");
        }

        @Override
        protected MessageTuple buildUpdateMessage(JSONArray updates) {
            MessageTuple message = super.buildUpdateMessage(updates);
            if(message == null || message.message.length() == 0)
                onEmptyMessage();
            return message;
        }

        @Override
        protected void displayUpdateDialog(MessageTuple tuple) {
            verifyMessage(tuple);
        }
    }

}
