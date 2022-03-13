/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.util;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.camackenzie.exvi.server.database.DatabaseEntry;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * @author callum
 */
@SuppressWarnings("unused")
public class AWSDynamoDB {

    @NotNull
    private final AmazonDynamoDB awsDynamoDB;
    @NotNull
    private final DynamoDB docClient;
    @NotNull
    private final HashMap<String, Table> tableNameMap;
    @NotNull
    private final Gson gson = new Gson();

    public AWSDynamoDB() {
        this.awsDynamoDB = AmazonDynamoDBClientBuilder
                .standard()
                .withRegion(Regions.US_EAST_2)
                .build();
        this.docClient = new DynamoDB(this.awsDynamoDB);
        this.tableNameMap = new HashMap<>();
    }

    @NotNull
    public HashMap<String, Table> getTableNameMap() {
        return this.tableNameMap;
    }

    public Table cacheTable(@NotNull String name) {
        Table t = this.getTable(name);
        this.tableNameMap.put(name, t);
        return t;
    }

    public Table getTable(@NotNull String name) {
        if (this.tableNameMap.containsKey(name)) {
            return this.tableNameMap.get(name);
        }
        return this.docClient.getTable(name);
    }

    @NotNull
    public DynamoDB getDocClient() {
        return this.docClient;
    }

    @NotNull
    public AmazonDynamoDB getAwsDynamoDB() {
        return this.awsDynamoDB;
    }

    public <T extends DatabaseEntry<?>> void putObjectInTable(@NotNull String table, T object) {
        Table t = this.cacheTable(table);
        this.putObjectInTable(t, object);
    }

    public <T extends DatabaseEntry<?>> void putObjectInTable(@NotNull Table table, T object) {
        table.putItem(Item.fromJSON(this.gson.toJson(object)));
    }

    public <T extends DatabaseEntry<?>> T getObjectFromTable(@NotNull Table table,
                                                          @NotNull String hashKey,
                                                          @NotNull String value,
                                                          @NotNull Class<T> cls) {
        return DatabaseEntry.fromItem(table.getItem(hashKey, value), cls);
    }

    public <T extends DatabaseEntry<?>> T getObjectFromTable(@NotNull String table,
                                                          @NotNull String hashKey,
                                                          @NotNull String value,
                                                          @NotNull Class<T> cls) {
        return this.getObjectFromTable(this.cacheTable(table), hashKey, value, cls);
    }

    public <T extends DatabaseEntry<?>> T getObjectFromTableOr(@NotNull Table table,
                                                            @NotNull String hashKey,
                                                            @NotNull String value,
                                                            @NotNull Class<T> cls,
                                                            T def) {
        T ret = this.getObjectFromTable(table, hashKey, value, cls);
        if (ret == null) {
            return def;
        }
        return ret;
    }

    public void deleteObjectFromTable(@NotNull Table table, @NotNull String hashKey, @NotNull String value) {
        table.deleteItem(hashKey, value);
    }

    public void deleteObjectFromTable(@NotNull String table, @NotNull String hashKey, @NotNull String value) {
        this.cacheTable(table).deleteItem(hashKey, value);
    }

}
