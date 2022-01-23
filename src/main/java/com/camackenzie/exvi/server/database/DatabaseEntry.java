/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.database;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.google.gson.Gson;
import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 *
 * @author callum
 */
public abstract class DatabaseEntry<T extends DatabaseEntry> {

    private static final Gson gson = new Gson();

    public static boolean matchesItem(Item item,
            Class<? extends DatabaseEntry> cls) {
        if (item == null) {
            return false;
        }
        int nFields = item.asMap().size();
        ArrayList<Field> fields = new ArrayList<>();
        Class superClass = cls;
        while (superClass != null) {
            for (var f : superClass.getDeclaredFields()) {
                fields.add(f);
            }
            superClass = superClass.getSuperclass();
        }
        if (fields.size() != nFields) {
            return false;
        }
        for (var field : fields) {
            if (!item.hasAttribute(field.getName())) {
                return false;
            }
        }
        return true;
    }

    public static <T extends DatabaseEntry> T fromItem(Item item,
            Class<T> cls) {
        if (DatabaseEntry.matchesItem(item, cls)) {
            return gson.fromJson(item.toJSON(), cls);
        }
        return null;
    }

}
