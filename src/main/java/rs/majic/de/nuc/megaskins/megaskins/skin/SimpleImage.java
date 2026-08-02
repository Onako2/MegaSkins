package rs.majic.de.nuc.megaskins.megaskins.skin;

import lombok.NonNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SimpleImage {

    public final SimplePixel[] pixels;
    private static Pos2d[] allowedPixels = new Pos2d[0];

    private SimpleImage(SimplePixel[] pixels) {
        this.pixels = pixels;
    }

    public static float compare(@NonNull SimpleImage a, @NonNull SimpleImage b) {
        // percentage value based on similarity between 0 and 1
        if (a.pixels.length != b.pixels.length) {
            return -1;
        }
        float similarity = 0;
        int comparedEntries = 0;
        int completelyDarkOrCompletelyWhite = 0;
        for (int i = 0; i < a.pixels.length; i++) {
            if (a.pixels[i].hasAlpha || b.pixels[i].hasAlpha) {
                continue;
            }
            if (a.pixels[i].isPureBlackOrWhite() || b.pixels[i].isPureBlackOrWhite()) {
                completelyDarkOrCompletelyWhite++;
            }
            similarity += SimplePixel.compare(a.pixels[i], b.pixels[i]);
            comparedEntries++;
        }
        if (comparedEntries < 500) {
            return -0.5f;
        }
        if (completelyDarkOrCompletelyWhite > comparedEntries / 1.5) {
            return -0.3f;
        }
        similarity /= comparedEntries;
        return similarity;
    }

    public static SimpleImage fromBufferedImage(@NonNull BufferedImage image) throws IOException {
        if (allowedPixels.length == 0) {
            // initialise
            BufferedImage template = ImageIO.read(new File("template.png"));
            // list all pixels that aren't transparent
            List<Pos2d> allowed = new ArrayList<>();
            for (int x = 0; x < template.getWidth(); x++) {
                for (int y = 0; y < template.getHeight(); y++) {
                    if (new Color(template.getRGB(x, y), true).getRed() != 255) {
                        allowed.add(new Pos2d(x, y));
                    }
                }
            }
            allowedPixels = allowed.toArray(new Pos2d[0]);
        }
        List<SimplePixel> simplePixelList = new ArrayList<>(image.getWidth() * image.getHeight());
        for (Pos2d pos : allowedPixels) {
            int pixel = image.getRGB(pos.x(), pos.y());
            Color color = new Color(pixel, true);
            simplePixelList.add(new SimplePixel(
                    (byte) color.getRed(),
                    (byte) color.getGreen(),
                    (byte) color.getBlue(),
                    color.getAlpha() < 10
            ));
        }
        return new SimpleImage(simplePixelList.toArray(new SimplePixel[0]));
    }

    public static class SimplePixel {
        public final byte r;
        public final byte g;
        public final byte b;
        public final boolean hasAlpha;

        public SimplePixel(byte r, byte g, byte b, boolean hasAlpha) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.hasAlpha = hasAlpha;
        }

        public static float compare(SimplePixel a, SimplePixel b) {
            int r1 = Byte.toUnsignedInt(a.r);
            int g1 = Byte.toUnsignedInt(a.g);
            int b1 = Byte.toUnsignedInt(a.b);

            int r2 = Byte.toUnsignedInt(b.r);
            int g2 = Byte.toUnsignedInt(b.g);
            int b2 = Byte.toUnsignedInt(b.b);

            int diffR = Math.abs(r1 - r2);
            int diffG = Math.abs(g1 - g2);
            int diffB = Math.abs(b1 - b2);

            int totalDifference = diffR + diffG + diffB;

            return 1.0f - (float) totalDifference / 765.0f;
        }

        public boolean isPureBlackOrWhite() {
            int r = Byte.toUnsignedInt(this.r);
            int g = Byte.toUnsignedInt(this.g);
            int b = Byte.toUnsignedInt(this.b);

            return (r == 0 && g == 0 && b == 0)
                    || (r == 255 && g == 255 && b == 255);
        }
    }

    public record Pos2d(int x, int y) {}
}
