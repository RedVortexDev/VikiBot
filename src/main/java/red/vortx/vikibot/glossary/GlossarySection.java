package red.vortx.vikibot.glossary;

import java.util.List;

public record GlossarySection(String title, List<Term> terms) {

    public GlossarySection {
        terms = List.copyOf(terms);
    }

}
