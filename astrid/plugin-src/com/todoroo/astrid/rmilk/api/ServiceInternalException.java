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
package com.todoroo.astrid.rmilk.api;


/**
 * Introduced in order to get rid of the {@link RuntimeException}, and have only one time of regular exception to cope with, from the API end-user
 * point of view.
 *
 * @author Edouard Mercier
 * @since 2008.04.23
 */
public class ServiceInternalException
    extends ServiceException
{
  private static final long serialVersionUID = -423838945284984432L;

  private final Exception enclosedException;

  public ServiceInternalException(String message)
  {
    this(message, null);
  }

  public ServiceInternalException(String message, Exception exception)
  {
    super(-1, message);
    this.enclosedException = exception;
  }

  public Exception getEnclosedException()
  {
    return enclosedException;
  }

}
