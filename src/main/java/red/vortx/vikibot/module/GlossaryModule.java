package red.vortx.vikibot.module;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
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
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import red.vortx.vikibot.VikiBot;
import red.vortx.vikibot.application.BotModule;
import red.vortx.vikibot.config.Config;
import red.vortx.vikibot.glossary.Glossary;
import red.vortx.vikibot.glossary.GlossaryMessage;
import red.vortx.vikibot.glossary.MediaWikiGlossary;
import red.vortx.vikibot.glossary.Term;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class GlossaryModule extends ListenerAdapter implements BotModule {

    private static final Logger LOGGER = Logger.getLogger(GlossaryModule.class.getName());
    private static final int MAX_AUTOCOMPLETE_CHOICES = 25;
    private static final String GLOSSARY_PAGE_TITLE = "קהילה:מילון";

    private final MediaWikiGlossary wiki = new MediaWikiGlossary(MediaWikiGlossary.API);
    private volatile Glossary glossary;
    private boolean refreshing;
    private boolean refreshAgain;

    @Override
    public void register(JDABuilder builder) {
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT);
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
        if (event.getChannel().getIdLong() != config.recentChangesChannel()) {
            return;
        }

        List<MessageEmbed> embeds = event.getMessage().getEmbeds();
        if (embeds.size() != 1) {
            return;
        }

        String title = embeds.getFirst().getTitle();
        if (title != null && title.contains(GLOSSARY_PAGE_TITLE)) {
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
        Config config = VikiBot.config();
        Config.Discord discord = config.discord();
        Guild guild = jda.getGuildById(discord.guildId());
        TextChannel channel = guild == null ? null : guild.getTextChannelById(discord.glossaryChannel());
        if (channel == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Configured glossary channel was not found: " + discord.glossaryChannel())
            );
        }

        MessageCreateData messageData = GlossaryMessage.message(next);
        OptionalLong configuredMessageId = discord.glossaryMessageId();
        if (configuredMessageId.isEmpty()) {
            return channel.sendMessage(messageData).submit().thenAccept(sent -> {
                discord.setGlossaryMessageId(sent.getIdLong());
                config.save();
                LOGGER.info("Published glossary and saved message ID");
            });
        }

        String messageId = Long.toString(configuredMessageId.getAsLong());
        return channel.retrieveMessageById(messageId).submit().thenCompose(existing ->
                existing.editMessage(MessageEditBuilder.fromCreateData(messageData).build()).submit().thenAccept(
                        _ -> LOGGER.info("Updated glossary message")
                )
        );
    }

}
