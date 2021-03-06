package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.awt.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandMuteRemove extends Command {

    private TVBot bot;

    public CommandMuteRemove() {
        this.bot = TVBot.getInstance();
        this.name = "unmute";
        this.arguments = "unmute @user";
        this.help = "Unmutes a user";
        this.requiredRole = TVRoles.STAFF.name;
    }
    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = Util.splitArgs(commandEvent.getArgs());

        Role muteRole = commandEvent.getGuild().getRolesByName("muted", true).get(0);

        if(args.length == 1) {
            String user = args[0];
            Pattern pattern = Pattern.compile("^<@!?(\\d+)>");
            Matcher matcher = pattern.matcher(user);
            if(matcher.find()) {
                String u = matcher.group(1);
                Member userInput = commandEvent.getGuild().getMemberById(Long.parseLong(u));
                if(userInput != null) {
                    if(commandEvent.getAuthor().getId().equals(u)) {
                        Util.sendMessage(commandEvent.getTextChannel(), "Not sure how you typed this command... but you can't unmute yourself");
                    } else {
                        try {
                            commandEvent.getGuild().removeRoleFromMember(userInput, muteRole).queue();

                            Util.simpleEmbed(commandEvent.getTextChannel(), userInput.getEffectiveName() + " has been unmuted");

                            List<String> mutedUsers = bot.getConfigManager().getConfigArray("muted");
                            if(mutedUsers.contains(u)) {
                                mutedUsers.remove(u);
                                bot.getConfigManager().setConfigValue("muted", mutedUsers);
                            }
                            Util.sendLog(commandEvent.getMessage(), userInput.getEffectiveName() + "has been unmuted.", Color.gray);
                        } catch(Exception ex) {
                            Util.simpleEmbed(commandEvent.getTextChannel(), "Error running " + this.getName() + " - error recorded");
                            Util.reportHome(commandEvent.getMessage(), ex);
                        }
                    }
                }
            }
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
        }
        Util.deleteMessage(commandEvent.getMessage());
    }
}