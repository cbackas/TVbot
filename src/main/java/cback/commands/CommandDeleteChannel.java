package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.util.Arrays;
import java.util.List;

public class CommandDeleteChannel implements Command {
    @Override
    public String getName() {
        return "deletechannel";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("removechannel");
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (message.getAuthor().getRolesForGuild(guild).contains(guild.getRoleByID("192441946210435072")) | message.getAuthor().getRolesForGuild(guild).contains(guild.getRoleByID("236988571330805760"))) {
            List<IChannel> mentionsC = message.getChannelMentions();
            for (IChannel c : mentionsC) {
                try {
                    c.delete();
                    Util.sendBufferedMessage(guild.getChannelByID(TVBot.LOG_CHANNEL_ID), "```Deleted " + c.getName() + " channel.\n- " + message.getAuthor().getDisplayName(guild) + "```");
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        }
    }

    @Override
    public boolean isLogged() {
        return false;
    }
}