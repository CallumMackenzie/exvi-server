/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.database;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.google.gson.Gson;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author callum
 */
public class DatabaseEntryTest {

    private static final Gson gson = new Gson();

    public DatabaseEntryTest() {
    }

    @org.junit.BeforeClass
    public static void setUpClass() throws Exception {
    }

    /**
     * Test of matchesItem method, of class DatabaseEntry.
     */
    @org.junit.Test
    public void testMatchesItem() {
        assertTrue(DatabaseEntry.matchesItem(Item.fromJSON(
                gson.toJson(new VerificationDatabaseEntry("a", "b", "c", "d", 0))
        ), VerificationDatabaseEntry.class));
    }

    @org.junit.Test
    public void testDoesntMatchItem() {
        assertFalse(DatabaseEntry.matchesItem(Item.fromJSON("{}"), VerificationDatabaseEntry.class));
    }

    /**
     * Test of fromItem method, of class DatabaseEntry.
     */
    @org.junit.Test
    public void testFromItem() {
        VerificationDatabaseEntry e1 = new VerificationDatabaseEntry("a", "b", "c", "d", 0);
        String entryJson = gson.toJson(e1);
        Item entryItem = Item.fromJSON(entryJson);
        VerificationDatabaseEntry e2
                = DatabaseEntry.fromItem(entryItem, VerificationDatabaseEntry.class);
        assertEquals(e1.getUsername(), e2.getUsername());
        assertEquals(e1.getEmail(), e2.getEmail());
        assertEquals(e1.getPhone(), e2.getPhone());
        assertEquals(e1.getVerificationCode(), e2.getVerificationCode());
        assertEquals(e1.getVerificationCodeUTC(), e2.getVerificationCodeUTC());
    }

}
