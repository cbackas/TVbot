package cback;

import cback.eventFunctions.ChannelChange;
import cback.eventFunctions.MemberChange;
import cback.commands.*;
import cback.database.DatabaseManager;
import cback.eventFunctions.NicknameChange;
import org.reflections.Reflections;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.modules.Configuration;
import sx.blah.discord.util.DiscordException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("FieldCanBeLocal")
public class TVBot {

    private static TVBot instance;
    private IDiscordClient client;

    private DatabaseManager databaseManager;
    private TraktManager traktManager;
    private ConfigManager configManager;
    private CommandManager commandManager;
    private Scheduler scheduler;

    private List<String> botAdmins = new ArrayList<>();
    public List<Command> registeredCommands = new ArrayList<>();

    private static final Pattern COMMAND_PATTERN = Pattern.compile("^!([^\\s]+) ?(.*)", Pattern.CASE_INSENSITIVE);
    public static final String ANNOUNCEMENT_CHANNEL_ID = "227852239769698304";
    public static final String GENERAL_CHANNEL_ID = "192441520178200577";
    public static final String LOG_CHANNEL_ID = "217456105679224846";
    public static final String BOTLOG_CHANNEL_ID = "231499461740724224";
    public static final String MEMBERLOG_CHANNEL_ID = "217450005462646794";
    public static final String DEV_CHANNEL_ID = "234045109908275201";


    public static void main(String[] args) {
        new TVBot();
    }

    public TVBot() {

        instance = this;

        //instantiate config manager first as connect() relies on tokens
        configManager = new ConfigManager(this);
        commandManager = new CommandManager(this);

        connect();
        client.getDispatcher().registerListener(this);
        client.getDispatcher().registerListener(new ChannelChange(this));
        client.getDispatcher().registerListener(new MemberChange(this));
        client.getDispatcher().registerListener(new NicknameChange());

        databaseManager = new DatabaseManager(this);
        traktManager = new TraktManager(this);
        scheduler = new Scheduler(this);

        registerAllCommands();

        botAdmins.add("109109946565537792");
        botAdmins.add("148279556619370496");
        botAdmins.add("73416411443113984");
        botAdmins.add("144412318447435776");

    }

    private void connect() {
        //don't load external modules and don't attempt to create modules folder
        Configuration.LOAD_EXTERNAL_MODULES = false;

        Optional<String> token = configManager.getTokenValue("botToken");
        if (!token.isPresent()) {
            System.out.println("-------------------------------------");
            System.out.println("Insert your bot's token in the config.");
            System.out.println("Exiting......");
            System.out.println("-------------------------------------");
            System.exit(0);
            return;
        }

        ClientBuilder clientBuilder = new ClientBuilder();
        clientBuilder.withToken(token.get());
        clientBuilder.setMaxReconnectAttempts(5);
        try {
            client = clientBuilder.login();
        } catch (DiscordException e) {
            e.printStackTrace();
        }
    }

    @EventSubscriber
    public void onMessageEvent(MessageReceivedEvent event) {
        if (event.getMessage().getAuthor().isBot()) return; //ignore bot messages
        IMessage message = event.getMessage();
        IGuild guild = null;
        boolean isPrivate = message.getChannel().isPrivate();
        if (!isPrivate) guild = message.getGuild();
        String text = message.getContent();
        Matcher matcher = COMMAND_PATTERN.matcher(text);
        if (matcher.matches()) {
            String baseCommand = matcher.group(1).toLowerCase();
            Optional<Command> command = registeredCommands.stream()
                    .filter(com -> com.getName().equalsIgnoreCase(baseCommand) || (com.getAliases() != null && com.getAliases().contains(baseCommand)))
                    .findAny();
            if (command.isPresent()) {
                System.out.println("@" + message.getAuthor().getName() + " issued \"" + text + "\" in " +
                        (isPrivate ? ("@" + message.getAuthor().getName()) : guild.getName()));

                String args = matcher.group(2);
                String[] argsArr = args.isEmpty() ? new String[0] : args.split(" ");
                command.get().execute(this, client, argsArr, guild, message, isPrivate);
            } else if (commandManager.getCommandValue(baseCommand) != null) {

                String response = commandManager.getCommandValue(baseCommand);

                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("``" + message.getAuthor().getDisplayName(guild) + "``\n").append(response);

                Util.sendMessage(message.getChannel(), stringBuilder.toString());

                Util.deleteMessage(message);
            }
        } else {
            String lowerCase = message.getContent().toLowerCase();

            //Check for discord invite link
            if (lowerCase.contains("discord.gg") || lowerCase.contains("discordapp.com/invite/")) {
                IGuild loungeGuild = client.getGuildByID("192441520178200577");
                if (Util.permissionCheck(message, "Admins") || lowerCase.contains("discord.gg/lounge") || lowerCase.contains("QeuTNRb") || lowerCase.contains("Empn64q")) {
                } else {
                    Util.sendPrivateMessage(message.getAuthor(), "Rule 3, Advertising your server is not allowed!");
                    Util.sendMessage(client.getChannelByID("226433456060497920"), message.getAuthor().mention() + " __might__ have advertised their server in " + message.getChannel().mention() + ". Could a human please investigate?");
                }
            }
        }
    }

    @EventSubscriber
    public void onReadyEvent(ReadyEvent event) {
        System.out.println("Logged in.");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public TraktManager getTraktManager() {
        return traktManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public IDiscordClient getClient() {
        return client;
    }

    public List<String> getBotAdmins() {
        return botAdmins;
    }

    private void registerAllCommands() {
        new Reflections("cback.commands").getSubTypesOf(Command.class).forEach(commandImpl -> {
            try {
                Command command = commandImpl.newInstance();
                Optional<Command> existingCommand = registeredCommands.stream().filter(cmd -> cmd.getName().equalsIgnoreCase(command.getName())).findAny();
                if (!existingCommand.isPresent()) {
                    registeredCommands.add(command);
                    System.out.println("Registered command: " + command.getName());
                } else {
                    System.out.println("Attempted to register two commands with the same name: " + existingCommand.get().getName());
                    System.out.println("Existing: " + existingCommand.get().getClass().getName());
                    System.out.println("Attempted: " + commandImpl.getName());
                }
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
    }

    public static TVBot getInstance() {
        return instance;
    }

}
