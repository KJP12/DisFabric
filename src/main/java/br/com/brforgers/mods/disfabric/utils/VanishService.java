package br.com.brforgers.mods.disfabric.utils;

import br.com.brforgers.mods.disfabric.listeners.MinecraftEventListener;
import me.drex.vanish.api.VanishAPI;
import me.drex.vanish.api.VanishEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Octal
 * */
public class VanishService {
    public static boolean isPlayerVanished(ServerPlayerEntity player) {
        if (!FabricLoader.getInstance().isModLoaded("melius-vanish"))
            return false;
        return VanishAPI.isVanished(player);
    }

    public static void listen() {
        if (FabricLoader.getInstance().isModLoaded("melius-vanish"))
            VanishEvents.VANISH_EVENT.register(MinecraftEventListener::onPlayerVanishChange);
    }
}
