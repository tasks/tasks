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

/**
 * 
 * @author Will Ross Jun 21, 2007
 */
@SuppressWarnings("nls")
public class RtmUser extends RtmData {

  private final String id;

  private final String username;

  private final String fullname;

  public RtmUser(String id, String username, String fullname) {
    this.id = id;
    this.username = username;
    this.fullname = fullname;
  }

  public RtmUser(Element elt) {
    if (!elt.getNodeName().equals("user")) { throw new IllegalArgumentException("Element " + elt.getNodeName() + " does not represent a User object."); }

    this.id = elt.getAttribute("id");
    this.username = elt.getAttribute("username");
    this.fullname = elt.getAttribute("fullname");
  }

  public String getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getFullname() {
    return fullname;
  }

}
