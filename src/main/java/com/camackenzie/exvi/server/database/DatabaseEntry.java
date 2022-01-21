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

/**
 *
 * @author callum
 */
public abstract class DatabaseEntry<T extends DatabaseEntry> {

    private static final Gson gson = new Gson();

    public static boolean matchesOuterItemAttributes(Item item,
            Class<? extends DatabaseEntry> cls) {
        if (item == null) {
            return false;
        }
        int nFields = item.asMap().size();
        Field[] fields = cls.getDeclaredFields();
        if (fields.length != nFields) {
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
        if (DatabaseEntry.matchesOuterItemAttributes(item, cls)) {
            return gson.fromJson(item.toJSON(), cls);
        }
        return null;
    }

}
