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
import java.util.HashMap;

/**
 *
 * @author callum
 */
public class AWSDynamoDB {

    private final AmazonDynamoDB awsDynamoDB;
    private final DynamoDB docClient;
    private final HashMap<String, Table> tableNameMap;
    private final Gson gson = new Gson();

    public AWSDynamoDB() {
        this.awsDynamoDB = AmazonDynamoDBClientBuilder
                .standard()
                .withRegion(Regions.US_EAST_2)
                .build();
        this.docClient = new DynamoDB(this.awsDynamoDB);
        this.tableNameMap = new HashMap<>();
    }

    public HashMap<String, Table> getTableNameMap() {
        return this.tableNameMap;
    }

    public Table cacheTable(String name) {
        Table t = this.getTable(name);
        this.tableNameMap.put(name, t);
        return t;
    }

    public Table getTable(String name) {
        if (this.tableNameMap.containsKey(name)) {
            return this.tableNameMap.get(name);
        }
        return this.docClient.getTable(name);
    }

    public DynamoDB getDocClient() {
        return this.docClient;
    }

    public AmazonDynamoDB getAwsDynamoDB() {
        return this.awsDynamoDB;
    }

    public <T extends DatabaseEntry> void putObjectInTable(String table, T object) {
        Table t = this.getTable(table);
        this.putObjectInTable(t, object);
    }

    public <T extends DatabaseEntry> void putObjectInTable(Table table, T object) {
        table.putItem(Item.fromJSON(this.gson.toJson(object)));
    }

    public <T extends DatabaseEntry> T getObjectFromTable(Table table, String hashKey,
            String value, Class<T> cls) {
        return gson.fromJson(table.getItem(hashKey, value).toJSON(), cls);
    }

    public <T extends DatabaseEntry> T getObjectFromTable(String table, String hashKey,
            String value, Class<T> cls) {
        return this.getObjectFromTable(this.getTable(table), hashKey, value, cls);
    }

    public <T extends DatabaseEntry> T getObjectFromTableOr(Table table, String hashKey,
            String value, Class<T> cls, T def) {
        Item item = table.getItem(hashKey, value);
        if (DatabaseEntry.matchesItem(item, cls)) {
            return gson.fromJson(item.toJSON(), cls);
        }
        return def;
    }

    public void deleteObjectFromTable(Table table, String hashKey, String value) {
        table.deleteItem(hashKey, value);
    }

}
