/*
 * Copyright 2012 Sebastian Annies, Hamburg
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.coremedia.iso;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;

import com.coremedia.iso.boxes.Box;
import com.googlecode.mp4parser.AbstractBox;
import com.todoroo.aacenc.AACToM4A;
import com.todoroo.aacenc.R;

/**
 * A Property file based BoxFactory
 */
public class PropertyBoxParserImpl extends AbstractBoxParser {
    Properties mapping;

    public PropertyBoxParserImpl(String... customProperties) {
        Context context = AACToM4A.getContext();
        InputStream raw = null, is = null;
        mapping = new Properties();
        try {
            raw = context.getResources().openRawResource(R.raw.isoparser);
            is = new BufferedInputStream(raw);
            mapping.load(is);
            Enumeration<URL> enumeration = Thread.currentThread().getContextClassLoader().getResources("isoparser-custom.properties");

            while (enumeration.hasMoreElements()) {
                URL url = enumeration.nextElement();
                InputStream customIS = new BufferedInputStream(url.openStream());
                try {
                    mapping.load(customIS);
                } finally {
                    customIS.close();
                }
            }
            for (String customProperty : customProperties) {
                mapping.load(new BufferedInputStream(getClass().getResourceAsStream(customProperty)));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if(raw != null)
                    raw.close();
                if(is != null)
                    is.close();
            } catch (IOException e) {
                e.printStackTrace();
                // ignore - I can't help
            }
        }
    }

    public PropertyBoxParserImpl(Properties mapping) {
        this.mapping = mapping;
    }

    Pattern p = Pattern.compile("(.*)\\((.*?)\\)");

    @SuppressWarnings("unchecked")
    public Class<? extends Box> getClassForFourCc(String type, byte[] userType, String parent) {
        FourCcToBox fourCcToBox = new FourCcToBox(type, userType, parent).invoke();
        try {
            return (Class<? extends Box>) Class.forName(fourCcToBox.clazzName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Box createBox(String type, byte[] userType, String parent) {

        FourCcToBox fourCcToBox = new FourCcToBox(type, userType, parent).invoke();
        String[] param = fourCcToBox.getParam();
        String clazzName = fourCcToBox.getClazzName();
        try {
            if (param[0].trim().length() == 0) {
                param = new String[]{};
            }
            Class clazz = Class.forName(clazzName);

            Class[] constructorArgsClazz = new Class[param.length];
            Object[] constructorArgs = new Object[param.length];
            for (int i = 0; i < param.length; i++) {

                if ("userType".equals(param[i])) {
                    constructorArgs[i] = userType;
                    constructorArgsClazz[i] = byte[].class;
                } else if ("type".equals(param[i])) {
                    constructorArgs[i] = type;
                    constructorArgsClazz[i] = String.class;
                } else if ("parent".equals(param[i])) {
                    constructorArgs[i] = parent;
                    constructorArgsClazz[i] = String.class;
                } else {
                    throw new InternalError("No such param: " + param[i]);
                }


            }
            Constructor<AbstractBox> constructorObject;
            try {
                if (param.length > 0) {
                    constructorObject = clazz.getConstructor(constructorArgsClazz);
                } else {
                    constructorObject = clazz.getConstructor();
                }

                return constructorObject.newInstance(constructorArgs);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }


        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private class FourCcToBox {
        private String type;
        private byte[] userType;
        private String parent;
        private String clazzName;
        private String[] param;

        public FourCcToBox(String type, byte[] userType, String parent) {
            this.type = type;
            this.parent = parent;
            this.userType = userType;
        }

        public String getClazzName() {
            return clazzName;
        }

        public String[] getParam() {
            return param;
        }

        public FourCcToBox invoke() {
            String constructor;
            if (userType != null) {
                if (!"uuid".equals((type))) {
                    throw new RuntimeException("we have a userType but no uuid box type. Something's wrong");
                }
                constructor = mapping.getProperty((parent) + "-uuid[" + Hex.encodeHex(userType).toUpperCase() + "]");
                if (constructor == null) {
                    constructor = mapping.getProperty("uuid[" + Hex.encodeHex(userType).toUpperCase() + "]");
                }
                if (constructor == null) {
                    constructor = mapping.getProperty("uuid");
                }
            } else {
                constructor = mapping.getProperty((parent) + "-" + (type));
                if (constructor == null) {
                    constructor = mapping.getProperty((type));
                }
            }
            if (constructor == null) {
                constructor = mapping.getProperty("default");
            }
            if (constructor == null) {
                throw new RuntimeException("No box object found for " + type);
            }
            Matcher m = p.matcher(constructor);
            boolean matches = m.matches();
            if (!matches) {
                throw new RuntimeException("Cannot work with that constructor: " + constructor);
            }
            clazzName = m.group(1);
            param = m.group(2).split(",");
            return this;
        }
    }
}
