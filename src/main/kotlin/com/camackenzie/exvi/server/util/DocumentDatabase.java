package com.camackenzie.exvi.server.util;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.api.BatchGetItemApi;
import com.amazonaws.services.dynamodbv2.document.api.BatchWriteItemApi;
import com.amazonaws.services.dynamodbv2.document.api.ListTablesApi;
import com.camackenzie.exvi.core.model.ExviSerializer;
import kotlinx.serialization.DeserializationStrategy;
import kotlinx.serialization.SerializationStrategy;
import org.jetbrains.annotations.NotNull;

// For convenience
@SuppressWarnings("unused")
public interface DocumentDatabase extends ListTablesApi, BatchGetItemApi, BatchWriteItemApi {

    Table getTable(@NotNull String table);

    default <T> void putObjectInTable(@NotNull String table, @NotNull SerializationStrategy<T> serializer, T object) {
        this.putObjectInTable(this.getTable(table), serializer, object);
    }

    default <T> void putObjectInTable(@NotNull Table table, @NotNull SerializationStrategy<T> serializer, T object) {
        table.putItem(Item.fromJSON(ExviSerializer.toJson(serializer, object)));
    }

    default <T> T getObjectFromTable(@NotNull Table table, @NotNull String hashKey,
                                     @NotNull String value, @NotNull DeserializationStrategy<T> serializer) {
        var item = table.getItem(hashKey, value);
        if (item == null) {
            return null;
        }
        return ExviSerializer.fromJson(serializer, item.toJSON());
    }

    default <T> T getObjectFromTable(@NotNull String table, @NotNull String hashKey,
                                     @NotNull String value, @NotNull DeserializationStrategy<T> serializer) {
        return this.getObjectFromTable(this.getTable(table), hashKey, value, serializer);
    }

    default void deleteObjectFromTable(@NotNull Table table, @NotNull String hashKey, @NotNull String value) {
        table.deleteItem(hashKey, value);
    }

    default void deleteObjectFromTable(@NotNull String table, @NotNull String hashKey, @NotNull String value) {
        this.getTable(table).deleteItem(hashKey, value);
    }
}
