package cback.commands;

import cback.TVbot;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.MessageBuilder;

import java.util.EnumSet;

public class CommandHelp implements Command {
    @Override
    public String getName() {
        return "help";
    }

    @Override
    public void execute(TVbot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        try {
            DiscordUtils.checkPermissions(message.getChannel().getModifiedPermissions(message.getAuthor()), EnumSet.of(Permissions.BAN));
            new MessageBuilder(client).withChannel(message.getChannel()).appendQuote("TVbot's Commands:").appendCode("XL",
                    "!help        //shows a list of commands\n" +
                            "!addshow [imdbid] [channelid]     //adds a new show to the calendar\n" +
                            "!removeshow [imdbid] [channelid]        //deletes a show from the calendar\n" +
                            "!addlog [message]               //skips the current sound\n"
            ).appendQuote("Mod+ commands included - regular users can not see staff commands").send();
        } catch (Exception e) {
            try {
                new MessageBuilder(client).withChannel(message.getChannel()).appendQuote("TVbot's Commands:").appendCode("XL",
                        "!help        //shows a list of commands\n"
                ).send();
            } catch (Exception f) {
            }
        }
    }
}
