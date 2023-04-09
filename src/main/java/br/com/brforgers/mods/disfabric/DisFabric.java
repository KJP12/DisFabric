package br.com.brforgers.mods.disfabric;

import br.com.brforgers.mods.disfabric.commands.EmoteCommand;
import br.com.brforgers.mods.disfabric.commands.ReportCommand;
import br.com.brforgers.mods.disfabric.commands.SuggestCommand;
import br.com.brforgers.mods.disfabric.listeners.DiscordEventListener;
import br.com.brforgers.mods.disfabric.listeners.MinecraftEventListener;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import kong.unirest.Unirest;
import me.sargunvohra.mcmods.autoconfig1u.AutoConfig;
import me.sargunvohra.mcmods.autoconfig1u.serializer.JanksonConfigSerializer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.command.CommandManager;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class DisFabric implements DedicatedServerModInitializer {

    public static final String MOD_ID = "disfabric";
    public static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setDaemon(true).build());
    public static final Logger logger = LogManager.getLogger(MOD_ID);
    public static Configuration config;
    public static JDA jda;
    public static long selfWebhook;
    public static Guild guild;
    public static GuildMessageChannel bridgeChannel;
    @Nullable
    public static GuildMessageChannel bugReportChannel, userReportChannel, suggestionChannel;

    public static boolean stop = false;

    @Override
    public void onInitializeServer() {
        AutoConfig.register(Configuration.class, JanksonConfigSerializer::new);
        {
            final var holder = AutoConfig.getConfigHolder(Configuration.class);
            config = holder.getConfig();

            boolean save = config.migrateAdmins();
            save |= config.migrateBridgeChannel();

            if (save) {
                holder.save();
            }
        }

        if (config.isWebhookEnabled) {
            if (config.webhookURL == null || config.webhookURL.isBlank()) {
                logger.error("Webhook is not set. Falling back to a regular message. Please set a webhook URL in ~/config/disfabric.json5");

                config.isWebhookEnabled = false;
            } else {
                final var url = Webhook.WEBHOOK_URL.matcher(config.webhookURL);
                selfWebhook = Long.parseUnsignedLong(url.group("id"));
            }
        }
        try {
            if (config.botToken == null || config.botToken.isBlank()) {
                logger.error("Unable to login. Please setup the config at ~/config/disfabric.json5");
            } else {
                JDABuilder builder = JDABuilder.createDefault(config.botToken).setHttpClient(new OkHttpClient.Builder()
                                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                                .build())
                        .setChunkingFilter(ChunkingFilter.ALL)
                        .addEventListeners(new DiscordEventListener())
                        .setMemberCachePolicy(MemberCachePolicy.ALL)
                        .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT);
                DisFabric.jda = builder.build();
                DisFabric.jda.awaitReady();
                DisFabric.bridgeChannel = requireMessageChannel(DisFabric.jda.getGuildChannelById(config.bridgeChannel), "No such bridge channel");
                bugReportChannel = maybeMessageChannel(jda.getGuildChannelById(config.bugReportChannel));
                userReportChannel = maybeMessageChannel(jda.getGuildChannelById(config.userReportChannel));
                suggestionChannel = maybeMessageChannel(jda.getGuildChannelById(config.suggestionChannel));
                DisFabric.guild = DisFabric.bridgeChannel.getGuild();
            }
        } catch (InvalidTokenException | InterruptedException ex) {
            jda = null;
            DisFabric.logger.error("Unable to login!", ex);
        }
        if(jda != null) {
            if (!config.botGameStatus.isEmpty())
                jda.getPresence().setActivity(Activity.playing(config.botGameStatus));

            if (!config.commandsOnly) {
                ServerLifecycleEvents.SERVER_STARTED.register((server) -> bridgeChannel.sendMessage(DisFabric.config.texts.serverStarted).queue());
            }

            ServerLifecycleEvents.SERVER_STOPPING.register((server) -> {
                stop = true;
                if (!config.commandsOnly) {
                    bridgeChannel.sendMessage(DisFabric.config.texts.serverStopped).complete();
                }
                Unirest.shutDown();
                DisFabric.jda.shutdownNow();
            });
            MinecraftEventListener.init();

        }
        var event = CommandRegistrationCallback.EVENT;
        event.register(new EmoteCommand("shrug", EmoteCommand.SHRUG));
        event.register(new EmoteCommand("tableflip", EmoteCommand.TABLE_FLIP));
        event.register(new EmoteCommand("unflip", EmoteCommand.UNFLIP));
        event.register((dispatcher, registry, environment) -> {
            if (environment == CommandManager.RegistrationEnvironment.DEDICATED && jda != null) {
                ReportCommand.register(dispatcher);
                SuggestCommand.register(dispatcher);
            }
        });
    }

    public static boolean hasPermission(@Nullable GuildChannel channel, Permission... permissions) {
        return channel != null && channel.getGuild().getSelfMember().hasPermission(permissions);
    }

    // SameParameterValue - This is supposed to be generic; we don't care.
    @SuppressWarnings("SameParameterValue")
    private static GuildMessageChannel requireMessageChannel(Channel channel, String error) {
        if (channel instanceof GuildMessageChannel gmc) {
            return gmc;
        }
        throw new IllegalArgumentException(error);
    }

    @Nullable
    private static GuildMessageChannel maybeMessageChannel(Channel channel) {
        if (channel instanceof GuildMessageChannel gmc && gmc.canTalk()) {
            return gmc;
        }
        return null;
    }
}
