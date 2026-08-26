package rs.majic.de.nuc.megaskins.megaskins.endpoint;

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
import rs.majic.de.nuc.megaskins.megaskins.skin.SimpleImage;
import rs.majic.de.nuc.megaskins.megaskins.skin.SkinManager;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static rs.majic.de.nuc.megaskins.megaskins.skin.SkinManager.isValidHash;
import static rs.majic.de.nuc.megaskins.megaskins.skin.SkinManager.skinData;

/**
 * Main API method
 */
@Slf4j
@Controller
public class Api {

    public static final BufferedImage output = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
    static final Random random = new Random();
    static final int MAX_RESULTS = 10;

    static {
        try {
            initialize();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Initializes everything, takes ages
     */
    static void initialize() throws IOException {
        skinData = new ConcurrentHashMap<>();
        File[] files = Constants.skinManager.getSkinsDescriptionFolder().listFiles(f -> f.getName().endsWith(".txt"));
        log.info("Processing skins...");
        for (File file : files) {
            String content = Files.readString(file.toPath());
            String hash = file.getName().replace(".txt", "");
            skinData.put(hash, content);
            if (Constants.skinManager.isUnsafe(content)) {
                BufferedImage image = ImageIO.read(Constants.skinManager.getSkinsImageFolder()
                        .toPath()
                        .resolve(hash + ".png").toFile());
                Constants.skinManager.bannedImages.put(hash, SimpleImage.fromBufferedImage(image));
            }
        }
        log.info("Filtering skins..");
        Map<String, SimpleImage> futureBan = new ConcurrentHashMap<>();
        files = Constants.skinManager.getSkinsDescriptionFolder().listFiles(f -> f.getName().endsWith(".txt"));
        for (File file : files) {
            String hash = file.getName().replace(".txt", "").replace(".png", "");
            if (Constants.skinManager.isUnsafeHash(hash)) {
                continue;
            }
            try {
                BufferedImage bufferedImage = ImageIO.read(Constants.skinManager.getSkinsImageFolder()
                        .toPath()
                        .resolve(hash + ".png").toFile());
                SimpleImage image = SimpleImage.fromBufferedImage(bufferedImage);
                if (SimpleImage.compare(image, image) < 0.0f) {
                    continue;
                }
                AtomicReference<Float> maxSimilarity = new AtomicReference<>((float) 0);
                Constants.skinManager.bannedImages.forEach((hashed, banned) -> {
                    if (maxSimilarity.get() < 0.95) {
                        if (SimpleImage.compare(banned, banned) >= 0.0f) {
                            float similarity = SimpleImage.compare(image, banned);
                            if (similarity > maxSimilarity.get()) {
                                if (similarity >= 0.95) {
                                    log.info("{} matched with {} {}", hash, hashed, similarity);
                                }
                                maxSimilarity.set(similarity);
                            }
                        }
                    }
                });
                if (maxSimilarity.get() >= 0.95) {
                    futureBan.put(hash, image);
                    log.info("Ban: {} Percentage: {}", hash, maxSimilarity.get());
                    log.info("Banned size: {}", Constants.skinManager.bannedImages.size() + futureBan.size());
                    File bannedDirectory = new File("banned");
                    if (!bannedDirectory.isDirectory()) {
                        Files.createDirectory(bannedDirectory.toPath());
                    }
                    try {
                        Files.copy(Path.of("skins/", hash + ".png"), bannedDirectory.toPath().resolve(hash + ".png"));
                    } catch (FileAlreadyExistsException ignore) {
                    } catch (Exception e) {
                        log.error("Error while copying banned skin image", e);
                    }
                }
            } catch (Exception e) {
                log.error("Error reading skin image {}", hash, e);
            }
        }
        log.info("Processed skins!");
        Constants.skinManager.bannedImages.putAll(futureBan);
    }

    /**
     * Filter for English characters
     * @param s input String
     * @return cleaned String
     */
    private static String filterEnglish(String s) {
        StringBuilder result = new StringBuilder(s.length());

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'a' && c <= 'z') {
                result.append(c);
            }
        }

        return result.toString();
    }

    @GetMapping(value = "/api/skin/image")
    public @ResponseBody ResponseEntity<byte[]> image(@RequestParam(name = "hash") String hash) throws IOException {
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

    @GetMapping(value = "/api/skin/head")
    public @ResponseBody ResponseEntity<byte[]> head(@RequestParam(name = "hash") String hash, @RequestParam(name = "scale", defaultValue = "1.0f") float scale) throws IOException {
        Constants.statistics.newRequest();
        if (scale < 1.0f || scale > 64.0f) {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "text/plain");
            return new ResponseEntity<>("Scaling must be between 1.0 and 64.0".getBytes(StandardCharsets.UTF_8), headers, HttpStatus.BAD_REQUEST);
        }
        if (!isValidHash(hash)) {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "image/png");
            return new ResponseEntity<>(headers, HttpStatus.BAD_REQUEST);
        }
        Path base = Constants.skinManager.getSkinsImageFolder().toPath().toAbsolutePath().normalize();
        Path target = base.resolve(hash + ".png").toAbsolutePath().normalize();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "image/png");
        SkinManager.SkinPreviewInformation info = Constants.skinManager.getSkinPreviewInformation(hash);
        if (!target.startsWith(base) || !Files.exists(target)) {
            return new ResponseEntity<>(headers, HttpStatus.FORBIDDEN);
        }
        if (info != null && info.unsafe()) {
            return new ResponseEntity<>(headers, HttpStatus.FORBIDDEN);
        }

        BufferedImage image = ImageIO.read(target.toFile());
        BufferedImage under = image.getSubimage(8, 8, 8, 8);
        BufferedImage upper = image.getSubimage(40, 8, 8, 8);

        Graphics g = output.getGraphics();
        try {
            g.setColor(Color.BLACK);
            g.drawRect(0, 0, 8, 8);
            g.drawImage(under, 0, 0, null);
            g.drawImage(upper, 0, 0, null);
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // why do upscaling stuff if it isn't needed :)
        if (((int) scale * 8) == 8) {
            ImageIO.write(output, "png", baos);
            return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
        }

        int a = (int) (8 * scale);
        BufferedImage scaleImg = new BufferedImage(a, a, BufferedImage.TYPE_INT_ARGB);
        AffineTransform scalingTransform = new AffineTransform();
        scalingTransform.scale(scale, scale);
        AffineTransformOp scaleOp = new AffineTransformOp(scalingTransform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        BufferedImage img = scaleOp.filter(output, scaleImg);

        ImageIO.write(img, "png", baos);
        return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
    }

    @GetMapping(value = "/api/skin/image/random")
    public @ResponseBody ResponseEntity<byte[]> random() throws IOException {
        Constants.statistics.newRequest();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "image/png");
        String chosenHash = skinData.keySet().toArray(String[]::new)[random.nextInt(skinData.size())];
        if (Constants.skinManager.isUnsafe(skinData.get(chosenHash)) || Constants.skinManager.isUnsafeHash(chosenHash)) {
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
    public @ResponseBody ResponseEntity<SkinManager.SkinPreviewInformation> skin(@RequestParam(name = "hash") String hash) {
        Constants.statistics.newRequest();
        if (!isValidHash(hash)) {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "image/png");
            return new ResponseEntity<>(headers, HttpStatus.BAD_REQUEST);
        }
        return aSkinPreviewInfo(hash);
    }

    @GetMapping(value = "/api/skin/random")
    public @ResponseBody ResponseEntity<SkinManager.SkinPreviewInformation> skinRandom() {
        Constants.statistics.newRequest();
        String chosenHash = skinData.keySet().toArray(String[]::new)[random.nextInt(skinData.size())];
        if (Constants.skinManager.isUnsafe(skinData.get(chosenHash)) || Constants.skinManager.isUnsafeHash(chosenHash)) {
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
    public @ResponseBody ResponseEntity<String> description(@RequestParam(name = "hash") String hash) {
        Constants.statistics.newRequest();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "text/plain");
        if (!isValidHash(hash)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST.toString(), headers, HttpStatus.BAD_REQUEST);
        }
        String description = Constants.skinManager.getDescription(hash);
        if (description == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND.toString(), headers, HttpStatus.NOT_FOUND);
        if (Constants.skinManager.isUnsafe(description) || Constants.skinManager.isUnsafeHash(hash))
            return new ResponseEntity<>(HttpStatus.FORBIDDEN.toString(), headers, HttpStatus.FORBIDDEN);
        String[] lines = description.split("\n");
        return new ResponseEntity<>(lines.length > 0 ? lines[0] : "", headers, HttpStatus.OK);
    }

    @GetMapping(value = "/api/skin/search")
    public @ResponseBody ResponseEntity<String[]> search(@RequestParam(name = "query") String query) {
        Constants.statistics.newRequest();
        log.info("Searching for: {}", query);
        long time = System.currentTimeMillis();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        // very basic search haha
        if (Constants.skinManager.isUnsafe(query)) {
            return new ResponseEntity<>(new String[0], headers, HttpStatus.FORBIDDEN);
        }
        List<String> results = new ArrayList<>();
        String queryLower = query.replace("%20", " ").replace("+", " ").toLowerCase(Locale.ENGLISH);
        String[] tokens = Arrays.stream(queryLower.split("[^\\p{L}0-9']+")).distinct().limit(10).toArray(String[]::new);
        StringBuilder querySplittedBuilder = new StringBuilder();
        for (String token : tokens) {
            querySplittedBuilder.append(token);
        }
        final String querySplitted = querySplittedBuilder.toString();
        Map<String, Integer> resultsMap = new HashMap<>(); // hash -> score
        skinData.forEach((hash, description) -> {
            if (Constants.skinManager.isUnsafe(description) || Constants.skinManager.isUnsafeHash(hash)) {
                return;
            }
            String descriptionLower = description.toLowerCase(Locale.ENGLISH);
            int score = 0;
            int coveredTokens = 0;
            for (String token : tokens) {
                int tokenScore = StringUtils.countOccurrencesOf(descriptionLower, token);
                if (tokenScore > 0) {
                    coveredTokens++;
                }
                if (tokenScore > 3) {
                    tokenScore = 3; // limit it to 3 :)
                }
                score += tokenScore;
            }
            score *= coveredTokens;
            if (filterEnglish(descriptionLower.replace(',', ' ').replace('"', ' ').replace(" ", "")).contains(querySplitted)) {
                score *= tokens.length;
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
        log.info("Time for search: {} ms.", System.currentTimeMillis() - time);
        return new ResponseEntity<>(results.stream().distinct().limit(MAX_RESULTS).toList().toArray(new String[0]), headers, HttpStatus.OK);
    }

    @GetMapping(value = "/api/skin/safety")
    public @ResponseBody ResponseEntity<Float> safety(@RequestParam(name = "hash") String hash) throws IOException {
        Constants.statistics.newRequest();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        if (!isValidHash(hash)) {
            return new ResponseEntity<>(-1.0f, headers, HttpStatus.BAD_REQUEST);
        }
        if (Constants.skinManager.isUnsafeHash(hash)) {
            return new ResponseEntity<>(1.0f, headers, HttpStatus.OK);
        }
        Path base = Constants.skinManager.getSkinsImageFolder().toPath().toAbsolutePath().normalize();
        Path target = base.resolve(hash + ".png").toAbsolutePath().normalize();
        if (!target.startsWith(base)) {
            return new ResponseEntity<>(headers, HttpStatus.FORBIDDEN);
        }
        if (!target.toFile().exists()) {
            if (!Constants.skinManager.downloadSkin(hash, target.toFile())) {
                return new ResponseEntity<>(headers, HttpStatus.NOT_FOUND);
            }
        }
        BufferedImage bufferedImage = ImageIO.read(target.toFile());
        SimpleImage image = SimpleImage.fromBufferedImage(bufferedImage);
        float highestSimilarity = 0;
        for (int i = 0; i < Constants.skinManager.bannedImages.size(); i++) {
            SimpleImage banned = (SimpleImage) Constants.skinManager.bannedImages.values().toArray()[i];
            float similarity = SimpleImage.compare(banned, image);
            if (similarity > highestSimilarity) {
                highestSimilarity = similarity;
            }
        }
        return new ResponseEntity<>(highestSimilarity, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/api/stats")
    public @ResponseBody ResponseEntity<Statistics.Numbers> stats() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        return new ResponseEntity<>(Constants.statistics.getStats(skinData.size()), headers, HttpStatus.OK);
    }
}
