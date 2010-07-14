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

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;

/**
 *
 * @author Will Ross Jun 22, 2007
 */
@SuppressWarnings("nls")
public class RtmTaskSeries extends RtmData {

    private final RtmTaskList list;
    
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

    private final boolean hasRecurrence;

    public RtmTaskSeries(RtmTaskList list, String id, Date created, Date modified, String name,
            String source, RtmTask task) {
        this.list = list;
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
        hasRecurrence = false;
    }

    public RtmTaskSeries(RtmTaskList list, Element elt) {
        this.list = list;
        id = elt.getAttribute("id");
        created = parseDate(elt.getAttribute("created"));
        modified = parseDate(elt.getAttribute("modified"));
        name = elt.getAttribute("name");
        source = elt.getAttribute("source");
        List<Element> children = children(elt, "task");
        if (children.size() > 1) {
            // assume it's a repeating task - pick the child with nearest
            // but not expired due date
            RtmTask selectedTask = new RtmTask(children.get(0));
            for(Element element : children) {
                RtmTask childTask = new RtmTask(element);
                if(childTask.getCompleted() == null) {
                    selectedTask = childTask;
                    break;
                }
            }
            task = selectedTask;
        } else {
            task = new RtmTask(child(elt, "task"));
        }
        notes = new RtmTaskNotes(child(elt, "notes"));
        locationId = elt.getAttribute("location_id");
        url = elt.getAttribute("url");
        hasRecurrence = children(elt, "rrule").size() > 0;

        Element elementTags = child(elt, "tags");
        if (elementTags.getChildNodes().getLength() > 0) {
            List<Element> elementTagList = children(elementTags, "tag");
            tags = new LinkedList<String>();
            for (Element elementTag : elementTagList) {
                String tag = text(elementTag);
                if (tag != null)
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

    public RtmTaskNotes getNotes() {
        return notes;
    }

    public String getLocationId() {
        return locationId;
    }

    @Override
    public String toString() {
        return "TaskSeries<" + id + "," + name + ">";
    }

    public String getURL() {
        return url;
    }

    public boolean hasRecurrence() {
        return hasRecurrence;
    }
    
    public RtmTaskList getList() {
        return list;
    }

}
