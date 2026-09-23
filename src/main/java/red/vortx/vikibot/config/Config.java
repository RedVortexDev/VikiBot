package red.vortx.vikibot.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.serde.ObjectDeserializer;
import com.electronwill.nightconfig.core.serde.annotations.SerdeAssert;
import com.electronwill.nightconfig.core.serde.annotations.SerdeKey;

public final class Config {

    private static final ObjectDeserializer DESERIALIZER = ObjectDeserializer.standard();

    @SerdeAssert(SerdeAssert.AssertThat.NOT_NULL)
    private Discord discord;

    private Config() {
    }

    static Config from(UnmodifiableConfig configuration) {
        return DESERIALIZER.deserializeFields(configuration, Config::new);
    }

    public Discord discord() {
        return discord;
    }

    public static final class Discord {

        @SerdeKey("guild-id")
        private long guildId;

        @SerdeKey("glossary-channel")
        private long glossaryChannel;

        private Discord() {
        }

        public long guildId() {
            return guildId;
        }

        public long glossaryChannel() {
            return glossaryChannel;
        }

    }

}
