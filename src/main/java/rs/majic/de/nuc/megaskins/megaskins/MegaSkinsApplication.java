package rs.majic.de.nuc.megaskins.megaskins;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import rs.majic.de.nuc.megaskins.megaskins.config.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

@Slf4j
@SpringBootApplication()
public class MegaSkinsApplication {

    /**
     * main method
     * @param args arguments
     * @throws IOException I don't like warnings
     */
    public static void main(String[] args) throws IOException {
        log.info("Initializing MegaSkins, please wait...");

        Config.MegaSkinsConfiguration config = Config.getConfig();
        if (config == null) {
            log.error("Something went terribly wrong! Shutting down MegaSkins");
            return;
        }
        Constants.skinManager.initializeFilesIfMissing();

        // Start Spring application and keep the context so we can shut it down on console command.
        SpringApplication app = new SpringApplication(MegaSkinsApplication.class);
        Map<String, Object> properties = Map.of(
                "server.port", config.port,
                "server.address", config.address
        );
        app.setDefaultProperties(
                properties
        );
        // spring.application.name=MegaSkins
        //server.address=0.0.0.0
        ConfigurableApplicationContext ctx = app.run(args);
        Thread shutdownListener = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String cmd = line.trim().toLowerCase();
                    if ("stop".equals(cmd) || "shutdown".equals(cmd)) {
                        log.info("Shutdown command received. Stopping application...");
                        int exitCode = SpringApplication.exit(ctx, () -> 0);
                        System.exit(exitCode);
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("Console shutdown listener stopped", e);
            }
        }, "shutdown-listener");
        shutdownListener.setDaemon(true);
        shutdownListener.start();
    }
}
