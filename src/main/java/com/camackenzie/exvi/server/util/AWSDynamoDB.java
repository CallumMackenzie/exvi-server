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
import com.amazonaws.services.dynamodbv2.document.Table;
import java.util.HashMap;

/**
 *
 * @author callum
 */
public class AWSDynamoDB {

    private final AmazonDynamoDB awsDynamoDB;
    private final DynamoDB docClient;
    private final HashMap<String, Table> tableNameMap;

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

}
