package com.camackenzie.exvi.server.util;

import com.camackenzie.exvi.core.api.GenericDataRequest;
import com.camackenzie.exvi.core.model.ActualExercise;
import com.camackenzie.exvi.core.model.ActualExerciseSet;
import com.camackenzie.exvi.core.model.Exercise;
import com.camackenzie.exvi.core.model.ExerciseSet;
import com.google.gson.InstanceCreator;
import org.jetbrains.annotations.NotNull;
import com.google.gson.*;

import java.util.ArrayList;

public class Eson {

    @NotNull
    private final Gson gson,
            defaultGson = new Gson();

    public Eson() {
        GsonBuilder builder = new GsonBuilder();
        this.registerTypeAdapters(builder);
        this.gson = builder.create();
    }

    @NotNull
    public Gson getGson() {
        return this.gson;
    }

    private void registerTypeAdapters(@NotNull GsonBuilder builder) {
        builder.registerTypeAdapter(GenericDataRequest.class,
                (InstanceCreator) type -> new GenericDataRequest("GenericDataRequest") {
                    @NotNull
                    @Override
                    public String toJson() {
                        return defaultGson.toJson(this);
                    }

                    @NotNull
                    @Override
                    public String getUID() {
                        return this.getRequester().get();
                    }
                });
    }
}
