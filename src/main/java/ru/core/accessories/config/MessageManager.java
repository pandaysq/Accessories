package ru.core.accessories.config;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

/**
 * Отправляет короткие служебные сообщения через MiniMessage.
 * Используется командой и обработчиками ошибок.
 * Новые сообщения добавляются как методы или ключи передаваемого текста, без логики в слушателях.
 */
public final class MessageManager {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public void send(Player player, String message) {
        player.sendMessage(miniMessage.deserialize(message));
    }

    public String render(String message) {
        return miniMessage.deserialize(message).toString();
    }
}