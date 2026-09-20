package ru.core.accessories.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import ru.core.accessories.manager.PluginServices;

import java.util.ArrayList;
import java.util.List;

/**
 * Обрабатывает /accessories reload и /accessories give.
 * Используется оператором для перезагрузки и выдачи зарегистрированных предметов.
 * Новая подкоманда добавляется в onCommand и список подсказок этого класса.
 */
public final class AccessoriesCommand implements org.bukkit.command.CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final PluginServices services;

    public AccessoriesCommand(JavaPlugin plugin, PluginServices services) {
        this.plugin = plugin;
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("accessories.admin")) {
            sender.sendMessage("§cНедостаточно прав.");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            services.config().reloadAndRegister(services.library());
            services.effects().reloadConfiguration();
            services.effects().refreshOnline();
            sender.sendMessage("§aAccessories перезагружен.");
            return true;
        }
        if (args.length >= 3 && args[0].equalsIgnoreCase("give")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§cИгрок не найден.");
                return true;
            }
            if (services.config().definition(args[2]) == null) {
                sender.sendMessage("§cАксессуар не найден в конфиге.");
                return true;
            }
            int amount = 1;
            if (args.length >= 4) {
                try {
                    amount = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
                } catch (NumberFormatException exception) {
                    sender.sendMessage("§cКоличество должно быть числом.");
                    return true;
                }
            }
            ItemStack item = services.library().create(args[2]).orElse(null);
            if (item == null) {
                sender.sendMessage("§cCustomItemsLibrary не смогла создать предмет.");
                return true;
            }
            item.setAmount(amount);
            target.getInventory().addItem(item);
            sender.sendMessage("§aАксессуар выдан игроку §f" + target.getName() + "§a.");
            return true;
        }
        sender.sendMessage("§eИспользование: /accessories reload или /accessories give <игрок> <id> [количество]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("accessories.admin")) return List.of();
        if (args.length == 1) return List.of("reload", "give");
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return new ArrayList<>(services.config().definitions().keySet());
        }
        return List.of();
    }
}