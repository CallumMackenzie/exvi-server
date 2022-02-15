/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.database;

import com.camackenzie.exvi.core.api.VerificationRequest;

/**
 *
 * @author callum
 */
public class VerificationDatabaseEntry
        extends DatabaseEntry {

    private String verificationCode,
            username,
            email,
            phone;
    private long verificationCodeUTC;

    public VerificationDatabaseEntry(String username,
            String email,
            String phone,
            String verificationCode,
            long verificationCodeUTC) {
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.verificationCode = verificationCode;
        this.verificationCodeUTC = verificationCodeUTC;
    }

    public VerificationDatabaseEntry(String username,
            String email,
            String phone,
            String verificationCode) {
        this(username,
                email,
                phone,
                verificationCode,
                System.currentTimeMillis());
    }

    public VerificationDatabaseEntry(VerificationRequest uvd, String code) {
        this(uvd.getUsername(),
                uvd.getEmail(),
                uvd.getPhone(),
                code);
    }

    /**
     * @return the verificationCode
     */
    public String getVerificationCode() {
        return verificationCode;
    }

    /**
     * @param verificationCode the verificationCode to set
     */
    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    /**
     * @return the username
     */
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
     * @return the verificationCodeUTC
     */
    public long getVerificationCodeUTC() {
        return verificationCodeUTC;
    }

    /**
     * @param verificationCodeUTC the verificationCodeUTC to set
     */
    public void setVerificationCodeUTC(long verificationCodeUTC) {
        this.verificationCodeUTC = verificationCodeUTC;
    }

}
