/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.database;

import com.camackenzie.exvi.core.util.EncodedStringCache;
import com.camackenzie.exvi.core.util.Identifiable;
import com.camackenzie.exvi.core.util.RawIdentifiable;
import com.camackenzie.exvi.server.util.ApiException;
import com.camackenzie.exvi.server.util.DocumentDatabase;
import com.camackenzie.exvi.server.util.Serializers;
import com.camackenzie.exvi.server.util.SortedCache;
import kotlin.Unit;
import kotlin.jvm.Transient;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.descriptors.SerialDescriptor;
import kotlinx.serialization.encoding.Decoder;
import kotlinx.serialization.encoding.Encoder;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Objects;

import static com.camackenzie.exvi.core.model.ExviSerializer.Builtin.element;
import static kotlinx.serialization.descriptors.SerialDescriptorsKt.buildClassSerialDescriptor;

/**
 * @author callum
 */
public class UserLoginEntry implements Identifiable {

    private static final SortedCache<UserLoginEntry> userCache
            = new SortedCache<>(Comparator.comparing(a -> a.accesses), 5);

    @NotNull
    public String username;
    @NotNull
    public String phone;
    @NotNull
    public String email;
    @NotNull
    private String passwordHash;
    @NotNull
    public String salt;
    private String[] accessKeys;

    // Used to score importance of account for caching
    @Transient
    private int accesses;

    private static final SerialDescriptor descriptor = buildClassSerialDescriptor(
            "com.camackenzie.exvi.server.database.UserLoginEntry",
            new SerialDescriptor[0],
            bt -> {
                var des = Serializers.string.getDescriptor();
                element(bt, "username", des);
                element(bt, "phone", des);
                element(bt, "email", des);
                element(bt, "passwordHash", des);
                element(bt, "salt", des);
                element(bt, "accessKeys", Serializers.stringArray.getDescriptor());
                return Unit.INSTANCE;
            }
    );

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

    private UserLoginEntry() {
        this("", "", "", "", "");
    }

    @NotNull
    public String[] getAccessKeys() {
        return this.accessKeys;
    }

    @NotNull
    public String getPasswordHash() {
        return passwordHash;
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

    public void resetAccessKeys() {
        this.accessKeys = new String[0];
    }

    public static void ensureAccessKeyValid(@NotNull DocumentDatabase database,
                                            @NotNull String user,
                                            @NotNull String key) {
        // Checked for cached user data
        UserLoginEntry authData = userCache.matchFirst(u -> u.username.equalsIgnoreCase(user));
        // Retrieve user data from database if it is not cached
        if (authData == null) authData = database.getObjectFromTable("exvi-user-login",
                "username", user, UserLoginEntry.serializer);
        // If no user data found
        if (authData == null) {
            throw new ApiException(400, "User does not exist");
        }
        // Validate the key
        boolean keyMatched = false;
        for (var akey : authData.getAccessKeys()) {
            if (akey.equals(key)) {
                keyMatched = true;
                break;
            }
        }
        if (keyMatched) {
            ++authData.accesses;
            userCache.cache(authData);
        } else throw new ApiException(400, "Invalid credentials");
    }

    public static void ensureAccessKeyValid(@NotNull DocumentDatabase database,
                                            @NotNull EncodedStringCache user,
                                            @NotNull EncodedStringCache key) {
        ensureAccessKeyValid(database, user.get(), key.get());
    }

    @Override
    public int compareTo(@NotNull Identifiable identifiable) {
        return this.username.compareTo(identifiable.getIdentifier().get());
    }

    @NotNull
    @Override
    public EncodedStringCache getIdentifier() {
        return new EncodedStringCache(username);
    }

    @NotNull
    @Override
    public RawIdentifiable toRawIdentifiable() {
        return new RawIdentifiable(username);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserLoginEntry that = (UserLoginEntry) o;
        return username.equals(that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    public static final KSerializer<UserLoginEntry> serializer = new KSerializer<>() {

        @Override
        public UserLoginEntry deserialize(@NotNull Decoder decoder) {
            var ret = new UserLoginEntry();
            var struct = decoder.beginStructure(descriptor);
            SerializerLoop:
            while (true) {
                var index = struct.decodeElementIndex(descriptor);
                switch (index) {
                    case 0:
                        ret.username = struct.decodeStringElement(descriptor, 0);
                        break;
                    case 1:
                        ret.phone = struct.decodeStringElement(descriptor, 1);
                        break;
                    case 2:
                        ret.email = struct.decodeStringElement(descriptor, 2);
                        break;
                    case 3:
                        ret.passwordHash = struct.decodeStringElement(descriptor, 3);
                        break;
                    case 4:
                        ret.salt = struct.decodeStringElement(descriptor, 4);
                        break;
                    case 5:
                        ret.accessKeys = struct.decodeSerializableElement(descriptor, 5, Serializers.stringArray, null);
                        break;
                    default:
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
        public void serialize(@NotNull Encoder encoder, UserLoginEntry e) {
            var struct = encoder.beginStructure(descriptor);
            struct.encodeStringElement(descriptor, 0, e.username);
            struct.encodeStringElement(descriptor, 1, e.phone);
            struct.encodeStringElement(descriptor, 2, e.email);
            struct.encodeStringElement(descriptor, 3, e.passwordHash);
            struct.encodeStringElement(descriptor, 4, e.salt);
            struct.encodeSerializableElement(descriptor, 5, Serializers.stringArray, e.accessKeys);
            struct.endStructure(descriptor);
        }
    };
}
