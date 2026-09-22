package red.vortx.vikibot.application;

import net.dv8tion.jda.api.JDABuilder;

import java.util.List;

public final class BotApplication {

    private final String token;
    private final List<BotModule> modules;

    public BotApplication(String token, List<BotModule> modules) {
        this.token = token;
        this.modules = modules;
    }

    public void start() {
        JDABuilder builder = JDABuilder.createDefault(token);
        modules.forEach(module -> module.register(builder));
        builder.build();
    }

}
