package br.com.brforgers.mods.disfabric.listeners;

import br.com.brforgers.mods.disfabric.DisFabric;
import br.com.brforgers.mods.disfabric.events.PlayerAdvancementCallback;
import br.com.brforgers.mods.disfabric.events.PlayerDeathCallback;
import br.com.brforgers.mods.disfabric.events.ServerChatCallback;
import br.com.brforgers.mods.disfabric.markdown.SpecialStringType;
import br.com.brforgers.mods.disfabric.utils.Utils;
import br.com.brforgers.mods.disfabric.utils.VanishService;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

public final class MinecraftEventListener {
    private static final Identifier DISFABRIC_CHAT = Identifier.of("disfabric", "decorator");

    private static final String
            playerName = "%playername%",
            playerMessage = "%playermessage%",
            deathMessage = "%deathmessage%",
            advancement = "%advancement%";

    public static void init() {
        if (!DisFabric.config.commandsOnly) {
            ServerMessageDecoratorEvent.EVENT.addPhaseOrdering(DISFABRIC_CHAT, Event.DEFAULT_PHASE);
            ServerMessageDecoratorEvent.EVENT.register(DISFABRIC_CHAT, (sender, message) ->
                    CompletableFuture.completedFuture(DisFabric.stop ? message : Text.of(Utils.convertMentionsFromNames(SpecialStringType.preprocess(message.getString())))));
            ServerChatCallback.EVENT.register((playerEntity, rawMessage) -> {
                if (DisFabric.stop || VanishService.isChatDisabled(playerEntity)) return;

                String convertedString = Utils.convertMentionsFromNames(rawMessage);
                if (DisFabric.config.isWebhookEnabled) {
                    JSONObject body = new JSONObject();
                    // TODO: Verify if this is applicable to all nickname mods
                    //  If not, add some detection logic for getName and getDisplayName against getEntityName.
                    body.put("username", Utils.playerName(playerEntity));
                    body.put("avatar_url", Utils.playerAvatarUrl(playerEntity));
                    JSONObject allowed_mentions = new JSONObject();
                    allowed_mentions.put("parse", new String[]{"users"});
                    body.put("allowed_mentions", allowed_mentions);
                    body.put("content", convertedString);
                    try {
                        Unirest.post(DisFabric.config.webhookURL).header("Content-Type", "application/json").body(body).asJsonAsync();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    bridge(DisFabric.config.texts.playerMessage,
                            playerName, Utils.sanitisedPlayerName(playerEntity),
                            playerMessage, convertedString);
                }
            });

            PlayerAdvancementCallback.EVENT.register((playerEntity, advancement) -> {
                if (!DisFabric.config.announceAdvancements || DisFabric.stop) return;
                if (VanishService.isPlayerVanished(playerEntity)) return;
                final var display = advancement.getDisplay();
                if (display == null || !display.shouldAnnounceToChat() ||
                        !playerEntity.getAdvancementTracker().getProgress(advancement).isDone()) return;

                final var template = switch (advancement.getDisplay().getFrame()) {
                    case GOAL -> DisFabric.config.texts.advancementGoal;
                    case TASK -> DisFabric.config.texts.advancementTask;
                    case CHALLENGE -> DisFabric.config.texts.advancementChallenge;
                };

                bridgeAdvancement(template, playerEntity, display);
            });

            PlayerDeathCallback.EVENT.register((playerEntity, damageSource) -> {
                if (!DisFabric.config.announceDeaths || DisFabric.stop) return;
                if (!VanishService.isPlayerVanished(playerEntity)) return;

                bridge(DisFabric.config.texts.deathMessage,
                        deathMessage, MarkdownSanitizer.escape(damageSource.getDeathMessage(playerEntity).getString()),
                        playerName, Utils.sanitisedPlayerName(playerEntity));
            });

            ServerPlayConnectionEvents.JOIN.register((handler, $2, $3) -> {
                if (announcePlayers() && !VanishService.isPlayerVanished(handler.player)) {
                    bridgePlayer(DisFabric.config.texts.joinServer, handler.player);
                }
            });

            ServerPlayConnectionEvents.DISCONNECT.register((handler, $2) -> {
                if (announcePlayers() && !VanishService.isPlayerVanished(handler.player)) {
                    bridgePlayer(DisFabric.config.texts.leftServer, handler.player);
                }
            });
            VanishService.listen();
        }
    }

    private static boolean announcePlayers() {
        return DisFabric.config.announcePlayers && !DisFabric.stop;
    }

    /**
     * @author Octal
     */
    public static void onPlayerVanishChange(ServerPlayerEntity player, boolean vanish) {
        if (announcePlayers()) {
            bridgePlayer(vanish ? DisFabric.config.texts.leftServer : DisFabric.config.texts.joinServer, player);
        }
    }

    public static void bridge(String template, String... params) {
        for (int i = 0; i < params.length; i += 2) {
            template = template.replace(params[i], params[i + 1]);
        }

        DisFabric.bridgeChannel.sendMessage(template).queue();
    }

    public static void bridgePlayer(String template, PlayerEntity player) {
        DisFabric.bridgeChannel.sendMessage(template.replace(playerName, Utils.sanitisedPlayerName(player))).queue();
    }

    public static void bridgeAdvancement(String template, PlayerEntity player, AdvancementDisplay display) {
        DisFabric.bridgeChannel.sendMessage(template
                .replace(playerName, Utils.sanitisedPlayerName(player))
                .replace(advancement, MarkdownSanitizer.escape(display.getTitle().getString()))
        ).queue();
    }
}
