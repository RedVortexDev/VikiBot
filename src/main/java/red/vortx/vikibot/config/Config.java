package red.vortx.vikibot.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.core.serde.ObjectDeserializer;
import com.electronwill.nightconfig.core.serde.ObjectSerializer;
import com.electronwill.nightconfig.core.serde.annotations.SerdeAssert;
import com.electronwill.nightconfig.core.serde.annotations.SerdeKey;
import com.electronwill.nightconfig.core.serde.annotations.SerdeSkipDeserializingIf;
import com.electronwill.nightconfig.toml.TomlFormat;

import java.nio.file.Path;
import java.util.OptionalLong;

public final class Config {

    private static final ObjectDeserializer DESERIALIZER = ObjectDeserializer.standard();
    private static final ObjectSerializer SERIALIZER = ObjectSerializer.standard();

    private final transient Path path;

    @SerdeAssert(SerdeAssert.AssertThat.NOT_NULL)
    private Discord discord;

    private Config(Path path) {
        this.path = path;
    }

    static Config from(UnmodifiableConfig configuration, Path path) {
        return DESERIALIZER.deserializeFields(configuration, () -> new Config(path));
    }

    public Discord discord() {
        return discord;
    }

    public void save() {
        try (CommentedFileConfig document = CommentedFileConfig.builder(path, TomlFormat.instance())
                .onFileNotFound(FileNotFoundAction.THROW_ERROR)
                .writingMode(WritingMode.REPLACE_ATOMIC)
                .sync()
                .build()
        ) {
            document.load();

            CommentedConfig discordSection = document.get("discord");
            SERIALIZER.serializeFields(discord, discordSection);

            document.save();
        } catch (RuntimeException exception) {
            throw new ConfigurationException("Cannot save config file: " + path, exception);
        }
    }

    public static final class Discord {

        @SerdeKey("guild-id")
        private long guildId;

        @SerdeKey("glossary-channel")
        private long glossaryChannel;

        @SerdeKey("glossary-message-id")
        @SerdeSkipDeserializingIf(SerdeSkipDeserializingIf.SkipDeIf.IS_MISSING)
        private long glossaryMessageId;

        private Discord() {
        }

        public long guildId() {
            return guildId;
        }

        public long glossaryChannel() {
            return glossaryChannel;
        }

        public OptionalLong glossaryMessageId() {
            return glossaryMessageId == 0L ? OptionalLong.empty() : OptionalLong.of(glossaryMessageId);
        }

        public void setGlossaryMessageId(long messageId) {
            glossaryMessageId = messageId;
        }

    }

}
