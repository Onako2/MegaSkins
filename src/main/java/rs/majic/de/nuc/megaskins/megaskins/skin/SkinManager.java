package rs.majic.de.nuc.megaskins.megaskins.skin;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import rs.majic.de.nuc.megaskins.megaskins.Constants;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SkinManager {
    private static final HttpClient client = HttpClient.newBuilder().build();
    // hash -> image
    public final Map<String, SimpleImage> bannedImages = new ConcurrentHashMap<>();
    @Getter
    private final File skinsImageFolder;
    @Getter
    private final File skinsDescriptionFolder;
    @Getter
    private String[] forbiddenTags = new String[]{"hitler", "naked", "nsfw"}; // default values;
    // hash -> description
    public static Map<String, String> skinData;

    public SkinManager(File skinsImageFolder, File skinsDescriptionFolder) {
        this.skinsImageFolder = skinsImageFolder;
        this.skinsDescriptionFolder = skinsDescriptionFolder;
        File filterList = new File("banned_words.txt");
        File templateSkin = new File("template.png");
        try {
            if (!filterList.exists()) {
                filterList.createNewFile();
                Files.writeString(filterList.toPath(), "hitler\nnaked\nnsfw");
            }
            this.forbiddenTags = Arrays.stream(Files.readString(filterList.toPath()).split("\n")).filter(string -> !string.isBlank()).toArray(String[]::new);
        } catch (Exception ignored) {
        }
        if (!templateSkin.exists()) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create("https://raw.githubusercontent.com/Onako2/MegaSkins/refs/heads/master/template.png"))
                        .header("User-Agent", "MegaSkins/1.0 (+https://github.com/Onako2/MegaSkins/")
                        .timeout(Duration.ofSeconds(60))
                        .build();
                HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());

                int code = resp.statusCode();
                if (code == 200) {
                    byte[] body = resp.body();
                    if (body == null || body.length == 0) {
                        log.error("Empty body for template skin, sus...");
                    } else {
                        Files.write(templateSkin.toPath(), body);
                        System.out.println("Saved: " + templateSkin.getName());
                    }
                } else if (code == 404) {
                    log.error("Not found (404): template skin");
                } else if (code == 429) {
                    log.error("Rate limited (429): template skin");
                } else {
                    log.error("Unexpected HTTP {} for template skin", code);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted", ie);
            } catch (IOException | RuntimeException e) {
                log.error("Request failed for template skin", e);
            }
        }
    }

    public static boolean isValidHash(String hash) {
        return hash != null && hash.matches("^[0-9a-f]{62,64}$");
    }

    public void initializeFilesIfMissing() throws IOException {
        if (!skinsImageFolder.isDirectory()) {
            skinsImageFolder.delete();
            skinsImageFolder.mkdir();
            downloadSkin("100a1dbd83d86095f28d9179efa704be575dcce9e59cbf2f42db4d614a71e2d7", skinsImageFolder.toPath().resolve("100a1dbd83d86095f28d9179efa704be575dcce9e59cbf2f42db4d614a71e2d7.png").toFile());
        }
        if (!skinsDescriptionFolder.isDirectory()) {
            skinsDescriptionFolder.delete();
            skinsDescriptionFolder.mkdir();
            Files.writeString(skinsDescriptionFolder.toPath().resolve("100a1dbd83d86095f28d9179efa704be575dcce9e59cbf2f42db4d614a71e2d7.txt"), """
                    A Minecraft skin featuring a character with sleek black hair partially covering the face, a single bright blue eye, and cyan-blue headphones. The character is dressed in a futuristic, high-contrast outfit consisting of cyan, light blue, and white geometric patterns across the torso, arms, and legs.
                    
                    {tags: ["cyan", "blue", "white", "black hair", "headphones", "futuristic", "gamer", "minecraft skin", "sci-fi"], portrays: null}""");
        }
    }

    public boolean downloadSkin(String hash, File output) {
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://textures.minecraft.net/texture/" + hash))
                .header("User-Agent", "MegaSkins/1.0 (+https://github.com/Onako2/MegaSkins/")
                .timeout(Duration.ofSeconds(60))
                .build();

        try {
            HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());

            int code = resp.statusCode();
            if (code == 200) {
                byte[] body = resp.body();
                if (body == null || body.length == 0) {
                    log.error("Empty body for {}", hash);
                } else {
                    Files.write(output.toPath(), body);
                    SkinConverter.convertFile(output, output); // 64x32 -> 64x64
                    log.info("Saved: {}", output.getName());
                    return true;
                }
            } else if (code == 404) {
                log.error("Not found (404): {}", hash);
            } else if (code == 429) {
                log.error("Rate limited (429).");
            } else {
                log.error("Unexpected HTTP {} for {}", code, hash);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", ie);
        } catch (IOException | RuntimeException e) {
            log.error("Request failed for {}", hash, e);
        }
        return false;
    }

    public SkinPreviewInformation getSkinPreviewInformation(String hash) {
        if (isUnsafeHash(hash)) return new SkinPreviewInformation(hash, true);
        try {
            String description = getDescription(hash);
            if (description == null || description.isBlank()) {
                Path base = Constants.skinManager.getSkinsImageFolder().toPath().toAbsolutePath().normalize();
                Path target = base.resolve(hash + ".png").toAbsolutePath().normalize();
                if (!target.startsWith(base)) {
                    return null;
                }
                if (!target.toFile().exists()) {
                    Constants.skinManager.downloadSkin(hash, target.toFile());
                }
                return null;
            }
            return new SkinPreviewInformation(hash, isUnsafe(description));
        } catch (Exception e) {
            log.error("Failed getting skin info for {}", hash, e);
            return null;
        }
    }

    public boolean isUnsafe(String description) {
        for (String tag : forbiddenTags) {
            if (description.toLowerCase().contains(tag)) {
                return true;
            }
        }
        return false;
    }

    public boolean isUnsafeHash(String hash) {
        return bannedImages.containsKey(hash);
    }

    public String getDescription(String hash) {
        try {
            String description = skinData.get(hash);
            if (description == null || description.isBlank()) {
                return null;
            }
            return description;
        } catch (Exception e) {
            log.error("Failed getting description for {}", hash, e);
            return null;
        }
    }

    public record SkinPreviewInformation(String hash, boolean unsafe) {
    }
}
