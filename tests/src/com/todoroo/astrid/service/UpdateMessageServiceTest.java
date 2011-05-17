package com.todoroo.astrid.service;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.json.JSONArray;
import org.weloveastrid.rmilk.MilkUtilities;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.RestClient;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.dao.StoreObjectDao.StoreObjectCriteria;
import com.todoroo.astrid.test.DatabaseTestCase;
import com.todoroo.astrid.utility.Constants;

public class UpdateMessageServiceTest extends DatabaseTestCase {

    @Autowired private StoreObjectDao storeObjectDao;

    public void testNoUpdates() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(String message) {
                fail("should not have displayed updates");
            }

            @Override
            String getUpdates(String url) throws IOException {
                assertTrue(url, url.contains("language=eng"));
                assertTrue(url.contains("version="));
                return "";
            }
        }.processUpdates(getContext());
    }

    public void testIOException() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(String message) {
                fail("should not have displayed updates");
            }

            @Override
            String getUpdates(String url) throws IOException {
                throw new IOException("yayaya");
            }
        }.processUpdates(getContext());
    }

    public void testNewUpdate() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(String message) {
                assertTrue(message.contains("yo"));
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'yo'}]";
            }
        }.processUpdates(getContext());
    }

    public void testMultipleUpdates() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(String message) {
                assertTrue(message.contains("yo"));
                assertTrue(message.contains("cat"));
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'yo'},{message:'cat'}]";
            }
        }.processUpdates(getContext());
    }

    public void testExistingUpdate() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(String message) {
                assertTrue(message.contains("yo"));
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'yo'}]";
            }
        }.processUpdates(getContext());

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(String message) {
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
        }.processUpdates(getContext());
    }

    public void testUpdateWithDate() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(String message) {
                assertTrue(message.contains("yo"));
                assertTrue(message.contains("date"));
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'yo',date:'date'}]";
            }
        }.processUpdates(getContext());
    }

    public void testUpdateWithInternalPluginOn() {
        clearLatestUpdates();
        MilkUtilities.INSTANCE.setToken("milk");

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(String message) {
                assertTrue(message.contains("rmilk man"));
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'rmilk man',plugin:'rmilk'}]";
            }
        }.processUpdates(getContext());
    }

    public void testUpdateWithInternalPluginOff() {
        clearLatestUpdates();
        MilkUtilities.INSTANCE.setToken(null);

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(String message) {
                fail("displayed update");
            }

            @Override
            protected void onEmptyMessage() {
                // expected
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'rmilk man',plugin:'rmilk'}]";
            }
        }.processUpdates(getContext());
    }

    public void testUpdateWithExternalPluginOn() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(String message) {
                assertTrue(message.contains("astrid man"));
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'astrid man',plugin:'" + Constants.PACKAGE + "'}]";
            }
        }.processUpdates(getContext());
    }

    public void testUpdateWithExternalPluginOff() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(String message) {
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
        }.processUpdates(getContext());
    }

    // ---

    private void clearLatestUpdates() {
        storeObjectDao.deleteWhere(StoreObjectCriteria.byType(UpdateMessageService.UpdateMessage.TYPE));
    }

    /** helper test class */
    abstract public class TestUpdateMessageService extends UpdateMessageService {

        public TestUpdateMessageService() {
            super();
            restClient = new RestClient() {

                public String post(String url, HttpEntity data) throws IOException {
                    return null;
                }

                public String get(String url) throws IOException {
                    return getUpdates(url);
                }
            };
        }

        @Override
        protected boolean shouldSkipUpdates() {
            return false;
        }

        abstract void verifyMessage(String message);

        abstract String getUpdates(String url) throws IOException;

        protected void onEmptyMessage() {
            fail("empty update message");
        }

        @Override
        protected StringBuilder buildUpdateMessage(JSONArray updates) {
            StringBuilder builder = super.buildUpdateMessage(updates);
            if(builder.length() == 0)
                onEmptyMessage();
            return builder;
        }

        @Override
        protected void displayUpdateDialog(StringBuilder builder) {
            verifyMessage(builder.toString());
        }
    }

}
