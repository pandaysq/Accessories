package ru.core.accessories.state;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Подключает ru.core.mana.api.ManaAPI через ServicesManager и reflection без
 * compileOnly-зависимости. Используется EffectEngine; при отсутствии Mana
 * вызывающий код выбирает NoManaProvider.
 */
public final class ManaBridge implements ManaProvider {
    private final JavaPlugin plugin;
    private final Object api;
    private final Method getMana;
    private final Method getRegen;
    private final Method addMax;
    private final Method removeMax;
    private final Method addRegen;
    private final Method removeRegen;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public ManaBridge(JavaPlugin plugin) {
        this.plugin = plugin;
        Object found = null;
        try {
            for (Class<?> serviceClass : Bukkit.getServicesManager().getKnownServices()) {
                if (!serviceClass.getName().equals("ru.core.mana.api.ManaAPI")) continue;
                RegisteredServiceProvider registration =
                        Bukkit.getServicesManager().getRegistration((Class) serviceClass);
                if (registration != null) found = registration.getProvider();
                break;
            }
        } catch (LinkageError exception) {
            plugin.getLogger().info("Mana API недоступен: " + exception.getMessage());
        }
        api = found;
        getMana = method("getMana", Player.class);
        getRegen = method("getRegenPerSecond", Player.class);
        addMax = method("addMaxModifier", Player.class, String.class, double.class);
        removeMax = method("removeMaxModifier", Player.class, String.class);
        addRegen = method("addRegenModifier", Player.class, String.class, double.class);
        removeRegen = method("removeRegenModifier", Player.class, String.class);
    }

    public boolean available() { return api != null; }

    private Method method(String name, Class<?>... parameters) {
        if (api == null) return null;
        try { return api.getClass().getMethod(name, parameters); }
        catch (NoSuchMethodException exception) {
            plugin.getLogger().warning("ManaAPI не содержит метод " + name);
            return null;
        }
    }

    public double getMana(Player player) { return number(getMana, player); }
    public double getManaRegen(Player player) { return number(getRegen, player); }

    private double number(Method method, Player player) {
        if (method == null) return 0.0;
        try { return ((Number) method.invoke(api, player)).doubleValue(); }
        catch (IllegalAccessException | InvocationTargetException exception) { return 0.0; }
    }

    public void addMaxModifier(Player player, String key, double amount) { invoke(addMax, player, key, amount); }
    public void removeMaxModifier(Player player, String key) { invoke(removeMax, player, key); }
    public void addRegenModifier(Player player, String key, double amount) { invoke(addRegen, player, key, amount); }
    public void removeRegenModifier(Player player, String key) { invoke(removeRegen, player, key); }

    private void invoke(Method method, Object... arguments) {
        if (method == null) return;
        try { method.invoke(api, arguments); }
        catch (IllegalAccessException | InvocationTargetException ignored) {}
    }
}