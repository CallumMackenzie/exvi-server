/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.util;

/**
 *
 * @author callum
 */
public interface SMSClient {

    public void sendText(String recipient, String message);
}