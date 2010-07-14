/*
 * Copyright 2007, MetaDimensional Technologies Inc.
 *
 *
 * This file is part of the RememberTheMilk Java API.
 *
 * The RememberTheMilk Java API is free software; you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * The RememberTheMilk Java API is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.todoroo.astrid.rmilk.api.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

@SuppressWarnings("nls")
public class RtmLists extends RtmData {

  private final Map<String, RtmList> lists;

  public RtmLists() {
    this.lists = new HashMap<String, RtmList>();
  }

  public RtmLists(Element elt) {
    this.lists = new HashMap<String, RtmList>();
    for (Element listElt : children(elt, "list")) {
      RtmList list = new RtmList(listElt);
      lists.put(list.getId(), list);
    }
  }

  public RtmList getList(String id) {
    return lists.get(id);
  }

  public Map<String, RtmList> getLists() {
    return Collections.unmodifiableMap(lists);
  }
}
