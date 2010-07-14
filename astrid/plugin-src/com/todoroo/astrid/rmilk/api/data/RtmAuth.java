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
public class RtmAuth extends RtmData {

  public enum Perms {
    read, write, delete
  }

  private final String token;

  private final Perms perms;

  private final RtmUser user;

  public RtmAuth(String token, Perms perms, RtmUser user) {
    this.token = token;
    this.perms = perms;
    this.user = user;
  }

  public RtmAuth(Element elt) {
    if (!elt.getNodeName().equals("auth")) { throw new IllegalArgumentException("Element " + elt.getNodeName() + " does not represent an Auth object."); }

    this.token = text(child(elt, "token"));
    this.perms = Enum.valueOf(Perms.class, text(child(elt, "perms")));
    this.user = new RtmUser(child(elt, "user"));
  }

  public String getToken() {
    return token;
  }

  public Perms getPerms() {
    return perms;
  }

  public RtmUser getUser() {
    return user;
  }
}
