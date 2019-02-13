package com.todoroo.astrid.gtasks.sync;

import static com.todoroo.astrid.gtasks.sync.GtasksTaskContainer.stripCarriageReturns;
import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class GtasksTaskContainerTest {
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
}
