# Accessories

Плагин для Paper 1.21.11 и Java 21. Он добавляет четыре слота аксессуаров: ожерелье, эмблему и два кольца. Предметы регистрируются и создаются через `CustomItemsLibrary`, а свойства экипировки, условия и эффекты читаются из конфигурации.

## Зависимость

Файл библиотеки должен находиться по пути:

```text
libs/CustomItemsLibrary-1_0_0.jar
```

Gradle подключает его как `compileOnly`, а `plugin.yml` требует установленный на сервере плагин `CustomItemsLibrary`. Вызовы библиотеки находятся только в `LibraryBridge`.

## Сборка и установка

```bash
./gradlew shadowJar
```

Готовый файл появится в `build/libs/Accessories-1.0.0.jar`. Перед запуском сервера:

1. Положите `CustomItemsLibrary-1_0_0.jar` и собранный `Accessories-1.0.0.jar` в `plugins/`.
2. Убедитесь, что `CustomItemsLibrary` загружается раньше Accessories.
3. Установите ресурс-пак из папки `resourcepack/`.
4. В `server.properties` укажите `resource-pack` и SHA-1 архива.

## Команды и права

`/accessories reload` перечитывает оба YAML, удаляет из памяти библиотеки старые ID, регистрирует все ID заново и пересчитывает эффекты онлайн-игроков.

`/accessories give <игрок> <id> [количество]` создаёт предмет через `ItemRegistry.createItem`. Обе подкоманды требуют `accessories.admin`.

Права слотов:

| Слот | Право | Тип |
| --- | --- | --- |
| necklace | `accessory.slot.necklace` | `NECKLACE` |
| emblem | `accessory.slot.emblem` | `EMBLEM` |
| ring1 | `accessory.slot.ring1` | `RING` |
| ring2 | `accessory.slot.ring2` | `RING` |

Право проверяется через `Player.hasPermission()`, поэтому подходят LuckPerms и стандартная Bukkit-система прав.

## Как работает меню

Кнопка `interface.button` восстанавливается в слот 1 инвентаря игрока при входе и после закрытия инвентаря. Она помечена PDC `accessories:ui`; такой предмет нельзя вынуть, выбросить, использовать в крафте или сохранить как дроп смерти.

Меню имеет 9 слотов. Рабочие слоты находятся в позициях 1, 3, 5 и 7. Остальные заполнены фоном. При отсутствии права показывается `slot_locked`, при пустом разрешённом слоте — силуэт. Обработчик отменяет обычные клики, shift-click, drag, double-click и hotbar swap до проверки типа предмета.

Предметы слотов хранятся в PDC игрока через `ItemStack.serializeAsBytes()`. В собственной метке аксессуара нет: единственный ID читается через `CustomItemsLibrary.getCustomId(ItemStack)`. Это позволяет менять свойства конфига без перевыдачи уже существующих предметов.

## Формат аксессуара

Описания находятся в `accessories.yml`:

```yaml
accessories:
  ruby_ring:
    name: '<red>Рубиновое кольцо'
    lore:
      - '<gray>Даёт силу'
    type: RING
    material: STICK
    model-number: 1001
    death-keep-chance: 25
    effects:
      - group: 1
        mode: all
        duration-after-trigger: 0
        conditions:
          - type: health
            value: '<=8'
        attributes:
          - attribute: ATTACK_DAMAGE
            operation: ADD_NUMBER
            amount: 2
        potion-effects:
          - type: STRENGTH
            amplifier: 0
            particles: false
            icon: false
```

`mode: all` требует выполнения всех условий, `mode: any` — хотя бы одного. Группа без условий всегда активна, пока аксессуар лежит в разрешённом слоте.

Поддерживаются условия состояния: `health`, `food_level`, `saturation`, `armor_points`, `armor_pieces_count`, `debuff_count`, `effect_count`, `no_damage_for`, `not_on_ground`, `swimming`, `in_water`, `sprinting`, `sneaking`, `gliding`, `on_fire`, `riding`, `blocking`, `world`, `time_of_day`, `y_level`, `light_level`, `experience_level`, `mana`, `mana_regen`. Для маны используется `NoManaProvider`, который всегда возвращает ноль до отдельной интеграции.

Сравнение чисел поддерживает `8`, `>=8`, `<=8`, `>8`, `<8` и диапазон `8..12`. События `killed_player`, `killed_mob`, `killed_any`, `died` записываются слушателями и должны использовать положительный `duration-after-trigger`.

Атрибуты поддерживают `ADD_NUMBER`, `ADD_SCALAR`, `MULTIPLY_SCALAR_1`. Для каждого сочетания слота, ID аксессуара и группы создаётся собственный `NamespacedKey`, поэтому модификаторы снимаются точно при смене условия или предмета.

## Добавление нового аксессуара

1. Добавьте новый ID в `accessories.yml`.
2. Укажите `type`, базовый `material`, `model-number`, MiniMessage-имя и lore.
3. При необходимости добавьте группы `effects`.
4. Добавьте модель и текстуру в `resourcepack/assets/core/models/item/accessories/` и `resourcepack/assets/core/textures/item/accessories/`.
5. Выполните `/accessories reload`.
6. Проверьте выдачу командой `/accessories give <игрок> <id>`.

Свойства нужно менять только в YAML. Уже выданный предмет хранит только ID библиотеки, поэтому изменение эффекта, права, типа слота или шанса смерти применяется без перевыдачи.

## Добавление нового условия

1. Создайте класс, реализующий `Condition`.
2. Получите состояние игрока из `Player` и `PlayerStateTracker.State`.
3. Зарегистрируйте тип в `ConditionRegistry.create`.
4. Добавьте пример в этот README и проверьте отрицательный случай.

Для числовых условий используйте `NumberMatcher`. Для событий добавьте запись через `PlayerStateTracker.recordEvent` в слушателе.

## Добавление нового атрибута, эффекта или слота

- Атрибут: добавьте имя в `AttributeRegistry.attribute`.
- Операция: добавьте значение в `AttributeRegistry.operation`.
- Зелье: оно читается по имени `PotionEffectType` в `EffectEngine`.
- Слот: добавьте имя в `AccessoryStorage.SLOT_NAMES`, индекс в `AccessoryMenu.SLOT_INDEXES`, право и тип в `config.yml`, затем обновите обработку визуального силуэта в `InterfaceItems`.

## Ресурс-пак

В проекте приведены JSON-модели и список ожидаемых PNG-файлов. Текстуры намеренно не включены: их рисует автор ресурспака. Используется формат `range_dispatch` для `custom_model_data`, доступный в линейке 1.21.4+.

Для 1.21.11 указан `pack_format: 75`. Если Mojang изменит формат в следующем минорном релизе, поменяйте только значение в `resourcepack/pack.mcmeta`.