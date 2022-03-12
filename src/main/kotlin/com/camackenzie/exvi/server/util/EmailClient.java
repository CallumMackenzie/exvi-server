/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.util;

import org.jetbrains.annotations.NotNull;

/**
 * @author callum
 */
public interface EmailClient {
    void sendEmail(@NotNull String sender,
                   @NotNull String recipient,
                   @NotNull String subject,
                   @NotNull String htmlBody,
                   @NotNull String plainTextBody);
}
