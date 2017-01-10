package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.*;

import java.awt.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandMuteAdd implements Command {
    @Override
    public String getName() {
        return "mute";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        List<IRole> userRoles = message.getAuthor().getRolesForGuild(guild);
        if (userRoles.contains(guild.getRoleByID(TVRoles.STAFF.id))) {
            if (userRoles.contains(guild.getRoleByID(TVRoles.HELPER.id)) || userRoles.contains(guild.getRoleByID(TVRoles.ADMIN.id)) || userRoles.contains(guild.getRoleByID(TVRoles.MOD.id)) || userRoles.contains(guild.getRoleByID(TVRoles.REDDITMOD.id))) {
                List<String> mutedUsers = bot.getConfigManager().getConfigArray("muted");

                Util.botLog(message);

                if (args[0].equalsIgnoreCase("list")) {

                    StringBuilder mutedList = new StringBuilder();
                    if (!mutedUsers.isEmpty()) {
                        for (String userID : mutedUsers) {

                            IUser userO = guild.getUserByID(userID);

                            String user = "NULL";
                            if (userO != null) {
                                user = userO.mention();
                            } else {
                                user = Util.requestUsernameByID(userID);
                            }

                            mutedList.append("\n").append(user);
                        }
                    } else {
                        mutedList.append("\n").append("There are currently no muted users.");
                    }

                    Util.sendMessage(message.getChannel(), "**Muted Users**: (plain text for users not on server)\n" + mutedList.toString());

                }

                else if (args.length >= 1) {
                    String text = message.getContent();

                    Pattern pattern = Pattern.compile("^!mute <@!?(\\d+)> ?(.+)?");
                    Matcher matcher = pattern.matcher(text);

                    if (matcher.find()) {
                        String u = matcher.group(1);
                        String reason = matcher.group(2);

                        if (reason == null) {
                            reason = "an unspecified reason";
                        }

                        IUser userInput = guild.getUserByID(u);
                        if (message.getAuthor().getID().equals(u)) {
                            Util.sendMessage(message.getChannel(), "You probably shouldn't mute yourself");
                        }

                        else {
                            try {
                                userInput.addRole(guild.getRoleByID("231269949635559424"));
                                Util.sendMessage(message.getChannel(), userInput.getDisplayName(guild) + " has been muted. Check " + guild.getChannelByID(TVBot.LOG_CHANNEL_ID).mention() + " for more info.");

                                if (!mutedUsers.contains(u)) {
                                    mutedUsers.add(u);
                                    bot.getConfigManager().setConfigValue("muted", mutedUsers);
                                }

                                Util.sendLog(message, "Muted " + userInput.getDisplayName(guild) + "\n**Reason:** " + reason, Color.gray);
                                Util.deleteMessage(message);
                            } catch (Exception e) {
                                e.printStackTrace();

                                Util.sendMessage(message.getChannel(), "Internal error - cback has been notified");
                                Util.errorLog(message, "Error running CommandBan - check stacktrace");
                            }
                        }
                    }
                } else {
                    Util.sendMessage(message.getChannel(), "Invalid arguments. Usage: ``!mute @user``");
                }
            }
        }
    }

}
