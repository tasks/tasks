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

import org.w3c.dom.Element;
import org.w3c.dom.Text;

import android.util.Log;

/**
 * Represents a single task note.
 *
 * @author Edouard Mercier
 * @since 2008.04.22
 */
@SuppressWarnings("nls")
public class RtmTaskNote
    extends RtmData
{

  private String id;

  private Date created;

  private Date modified;

  private String title;

  private String text;

  public RtmTaskNote(Element element)
  {
    id = element.getAttribute("id");
    created = parseDate(element.getAttribute("created"));
    modified = parseDate(element.getAttribute("modified"));
    title = element.getAttribute("title");

    // The note text itself might be split across multiple children of the
    // note element, so get all of the children.
    for (int i=0; i < element.getChildNodes().getLength(); i++) {
        Object innerNote = element.getChildNodes().item(i);
        if(!(innerNote instanceof Text)) {
            Log.w("rtm-note", "Expected text type, got " + innerNote.getClass());
            continue;
        }

    	Text innerText = (Text) innerNote;

    	if (text == null)
    	    text = innerText.getData();
    	else
    	    text = text.concat(innerText.getData());

    }
  }

  public String getId()
  {
    return id;
  }

  public Date getCreated()
  {
    return created;
  }

  public Date getModified()
  {
    return modified;
  }

  public String getTitle()
  {
    return title;
  }

  public String getText()
  {
    return text;
  }

}
