package br.com.brforgers.mods.disfabric;

import me.sargunvohra.mcmods.autoconfig1u.ConfigData;
import me.sargunvohra.mcmods.autoconfig1u.annotation.Config;
import me.sargunvohra.mcmods.autoconfig1u.annotation.ConfigEntry;
import me.sargunvohra.mcmods.autoconfig1u.shadowed.blue.endless.jankson.Comment;

import java.util.HashSet;
import java.util.Set;

@Config(name = DisFabric.MOD_ID)
public class Configuration implements ConfigData {
    @Comment(value = "Do you only what to use DisFabric for commands?")
    @ConfigEntry.Category(value = "Commands")
    public boolean commandsOnly = false;

    @Comment(value = "Allow users to `!whitelist` themselves?")
    @ConfigEntry.Category(value = "Commands")
    public boolean publicWhitelist = false;

    @Comment(value = "Sets if DisFabric Should Modify In-Game Chat Messages")
    @ConfigEntry.Category(value = "MinecraftChat")
    public boolean modifyChatMessages = true;

    @Comment(value = "Bot Token; see https://discordpy.readthedocs.io/en/latest/discord.html")
    @ConfigEntry.Category(value = "Discord")
    public String botToken = "";

    @Comment(value = "Bot Game Status; What will be displayed on the bot's game status (leave empty for nothing)")
    @ConfigEntry.Category(value = "Discord")
    public String botGameStatus = "";

    @Comment(value = "Enable Webhook; If enabled, player messages will be send using a webhook with the players name and head, instead of a regular message.")
    @ConfigEntry.Category(value = "Discord")
    public boolean isWebhookEnabled = true;

    @Comment(value = "Webhook URL; see https://support.discord.com/hc/en-us/articles/228383668-Intro-to-Webhooks")
    @ConfigEntry.Category(value = "Discord")
    public String webhookURL = "";

    @Deprecated(forRemoval = true)
    @Comment(value = "[Deprecated] - Use `admins` instead.")
    @ConfigEntry.Category(value = "Discord")
    public Set<String> adminsIds = new HashSet<>();

    @Comment(value = """
            Admins IDs in Discord, either role or user; see https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID-

            If you're adding more than one admin, separate using commas, like this:
            "adminsIds": [\s
            \t\t000,
            \t\t111,
            \t\t222
            \t]""")
    @ConfigEntry.Category(value = "Discord")
    public Set<Long> admins = new HashSet<>();

    @ConfigEntry.Category(value = "Discord")
    public Set<Long> roles = new HashSet<>();

    @Comment(value = "Allow another bot to run a command as long as it has an allowed role.")
    @ConfigEntry.Category(value = "Discord")
    public boolean allowBots = false;

    @Deprecated
    boolean migrateAdmins() {
        if (adminsIds != null && !adminsIds.isEmpty()) {
            for (var str : adminsIds) {
                admins.add(Long.parseUnsignedLong(str));
            }
            adminsIds = null;
            return true;
        }
        return false;
    }

    @Comment(value = "The permission level that admins have when issuing commands.")
    @ConfigEntry.Category(value = "Discord")
    public int adminPermissionLevel = 4;

    @Deprecated(forRemoval = true)
    @Comment(value = "[Deprecated] Use `bridgeChannel` instead.")
    @ConfigEntry.Category(value = "Discord")
    public String channelId;

    @Comment(value = "The bridge channel in Discord.")
    @ConfigEntry.Category(value = "Discord")
    public long bridgeChannel = 0L;

    @Deprecated
    boolean migrateBridgeChannel() {
        if (channelId != null && !channelId.isBlank()) {
            try {
                bridgeChannel = Long.parseUnsignedLong(channelId);
            } catch (NumberFormatException e) {
                if (bridgeChannel == 0L) {
                    throw new RuntimeException("Malformed config: channelId cannot be parsed as a Discord snowflake.");
                }
            }
            channelId = null;
            return true;
        }
        return false;
    }

    @Comment("Bug reports channel in Discord. Will be disabled when set to 0.")
    @ConfigEntry.Category("Discord")
    public long bugReportChannel = 0L;

    @Comment("Create thread on bug report? Will be automatically disabled if disallowed.")
    @ConfigEntry.Category("Discord")
    public boolean bugReportAutoThread = true;

    @Comment("Player reports channel in Discord. Will be disabled when set to 0.")
    @ConfigEntry.Category("Discord")
    public long userReportChannel = 0L;

    @Comment("Role to ping on player report. Staff role recommended. Will not ping when set to 0.")
    @ConfigEntry.Category("Discord")
    public long userReportStaffRole = 0L;

    @Comment("Suggestions channel in Discord. Will be disabled when set to 0.")
    @ConfigEntry.Category("Discord")
    public long suggestionChannel = 0L;

    @Comment(value = "Should announce when a players join/leave the server?")
    @ConfigEntry.Category(value = "Discord")
    public boolean announcePlayers = true;

    @Comment(value = "Should announce when a players get an advancement?")
    @ConfigEntry.Category(value = "Discord")
    public boolean announceAdvancements = true;

    @Comment(value = "Should announce when a player die?")
    @ConfigEntry.Category(value = "Discord")
    public boolean announceDeaths = true;

    public Texts texts = new Texts();

    public static class Texts {

        @Comment(value = """
                Minecraft -> Discord
                Player chat message (Only used when Webhook is disabled)
                Available placeholders:
                %playername% | Player name
                %playermessage% | Player message""")
        @ConfigEntry.Category(value = "Texts")
        public String playerMessage = "**%playername%:** %playermessage%";

        @Comment(value = "Minecraft -> Discord\n"+
                "Server started message")
        @ConfigEntry.Category(value = "Texts")
        public String serverStarted = "**Server started!**";

        @Comment(value = "Minecraft -> Discord\n"+
                "Server stopped message")
        @ConfigEntry.Category(value = "Texts")
        public String serverStopped = "**Server stopped!**";

        @Comment(value = """
                Minecraft -> Discord
                Join server
                Available placeholders:
                %playername% | Player name""")
        @ConfigEntry.Category(value = "Texts")
        public String joinServer = "**%playername% joined the game**";

        @Comment(value = """
                Minecraft -> Discord
                Left server
                Available placeholders:
                %playername% | Player name""")
        @ConfigEntry.Category(value = "Texts")
        public String leftServer = "**%playername% left the game**";

        @Comment(value = """
                Minecraft -> Discord
                Death message
                Available placeholders:
                %playername% | Player name
                %deathmessage% | Death message""")
        @ConfigEntry.Category(value = "Texts")
        public String deathMessage = "**%deathmessage%**";

        @Comment(value = """
                Minecraft -> Discord
                Advancement type task message
                Available placeholders:
                %playername% | Player name
                %advancement% | Advancement name""")
        @ConfigEntry.Category(value = "Texts")
        public String advancementTask = "%playername% has made the advancement **[%advancement%]**";

        @Comment(value = """
                Minecraft -> Discord
                Advancement type challenge message
                Available placeholders:
                %playername% | Player name
                %advancement% | Advancement name""")
        @ConfigEntry.Category(value = "Texts")
        public String advancementChallenge = "%playername% has completed the challenge **[%advancement%]**";

        @Comment(value = """
                Minecraft -> Discord
                Advancement type goal message
                Available placeholders:
                %playername% | Player name
                %advancement% | Advancement name""")
        @ConfigEntry.Category(value = "Texts")
        public String advancementGoal = "%playername% has reached the goal **[%advancement%]**";
    }
}
