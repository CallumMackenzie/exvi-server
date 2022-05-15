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
 * @author callum
 */
public class AuthUtils {

    private static final int MAX_ACCESS_KEYS = 4;

    public static String getAccessKey(@NotNull DocumentDatabase database, @NotNull String username) {
        // Get user data
        UserLoginEntry entry = database.getObjectFromTable("exvi-user-login",
                "username",
                username,
                UserLoginEntry.serializer);

        // Return random access key if there are more than MAX_ACCESS_KEYS
        if (entry.getAccessKeys().length >= MAX_ACCESS_KEYS)
            return entry.getAccessKeys()[(int) (Math.random() * entry.getAccessKeys().length)];

        // Generate and store new access key
        String accessKey = CryptographyUtils.generateSalt(256);
        entry.addAccessKey(accessKey);
        database.deleteObjectFromTable("exvi-user-login", "username", username);
        database.putObjectInTable("exvi-user-login", UserLoginEntry.serializer, entry);
        return accessKey;
    }

    public static String decryptPasswordHash(@NotNull String hash) {
        return new String(CryptographyUtils.bytesFromBase64String(hash),
                StandardCharsets.UTF_8);
    }

}
