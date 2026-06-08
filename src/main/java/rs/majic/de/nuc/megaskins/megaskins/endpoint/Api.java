package rs.majic.de.nuc.megaskins.megaskins.endpoint;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import rs.majic.de.nuc.megaskins.megaskins.Constants;
import rs.majic.de.nuc.megaskins.megaskins.skin.SkinManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

@Slf4j
@Controller
public class Api {

    static File[] files = Constants.skinManager.getSkinsDescriptionFolder().listFiles(f -> f.getName().endsWith(".txt"));
    static Random random = new Random();

    @GetMapping(value="/api/skin/image")
    public @ResponseBody ResponseEntity<byte[]> image(@RequestParam(name="hash", required=true) String hash) throws IOException {
        if (!isValidHash(hash)) {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "image/png");
            return new ResponseEntity<>(headers, HttpStatus.BAD_REQUEST);
        }
        Path base = Constants.skinManager.getSkinsImageFolder().toPath().toAbsolutePath().normalize();
        Path target = base.resolve(hash + ".png").toAbsolutePath().normalize();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "image/png");
        if (!target.startsWith(base) || !Files.exists(target)) {
            return new ResponseEntity<>(headers, HttpStatus.FORBIDDEN);
        }
        SkinManager.SkinPreviewInformation info = Constants.skinManager.getSkinPreviewInformation(hash);
        if (info != null && info.unsafe()) {
            return new ResponseEntity<>(headers, HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<>(Files.readAllBytes(target), headers, HttpStatus.OK);
    }

    @GetMapping(value = "/api/skin/image/random")
    public @ResponseBody ResponseEntity<byte[]> random() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "image/png");
        if (files == null || files.length == 0) {
            return new ResponseEntity<>(headers, HttpStatus.NOT_FOUND);
        }
        File chosen = files[random.nextInt(files.length)];
        if (Constants.skinManager.isUnsafe(Files.readString(chosen.toPath()))) {
            return random();
        }
        String chosenHash = chosen.getName().replaceFirst("\\.txt$", "");
        ResponseEntity<byte[]> response = image(chosenHash);
        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("Error random image " + chosenHash);
            return new ResponseEntity<>(headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @GetMapping(value = "/api/skin")
    public @ResponseBody ResponseEntity<SkinManager.SkinPreviewInformation> skin(@RequestParam(name="hash", required=true) String hash) throws IOException {
        return aSkinPreviewInfo(hash);
    }

    @GetMapping(value = "/api/skin/random")
    public @ResponseBody ResponseEntity<SkinManager.SkinPreviewInformation> skinRandom() throws IOException {
        if (files == null || files.length == 0) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        File chosen = files[random.nextInt(files.length)];
        String description = Files.readString(chosen.toPath());
        if (Constants.skinManager.isUnsafe(description)) {
            return skinRandom();
        }
        String chosenHash = chosen.getName().replaceFirst("\\.txt$", "");
        return aSkinPreviewInfo(chosenHash);
    }

    private ResponseEntity<SkinManager.SkinPreviewInformation> aSkinPreviewInfo(String hash) {
        SkinManager.SkinPreviewInformation info = Constants.skinManager.getSkinPreviewInformation(hash);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        if (info == null) return new ResponseEntity<>(headers, HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(info, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/api/skin/description")
    public @ResponseBody ResponseEntity<String> description(@RequestParam(name="hash", required = true) String hash, HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        // prevent proxy requests for now :)
        if (realIp != null) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        if (!isValidHash(hash)) {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "text/plain");
            return new ResponseEntity<>("BAD_REQUEST", headers, HttpStatus.BAD_REQUEST);
        }
        String description = Constants.skinManager.getDescription(hash);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "text/plain");
        if (description == null) return new ResponseEntity<>("NOT_FOUND", headers, HttpStatus.NOT_FOUND);
        if (Constants.skinManager.isUnsafe(description)) return new ResponseEntity<>(headers, HttpStatus.FORBIDDEN);
        String[] lines = description.split("\n");
        return new ResponseEntity<>(lines.length > 0 ? lines[0] : "", headers, HttpStatus.OK);
    }

    private boolean isValidHash(String hash) {
        if (hash == null) return false;
        return hash.matches("^[0-9a-zA-Z_-]{8,128}$");
    }
}
