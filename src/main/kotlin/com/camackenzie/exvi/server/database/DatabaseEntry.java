/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.database;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

/**
 * @author callum
 */
@SuppressWarnings("unused")
public abstract class DatabaseEntry<T extends DatabaseEntry<?>> {

    @NotNull
    private static final Gson gson = new Gson();

    public static boolean matchesItem(Item item, @NotNull Class<?> cls) {
        if (item == null) {
            return false;
        }
        int nFields = item.asMap().size();
        ArrayList<Field> fields = new ArrayList<>();
        Class<?> superClass = cls;
        while (superClass != null) {
            for (var f : superClass.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())
                        && !Modifier.isTransient(f.getModifiers())) {
                    fields.add(f);
                }
            }
            superClass = superClass.getSuperclass();
        }
        if (nFields != fields.size()) {
            return false;
        }
        for (var field : fields) {
            if (!item.hasAttribute(field.getName())) {
                return false;
            }
        }
        return true;
    }

    public static <T> T fromItem(Item item, @NotNull Class<T> cls) {
        if (DatabaseEntry.matchesItem(item, cls)) {
            return gson.fromJson(item.toJSON(), cls);
        }
        return null;
    }

}
