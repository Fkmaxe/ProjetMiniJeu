package fr.nexus.core.bootstrap;

import org.bukkit.plugin.java.JavaPlugin;

/** Point d'entrée Paper de NexusCore ; les services sont branchés par le bootstrap (P1-05). */
public final class NexusCorePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getComponentLogger().info("NexusCore {} enabled", getPluginMeta().getVersion());
    }

    @Override
    public void onDisable() {
        getComponentLogger().info("NexusCore disabled");
    }
}
