package org.tasks.caldav;

import static org.tasks.injection.TestModule.newPreferences;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CaldavClientTest {

  @Test
  public void dontCrashOnSpaceInUrl() throws NoSuchAlgorithmException, KeyManagementException {
    Context context = ApplicationProvider.getApplicationContext();
    new CaldavClient(context, null, newPreferences(context), null)
        .forUrl("https://example.com/remote.php/a space/", "username", "password");
  }
}
