package red.vortx.vikibot.module;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.GatewayIntent;
import red.vortx.vikibot.VikiBot;
import red.vortx.vikibot.application.BotModule;
import red.vortx.vikibot.config.Config;
import red.vortx.vikibot.glossary.Glossary;
import red.vortx.vikibot.glossary.GlossaryChange;
import red.vortx.vikibot.glossary.GlossaryMessage;
import red.vortx.vikibot.glossary.MediaWikiGlossary;
import red.vortx.vikibot.glossary.Term;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class GlossaryModule extends ListenerAdapter implements BotModule {

    private static final Logger LOGGER = Logger.getLogger(GlossaryModule.class.getName());
    private static final int HISTORY_PAGE_SIZE = 100;
    private static final int MAX_AUTOCOMPLETE_CHOICES = 25;

    private final MediaWikiGlossary wiki = new MediaWikiGlossary(MediaWikiGlossary.API);
    private volatile Glossary glossary;
    private volatile String glossaryMessageId;
    private boolean refreshing;
    private boolean refreshAgain;

    @Override
    public void register(JDABuilder builder) {
        builder.enableIntents(GatewayIntent.MESSAGE_CONTENT);
        builder.addEventListeners(this);
    }

    @Override
    public void onReady(ReadyEvent event) {
        Config.Discord config = VikiBot.config().discord();
        Guild guild = event.getJDA().getGuildById(config.guildId());
        if (guild == null) {
            LOGGER.severe("Configured guild was not found: " + config.guildId());
            return;
        }
        if (guild.getTextChannelById(config.glossaryChannel()) == null) {
            LOGGER.severe("Configured glossary channel was not found: " + config.glossaryChannel());
            return;
        }

        guild.upsertCommand("term", "חיפוש מושג במילון")
                .addOption(OptionType.STRING, "name", "English term", true, true)
                .queue(
                        _ -> LOGGER.info("Registered /term in guild " + guild.getId()),
                        failure -> LOGGER.log(Level.SEVERE, "Failed to register /term", failure)
                );
        refresh(event.getJDA());
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Config.Discord config = VikiBot.config().discord();
        if (!event.isFromGuild() || event.getGuild().getIdLong() != config.guildId()
                || event.getAuthor().getIdLong() == event.getJDA().getSelfUser().getIdLong()) {
            return;
        }

        Message message = event.getMessage();
        StringBuilder text = new StringBuilder(message.getContentRaw());
        for (MessageEmbed embed : message.getEmbeds()) {
            append(text, embed.getTitle());
            append(text, embed.getDescription());
            append(text, embed.getUrl());
            embed.getFields().forEach(field -> {
                append(text, field.getName());
                append(text, field.getValue());
            });
        }
        if (GlossaryChange.isGlossaryChange(text.toString())) {
            refresh(event.getJDA());
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        Config.Discord config = VikiBot.config().discord();
        if (!event.getName().equals("term") || event.getGuild() == null || event.getGuild().getIdLong() != config.guildId()) {
            return;
        }

        Glossary current = glossary;
        if (current == null) {
            event.reply("עדיין טוען מילון.").setEphemeral(true).queue();
            return;
        }

        OptionMapping option = event.getOption("name");
        if (option == null) {
            return;
        }

        String query = option.getAsString();
        Optional<Term> found = current.find(query);
        if (found.isEmpty()) {
            event.reply("לא מצאתי את המונח \"" + query + "\" במילון.")
                    .setEphemeral(true).queue();
            return;
        }

        Term term = found.orElseThrow();
        event.replyEmbeds(GlossaryMessage.term(term))
                .setAllowedMentions(List.of())
                .queue();
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        Config.Discord config = VikiBot.config().discord();
        if (!event.getName().equals("term") || event.getGuild() == null
                || event.getGuild().getIdLong() != config.guildId()
                || !event.getFocusedOption().getName().equals("name")) {
            return;
        }

        Glossary current = glossary;
        if (current == null) {
            event.replyChoices(List.of()).queue();
            return;
        }

        List<Command.Choice> choices = current.findEnglishTerms(event.getFocusedOption().getValue()).stream()
                .limit(MAX_AUTOCOMPLETE_CHOICES)
                .map(term -> new Command.Choice(term.english(), term.english()))
                .toList();
        event.replyChoices(choices).queue();
    }

    private static void append(StringBuilder text, String value) {
        if (value != null) {
            text.append(' ').append(value);
        }
    }

    private synchronized void refresh(JDA jda) {
        if (refreshing) {
            refreshAgain = true;
            return;
        }
        refreshing = true;
        wiki.fetch()
                .thenCompose(next -> {
                    glossary = next;
                    return publish(jda, next);
                })
                .whenComplete((_, failure) -> {
                    if (failure != null) {
                        LOGGER.log(Level.SEVERE, "Failed to refresh glossary", failure);
                    }
                    boolean retry;
                    synchronized (this) {
                        refreshing = false;
                        retry = refreshAgain;
                        refreshAgain = false;
                    }
                    if (retry) {
                        refresh(jda);
                    }
                });
    }

    private CompletableFuture<Void> publish(JDA jda, Glossary next) {
        Config.Discord config = VikiBot.config().discord();
        Guild guild = jda.getGuildById(config.guildId());
        TextChannel channel = guild == null ? null : guild.getTextChannelById(config.glossaryChannel());
        if (channel == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Configured glossary channel was not found: " + config.glossaryChannel()));
        }

        MessageEmbed embed = GlossaryMessage.embed(next);
        return findMessage(channel, jda.getSelfUser().getIdLong(), null)
                .thenCompose(existing -> {
                    if (existing.isEmpty()) {
                        return channel.sendMessageEmbeds(embed).submit().thenAccept(
                                sent -> {
                                    glossaryMessageId = sent.getId();
                                    LOGGER.info("Published glossary in channel " + channel.getId());
                                });
                    }
                    Message message = existing.orElseThrow();
                    glossaryMessageId = message.getId();
                    if (message.getEmbeds().getFirst().getDescription().equals(embed.getDescription())) {
                        return CompletableFuture.completedFuture(null);
                    }
                    return message.editMessageEmbeds(embed).submit().thenAccept(
                            edited -> LOGGER.info("Updated glossary message " + edited.getId()));
                });
    }

    private CompletableFuture<Optional<Message>> findMessage(TextChannel channel, long botId, String before) {
        if (glossaryMessageId != null && before == null) {
            return channel.retrieveMessageById(glossaryMessageId).submit().thenApply(Optional::of);
        }

        CompletableFuture<List<Message>> page = before == null
                ? channel.getHistory().retrievePast(HISTORY_PAGE_SIZE).submit()
                : channel.getHistoryBefore(before, HISTORY_PAGE_SIZE).submit()
                .thenApply(history -> history.getRetrievedHistory());

        return page.thenCompose(messages -> {
            for (Message message : messages) {
                if (message.getAuthor().getIdLong() == botId && message.getEmbeds().stream()
                        .anyMatch(GlossaryMessage::isGlossary)) {
                    return CompletableFuture.completedFuture(Optional.of(message));
                }
            }
            if (messages.size() < HISTORY_PAGE_SIZE) {
                return CompletableFuture.completedFuture(Optional.empty());
            }
            return findMessage(channel, botId, messages.getLast().getId());
        });
    }

}
