package ru.core.accessories.manager;

import ru.core.accessories.api.AccessoriesAPI;
import ru.core.accessories.config.AccessoryConfig;
import ru.core.accessories.config.MessageManager;
import ru.core.accessories.effect.EffectEngine;
import ru.core.accessories.integration.LibraryBridge;
import ru.core.accessories.state.PlayerStateTracker;
import ru.core.accessories.ui.AccessoryMenu;
import ru.core.accessories.ui.InterfaceItems;

/**
 * Контейнер сервисов Accessories.
 * Используется командами, слушателями и планировщиком эффектов.
 * Новый общий сервис сначала добавляется сюда и создаётся в PluginBootstrap.
 */
public final class PluginServices {
    private final AccessoryConfig config;
    private final MessageManager messages;
    private final LibraryBridge library;
    private final InterfaceItems interfaceItems;
    private final AccessoryMenu menu;
    private final PlayerStateTracker state;
    private final EffectEngine effects;
    private final AccessoriesAPI api;

    public PluginServices(AccessoryConfig config, MessageManager messages, LibraryBridge library,
                          InterfaceItems interfaceItems, AccessoryMenu menu,
                          PlayerStateTracker state, EffectEngine effects, AccessoriesAPI api) {
        this.config = config;
        this.messages = messages;
        this.library = library;
        this.interfaceItems = interfaceItems;
        this.menu = menu;
        this.state = state;
        this.effects = effects;
        this.api = api;
    }

    public AccessoryConfig config() { return config; }
    public MessageManager messages() { return messages; }
    public LibraryBridge library() { return library; }
    public InterfaceItems interfaceItems() { return interfaceItems; }
    public AccessoryMenu menu() { return menu; }
    public PlayerStateTracker state() { return state; }
    public EffectEngine effects() { return effects; }
    public AccessoriesAPI api() { return api; }

    public void shutdown() {
        effects.shutdown();
        org.bukkit.Bukkit.getServicesManager().unregister(AccessoriesAPI.class, api);
    }
}