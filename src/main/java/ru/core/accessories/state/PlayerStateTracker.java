package ru.core.accessories.state;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.core.accessories.config.AccessoryConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранит лёгкое состояние игроков, необходимое условиям и событиям.
 * Используется слушателями событий и ConditionRegistry, не выполняет тяжёлые операции.
 * Новое событие добавляется в recordEvent и проверяется через eventActive.
 */
public final class PlayerStateTracker {
    private final JavaPlugin plugin;
    private final AccessoryConfig config;
    private final Map<UUID, State> states = new ConcurrentHashMap<>();

    public PlayerStateTracker(JavaPlugin plugin, AccessoryConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public State state(Player player) {
        return states.computeIfAbsent(player.getUniqueId(), ignored -> new State());
    }

    public void remove(Player player) {
        states.remove(player.getUniqueId());
    }

    public void damage(Player player) {
        state(player).lastDamageAt = System.currentTimeMillis();
    }

    public void hit(Player player, org.bukkit.entity.Entity target) {
        State state = state(player);
        state.lastHit = target;
        state.lastHitAt = System.currentTimeMillis();
    }

    public void move(Player player) {
        state(player).lastMoveAt = System.currentTimeMillis();
    }

    public void recordEvent(Player player, String event) {
        State state = state(player);
        state.eventStarted.put(event, System.currentTimeMillis());
        state.eventEntityTypes.remove(event);
        state.events.put(event, System.currentTimeMillis()
                + Math.max(1, config.main().getInt("settings.event-trigger-seconds", 60)) * 1000L);
    }

    public void recordEvent(Player player, String event, String entityType) {
        State state = state(player);
        state.eventStarted.put(event, System.currentTimeMillis());
        state.eventEntityTypes.put(event, entityType);
        state.events.put(event, System.currentTimeMillis()
                + Math.max(1, config.main().getInt("settings.event-trigger-seconds", 60)) * 1000L);
    }

    public boolean eventActive(Player player, String event) {
        return state(player).eventActive(event);
    }

    public static final class State {
        private long lastDamageAt;
        private long lastHitAt;
        private long lastMoveAt;
        private org.bukkit.entity.Entity lastHit;
        private final Map<String, Long> events = new ConcurrentHashMap<>();
        private final Map<String, Long> eventStarted = new ConcurrentHashMap<>();

        public long lastDamageAt() { return lastDamageAt; }
        public long lastHitAt() { return lastHitAt; }
        public long lastMoveAt() { return lastMoveAt; }
        public org.bukkit.entity.Entity lastHit() { return lastHit; }
        public boolean eventActive(String event) {
            return eventStarted.containsKey(event);
        }
        public boolean eventActive(String event, String entityType) {
            if (!eventActive(event)) return false;
            return entityType == null || entityType.isBlank()
                    || entityType.equalsIgnoreCase(eventEntityTypes.get(event));
        }
        public long eventTriggeredAt(String event) {
            return eventStarted.getOrDefault(event, 0L);
        }
        private final Map<String, String> eventEntityTypes = new ConcurrentHashMap<>();
    }
}