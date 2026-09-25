package red.vortx.vikibot.glossary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public record Glossary(List<Term> terms) {

    public static Glossary parse(String wikitext) {
        List<Term> terms = new ArrayList<>();
        List<String> cells = new ArrayList<>();
        boolean inTable = false;

        for (String line : wikitext.lines().toList()) {
            String value = line.trim();
            if (value.startsWith("{|")) {
                inTable = true;
                continue;
            }
            if (!inTable) {
                continue;
            }
            if (value.equals("|-")) {
                addRow(terms, cells);
                cells.clear();
                continue;
            }
            if (value.equals("|}")) {
                addRow(terms, cells);
                break;
            }
            if (value.startsWith("|")) {
                for (String cell : value.substring(1).split("\\|\\|", -1)) {
                    cells.add(cell.trim());
                }
            }
        }

        return new Glossary(terms);
    }

    public Optional<Term> find(String query) {
        String normalized = query.strip().toLowerCase(Locale.ROOT);
        return terms.stream()
                .filter(term ->
                        term.english().toLowerCase(Locale.ROOT).equals(normalized)
                                || term.hebrew().toLowerCase(Locale.ROOT).equals(normalized)
                )
                .findFirst();
    }


    public List<Term> findEnglishTerms(String prefix) {
        String normalized = prefix.strip().toLowerCase(Locale.ROOT);
        return terms.stream()
                .filter(term -> term.english().toLowerCase(Locale.ROOT).startsWith(normalized))
                .toList();
    }

    private static void addRow(List<Term> terms, List<String> cells) {
        if (cells.isEmpty()) {
            return;
        }
        if ((cells.size() != 2 && cells.size() != 3) || cells.get(0).isBlank() || cells.get(1).isBlank()) {
            throw new IllegalArgumentException("Invalid glossary row: " + cells);
        }
        String note = cells.size() == 3 ? cells.get(2) : "";
        terms.add(new Term(cells.get(0), cells.get(1), note));
    }

}
