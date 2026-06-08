package rs.majic.de.nuc.megaskins.megaskins.endpoint;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import rs.majic.de.nuc.megaskins.megaskins.Constants;
import rs.majic.de.nuc.megaskins.megaskins.Statistics;
import rs.majic.de.nuc.megaskins.megaskins.skin.SkinManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Controller
public class Api {

    static void initialize() throws IOException {
        skinData = new ConcurrentHashMap<>();
        File[] files = Constants.skinManager.getSkinsDescriptionFolder().listFiles(f -> f.getName().endsWith(".txt"));
        for (File file : files) {
            skinData.put(file.getName().replace(".txt", ""), Files.readString(file.toPath()));
        }
    }

    static {
        try {
            initialize();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // hash -> description
    static Map<String, String> skinData;
    static final Random random = new Random();
    static final int MAX_RESULTS = 10;

    @GetMapping(value="/api/skin/image")
    public @ResponseBody ResponseEntity<byte[]> image(@RequestParam(name="hash") String hash) throws IOException {
        Constants.statistics.newRequest();
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
        Constants.statistics.newRequest();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "image/png");
        String chosenHash = skinData.keySet().toArray(String[]::new)[random.nextInt(skinData.size())];
        if (Constants.skinManager.isUnsafe(skinData.get(chosenHash))) {
            return random();
        }
        ResponseEntity<byte[]> response = image(chosenHash);
        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("Error random image {}", chosenHash);
            return new ResponseEntity<>(headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @GetMapping(value = "/api/skin")
    public @ResponseBody ResponseEntity<SkinManager.SkinPreviewInformation> skin(@RequestParam(name="hash") String hash) {
        Constants.statistics.newRequest();
        return aSkinPreviewInfo(hash);
    }

    @GetMapping(value = "/api/skin/random")
    public @ResponseBody ResponseEntity<SkinManager.SkinPreviewInformation> skinRandom() {
        Constants.statistics.newRequest();
        String chosenHash = skinData.keySet().toArray(String[]::new)[random.nextInt(skinData.size())];
        if (Constants.skinManager.isUnsafe(skinData.get(chosenHash))) {
            return skinRandom();
        }
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
    public @ResponseBody ResponseEntity<String> description(@RequestParam(name="hash") String hash, HttpServletRequest request) {
        Constants.statistics.newRequest();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "text/plain");
        if (!isValidHash(hash)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST.toString(), headers, HttpStatus.BAD_REQUEST);
        }
        String description = Constants.skinManager.getDescription(hash);
        if (description == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND.toString(), headers, HttpStatus.NOT_FOUND);
        if (Constants.skinManager.isUnsafe(description)) return new ResponseEntity<>(HttpStatus.FORBIDDEN.toString(), headers, HttpStatus.FORBIDDEN);
        String[] lines = description.split("\n");
        return new ResponseEntity<>(lines.length > 0 ? lines[0] : "", headers, HttpStatus.OK);
    }

    @GetMapping(value="/api/skin/search")
    public @ResponseBody ResponseEntity<String[]> search(@RequestParam(name="query") String query, HttpServletRequest request) {
        Constants.statistics.newRequest();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        // very basic search haha
        if (Constants.skinManager.isUnsafe(query)) {
            return new ResponseEntity<>(new String[0], headers, HttpStatus.FORBIDDEN);
        }
        List<String> results = new ArrayList<>();
        String[] tokens = query.toLowerCase(Locale.ENGLISH).split("[^\\p{L}0-9']+");
        Map<String, Integer> resultsMap = new ConcurrentHashMap<>(); // hash -> score
        skinData.forEach((hash, description) -> {
            if (Constants.skinManager.isUnsafe(description)) {
                return;
            }
            int score = 0;
            for (String token : tokens) {
                int tokenScore = StringUtils.countOccurrencesOf(description.toLowerCase(Locale.ENGLISH), token);
                if (tokenScore > 3) {
                    tokenScore = 3; // limit it to 3 :)
                }
                score += tokenScore;
            }
            if (score > 0) {
                resultsMap.put(hash, score);
            }
        });
        // only top 5
        Integer[] scores = resultsMap.values().stream().sorted(Comparator.reverseOrder()).limit(MAX_RESULTS).toArray(Integer[]::new);
        for (int score : scores) {
            resultsMap.keySet().stream().filter(hash -> {
                int hashScore = resultsMap.get(hash);
                return score == hashScore;
            }).forEach(results::add);
        }
        return new ResponseEntity<>(results.stream().distinct().limit(MAX_RESULTS).toList().toArray(new String[0]), headers, HttpStatus.OK);
    }

    @GetMapping(value="/api/stats")
    public @ResponseBody ResponseEntity<Statistics.Numbers> stats() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        return new ResponseEntity<>(Constants.statistics.getStats(skinData.size()), headers, HttpStatus.OK);
    }

    private boolean isValidHash(String hash) {
        if (hash == null) return false;
        return hash.matches("^[0-9a-zA-Z_-]{8,128}$");
    }
}
