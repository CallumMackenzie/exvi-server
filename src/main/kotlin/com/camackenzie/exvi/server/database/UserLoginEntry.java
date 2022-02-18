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

/**
 *
 * @author callum
 */
public class UserLoginEntry extends DatabaseEntry<UserLoginEntry> {

    public static void ensureAccessKeyValid(AWSDynamoDB database,
            String user,
            String key) {
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

    public static void ensureAccessKeyValid(AWSDynamoDB database,
                                            EncodedStringCache user,
                                            EncodedStringCache key) {
        ensureAccessKeyValid(database, user.get(), key.get());
    }

    private String username;
    private String phone;
    private String email;
    private String passwordHash;
    private String salt;
    private String[] accessKeys;

    public UserLoginEntry(String username,
            String phone,
            String email,
            String passwordHash,
            String salt) {
        this.username = username;
        this.phone = phone;
        this.email = email;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.accessKeys = new String[0];
    }

    public String[] getAccessKeys() {
        return this.accessKeys;
    }

    public void addAccessKey(String key) {
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
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the phone
     */
    public String getPhone() {
        return phone;
    }

    /**
     * @param phone the phone to set
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return the passwordHash
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * @param passwordHash the passwordHash to set
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * @return the salt
     */
    public String getSalt() {
        return salt;
    }

    /**
     * @param salt the salt to set
     */
    public void setSalt(String salt) {
        this.salt = salt;
    }

}
