/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.database;

import com.camackenzie.exvi.core.api.GenericDataResult;
import com.camackenzie.exvi.core.util.EncodedStringCache;
import com.camackenzie.exvi.server.util.AWSDynamoDB;
import com.camackenzie.exvi.server.util.ApiException;
import com.camackenzie.exvi.server.util.DocumentDatabase;
import org.jetbrains.annotations.NotNull;

/**
 * @author callum
 */
public class UserLoginEntry {

    public static void ensureAccessKeyValid(@NotNull DocumentDatabase database,
                                            @NotNull String user,
                                            @NotNull String key) {
        UserLoginEntry authData = database.getObjectFromTable("exvi-user-login",
                "username", user, UserLoginEntry.class);
        if (authData == null) {
            throw new ApiException(400, "User does not exist");
        }
        boolean keyMatched = false;
        for (var akey : authData.getAccessKeys()) {
            if (akey.equals(key)) {
                keyMatched = true;
                break;
            }
        }
        if (!keyMatched) {
            throw new ApiException(400, "Invalid credentials");
        }
    }

    public static void ensureAccessKeyValid(@NotNull DocumentDatabase database,
                                            @NotNull EncodedStringCache user,
                                            @NotNull EncodedStringCache key) {
        ensureAccessKeyValid(database, user.get(), key.get());
    }

    @NotNull
    private String username;
    @NotNull
    private String phone;
    @NotNull
    private String email;
    @NotNull
    private String passwordHash;
    @NotNull
    private String salt;
    private String[] accessKeys;

    public UserLoginEntry(@NotNull String username,
                          @NotNull String phone,
                          @NotNull String email,
                          @NotNull String passwordHash,
                          @NotNull String salt) {
        this.username = username;
        this.phone = phone;
        this.email = email;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.accessKeys = new String[0];
    }

    @NotNull
    public String[] getAccessKeys() {
        return this.accessKeys;
    }

    public void addAccessKey(@NotNull String key) {
        if (this.accessKeys == null) {
            this.accessKeys = new String[]{key};
        } else {
            String[] newKeys = new String[this.accessKeys.length + 1];
            newKeys[0] = key;
            System.arraycopy(this.accessKeys, 0, newKeys, 1, this.accessKeys.length);
            this.accessKeys = newKeys;
        }
    }

    //    public void removeAccessKey(String key) {
//        int toRemove = -1;
//        for (int i = 0; i < this.accessKeys.length; ++i) {
//            if (this.accessKeys[i].equals(key)) {
//                toRemove = i;
//                break;
//            }
//        }
//        if (toRemove != -1) {
//            String[] newKeys = new String[this.accessKeys.length - 1];
//            System.arraycopy(this.accessKeys, 0, newKeys, 0, toRemove);
//            System.arraycopy(this.accessKeys, toRemove + 1, newKeys, toRemove + 1, this.accessKeys.length - toRemove);
//        }
//    }
    @NotNull
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(@NotNull String username) {
        this.username = username;
    }

    /**
     * @return the phone
     */
    @NotNull
    public String getPhone() {
        return phone;
    }

    /**
     * @param phone the phone to set
     */
    public void setPhone(@NotNull String phone) {
        this.phone = phone;
    }

    /**
     * @return the email
     */
    @NotNull
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(@NotNull String email) {
        this.email = email;
    }

    /**
     * @return the passwordHash
     */
    @NotNull
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * @param passwordHash the passwordHash to set
     */
    public void setPasswordHash(@NotNull String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * @return the salt
     */
    @NotNull
    public String getSalt() {
        return salt;
    }

    /**
     * @param salt the salt to set
     */
    public void setSalt(@NotNull String salt) {
        this.salt = salt;
    }

}
