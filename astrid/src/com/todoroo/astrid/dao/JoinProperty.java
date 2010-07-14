/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

public interface JoinProperty {

    /**
     * @return SQL select statement describing how to load this property
     * in a join statement
     */
    public String joinTable();

}
