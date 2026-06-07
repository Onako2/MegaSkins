package rs.majic.de.nuc.megaskins.megaskins;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

import java.io.IOException;

@SpringBootApplication(exclude={DataSourceAutoConfiguration.class})
public class MegaSkinsApplication {

    public static void main(String[] args) throws IOException {
        Constants.skinManager.initializeFilesIfMissing();
        SpringApplication.run(MegaSkinsApplication.class, args);
    }

}
