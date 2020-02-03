/*
 * Copyright © 2013 – 2016 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package org.tasks.caldav;

import java.util.LinkedList;
import java.util.List;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.collections4.map.MultiKeyMap;

/**
 * Primitive cookie store that stores cookies in a (volatile) hash map. Will be sufficient for
 * session cookies.
 */
@SuppressWarnings("all")
public class MemoryCookieStore implements CookieJar {

  /**
   * Stored cookies. The multi-key consists of three parts: name, domain, and path. This ensures
   * that cookies can be overwritten. [RFC 6265 5.3 Storage Model] Not thread-safe!
   */
  private final MultiKeyMap<String, Cookie> storage =
      MultiKeyMap.multiKeyMap(new HashedMap<MultiKey<? extends String>, Cookie>());

  @Override
  public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
    synchronized (storage) {
      for (Cookie cookie : cookies) {
        storage.put(cookie.name(), cookie.domain(), cookie.path(), cookie);
      }
    }
  }

  @Override
  public List<Cookie> loadForRequest(HttpUrl url) {
    List<Cookie> cookies = new LinkedList<>();

    synchronized (storage) {
      MapIterator<MultiKey<? extends String>, Cookie> iter = storage.mapIterator();
      while (iter.hasNext()) {
        iter.next();
        Cookie cookie = iter.getValue();

        // remove expired cookies
        if (cookie.expiresAt() <= System.currentTimeMillis()) {
          iter.remove();
          continue;
        }

        // add applicable cookies
        if (cookie.matches(url)) {
          cookies.add(cookie);
        }
      }
    }

    return cookies;
  }
}
