/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.sql;

import android.test.AndroidTestCase;

import com.todoroo.andlib.sql.Query.QueryTemplateHelper;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;

public class QueryTemplateHelperTest extends AndroidTestCase {

    public StringBuilder selection = new StringBuilder();
    public StringBuilder order = new StringBuilder();
    public StringBuilder groupBy = new StringBuilder();

    public void testBasic() {
        QueryTemplateHelper.queryForContentResolver("",
                selection, order, groupBy);
        assertEquals(selection.toString(), "");
        assertEquals(order.toString(), "");
        assertEquals(groupBy.toString(), "");
    }

    public void testSelection() {
        QueryTemplateHelper.queryForContentResolver("WHERE foo = bar",
                selection, order, groupBy);
        assertEquals(selection.toString(), "foo = bar");
        assertEquals(order.toString(), "");
        assertEquals(groupBy.toString(), "");
    }

    public void testOrder() {
        QueryTemplateHelper.queryForContentResolver("ORDER BY cats",
                selection, order, groupBy);
        assertEquals(selection.toString(), "");
        assertEquals(order.toString(), "cats");
        assertEquals(groupBy.toString(), "");
    }

    public void testWhereOrder() {
        QueryTemplateHelper.queryForContentResolver("WHERE foo = bar ORDER BY cats",
                selection, order, groupBy);
        assertEquals(selection.toString(), "foo = bar");
        assertEquals(order.toString(), "cats");
        assertEquals(groupBy.toString(), "");
    }

    public void testGroupBy() {
        QueryTemplateHelper.queryForContentResolver("GROUP BY dogs",
                selection, order, groupBy);
        assertEquals(selection.toString(), "");
        assertEquals(order.toString(), "");
        assertEquals(groupBy.toString(), "dogs");
    }

    public void testWhereGroupBy() {
        QueryTemplateHelper.queryForContentResolver("WHERE foo = bar GROUP BY dogs",
                selection, order, groupBy);
        assertEquals(selection.toString(), "foo = bar");
        assertEquals(order.toString(), "");
        assertEquals(groupBy.toString(), "dogs");
    }

    public void testOrderGroupBy() {
        QueryTemplateHelper.queryForContentResolver("GROUP BY dogs ORDER BY cats",
                selection, order, groupBy);
        assertEquals(order.toString(), "cats");
        assertEquals(groupBy.toString(), "dogs");
    }

    public void testWhereGroupByAndOrder() {
        QueryTemplateHelper.queryForContentResolver("WHERE foo = bar GROUP BY dogs ORDER BY cats",
                selection, order, groupBy);
        assertEquals(selection.toString(), "foo = bar");
        assertEquals(order.toString(), "cats");
        assertEquals(groupBy.toString(), "dogs");
    }

    public void testRealQueryTemplate() {
        QueryTemplateHelper.queryForContentResolver(
                new QueryTemplate().where(TaskCriteria.completed()).
                orderBy(Order.asc(Task.DUE_DATE)).toString(),
                selection, order, groupBy);
        assertEquals(TaskCriteria.completed().toString(), selection.toString());
        assertEquals(Order.asc(Task.DUE_DATE).toString(), order.toString());
        assertEquals("", groupBy.toString());
    }

    public void testRealQueryTemplateTwo() {
        QueryTemplateHelper.queryForContentResolver(
                new QueryTemplate().where(TaskCriteria.isActive()).
                orderBy(Order.asc(Task.ELAPSED_SECONDS)).groupBy(Task.NOTES).toString(),
                selection, order, groupBy);
        assertEquals(TaskCriteria.isActive().toString(), selection.toString());
        assertEquals(Order.asc(Task.ELAPSED_SECONDS).toString(), order.toString());
        assertEquals(Task.NOTES.toString(), groupBy.toString());
    }



}
