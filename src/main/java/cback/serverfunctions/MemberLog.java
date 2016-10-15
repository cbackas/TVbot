package cback.serverfunctions;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.UserJoinEvent;
import sx.blah.discord.handle.impl.events.UserLeaveEvent;
import sx.blah.discord.handle.obj.IGuild;

public class MemberLog {
    String logChannel = "217450005462646794";

    @EventSubscriber
    public void memberJoin(UserJoinEvent event) {
        IGuild server = event.getGuild();
        String user = event.getUser().mention();
        Util.sendMessage(event.getClient().getChannelByID(logChannel), user + " **joined** the server.");
        if (event.getUser().isBot()) {
            Util.sendMessage(server.getChannelByID(TVBot.BOTLOG_CHANNEL_ID), "A bot has joined the server - " + user);
        }
    }

    @EventSubscriber
    public void memberLeave(UserLeaveEvent event) {
        String user = event.getUser().mention();
        Util.sendMessage(event.getClient().getChannelByID(logChannel), user + " **left** the server.");
    }
}
