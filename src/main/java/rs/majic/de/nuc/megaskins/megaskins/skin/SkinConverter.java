package rs.majic.de.nuc.megaskins.megaskins.skin;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Slf4j
// sowwy LLM generated
public final class SkinConverter {

    private SkinConverter() {
        // Utility class
    }

    /**
     * Detects whether a skin is legacy (64x32).
     */
    public static boolean isLegacySkin(BufferedImage img) {
        return img != null && img.getWidth() == 64 && img.getHeight() == 32;
    }

    /**
     * Detects whether a skin is modern (64x64).
     */
    public static boolean isModernSkin(BufferedImage img) {
        return img != null && img.getWidth() == 64 && img.getHeight() == 64;
    }

    /**
     * Loads an image from file.
     */
    public static BufferedImage load(File file) throws IOException {
        return ImageIO.read(file);
    }

    /**
     * Saves a skin to file (PNG).
     */
    public static void save(BufferedImage img, File file) throws IOException {
        ImageIO.write(img, "png", file);
    }

    /**
     * Converts a legacy 64x32 skin into a modern 64x64 skin.
     *
     * If the skin is already 64x64, it is returned unchanged.
     */
    public static BufferedImage convertToModern(BufferedImage input) {
        if (input == null) {
            throw new IllegalArgumentException("Input image cannot be null");
        }

        if (isModernSkin(input)) {
            return null;
        }

        if (!isLegacySkin(input)) {
            throw new IllegalArgumentException(
                    "Unsupported skin format: " + input.getWidth() + "x" + input.getHeight()
            );
        }

        BufferedImage output = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = output.createGraphics();
        g.drawImage(input, 0, 0, null);
        BufferedImage rl = input.getSubimage(0, 16, 16, 16);
        g.drawImage(rl, 16, 48, null);
        BufferedImage ra = input.getSubimage(40, 16, 16, 16);
        g.drawImage(ra, 32, 48, null);

        g.dispose();

        return output;
    }

    /**
     * Convenience method: load -> convert -> save.
     */
    public static boolean convertFile(File inputFile, File outputFile) throws IOException {
        try {
            BufferedImage img = load(inputFile);
            BufferedImage converted = convertToModern(img);
            if (converted != null) {
                log.info("Converted image {} from legacy to modern format", outputFile.getName());
                save(converted, outputFile);
            }
            return true;
        } catch (Exception e) {
            log.error("Failed converting image {} to modern format", inputFile, e);
            return false;
        }
    }
}