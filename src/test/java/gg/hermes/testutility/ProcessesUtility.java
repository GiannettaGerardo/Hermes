package gg.hermes.testutility;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ProcessesUtility
{
    public static final Gson gson = new Gson();
    private static final Map<String, ModularHermesProcess> processes;

    static {
        try (
            InputStream inputStream = ClassLoader.getSystemResourceAsStream("test-modular-processes.json");
            Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        ) {
            processes = gson.fromJson(reader, new TypeToken<Map<String, ModularHermesProcess>>() {}.getType());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ModularHermesProcess get(final String processName) {
        return processes.get(processName);
    }
}
