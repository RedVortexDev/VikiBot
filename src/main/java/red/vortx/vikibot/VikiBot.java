package red.vortx.vikibot;

import red.vortx.vikibot.application.BotApplication;
import red.vortx.vikibot.config.Config;
import red.vortx.vikibot.config.ConfigurationLoader;
import red.vortx.vikibot.module.GlossaryModule;
import red.vortx.vikibot.module.StatusModule;

import java.nio.file.Path;
import java.util.List;

public final class VikiBot {

    private static final String TOKEN_ENVIRONMENT_VARIABLE = "DISCORD_TOKEN";
    private static final String CONFIG_FILE_NAME = "config.toml";
    private static Config CONFIG;

    private VikiBot() {
    }

    static void main() {
        String token = System.getenv(TOKEN_ENVIRONMENT_VARIABLE);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Environment variable " + TOKEN_ENVIRONMENT_VARIABLE + " must be set");
        }

        CONFIG = new ConfigurationLoader().load(Path.of(CONFIG_FILE_NAME));

        new BotApplication(token, List.of(
                new StatusModule(),
                new GlossaryModule()
        )).start();
    }

    public static Config config() {
        return CONFIG;
    }

}
