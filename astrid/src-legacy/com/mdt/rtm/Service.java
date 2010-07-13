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
package com.mdt.rtm;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import com.mdt.rtm.data.RtmAuth;
import com.mdt.rtm.data.RtmFrob;
import com.mdt.rtm.data.RtmList;
import com.mdt.rtm.data.RtmLists;
import com.mdt.rtm.data.RtmLocation;
import com.mdt.rtm.data.RtmTask.Priority;
import com.mdt.rtm.data.RtmTaskNote;
import com.mdt.rtm.data.RtmTaskSeries;
import com.mdt.rtm.data.RtmTasks;

/**
 * Represents the Remember the Milk service API.
 *
 * @author Will Ross Jun 21, 2007
 */
public interface Service
{

  /**
   * Checks whether the service is authorized to communicate with the RTM server. Depends on the user's login info, and whether or not that user has
   * authorized the service wrapper to communicate with RTM.
   *
   * @return true if the service API has permission to interact with full permissions (including delete) with RTM
   * @throws ServiceException
   *           if there is a problem checking for authorization
   */
  boolean isServiceAuthorized()
      throws ServiceException;

  /**
   * Begins the process of obtaining authorization for the service API to communicate with RTM on behalf of a particular user.
   *
   * @return the URL that the user should be prompted to log in to to complete authorization
   * @throws ServiceException
   *           if the authorization process cannot be started
   */
  String beginAuthorization(RtmAuth.Perms permissions)
      throws ServiceException;

  /**
   * The same method as the previous {@link #beginAuthorization(com.mdt.rtm.data.RtmAuth.Perms)}, except that you need to invoke yourself the
   * {@link #auth_getFrob()} beforehand.
   *
   * This has been introduced, in order to provide better control over the API.
   */
  String beginAuthorization(RtmFrob frob, RtmAuth.Perms permissions)
      throws ServiceException;

  /**
   * Completes the process of obtaining authorization for the service API to communicate with RTM on behalf of a particular user.
   *
   * Once this is called successfully, <code>isServiceAuthorized()</code> should return true until the user goes to RTM and explicitly denies the
   * service access. It also might be possible for authorization to time out, in which case this process would need to be started again.
   *
   * @return the newly created authentication token
   * @throws ServiceException
   *           if the authorization process cannot be completed
   */
  String completeAuthorization()
      throws ServiceException;

  /**
   * Same as the previous {@link #completeAuthorization()} method, except that the frob taken is implicitly given. Very useful when you need to handle
   * multiple authentication tokens.
   */
  String completeAuthorization(RtmFrob frob)
      throws ServiceException;

  RtmAuth auth_checkToken(String authToken)
      throws ServiceException;

  RtmFrob auth_getFrob()
      throws ServiceException;

  String auth_getToken(String frob)
      throws ServiceException;

  void contacts_add()
      throws ServiceException;

  void contacts_delete()
      throws ServiceException;

  void contacts_getList()
      throws ServiceException;

  void groups_add()
      throws ServiceException;

  void groups_addContact()
      throws ServiceException;

  void groups_delete()
      throws ServiceException;

  void groups_getList()
      throws ServiceException;

  void groups_removeContact()
      throws ServiceException;

  RtmList lists_add(String timelineId, String listName)
      throws ServiceException;

  void lists_archive()
      throws ServiceException;

  void lists_delete(String timelineId, String listId)
      throws ServiceException;

  RtmLists lists_getList()
      throws ServiceException;

  RtmList lists_getList(String listName)
      throws ServiceException;

  void lists_setDefaultList()
      throws ServiceException;

  RtmList lists_setName(String timelineId, String listId, String newName)
      throws ServiceException;

  void lists_unarchive()
      throws ServiceException;

  void reflection_getMethodInfo()
      throws ServiceException;

  void reflection_getMethods()
      throws ServiceException;

  void settings_getList()
      throws ServiceException;

  RtmTaskSeries tasks_add(String timelineId, String listId, String name)
      throws ServiceException;

  void tasks_addTags()
      throws ServiceException;

  public void tasks_complete(String timelineId, String listId, String taskSeriesId, String taskId)
      throws ServiceException;

  void tasks_delete(String timelineId, String listId, String taskSeriesId, String taskId)
      throws ServiceException;

  RtmTasks tasks_getList(String listId, String filter, Date lastSync)
      throws ServiceException;

  RtmTaskSeries tasks_getTask(String taskId, String taskName)
      throws ServiceException;

  /**
   * @return Warning: the very first task with the given name is returned!
   */
  RtmTaskSeries tasks_getTask(String taskName)
      throws ServiceException;

  void tasks_movePriority()
      throws ServiceException;

  RtmTaskSeries tasks_moveTo(String timelineId, String fromListId, String toListId, String taskSeriesId, String taskId)
      throws ServiceException;

  void tasks_postpone()
      throws ServiceException;

  void tasks_removeTags()
      throws ServiceException;

  /**
   * THINK: Would it not be better to have a {@link GregorianCalendar} parameter instead?
   */
  void tasks_setDueDate(String timelineId, String listId, String taskSeriesId, String taskId, Date due, boolean hasDueTime)
      throws ServiceException;

  void tasks_setEstimate(String timelineId, String listId, String taskSeriesId, String taskId, String newEstimate)
      throws ServiceException;

  void tasks_setName(String timelineId, String listId, String taskSeriesId, String taskId, String newName)
      throws ServiceException;

  void tasks_setPriority(String timelineId, String listId, String taskSeriesId, String taskId, Priority priority)
      throws ServiceException;

  void tasks_setRecurrence()
      throws ServiceException;

  void tasks_setTags(String timelineId, String listId, String taskSeriesId, String taskId, String[] tags)
      throws ServiceException;

  void tasks_setURL()
      throws ServiceException;

  void tasks_uncomplete(String timelineId, String listId, String taskSeriesId, String taskId)
      throws ServiceException;

  RtmTaskNote tasks_notes_add(String timelineId, String listId, String taskSeriesId, String taskId, String title, String text)
      throws ServiceException;

  void tasks_notes_delete(String timelineId, String noteId)
      throws ServiceException;

  RtmTaskNote tasks_notes_edit(String timelineId, String noteId, String title, String text)
      throws ServiceException;

  RtmTaskSeries tasks_setLocation(String timelineId, String listId, String taskSeriesId, String taskId, String locationId)
      throws ServiceException;

  RtmTaskSeries tasks_setURL(String timelineId, String listId, String taskSeriesId, String taskId, String url)
      throws ServiceException;

  void test_echo()
      throws ServiceException;

  void test_login()
      throws ServiceException;

  void time_convert()
      throws ServiceException;

  void time_parse()
      throws ServiceException;

  String timelines_create()
      throws ServiceException;

  void timezones_getList()
      throws ServiceException;

  void transactions_undo()
      throws ServiceException;

  List<RtmLocation> locations_getList()
      throws ServiceException;

}
