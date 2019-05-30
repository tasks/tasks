package org.tasks.caldav;

import static androidx.test.InstrumentationRegistry.getTargetContext;
import static org.tasks.injection.TestModule.newPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CaldavClientTest {

  @Test
  public void dontCrashOnSpaceInUrl() {
    new CaldavClient(null, newPreferences(getTargetContext()), null)
        .forUrl("https://example.com/remote.php/a space/", "username", "password");
  }
}
