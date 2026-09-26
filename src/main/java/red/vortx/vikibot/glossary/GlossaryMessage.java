package red.vortx.vikibot.glossary;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.separator.Separator.Spacing;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class GlossaryMessage {

    public static final String TITLE = "מילון מונחים";
    public static final String PAGE_URL = "https://he.minecraft.wiki/w/קהילה:מילון";

    private GlossaryMessage() {
    }

    public static MessageEmbed term(Term term) {
        return new EmbedBuilder()
                .setColor(Color.GREEN)
                .setTitle(term.english(), PAGE_URL)
                .setDescription(formatDefinition(term))
                .build();
    }

    public static MessageCreateData message(Glossary glossary) {
        List<ContainerChildComponent> content = new ArrayList<>();
        content.add(TextDisplay.of("# [מילון מונחים](" + PAGE_URL + ")"));
        for (GlossarySection section : glossary.sections()) {
            content.add(Separator.createDivider(Spacing.SMALL));
            content.add(TextDisplay.of(formatSection(section)));
        }
        return new MessageCreateBuilder()
                .useComponentsV2(true)
                .addComponents(Container.of(content))
                .build();
    }

    private static String formatSection(GlossarySection section) {
        String heading = section.title().isBlank() ? TITLE : "## " + section.title();
        return heading + "\n" + formatTerms(section.terms());
    }

    private static String formatTerms(List<Term> terms) {
        StringBuilder text = new StringBuilder();
        for (Term term : terms) {
            text.append("**").append(term.english()).append("** - ").append(term.hebrew());
            if (!term.note().isBlank()) {
                text.append("\n> ").append(term.note());
            }
            text.append('\n');
        }
        return text.toString();
    }

    private static String formatDefinition(Term term) {
        StringBuilder definition = new StringBuilder("__").append(term.hebrew()).append("__");
        if (!term.note().isBlank()) {
            definition.append("\n(").append(term.note()).append(")");
        }
        return definition.toString();
    }

}
