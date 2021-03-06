package cback;

import cback.database.tv.Airing;
import cback.database.tv.Show;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Scheduler {

    /**
     * Check for airings every X seconds
     */
    private static final int CHECK_AIRING_INTERVAL = 300; //5 minutes
    /**
     * Number of seconds in one day
     */
    private static final int DAILY_INTERVAL = 86400; //24 hours
    /**
     * Send alert if show airs within X seconds from time of checking
     */
    private static final int ALERT_TIME_THRESHOLD = 660; //11 minutes
    /**
     * Delete message from database if show aired over X seconds from time of checking
     */
    private static final int DELETE_THRESHOLD = 86400; //1 day

    private TVBot bot;

    private ScheduledExecutorService exec;

    public Scheduler(TVBot bot) {
        this.bot = bot;
        exec = Executors.newSingleThreadScheduledExecutor();
        onInit();
    }

    private void onInit() {

        int time = Util.getCurrentTime(); //current epoch time in seconds

        //Checks periodically for upcoming airings to announce them
        int airingCheckWaitTime = roundUp(time, CHECK_AIRING_INTERVAL) - time; //seconds until next 5 minute interval
        Util.getLogger().info(airingCheckWaitTime + " seconds until first 5min check");
        exec.scheduleAtFixedRate(() -> {

            //establish current time (will be 5 min interval)
            int currentTime = Util.getCurrentTime();

            processNewAirings(currentTime);

        }, airingCheckWaitTime, CHECK_AIRING_INTERVAL, TimeUnit.SECONDS);

        //Checks daily (and at startup) to grab future airings from Trakt and prune database
        exec.scheduleAtFixedRate(() -> {

            //delete old airings from database
            pruneDeletedAirings();
            //update database with new airings for next 3 days
            bot.getTraktManager().updateAiringData();

        }, 0, DAILY_INTERVAL, TimeUnit.SECONDS);

        //midnight
        int currentTimeEST = time - getOffset(); //EST time, second offset changes depending on daylight savings
        int midnightWaitTime = roundUp(currentTimeEST, DAILY_INTERVAL) - currentTimeEST + 45; //seconds until midnight
        exec.scheduleAtFixedRate(() -> {

            Util.getLogger().info("Midnight processed.");
            updateUserCount();
            resetUserChange();
            sendDailyMessage();
            //Set status
            bot.getClient().getPresence().setActivity(Activity.watching("all of your messages. Type " + TVBot.COMMAND_PREFIX + "help"));

        }, midnightWaitTime, DAILY_INTERVAL, TimeUnit.SECONDS);
    }

    private static int getOffset() {
        //winter - 18000s
        //summer - 14400s

        boolean inSavingsTime = TimeZone.getTimeZone("US/Eastern").inDaylightTime(new Date());
        if (inSavingsTime) {
            return 14400;
        } else {
            return 18000;
        }
    }

    /**
     * Send messages for shows that are about to air and set database values accordingly
     */
    public void processNewAirings(int currentTime) {
        //get next 30 shows airing
        List<Airing> nextAirings = bot.getDatabaseManager().getTV().getNewAirings();

        Util.getLogger().info("Checking " + nextAirings.size() + " airings... " + nextAirings.stream().filter(airing -> airing.getAiringTime() - currentTime <= ALERT_TIME_THRESHOLD).count() + " ready to announce");

        //if it airs in next 11 minutes, send message and update in database
        nextAirings.stream().filter(airing -> airing.getAiringTime() - currentTime <= ALERT_TIME_THRESHOLD).forEach(airing -> {
            try {
                Util.getLogger().info("Starting announce for airing " + airing.getShowID() + " " + airing.getEpisodeInfo() + " " + airing.getAiringTime());
                Show show = bot.getDatabaseManager().getTV().getShow(airing.getShowID());

                if (show == null) { //only announce if show is still in database
                    Util.getLogger().info("Tried to announce airing for unsaved show, deleting airing...");
                    bot.getDatabaseManager().getTV().deleteAiring(airing.getEpisodeID());
                    return;
                } else if (bot.getClient().getTextChannelById(Long.parseLong(show.getChannelID())) == null) { //only announce if channel hasnt been deleted
                    Util.getLogger().info("Tried to announce airing for show with no channel, deleting show and airing...");
                    bot.getDatabaseManager().getTV().deleteAiring(airing.getEpisodeID());
                    bot.getDatabaseManager().getTV().deleteShow(show.getShowID());
                    return;
                } else if (airing.getAiringTime() - currentTime < -ALERT_TIME_THRESHOLD) { //only announce if it hasnt already aired in the past
                    Util.getLogger().info("Tried to announce old airing, deleting...");
                    bot.getDatabaseManager().getTV().deleteAiring(airing.getEpisodeID());
                    return;
                }

                ////Temporary to populate database with show networks
                String network = "null";
                if (show.getNetwork() == null || show.getNetwork().equalsIgnoreCase("null")) {
                    try {
                        TraktManager tm = bot.getTraktManager();
                        network = tm.showSummary(show.getShowID()).network;
                        Util.getLogger().info("new network " + network);
                        show.setNetwork(network);
                        bot.getDatabaseManager().getTV().updateShowNetwork(show);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    network = show.getNetwork();
                }
                /////////////////////////////////////////////////////

                TextChannel announceChannel = Channels.NEWEPISODE_CH_ID.getChannel();
                TextChannel globalChannel = Channels.GENERAL_CH_ID.getChannel();
                TextChannel showChannel = bot.getClient().getTextChannelById(Long.parseLong(show.getChannelID()));

                String message = "**" + show.getShowName() + " " + airing.getEpisodeInfo() + "** is about to start on " + network + ". Go to " + showChannel.getAsMention() + " for live episode discussion!";

                Util.sendMessage(announceChannel, message);
                Util.sendMessage(globalChannel, message);
                Util.sendMessage(showChannel, "**" + show.getShowName() + " " + airing.getEpisodeInfo() + "** is about to start.");

                //sent message - set database values accordingly
                airing.setSentStatus(true);
                bot.getDatabaseManager().getTV().updateAiringSentStatus(airing);

                Util.getLogger().info("Sent announcement for " + show.getShowName() + " - " + airing.getEpisodeInfo());

            } catch (Exception e) {
                Util.reportHome(e);
                e.printStackTrace();
            }
        });
    }

    /***
     * Delete airing entries from the database if episode aired over a week ago
     */
    public void pruneDeletedAirings() {
        //delete old airings
        List<Airing> oldAirings = bot.getDatabaseManager().getTV().getOldAirings();
        oldAirings.stream()
                .filter(airing -> Util.getCurrentTime() - airing.getAiringTime() >= DELETE_THRESHOLD)
                .forEach(airing -> bot.getDatabaseManager().getTV().deleteAiring(airing.getEpisodeID()));
    }

    /**
     * Update the number of Lounge server members in the config
     */
    public void updateUserCount() {
        Guild loungeGuild = bot.getHomeGuild();

        if (loungeGuild != null) {
            bot.getConfigManager().setConfigValue("userCount", String.valueOf(loungeGuild.getMembers().size()));
        }
    }

    /**
     * Reset daily user change
     */
    public void resetUserChange() {
        bot.getConfigManager().setConfigValue("left", "0");
        bot.getConfigManager().setConfigValue("joined", "0");
    }

    /**
     * Rounds i to next number divisible by v
     *
     * @param i
     * @param v
     * @return the rounded number divisible by v
     */
    public static int roundUp(double i, int v) {
        double rounded = Math.ceil(i / v) * v;
        return (int) rounded;
    }

    /**
     * Sends a message in the new show channel daily to separate days
     */
    public void sendDailyMessage() {
        LocalDate now = LocalDate.now();
        String dayOfWeek = now.getDayOfWeek().name();
        String month = now.getMonth().name();
        int day = now.getDayOfMonth();

        List<Airing> nextAirings = bot.getDatabaseManager().getTV().getNewAirings();

        //if it airs in next 24 hours it goes on the count
        int count = 0;
        for (Airing a : nextAirings) {
            if ((a.getAiringTime() - Util.getCurrentTime() <= DAILY_INTERVAL) && (a.getAiringTime() - Util.getCurrentTime() > 0)) {
                Show show = bot.getDatabaseManager().getTV().getShow(a.getShowID());
                if (show != null) {
                    count++;
                }
            }
        }

        EmbedBuilder embed = new EmbedBuilder();

        embed
                .setTitle(dayOfWeek + ", " + month + " " + day)
                .setColor(Util.getBotColor());

        if (count > 1) {
            embed.setDescription("There are " + count + " episodes airing today! Stick around to see what they are.");
        } else if (count == 1) {
            embed.setDescription("There is " + count + " episode airing today! Stick around to see what it is.");
        } else {
            embed.setDescription("There aren't any new episodes airing today. Maybe tomorrow will be interesting.");
        }

        Util.sendEmbed(Channels.NEWEPISODE_CH_ID.getChannel(), embed.build());
    }
}