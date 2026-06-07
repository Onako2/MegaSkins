package rs.majic.de.nuc.megaskins.megaskins;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@SpringBootApplication(exclude={DataSourceAutoConfiguration.class})
public class MegaSkinsApplication {

    public static void main(String[] args) throws IOException {
        Constants.skinManager.initializeFilesIfMissing();

        // Start Spring application and keep the context so we can shut it down on console command.
        ConfigurableApplicationContext ctx = SpringApplication.run(MegaSkinsApplication.class, args);
        Thread shutdownListener = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String cmd = line.trim().toLowerCase();
                    if ("stop".equals(cmd) || "shutdown".equals(cmd)) {
                        System.out.println("Shutdown command received. Stopping application...");
                        int exitCode = org.springframework.boot.SpringApplication.exit(ctx, () -> 0);
                        System.exit(exitCode);
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("Console shutdown listener stopped: " + e.getMessage());
            }
        }, "shutdown-listener");
        shutdownListener.setDaemon(true);
        shutdownListener.start();
    }

}
