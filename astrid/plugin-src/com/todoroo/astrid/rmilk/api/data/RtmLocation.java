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
 * Represents a location.
 * 
 * @author Edouard Mercier
 * @since 2008.05.22
 */
@SuppressWarnings("nls")
public class RtmLocation
    extends RtmData
{

  public final String id;

  public final String name;

  public final float longitude;

  public final float latitude;

  public final String address;

  public final boolean viewable;

  public int zoom;

  public RtmLocation(Element element)
  {
    id = element.getAttribute("id");
    name = element.getAttribute("name");
    longitude = Float.parseFloat(element.getAttribute("longitude"));
    latitude = Float.parseFloat(element.getAttribute("latitude"));
    address = element.getAttribute("address");
    zoom = Integer.parseInt(element.getAttribute("zoom"));
    viewable = element.getAttribute("viewable").equals("1") ? true : false;
  }

}
