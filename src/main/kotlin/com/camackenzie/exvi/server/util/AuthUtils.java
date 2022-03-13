/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.util;

import com.camackenzie.exvi.core.util.CryptographyUtils;
import com.camackenzie.exvi.server.database.UserLoginEntry;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

/**
 *
 * @author callum
 */
public class AuthUtils {

    public static String generateAccessKey(@NotNull AWSDynamoDB database, @NotNull String username) {
        String accessKey = CryptographyUtils.generateSalt(256);
        UserLoginEntry entry = database.getObjectFromTable("exvi-user-login",
                "username",
                username,
                UserLoginEntry.class);

        if (entry.getAccessKeys().length >= 4) {
            return entry.getAccessKeys()[0];
        }

        entry.addAccessKey(accessKey);
        database.deleteObjectFromTable("exvi-user-login", "username", username);
        database.putObjectInTable("exvi-user-login", entry);
        return accessKey;
    }

    public static String decryptPasswordHash(@NotNull String hash) {
        return new String(CryptographyUtils.bytesFromBase64String(hash),
                StandardCharsets.UTF_8);
    }

}
