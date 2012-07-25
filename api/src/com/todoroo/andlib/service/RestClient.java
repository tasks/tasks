/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.service;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;

/**
 * RestClient stub invokes the HTML requests as desired
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public interface RestClient {
    public String get(String url) throws IOException;
    public String post(String url, HttpEntity data, Header... headers) throws IOException;
}