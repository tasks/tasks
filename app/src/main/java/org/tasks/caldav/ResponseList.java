package org.tasks.caldav;

import at.bitfire.dav4jvm.Response;
import at.bitfire.dav4jvm.Response.HrefRelation;
import java.util.ArrayList;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;

public class ResponseList extends ArrayList<Response>
    implements Function2<Response, HrefRelation, Unit> {

  @Override
  public Unit invoke(Response response, HrefRelation hrefRelation) {
    add(response);
    return null;
  }
}
