package gg.hermes;

import com.google.gson.Gson;

public final class Utility
{
    private static final Gson gson = new Gson();

    private Utility() {}

    public static String objToJsonString(final Object obj) {
        return gson.toJson(obj);
    }
}
