package red.vortx.vikibot.application;

import net.dv8tion.jda.api.JDABuilder;

public interface BotModule {

    void register(JDABuilder builder);

}
