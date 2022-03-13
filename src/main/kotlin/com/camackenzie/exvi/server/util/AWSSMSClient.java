/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.util;

import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

/**
 *
 * @author callum
 */
public class AWSSMSClient implements SMSClient {

    @NotNull
    private final SnsClient client;

    public AWSSMSClient() {
        this.client = SnsClient.builder()
                .region(Region.US_EAST_2)
                .build();
    }

    @Override
    public void sendText(@NotNull String recipient, @NotNull String message) {
        PublishRequest pr = PublishRequest.builder()
                .message(message)
                .phoneNumber(recipient)
                .build();
        PublishResponse result = this.client.publish(pr);
        if (!result.sdkHttpResponse().isSuccessful()) {
            throw new RuntimeException("Message id (" + result.messageId() + ") could not be sent to " + recipient
                    + ". Response code " + result.sdkHttpResponse().statusCode()
                    + ": " + result.sdkHttpResponse().statusText());
        }
    }

}
