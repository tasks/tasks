package org.tasks.caldav;

import at.bitfire.dav4jvm.Response;
import at.bitfire.dav4jvm.Response.HrefRelation;
import java.util.ArrayList;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;

public class ResponseList extends ArrayList<Response>
    implements Function2<Response, HrefRelation, Unit> {

  private final HrefRelation filter;

  public ResponseList() {
    this(null);
  }

  public ResponseList(HrefRelation filter) {
    this.filter = filter;
  }

  @Override
  public Unit invoke(Response response, HrefRelation hrefRelation) {
    if (filter == null || hrefRelation == filter) {
      add(response);
    }
    return null;
  }
}
