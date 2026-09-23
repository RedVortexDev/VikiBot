package red.vortx.vikibot.glossary;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;
import java.time.Instant;

public final class GlossaryMessage {

    public static final String TITLE = "מילון מונחים";
    public static final String PAGE_URL = "https://he.minecraft.wiki/w/קהילה:מילון";

    private GlossaryMessage() {
        throw new UnsupportedOperationException();
    }

    public static MessageEmbed embed(Glossary glossary) {
        StringBuilder description = new StringBuilder();
        for (Term term : glossary.terms()) {
            description.append(formatGlossaryTerm(term)).append('\n');
        }

        return new EmbedBuilder()
                .setColor(Color.GREEN)
                .setTitle(TITLE, PAGE_URL)
                .setDescription(description.toString())
                .setFooter("עודכן לאחרונה")
                .setTimestamp(Instant.now())
                .build();
    }

    public static MessageEmbed term(Term term) {
        return new EmbedBuilder()
                .setColor(Color.GREEN)
                .setTitle(term.english(), PAGE_URL)
                .setDescription(formatDefinition(term))
                .build();
    }

    public static boolean isGlossary(MessageEmbed embed) {
        return TITLE.equals(embed.getTitle()) && PAGE_URL.equals(embed.getUrl());
    }

    private static String formatGlossaryTerm(Term term) {
        StringBuilder definition = new StringBuilder()
                .append("**").append(term.english()).append("** - ").append(term.hebrew());
        if (!term.note().isBlank()) {
            definition.append("\n> ").append(term.note());
        }
        return definition.toString();
    }

    private static String formatDefinition(Term term) {
        StringBuilder definition = new StringBuilder("__").append(term.hebrew()).append("__");
        if (!term.note().isBlank()) {
            definition.append("\n(").append(term.note()).append(")");
        }
        return definition.toString();
    }

}
