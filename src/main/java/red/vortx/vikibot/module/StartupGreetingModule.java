package red.vortx.vikibot.module;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import red.vortx.vikibot.VikiBot;
import red.vortx.vikibot.application.BotModule;
import red.vortx.vikibot.config.Config;

import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class StartupGreetingModule extends ListenerAdapter implements BotModule {

    private static final Logger LOGGER = Logger.getLogger(StartupGreetingModule.class.getName());

    @Override
    public void register(JDABuilder builder) {
        builder.addEventListeners(this);
    }

    @Override
    public void onReady(ReadyEvent event) {
        JDA jda = event.getJDA();

        Config.Discord config = VikiBot.config().discord();

        Guild guild = jda.getGuildById(config.guildId());
        if (guild == null) {
            LOGGER.severe("Configured guild was not found: " + config.guildId());
            return;
        }

        TextChannel channel = guild.getTextChannelById(config.botLogChannel());
        if (channel == null) {
            LOGGER.severe("Configured log channel was not found in guild " + guild.getId());
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.GREEN)
                .setTitle("Started")
                .setDescription("Viki has started!");

        channel.sendMessageEmbeds(embed.build()).queue(
                _ -> LOGGER.info("Sent startup greeting to channel " + channel.getId()),
                failure -> LOGGER.log(Level.SEVERE, "Failed to send startup greeting", failure)
        );
    }

}
