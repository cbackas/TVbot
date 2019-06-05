package cback.commands;

import cback.TVBot;

import java.util.List;

public interface Command {
    String getName();

    List<String> getAliases();

    String getSyntax();

    String getDescription();

    List<Long> getPermissions();

    void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot);

}
