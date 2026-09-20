package ru.core.accessories.state;

import org.bukkit.entity.Player;

/**
 * Безопасный провайдер по умолчанию: у игрока нет доступной маны.
 * Используется до подключения внешнего mana-плагина.
 * При добавлении интеграции заменяется в PluginBootstrap, условия менять не нужно.
 */
public final class NoManaProvider implements ManaProvider {
    @Override
    public double getMana(Player player) { return 0; }

    @Override
    public double getManaRegen(Player player) { return 0; }
}