package com.todoroo.astrid.service;

import java.io.IOException;

import org.json.JSONArray;
import org.weloveastrid.rmilk.MilkUtilities;

import com.todoroo.andlib.service.RestClient;
import com.todoroo.andlib.test.TodorooTestCase;
import com.todoroo.astrid.utility.AstridPreferences;
import com.todoroo.astrid.utility.Constants;

public class UpdateMessageServiceTest extends TodorooTestCase {

    public void testNoUpdates() {
        AstridPreferences.setLatestUpdates(null);

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
        }.processUpdates();
    }

    public void testIOException() {
        AstridPreferences.setLatestUpdates(null);

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(String message) {
                fail("should not have displayed updates");
            }

            @Override
            String getUpdates(String url) throws IOException {
                throw new IOException("yayaya");
            }
        }.processUpdates();
    }

    public void testNewUpdate() {
        AstridPreferences.setLatestUpdates(null);

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(String message) {
                assertTrue(message.contains("yo"));
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'yo'}]";
            }
        }.processUpdates();
    }

    public void testMultipleUpdates() {
        AstridPreferences.setLatestUpdates(null);

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
        }.processUpdates();
    }

    public void testExistingUpdate() {
        AstridPreferences.setLatestUpdates(null);

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(String message) {
                assertTrue(message.contains("yo"));
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'yo'}]";
            }
        }.processUpdates();

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
        }.processUpdates();
    }

    public void testUpdateWithDate() {
        AstridPreferences.setLatestUpdates(null);

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
        }.processUpdates();
    }

    public void testUpdateWithInternalPluginOn() {
        AstridPreferences.setLatestUpdates(null);
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
        }.processUpdates();
    }

    public void testUpdateWithInternalPluginOff() {
        AstridPreferences.setLatestUpdates(null);
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
        }.processUpdates();
    }

    public void testUpdateWithExternalPluginOn() {
        AstridPreferences.setLatestUpdates(null);

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(String message) {
                assertTrue(message.contains("astrid man"));
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'astrid man',plugin:'" + Constants.PACKAGE + "'}]";
            }
        }.processUpdates();
    }

    public void testUpdateWithExternalPluginOff() {
        AstridPreferences.setLatestUpdates(null);

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
        }.processUpdates();
    }

    // ---

    /** helper test class */
    abstract public class TestUpdateMessageService extends UpdateMessageService {

        public TestUpdateMessageService() {
            super();
            restClient = new RestClient() {

                public String post(String url, String data) throws IOException {
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
