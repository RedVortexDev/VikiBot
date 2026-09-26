package red.vortx.vikibot.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.toml.TomlFormat;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigurationLoader {

    public Config load(Path configPath) {
        if (!Files.isRegularFile(configPath) || !Files.isReadable(configPath)) {
            throw new ConfigurationException("Cannot read config file: " + configPath);
        }

        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            CommentedConfig document = TomlFormat.instance().createParser().parse(reader);
            return Config.from(document, configPath.toAbsolutePath());
        } catch (IOException | ParsingException exception) {
            throw new ConfigurationException("Invalid TOML in " + configPath, exception);
        } catch (RuntimeException exception) {
            throw new ConfigurationException("Configuration does not match the schema in " + configPath, exception);
        }
    }

}
