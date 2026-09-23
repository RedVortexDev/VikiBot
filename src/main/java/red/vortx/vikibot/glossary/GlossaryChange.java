package red.vortx.vikibot.glossary;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class GlossaryChange {

    private static final Pattern PAGE_TITLE = Pattern.compile("(?<![\\p{L}\\p{N}])קהילה:מילון(?![\\p{L}\\p{N}_/])");

    private GlossaryChange() {
    }

    public static boolean isGlossaryChange(String text) {
        try {
            return PAGE_TITLE.matcher(URLDecoder.decode(text, StandardCharsets.UTF_8)).find();
        } catch (IllegalArgumentException exception) {
            return PAGE_TITLE.matcher(text).find();
        }
    }

}
