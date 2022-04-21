/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.database;

import com.camackenzie.exvi.core.api.VerificationRequest;
import com.camackenzie.exvi.server.util.Serializers;
import kotlin.Unit;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.descriptors.SerialDescriptor;
import kotlinx.serialization.encoding.CompositeDecoder;
import kotlinx.serialization.encoding.Decoder;
import kotlinx.serialization.encoding.Encoder;
import org.jetbrains.annotations.NotNull;

import static com.camackenzie.exvi.core.model.ExviSerializer.Builtin.element;
import static kotlinx.serialization.descriptors.SerialDescriptorsKt.buildClassSerialDescriptor;

/**
 * @author callum
 */
@SuppressWarnings("unused")
public class VerificationDatabaseEntry {

    @NotNull
    private String verificationCode,
            username,
            email,
            phone;
    private long verificationCodeUTC;

    private static final SerialDescriptor descriptor = buildClassSerialDescriptor(
            "com.camackenzie.exvi.server.database.VerificationDatabaseEntry",
            new SerialDescriptor[0],
            bt -> {
                var des = Serializers.string.getDescriptor();
                element(bt, "verificationCode", des);
                element(bt, "username", des);
                element(bt, "email", des);
                element(bt, "phone", des);
                element(bt, "verificationCodeUTC", Serializers.longType.getDescriptor());
                return Unit.INSTANCE;
            }
    );

    public VerificationDatabaseEntry(@NotNull String username,
                                     @NotNull String email,
                                     @NotNull String phone,
                                     @NotNull String verificationCode,
                                     long verificationCodeUTC) {
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.verificationCode = verificationCode;
        this.verificationCodeUTC = verificationCodeUTC;
    }

    public VerificationDatabaseEntry(@NotNull String username,
                                     @NotNull String email,
                                     @NotNull String phone,
                                     @NotNull String verificationCode) {
        this(username,
                email,
                phone,
                verificationCode,
                System.currentTimeMillis());
    }

    public VerificationDatabaseEntry(@NotNull VerificationRequest uvd, @NotNull String code) {
        this(uvd.getUsername().get(),
                uvd.getEmail().get(),
                uvd.getPhone().get(),
                code);
    }

    private VerificationDatabaseEntry() {
    }

    /**
     * @return the verificationCode
     */
    @NotNull
    public String getVerificationCode() {
        return verificationCode;
    }

    /**
     * @param verificationCode the verificationCode to set
     */
    public void setVerificationCode(@NotNull String verificationCode) {
        this.verificationCode = verificationCode;
    }

    /**
     * @return the username
     */
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

    public static final KSerializer<VerificationDatabaseEntry> serializer = new KSerializer<>() {

        @Override
        public VerificationDatabaseEntry deserialize(@NotNull Decoder decoder) {
            var ret = new VerificationDatabaseEntry();
            var struct = decoder.beginStructure(descriptor);
            SerializerLoop:
            while (true) {
                var index = struct.decodeElementIndex(descriptor);
                switch (index) {
                    case 0:
                        ret.verificationCode = struct.decodeStringElement(descriptor, 0);
                        break;
                    case 1:
                        ret.username = struct.decodeStringElement(descriptor, 1);
                        break;
                    case 2:
                        ret.email = struct.decodeStringElement(descriptor, 2);
                        break;
                    case 3:
                        ret.phone = struct.decodeStringElement(descriptor, 3);
                        break;
                    case 4:
                        ret.verificationCodeUTC = struct.decodeLongElement(descriptor, 4);
                        break;
                    case CompositeDecoder.DECODE_DONE:
                        break SerializerLoop;
                }
            }
            struct.endStructure(descriptor);
            return ret;
        }

        @NotNull
        @Override
        public SerialDescriptor getDescriptor() {
            return descriptor;
        }

        @Override
        public void serialize(@NotNull Encoder encoder, VerificationDatabaseEntry e) {
            var struct = encoder.beginStructure(descriptor);
            struct.encodeStringElement(descriptor, 0, e.verificationCode);
            struct.encodeStringElement(descriptor, 1, e.username);
            struct.encodeStringElement(descriptor, 2, e.email);
            struct.encodeStringElement(descriptor, 3, e.phone);
            struct.encodeLongElement(descriptor, 4, e.verificationCodeUTC);
            struct.endStructure(descriptor);
        }
    };

}
