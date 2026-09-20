package ru.core.accessories.condition;

import org.bukkit.entity.Player;
import ru.core.accessories.config.ConditionDefinition;
import ru.core.accessories.state.ManaProvider;
import ru.core.accessories.state.PlayerStateTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Превращает типы условий из YAML в исполняемые объекты.
 * Используется EffectEngine для условий состояния, цели и событий.
 * Новое условие добавляется в create; сложную логику лучше вынести в отдельный класс.
 */
public final class ConditionRegistry {
    private final ManaProvider mana;
    private final int targetRayDistance;
    private final int targetLastHitSeconds;

    public ConditionRegistry(ManaProvider mana, int targetRayDistance, int targetLastHitSeconds) {
        this.mana = mana;
        this.targetRayDistance = targetRayDistance;
        this.targetLastHitSeconds = targetLastHitSeconds;
    }

    public List<Condition> create(List<ConditionDefinition> definitions) {
        List<Condition> result = new ArrayList<>();
        for (ConditionDefinition definition : definitions) {
            Condition condition = create(definition);
            if (condition != null) result.add(condition);
        }
        return result;
    }

    private Condition create(ConditionDefinition definition) {
        String type = definition.type().toLowerCase(Locale.ROOT);
        Map<String, Object> values = definition.values();
        String value = String.valueOf(values.getOrDefault("value", ""));
        return switch (type) {
            case "health" -> new HealthCondition(value);
            case "food_level" -> new FoodLevelCondition(value);
            case "saturation" -> numeric((player, state) -> player.getSaturation(), value);
            case "armor_points" -> numeric((player, state) -> player.getAttribute(
                    org.bukkit.attribute.Attribute.ARMOR) == null ? 0 : player.getAttribute(
                    org.bukkit.attribute.Attribute.ARMOR).getValue(), value);
            case "armor_pieces_count" -> numeric((player, state) -> (double) java.util.Arrays.stream(
                    player.getInventory().getArmorContents()).filter(item -> item != null && !item.getType().isAir()).count(), value);
            case "debuff_count" -> numeric((player, state) -> player.getActivePotionEffects().stream()
                    .filter(effect -> effect.getType().isBeneficial() == false).count(), value);
            case "effect_count" -> numeric((player, state) -> player.getActivePotionEffects().size(), value);
            case "experience_level" -> numeric((player, state) -> player.getLevel(), value);
            case "y_level" -> numeric((player, state) -> player.getY(), value);
            case "light_level" -> numeric((player, state) -> player.getLocation().getBlock().getLightLevel(), value);
            case "time_of_day" -> numeric((player, state) -> player.getWorld().getTime(), value);
            case "no_damage_for" -> numeric((player, state) -> state.lastDamageAt() == 0 ? Double.MAX_VALUE
                    : (System.currentTimeMillis() - state.lastDamageAt()) / 1000.0, value);
            case "not_on_ground", "swimming", "in_water", "sprinting", "sneaking", "gliding",
                 "on_fire", "riding", "blocking" -> new StateBooleanCondition(type,
                    Boolean.parseBoolean(value.isEmpty() ? "true" : value));
            case "world" -> new WorldCondition(value);
            case "mana" -> numeric((player, state) -> mana.getMana(player), value);
            case "mana_regen" -> numeric((player, state) -> mana.getManaRegen(player), value);
            case "armor_durability_total" -> numeric((player, state) -> durability(player, -1), value);
            case "armor_durability_slot" -> numeric((player, state) -> durability(player,
                    Integer.parseInt(String.valueOf(values.getOrDefault("slot", 0)))), value);
            case "low_air" -> numeric((player, state) -> player.getRemainingAir(), value);
            case "not_moving" -> numeric((player, state) -> state.lastMoveAt() == 0 ? Double.MAX_VALUE
                    : (System.currentTimeMillis() - state.lastMoveAt()) / 1000.0, value);
            case "target_debuff_count" -> numeric((player, state) -> targetEffects(state, false), value);
            case "target_effect_count" -> numeric((player, state) -> targetEffects(state, true), value);
            case "holding_item" -> (player, state) -> player.getInventory().getItemInMainHand().getType()
                    .name().equalsIgnoreCase(value);
            case "weather" -> (player, state) -> (player.getWorld().hasStorm() ? "storm" : "clear")
                    .equalsIgnoreCase(value);
            case "biome" -> (player, state) -> player.getLocation().getBlock().getBiome().name()
                    .equalsIgnoreCase(value);
            case "armor_slot_present" -> (player, state) -> armorSlotPresent(player, values);
            case "looking_at_player", "looking_at_mob" -> (player, state) -> lookingAt(player, type);
            case "killed_player", "killed_mob", "killed_any", "died" ->
                    (player, state) -> state.eventActive(type);
            default -> (player, state) -> false;
        };
    }

    private double durability(Player player, int slot) {
        org.bukkit.inventory.ItemStack[] armor = player.getInventory().getArmorContents();
        int start = slot < 0 ? 0 : slot;
        int end = slot < 0 ? armor.length : Math.min(armor.length, slot + 1);
        int total = 0;
        for (int index = start; index < end; index++) {
            org.bukkit.inventory.ItemStack item = armor[index];
            if (item != null && item.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable damageable) {
                total += item.getType().getMaxDurability() - damageable.getDamage();
            }
        }
        return total;
    }

    private double targetEffects(PlayerStateTracker.State state, boolean all) {
        if (!(state.lastHit() instanceof Player target)
                || System.currentTimeMillis() - state.lastHitAt() > targetLastHitSeconds * 1000L) return 0;
        return all ? target.getActivePotionEffects().size()
                : target.getActivePotionEffects().stream().filter(effect -> !effect.getType().isBeneficial()).count();
    }

    private boolean armorSlotPresent(Player player, Map<String, Object> values) {
        int slot = Integer.parseInt(String.valueOf(values.getOrDefault("slot", 0)));
        boolean expected = Boolean.parseBoolean(String.valueOf(values.getOrDefault("value", true)));
        org.bukkit.inventory.ItemStack[] armor = player.getInventory().getArmorContents();
        boolean present = slot >= 0 && slot < armor.length && armor[slot] != null && !armor[slot].getType().isAir();
        return present == expected;
    }

    private boolean lookingAt(Player player, String type) {
        org.bukkit.entity.Entity target = player.getTargetEntity(targetRayDistance);
        if (target == null) return false;
        return type.equals("looking_at_player") ? target instanceof Player
                : !(target instanceof Player) && target instanceof org.bukkit.entity.LivingEntity;
    }

    private Condition numeric(NumericValue value, String expression) {
        return (player, state) -> NumberMatcher.matches(value.get(player, state), expression);
    }

    @FunctionalInterface
    private interface NumericValue {
        double get(Player player, PlayerStateTracker.State state);
    }
}