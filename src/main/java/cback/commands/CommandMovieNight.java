package cback.commands;

import cback.TVBot;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

public class CommandMovieNight implements Command {
    @Override
    public String getName() {
        return "movienight";
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        String variation = args[0];
        if(variation.equalsIgnoreCase("set")) {

        } else if(variation.equalsIgnoreCase("start")) {

        }
    }

    @Override
    public boolean isLogged() {
        return true;
    }
}
