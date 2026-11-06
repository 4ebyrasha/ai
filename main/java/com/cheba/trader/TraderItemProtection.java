package com.cheba.trader;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Определяет GP-предметы для ограничений в окнах торговли.

 * Редакторы торговца этот класс для блокировки предметов не используют
 */
public final class TraderItemProtection {
    private static final String[] GP_MARKER_KEYS = {
            "GP",
            "gp"
    };

    /*
     * Эта capability используется только как вспомогательный способ
     * найти исходный слот предмета при аварийном возврате из буфера.
     * Она НЕ определяет, является ли предмет защищенным
     */
    private static final String PERSISTENT_ITEMS_CLASS =
            "com.cheba.stalkerbase.gndm.CapabilityPersistentItems";

    private TraderItemProtection() {
    }

    /**
     * Основной метод для торговых операций.
     * Защита определяется только GP-маркером в NBT
     */
    public static boolean isProtected(
            EntityPlayer player,
            ItemStack stack
    ) {
        return isGpItem(stack);
    }

    public static boolean hasProtectedNbt(ItemStack stack) {
        return isGpItem(stack);
    }

    /**
     * Проверка только GP-предмета
     */
    public static boolean isGpItem(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return false;
        }

        NBTTagCompound tag = stack.getTagCompound();

        if (tag == null) {
            return false;
        }

        for (String key : GP_MARKER_KEYS) {
            if (!tag.hasKey(key)) {
                continue;
            }

            byte tagType = tag.getTagId(key);

            /*
             * Boolean: GP=true — защищенный предмет
             */
            if (tagType == 1) {
                if (tag.getBoolean(key)) {
                    return true;
                }

                continue;
            }

            /*
             * Integer: ненулевое значение означает GP
             */
            if (tagType == 3) {
                if (tag.getInteger(key) != 0) {
                    return true;
                }

                continue;
            }

            /*
             * Long: ненулевое значение означает GP
             */
            if (tagType == 4) {
                if (tag.getLong(key) != 0L) {
                    return true;
                }

                continue;
            }

            /*
             * String: пустая строка, "false" и "0" не считаются
             * включенным маркером
             */
            if (tagType == 8) {
                String value = tag.getString(key).trim();

                if (!value.isEmpty()
                        && !"false".equalsIgnoreCase(value)
                        && !"0".equals(value)) {
                    return true;
                }

                continue;
            }

            /*
             * Если GP-маркер хранится как compound или другой
             * тип NBT, само его наличие считается маркером
             */
            return true;
        }

        return false;
    }

    /**
     * Возвращает исходный слот из capability StalkerBase
     * только для вспомогательного возврата предмета

     * Этот метод не участвует в определении GP и не запрещает
     * перемещать GND-предметы
     */
    public static int getPersistentSlot(
            EntityPlayer player,
            ItemStack stack
    ) {
        if (player == null || stack == null || stack.isEmpty()) {
            return -1;
        }

        try {
            Class<?> holder = Class.forName(
                    PERSISTENT_ITEMS_CLASS,
                    false,
                    TraderItemProtection.class.getClassLoader()
            );

            Field field = holder.getField("PERSISTENT_ITEMS_CAP");
            Object value = field.get(null);

            if (!(value instanceof Capability)) {
                return -1;
            }

            Capability<?> capability = (Capability<?>) value;

            if (!player.hasCapability(capability, null)) {
                return -1;
            }

            Object instance = getCapabilityInstance(player, capability);

            if (instance == null) {
                return -1;
            }

            Object valueItems = instance.getClass()
                    .getMethod("getItems")
                    .invoke(instance);

            if (!(valueItems instanceof List)) {
                return -1;
            }

            for (Object entry : (List<?>) valueItems) {
                if (entry == null) {
                    continue;
                }

                Object stored = entry.getClass()
                        .getMethod("getStack")
                        .invoke(entry);

                if (!(stored instanceof ItemStack)) {
                    continue;
                }

                if (!sameItemAndTag((ItemStack) stored, stack)) {
                    continue;
                }

                Object slotValue = entry.getClass()
                        .getMethod("getSlot")
                        .invoke(entry);

                if (slotValue instanceof Number) {
                    return ((Number) slotValue).intValue();
                }
            }

        } catch (ReflectiveOperationException
                 | RuntimeException
                 | LinkageError ignored) {
            // StalkerBase может отсутствовать или иметь другую реализацию.
        }

        return -1;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object getCapabilityInstance(
            EntityPlayer player,
            Capability<?> capability
    ) {
        return player.getCapability(
                (Capability) capability,
                (EnumFacing) null
        );
    }

    private static boolean sameItemAndTag(
            ItemStack first,
            ItemStack second
    ) {
        return first != null
                && second != null
                && !first.isEmpty()
                && !second.isEmpty()
                && ItemStack.areItemsEqual(first, second)
                && ItemStack.areItemStackTagsEqual(first, second);
    }
}
