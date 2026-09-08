package rs.majic.de.nuc.megaskins.megaskins.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Slf4j
public class Config {

    public static final File configFile = new File("config.json").getAbsoluteFile();
    private static MegaSkinsConfiguration config = null;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    public static void initialize() throws IOException {
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            config = defaultConfig();
            Files.writeString(configFile.toPath(), GSON.toJson(config));
        } else {
            try {
                config = GSON.fromJson(Files.readString(configFile.toPath()), MegaSkinsConfiguration.class);
                config.check();
            } catch (Exception e) {
                log.error("Failed loading the config", e);
                if (
                        configFile.renameTo(
                                new File(configFile.getAbsolutePath() + "." + System.currentTimeMillis() + ".bkp.")
                        )
                ) {
                    Files.writeString(configFile.toPath(), GSON.toJson(defaultConfig()));
                    initialize();
                }
            }
        }
    }

    public static @Nullable MegaSkinsConfiguration getConfig() {
        if (config == null) {
            try {
                initialize();
            } catch (Exception e) {
                log.error("Failed loading the config", e);
            }
        }
        return config;
    }

    public static @NonNull MegaSkinsConfiguration defaultConfig() {
        return MegaSkinsConfiguration.defaultConfig();
    }

    public static class MegaSkinsConfiguration {
        public final int port;
        public final String address;

        public MegaSkinsConfiguration(int port, String address) {
            this.port = port;
            this.address = address;
        }

        // default config
        public static MegaSkinsConfiguration defaultConfig() {
            return new MegaSkinsConfiguration(8080, "0.0.0.0");
        }

        public void check() throws ConfigException {
            if (port <= 0 || port > 65535) {
                throw new ConfigException("Invalid port number");
            }
            if (address == null || address.isBlank()) {
                throw new ConfigException("Invalid address");
            }
        }
    }
}
