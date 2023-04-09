package br.com.brforgers.mods.disfabric.listeners;

import br.com.brforgers.mods.disfabric.DisFabric;
import br.com.brforgers.mods.disfabric.markdown.SpecialStringType;
import br.com.brforgers.mods.disfabric.utils.DiscordCommandOutput;
import br.com.brforgers.mods.disfabric.utils.Utils;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.ProfileLookupCallback;
import dev.gegy.mdchat.TextStyler;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class DiscordEventListener extends ListenerAdapter {
    private static final Style LINK = Style.EMPTY.withFormatting(Formatting.AQUA, Formatting.UNDERLINE);

    private boolean checkAdminPermissions(Member authorMember, long authorId) {
        if (DisFabric.config.admins.contains(authorId)) {
            return true;
        }

        final var configRoles = DisFabric.config.roles;

        if (!configRoles.isEmpty() && authorMember != null) {
            for (var role : authorMember.getRoles()) {
                if (configRoles.contains(role.getIdLong())) return true;
            }
        }

        return false;
    }

    /**
     * Checks whether the author can execute commands.
     */
    private static boolean checkAuthor(User author) {
        final var self = DisFabric.jda.getSelfUser();
        // Make sure the bot *can't* execute its own messages.
        if (author.equals(self) || author.getIdLong() == DisFabric.selfWebhook) return false;
        if (!author.isBot()) return true;

        return DisFabric.config.allowBots;
    }

    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        MinecraftServer server = getServer();
        MessageChannel channel = e.getChannel();
        if (server != null) {
            int playerNumber = server.getCurrentPlayerCount();
            int maxPlayer = server.getMaxPlayerCount();
            DisFabric.jda.getPresence().setActivity(Activity.playing(playerNumber + " / " + maxPlayer));
        }
        if (server != null && channel.getIdLong() == DisFabric.config.bridgeChannel && checkAuthor(e.getAuthor())) {
            String raw = e.getMessage().getContentRaw();
            if (raw.startsWith("!")) {
                int space = raw.indexOf(' ', 1);
                switch (space == -1 ? raw.substring(1) : raw.substring(1, space)) {
                    case "console" -> {
                        long authorId = e.getAuthor().getIdLong();
                        Member authorMember = e.getMember();

                        if (!checkAdminPermissions(authorMember, authorId)) {
                            return; // Author doesn't have an admin role or admin user ID, so return and don't execute the function
                        }

                        String command = raw.substring(space + 1);
                        server.execute(() -> server.getCommandManager().executeWithPrefix(getDiscordCommandSource(e), command));
                    }

                    case "whitelist" -> {
                        long authorId = e.getAuthor().getIdLong();
                        Member authorMember = e.getMember();

                        if (!DisFabric.config.publicWhitelist && !checkAdminPermissions(authorMember, authorId)) {
                            return; // Author doesn't have an admin role or admin user ID, so return and don't execute the function
                        }

                        String username = raw.substring(space + 1).strip();

                        if (username.isBlank() || space == -1) {
                            channel.sendMessage("Enter a username").queue();
                            return;
                        } else if (username.indexOf(' ') >= 0) {
                            channel.sendMessage("Invalid username").queue();
                            return;
                        }

                        GameProfile profile;
                        var lookupCallback = new ProfileLookupCallback() {
                            GameProfile profile;

                            @Override
                            public void onProfileLookupSucceeded(final GameProfile profile) {
                                this.profile = profile;
                            }

                            @Override
                            public void onProfileLookupFailed(final GameProfile profile, final Exception exception) {

                            }
                        };
                        server.getGameProfileRepo().findProfilesByNames(new String[]{username}, Agent.MINECRAFT, lookupCallback);

                        profile = lookupCallback.profile;

                        if (profile == null) {
                            channel.sendMessage("Invalid username.").queue();
                            return;
                        }

                        server.execute(() -> {
                            var playerManager = server.getPlayerManager();
                            var whitelist = playerManager.getWhitelist();
                            var entry = new WhitelistEntry(profile);

                            if (whitelist.isAllowed(profile)) {
                                channel.sendMessage("`" + profile.getName() + "` is already whitelisted.").queue();
                            } else {
                                whitelist.add(entry);
                                channel.sendMessage(e.getAuthor().getAsMention() + " ➠ Whitelisted `" + profile.getName() + '`').queue();
                            }
                        });
                    }
                    case "spawn" -> {
                        ServerWorld serverWorld = Objects.requireNonNull(getServer()).getOverworld();
                        channel.sendMessage("Spawn location: " + Vec3d.of(serverWorld.getSpawnPos())).queue();
                    }
                    case "online" -> {
                        List<ServerPlayerEntity> onlinePlayers = server.getPlayerManager().getPlayerList();
                        StringBuilder playerList = new StringBuilder("```ansi\n=============== \u001B[32;1mOnline Players\u001B[0m (\u001B[34m")
                                .append(onlinePlayers.size()).append("\u001B[0m) ===============\n");
                        for (ServerPlayerEntity player : onlinePlayers) {
                            playerList.append("\n - \u001B[36m").append(player.getEntityName()).append("\u001B[0m");
                        }
                        playerList.append("```");
                        channel.sendMessage(playerList.toString()).queue();
                    }
                    case "tps" -> {
                        StringBuilder tps = new StringBuilder("Server TPS: ");
                        double serverTickTime = MathHelper.average(server.lastTickLengths) * 1.0E-6D;
                        tps.append(Math.min(1000.0 / serverTickTime, 20));
                        channel.sendMessage(tps.toString()).queue();
                    }
                    case "help" -> channel.sendMessage("""
                            ```ansi
                            =============== \u001B[32;1mCommands\u001B[0m ===============

                            To whitelist yourself on this server use:
                            !\u001B[33mwhitelist\u001B[0m <\u001B[30mminecraft username\u001B[0m>

                            !\u001B[33monline\u001B[0m: list server online players
                            !\u001B[33mtps\u001B[0m: shows loaded dimensions tps's
                            !\u001B[33mspawn\u001B[0m: shows the location of spawn
                            !\u001B[33mconsole\u001B[0m <\u001B[30mcommand\u001B[0m>: executes commands in the server console (admins only)
                            ```""").queue();
                    default -> channel.sendMessage("Unknown command. Run `!help` for available commands.").queue();
                }
            } else if (!DisFabric.config.commandsOnly) {
                var username = Utils.convertMemberToFormattedText(e.getAuthor(), e.getMember(), "");

                /*/ New potential method for consistency with in-game chat?
                var message = raw.isBlank() ? Text.empty() :
                        server.getMessageDecorator().decorate(null, Text.of(raw)).join().copy();
                /*/
                var styledMessage = TextStyler.INSTANCE.apply(SpecialStringType.preprocess(raw));

                var message = styledMessage == null ? Text.empty() : styledMessage.copy();
                /**/

                for (var attachment : e.getMessage().getAttachments()) {
                    var style = LINK.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, attachment.getUrl()));
                    message.append(" <").append(Text.literal(attachment.getFileName()).fillStyle(style)).append(">");
                }

                server.getPlayerManager().broadcast(Utils.createFakeChatMessage(username, message), false);
            }
        }
    }

    public static ServerCommandSource getDiscordCommandSource(@NotNull MessageReceivedEvent e) {
        ServerWorld serverWorld = Objects.requireNonNull(getServer()).getOverworld();

        User author = e.getAuthor();
        String username = author.getName() + '#' + author.getDiscriminator();

        return new ServerCommandSource(new DiscordCommandOutput(), serverWorld == null ? Vec3d.ZERO : Vec3d.of(serverWorld.getSpawnPos()), Vec2f.ZERO, serverWorld, DisFabric.config.adminPermissionLevel, username, Text.of(username), getServer(), null);
    }

    private static MinecraftServer getServer() {
        @SuppressWarnings("deprecation")
        Object gameInstance = FabricLoader.getInstance().getGameInstance();
        if (gameInstance instanceof MinecraftServer) {
            return (MinecraftServer) gameInstance;
        } else {
            return null;
        }
    }
}
