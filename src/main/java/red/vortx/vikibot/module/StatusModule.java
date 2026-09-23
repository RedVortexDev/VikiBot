package red.vortx.vikibot.module;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import red.vortx.vikibot.application.BotModule;

public class StatusModule extends ListenerAdapter implements BotModule {

    @Override
    public void register(JDABuilder builder) {
        builder.addEventListeners(this);
    }

    @Override
    public void onReady(ReadyEvent event) {
        event.getJDA().getPresence().setActivity(Activity.customStatus("!מיינקראפט ויקי"));
    }

}
