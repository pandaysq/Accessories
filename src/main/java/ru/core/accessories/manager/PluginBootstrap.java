package ru.core.accessories.manager;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import ru.core.accessories.AccessoriesPlugin;
import ru.core.accessories.commands.AccessoriesCommand;
import ru.core.accessories.config.AccessoryConfig;
import ru.core.accessories.config.MessageManager;
import ru.core.accessories.effect.EffectEngine;
import ru.core.accessories.integration.LibraryBridge;
import ru.core.accessories.listener.AccessoryListener;
import ru.core.accessories.listener.PlayerListener;
import ru.core.accessories.state.PlayerStateTracker;
import ru.core.accessories.ui.AccessoryMenu;
import ru.core.accessories.ui.InterfaceItems;

/**
 * Собирает зависимости и регистрирует команды, слушатели и тикер.
 * Вызывается только из AccessoriesPlugin.
 * Новые зависимости и события подключаются здесь, чтобы main-класс оставался регистрационным.
 */
public final class PluginBootstrap {
    private final AccessoriesPlugin plugin;

    public PluginBootstrap(AccessoriesPlugin plugin) {
        this.plugin = plugin;
    }

    public PluginServices register() {
        AccessoryConfig config = new AccessoryConfig(plugin);
        MessageManager messages = new MessageManager();
        LibraryBridge library = new LibraryBridge();
        InterfaceItems interfaceItems = new InterfaceItems(plugin, config);
        PlayerStateTracker state = new PlayerStateTracker(plugin, config);
        AccessoryMenu menu = new AccessoryMenu(plugin, config, library, interfaceItems);
        EffectEngine effects = new EffectEngine(plugin, config, library, state);
        PluginServices services = new PluginServices(config, messages, library, interfaceItems, menu, state, effects);

        effects.start();
        registerCommand(new AccessoriesCommand(plugin, services));
        Bukkit.getPluginManager().registerEvents(new AccessoryListener(plugin, services), plugin);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(plugin, services), plugin);
        config.reloadAndRegister(library);
        return services;
    }

    private void registerCommand(AccessoriesCommand executor) {
        PluginCommand command = plugin.getCommand("accessories");
        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }
}