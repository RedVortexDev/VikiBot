package red.vortx.vikibot.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import com.electronwill.nightconfig.core.conversion.Path;
import com.electronwill.nightconfig.core.conversion.SpecLongInRange;
import com.electronwill.nightconfig.core.conversion.SpecNotNull;

public final class Config {

    @SpecNotNull
    private Discord discord;

    private Config() {
    }

    static Config from(UnmodifiableConfig configuration) {
        return new ObjectConverter().toObject(configuration, Config::new);
    }

    public Discord discord() {
        return discord;
    }

    public static final class Discord {

        @Path("guild-id")
        @SpecLongInRange(min = 1, max = Long.MAX_VALUE)
        private long guildId;

        @Path("glossary-channel")
        @SpecLongInRange(min = 1, max = Long.MAX_VALUE)
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
