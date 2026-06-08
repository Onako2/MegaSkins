package rs.majic.de.nuc.megaskins.megaskins;

import lombok.Getter;
import rs.majic.de.nuc.megaskins.megaskins.skin.SkinManager;

public class Statistics {
    private final SkinManager skinManager;
    @Getter
    private int requestsThisSession;
    public Statistics(SkinManager skinManager) {
        this.skinManager = skinManager;
        requestsThisSession = 0;
    }

    public void newRequest() {
        requestsThisSession++;
    }

    public Numbers getStats(int descriptionCount) {
        return new Numbers(descriptionCount, requestsThisSession);
    }

    public record Numbers(int descriptionCount, int requestsThisSession) {}
}
