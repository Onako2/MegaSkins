package rs.majic.de.nuc.megaskins.megaskins;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Slf4j
@SpringBootApplication()
public class MegaSkinsApplication {

    public static void main(String[] args) throws IOException {
        log.info("Initializing MegaSkins, please wait...");
        Constants.skinManager.initializeFilesIfMissing();

        // Start Spring application and keep the context so we can shut it down on console command.
        ConfigurableApplicationContext ctx = SpringApplication.run(MegaSkinsApplication.class, args);
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
