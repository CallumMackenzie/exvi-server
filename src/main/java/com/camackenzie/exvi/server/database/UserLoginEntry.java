/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.database;

/**
 *
 * @author callum
 */
public class UserLoginEntry extends DatabaseEntry {

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
    }

    public String[] getAccessKeys() {
        return this.accessKeys;
    }

    public void addAccessKey(String key) {
        String[] newKeys = new String[this.accessKeys.length + 1];
        newKeys[0] = key;
        System.arraycopy(this.accessKeys, 0, newKeys, 1, this.accessKeys.length);
        this.accessKeys = newKeys;
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
