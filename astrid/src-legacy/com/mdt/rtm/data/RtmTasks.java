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
package com.mdt.rtm.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;

/**
 * 
 * @author Will Ross Jun 21, 2007
 */
public class RtmTasks extends RtmData {

  private final List<RtmTaskList> lists;

  public RtmTasks() {
    this.lists = new ArrayList<RtmTaskList>();
  }

  public RtmTasks(Element elt) {
    this.lists = new ArrayList<RtmTaskList>();
    for (Element listElt : children(elt, "list")) {
      lists.add(new RtmTaskList(listElt));
    }
  }

  public List<RtmTaskList> getLists() {
    return Collections.unmodifiableList(lists);
  }
}
