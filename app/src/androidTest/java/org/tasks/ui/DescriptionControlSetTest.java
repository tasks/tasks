package org.tasks.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.tasks.ui.DescriptionControlSet.stripCarriageReturns;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DescriptionControlSetTest {
  @Test
  public void replaceCRLF() {
    assertEquals("aaa\nbbb", stripCarriageReturns("aaa\r\nbbb"));
  }

  @Test
  public void replaceCR() {
    assertEquals("aaa\nbbb", stripCarriageReturns("aaa\rbbb"));
  }

  @Test
  public void dontReplaceLF() {
    assertEquals("aaa\nbbb", stripCarriageReturns("aaa\nbbb"));
  }

  @Test
  public void checkIfNull() {
    assertNull(stripCarriageReturns(null));
  }
}
