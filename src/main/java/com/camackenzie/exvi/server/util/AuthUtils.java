/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.util;

import com.camackenzie.exvi.core.util.CryptographyUtils;
import com.camackenzie.exvi.server.database.UserLoginEntry;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author callum
 */
public class AuthUtils {

    public static String generateAccessKey(AWSDynamoDB database, String username) {
        String accessKey = CryptographyUtils.generateSalt(1024);
        UserLoginEntry entry = database.getObjectFromTable("exvi-user-login",
                "username",
                username,
                UserLoginEntry.class);
        entry.addAccessKey(accessKey);
        database.deleteObjectFromTable("exvi-user-login", "username", username);
        database.putObjectInTable("exvi-user-login", entry);
        return accessKey;
    }

    public static String decryptPasswordHash(String hash) {
        return new String(CryptographyUtils.bytesFromBase64String(hash),
                StandardCharsets.UTF_8);
    }

}
