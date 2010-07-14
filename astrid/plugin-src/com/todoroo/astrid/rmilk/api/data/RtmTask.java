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

import android.util.Log;

/**
 *
 * @author Will Ross Jun 21, 2007
 */
@SuppressWarnings("nls")
public class RtmTask
    extends RtmData
{

    private static final String TAG = "rtm-task";

  private final String id;

  private final Date due;

  private final boolean hasDueTime;

  private final Date added;

  private final Date completed;

  private final Date deleted;

  private final Priority priority;

  private final int postponed;

  private final String estimate;

  public enum Priority
  {
    High, Medium, Low, None
  }

  public static String convertPriority(Priority priority)
  {
    switch (priority)
    {
    case None:
      return new String(new char[] { 'n' });
    case Low:
      return new String(new char[] { '3' });
    case Medium:
      return new String(new char[] { '2' });
    case High:
      return new String(new char[] { '1' });
    default:
      Log.e(TAG, "Unrecognized RTM task priority: '" + priority + "'");
      return new String(new char[] { 'n' });
    }
  }

  public RtmTask(String id, Date due, boolean hasDueTime, Date added, Date completed, Date deleted, Priority priority, int postponed, String estimate)
  {
    this.id = id;
    this.due = due;
    this.hasDueTime = hasDueTime;
    this.added = added;
    this.completed = completed;
    this.deleted = deleted;
    this.priority = priority;
    this.postponed = postponed;
    this.estimate = estimate;
  }

  public RtmTask(Element elt)
  {
    id = elt.getAttribute("id");
    String dueStr = elt.getAttribute("due");
    due = (dueStr == null || dueStr.length() == 0) ? null : parseDate(dueStr);
    hasDueTime = Integer.parseInt(elt.getAttribute("has_due_time")) != 0;
    String addedStr = elt.getAttribute("added");
    added = (addedStr == null || addedStr.length() == 0) ? null : parseDate(addedStr);
    String completedStr = elt.getAttribute("completed");
    completed = (completedStr == null || completedStr.length() == 0) ? null : parseDate(completedStr);
    String deletedStr = elt.getAttribute("deleted");
    deleted = (deletedStr == null || deletedStr.length() == 0) ? null : parseDate(deletedStr);
    String priorityStr = elt.getAttribute("priority");
    if (priorityStr.length() > 0)
    {
      switch (priorityStr.charAt(0))
      {
      case 'N':
      case 'n':
        priority = Priority.None;
        break;
      case '3':
        priority = Priority.Low;
        break;
      case '2':
        priority = Priority.Medium;
        break;
      case '1':
        priority = Priority.High;
        break;
      default:
        System.err.println("Unrecognized RTM task priority: '" + priorityStr + "'");
        priority = Priority.Medium;
      }
    }
    else
    {
      priority = Priority.None;
    }
    if (elt.hasAttribute("postponed") == true && elt.getAttribute("postponed").length() > 0)
    {
      postponed = Integer.parseInt(elt.getAttribute("postponed"));
    }
    else
    {
      postponed = 0;
    }
    estimate = elt.getAttribute("estimate");
  }

  public String getId()
  {
    return id;
  }

  public Date getDue()
  {
    return due;
  }

  public boolean getHasDueTime()
  {
    return hasDueTime;
  }

  public Date getAdded()
  {
    return added;
  }

  public Date getCompleted()
  {
    return completed;
  }

  public Date getDeleted()
  {
    return deleted;
  }

  public Priority getPriority()
  {
    return priority;
  }

  public int getPostponed()
  {
    return postponed;
  }

  public String getEstimate()
  {
    return estimate;
  }

  @Override
  public String toString()
  {
    return "Task<" + id + ">";
  }

}
