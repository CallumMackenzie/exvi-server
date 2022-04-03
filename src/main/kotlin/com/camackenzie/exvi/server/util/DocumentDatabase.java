package com.camackenzie.exvi.server.util;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.api.BatchGetItemApi;
import com.amazonaws.services.dynamodbv2.document.api.BatchWriteItemApi;
import com.amazonaws.services.dynamodbv2.document.api.ListTablesApi;
import com.camackenzie.exvi.server.database.DatabaseEntry;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;

// For convenience
@SuppressWarnings("unused")
public interface DocumentDatabase extends ListTablesApi, BatchGetItemApi, BatchWriteItemApi {

    @NotNull
    Gson getGson();

    Table getTable(@NotNull String table);

    default <T extends DatabaseEntry<?>> void putObjectInTable(@NotNull String table, T object) {
        this.putObjectInTable(this.getTable(table), object);
    }

    default <T extends DatabaseEntry<?>> void putObjectInTable(@NotNull Table table, T object) {
        table.putItem(Item.fromJSON(this.getGson().toJson(object)));
    }

    default <T extends DatabaseEntry<?>> T getObjectFromTable(@NotNull Table table, @NotNull String hashKey,
                                                              @NotNull String value, @NotNull Class<T> cls) {
        return DatabaseEntry.fromItem(table.getItem(hashKey, value), cls);
    }

    default <T extends DatabaseEntry<?>> T getObjectFromTable(@NotNull String table, @NotNull String hashKey,
                                                              @NotNull String value, @NotNull Class<T> cls) {
        return this.getObjectFromTable(this.getTable(table), hashKey, value, cls);
    }

    default <T extends DatabaseEntry<?>> T getObjectFromTableOr(@NotNull Table table, @NotNull String hashKey,
                                                                @NotNull String value, @NotNull Class<T> cls,
                                                                T def) {
        T ret = this.getObjectFromTable(table, hashKey, value, cls);
        if (ret == null) {
            return def;
        }
        return ret;
    }

    default void deleteObjectFromTable(@NotNull Table table, @NotNull String hashKey, @NotNull String value) {
        table.deleteItem(hashKey, value);
    }

    default void deleteObjectFromTable(@NotNull String table, @NotNull String hashKey, @NotNull String value) {
        this.getTable(table).deleteItem(hashKey, value);
    }
}
