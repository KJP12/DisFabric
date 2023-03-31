package br.com.brforgers.mods.disfabric.utils;

import br.com.brforgers.mods.disfabric.DisFabric;
import net.fabricmc.loader.api.FabricLoader;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class HttpServices {
    public static final MediaType jsonMediaType = MediaType.get("application/json; charset=utf-8");

    public static final String USER_AGENT;

    static {
        final var self = FabricLoader.getInstance().getModContainer(DisFabric.MOD_ID).get().getMetadata();
        final var version = self.getVersion();
        USER_AGENT = "DisFabric " + version + " (+" + self.getContact().get("homepage").get() + ")";
    }

    public static final Consumer<Request.Builder> SET_UA = builder -> builder.addHeader("User-Agent", USER_AGENT);

    public static final OkHttpClient client = new OkHttpClient();

    public static MicroSerialRatelimiter createRatelimitedClient(String url, Consumer<Request.Builder> defaults,
                                                                 TimeUnit ratelimitUnits) {
        return new MicroSerialRatelimiter(DisFabric.scheduler, client, url, defaults, ratelimitUnits);
    }
}
