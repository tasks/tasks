package com.todoroo.astrid.dao;

import java.lang.reflect.Field;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.astrid.data.RemoteModel;

@SuppressWarnings("nls")
public class DaoReflectionHelpers {

    public static <T> T getStaticFieldByReflection(Class<?> cls, Class<T> cast, String fieldName) {
        try {
            Field field = cls.getField(fieldName);
            Object obj = field.get(null);
            if (obj == null) {
                throw new RuntimeException(fieldName + " field for class " + cls.getName() + " is null");
            }
            return cast.cast(obj);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Class " + cls.getName() + " does not declare field " + fieldName);
        } catch (IllegalAccessException e2) {
            throw new RuntimeException(fieldName + " field for class " + cls.getName() + " is not accessible");
        } catch (ClassCastException e3) {
            throw new RuntimeException(fieldName + " field for class " + cls.getName() + " cannot be cast to type " + cast.getName());
        }
    }

    public static Property<?>[] getModelProperties(Class<? extends AbstractModel> model) {
        return getStaticFieldByReflection(model, Property[].class, "PROPERTIES");
    }

    public static <OE> Class<OE> getOutstandingClass(Class<? extends RemoteModel> model) {
        return DaoReflectionHelpers.getStaticFieldByReflection(model, Class.class, "OUTSTANDING_MODEL");
    }

}
