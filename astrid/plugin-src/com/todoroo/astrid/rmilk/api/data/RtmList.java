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

import org.w3c.dom.Element;

@SuppressWarnings("nls")
public class RtmList extends RtmData {

  private final String id;
  private final boolean smart;
  private final String name;

  public RtmList(String id, String name, boolean smart) {
    this.id = id;
    this.name = name;
    this.smart = smart;
  }

  public RtmList(Element elt) {
    id = elt.getAttribute("id");
    name = elt.getAttribute("name");
    smart = elt.getAttribute("smart") == "1";
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public boolean isSmart() {
      return smart;
  }
}
