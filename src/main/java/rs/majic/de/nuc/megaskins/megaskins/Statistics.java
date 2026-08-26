package rs.majic.de.nuc.megaskins.megaskins;

import lombok.Getter;
import rs.majic.de.nuc.megaskins.megaskins.skin.SkinManager;

/**
 * I love stats
 */
public class Statistics {
    private final SkinManager skinManager;
    @Getter
    private int requestsThisSession;
    public Statistics(SkinManager skinManager) {
        this.skinManager = skinManager;
        requestsThisSession = 0;
    }

    /**
     * add counter
     */
    public void newRequest() {
        requestsThisSession++;
    }

    /**
     * Get the current stats for MegaSkins
     * @param descriptionCount current descriptions inside the folder
     * @return current stats
     */
    public Numbers getStats(int descriptionCount) {
        return new Numbers(descriptionCount, skinManager.bannedImages.size(), requestsThisSession);
    }

    public record Numbers(int descriptionCount, int bannedCount, int requestsThisSession) {}
}
