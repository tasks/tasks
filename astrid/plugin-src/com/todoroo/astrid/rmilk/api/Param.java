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

import java.util.Date;

import com.todoroo.astrid.rmilk.api.data.RtmData;

/**
 *
 * @author Will Ross Jun 21, 2007
 */
public class Param
    implements Comparable<Param>
{

  private final String name;

  private final String value;

  public Param(String name, String value)
  {
    this.name = name;
    this.value = value;
  }

  public Param(String name, Date value)
  {
    this.name = name;
    this.value = RtmData.formatDate(value);
  }

  public String getName()
  {
    return name;
  }

  public String getValue()
  {
    return value;
  }

  public int compareTo(Param p)
  {
    return name.compareTo(p.getName());
  }
}
