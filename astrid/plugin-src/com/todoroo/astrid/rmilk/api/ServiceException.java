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

import java.io.IOException;

/**
 *
 * @author Will Ross Jun 21, 2007
 */
@SuppressWarnings("nls")
public class ServiceException extends IOException {

  private static final long serialVersionUID = -6711156026040643361L;

  int responseCode;

  String responseMessage;

  public ServiceException(int responseCode, String responseMessage) {
    super("Service invocation failed.  Code: " + responseCode + "; message: " + responseMessage);
    this.responseCode = responseCode;
    this.responseMessage = responseMessage;
  }

  int getResponseCode() {
    return responseCode;
  }

  String getResponseMessage() {
    return responseMessage;
  }
}
