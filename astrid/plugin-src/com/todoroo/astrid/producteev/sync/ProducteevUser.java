/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.producteev.sync;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Arne Jans <arne.jans@gmail.com>
 */
@SuppressWarnings("nls")
public class ProducteevUser implements Comparable<ProducteevUser> {

    private final long id;

    private final String email;

    private final String firstname;

    private final String lastname;

    public ProducteevUser(long id, String email, String firstname,
            String lastname) {
        this.id = id;
        this.email = email;
        this.firstname = firstname;
        this.lastname = lastname;
    }

    public ProducteevUser(JSONObject elt) throws JSONException {
        this.id = elt.getLong("id");
        this.email = elt.getString("email");
        this.firstname = elt.getString("firstname");
        this.lastname = elt.getString("lastname");
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @return the firstname
     */
    public String getFirstname() {
        return firstname;
    }

    /**
     * @return the lastname
     */
    public String getLastname() {
        return lastname;
    }

    /**
     * @return the id
     */
    public long getId() {
        return id;
    }

    @Override
    public String toString() {
        String displayString = "";
        boolean hasFirstname = false;
        boolean hasLastname = false;
        if (firstname != null && firstname.length() > 0) {
            displayString += firstname;
            hasFirstname = true;
        }
        if (lastname != null && lastname.length() > 0)
            hasLastname = true;
        if (hasFirstname && hasLastname)
            displayString += " ";
        if (hasLastname)
            displayString += lastname;

        if (!hasFirstname && !hasLastname && email != null
                && email.length() > 0)
            displayString += email;
        return displayString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProducteevUser that = (ProducteevUser) o;

        if (id != that.id) return false;
        if (email != null ? !email.equals(that.email) : that.email != null) return false;
        if (firstname != null ? !firstname.equals(that.firstname) : that.firstname != null) return false;
        if (lastname != null ? !lastname.equals(that.lastname) : that.lastname != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (firstname != null ? firstname.hashCode() : 0);
        result = 31 * result + (lastname != null ? lastname.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(ProducteevUser o) {
        int ret = toString().compareTo(o.toString());
        return ret == 0 ? (new Long(id).compareTo(o.id)) : ret;
    }
}
