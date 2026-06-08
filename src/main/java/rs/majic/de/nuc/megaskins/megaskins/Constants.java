package rs.majic.de.nuc.megaskins.megaskins;

import rs.majic.de.nuc.megaskins.megaskins.skin.SkinManager;

import java.io.File;

public class Constants {
    public static final SkinManager skinManager = new SkinManager(new File("skins/"), new File("skins_description/"), new String[]{
            "naked", "swastika", "hitler", "nude", "cannot fulfill", "nsfw"
    });
    public static final Statistics statistics = new Statistics(skinManager);
}
