package ru.core.accessories;

import org.bukkit.plugin.java.JavaPlugin;
import ru.core.accessories.manager.PluginBootstrap;
import ru.core.accessories.manager.PluginServices;

/**
 * Точка входа Accessories.
 * Используется Paper при включении и выключении плагина.
 * Новые глобальные сервисы добавляются в PluginBootstrap, а не в этот класс.
 */
public final class AccessoriesPlugin extends JavaPlugin {
    private PluginServices services;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("accessories.yml", false);
        services = new PluginBootstrap(this).register();
    }

    @Override
    public void onDisable() {
        if (services != null) {
            services.shutdown();
        }
    }

    public PluginServices services() {
        return services;
    }
}