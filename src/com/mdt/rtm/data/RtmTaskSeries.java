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

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Element;

/**
 *
 * @author Will Ross Jun 22, 2007
 */
public class RtmTaskSeries extends RtmData {

  private static final Logger log = Logger.getLogger("TaskSeries");

  private final String id;

  private final Date created;

  private final Date modified;

  private final String name;

  private final String source;

  private final RtmTask task;

  private final LinkedList<String> tags;

  private final RtmTaskNotes notes;

  private final String locationId;

  private final String url;

  public RtmTaskSeries(String id, Date created, Date modified, String name, String source, RtmTask task) {
    this.id = id;
    this.created = created;
    this.modified = modified;
    this.name = name;
    this.source = source;
    this.task = task;
    this.locationId = null;
    notes = null;
    url = null;
    tags = null;
  }

  public RtmTaskSeries(Element elt) {
    id = elt.getAttribute("id");
    created = parseDate(elt.getAttribute("created"));
    modified = parseDate(elt.getAttribute("modified"));
    name = elt.getAttribute("name");
    source = elt.getAttribute("source");
    task = new RtmTask(child(elt, "task"));

    if (children(elt, "task").size() > 1) {
      log.severe("WARANING: Assumption incorrect: found a TaskSeries with more than one child Task.");
    }
    notes = new RtmTaskNotes(child(elt, "notes"));
    locationId = elt.getAttribute("location_id");
    url = elt.getAttribute("url");

    Element elementTags = child(elt, "tags");
    if(elementTags.getChildNodes().getLength() > 0) {
        List<Element> elementTagList = children(elementTags, "tag");
        tags = new LinkedList<String>();
        for (Element elementTag : elementTagList) {
            String tag = text(elementTag);
            if(tag != null)
                tags.add(tag);
        }
    } else {
        tags = null;
    }
  }

  public String getId() {
    return id;
  }

  public Date getCreated() {
    return created;
  }

  public Date getModified() {
    return modified;
  }

  public String getName() {
    return name;
  }

  public String getSource() {
    return source;
  }

  public RtmTask getTask() {
    return task;
  }

  public LinkedList<String> getTags() {
      return tags;
  }

  public RtmTaskNotes getNotes()
  {
    return notes;
  }

  public String getLocationId()
  {
    return locationId;
  }

  @Override
  public String toString()
  {
    return "TaskSeries<" + id + "," + name + ">";
  }

  public String getURL()
  {
    return url;
  }

}
