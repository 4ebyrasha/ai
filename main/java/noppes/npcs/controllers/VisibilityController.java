package noppes.npcs.controllers;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.EntityNPCInterface;

public class VisibilityController {
    public static VisibilityController instance = new VisibilityController();
    private Map<Integer, EntityNPCInterface> trackedEntityHashTable = new ConcurrentHashMap();

    public VisibilityController() {
        this.trackedEntityHashTable = new TreeMap();
    }

    public void trackNpc(EntityNPCInterface npc) {
        // Если конфиг включен
        if (CustomNpcs.EnableInvisibleNpcs) {
            boolean hasOptions = npc.display.hasVisibleOptions();
            // Добавляем в трекер, если есть условия ИЛИ если NPC изначально невидим
            if ((hasOptions || npc.display.getVisible() != 0) && !this.trackedEntityHashTable.containsKey(npc.getEntityId())) {
                this.trackedEntityHashTable.put(npc.getEntityId(), npc);
            }

            // Удаляем из трекера, если условий нет и он видим
            if (!hasOptions && npc.display.getVisible() == 0 && this.trackedEntityHashTable.containsKey(npc.getEntityId())) {
                this.trackedEntityHashTable.remove(npc.getEntityId());
            }
        }
    }

    public void onUpdate(EntityPlayerMP player) {
        if (CustomNpcs.EnableInvisibleNpcs) {
            Iterator<Map.Entry<Integer, EntityNPCInterface>> iterator = this.trackedEntityHashTable.entrySet().iterator();
            while(iterator.hasNext()) {
                Map.Entry<Integer, EntityNPCInterface> entry = iterator.next();
                checkIsVisible(entry.getValue(), player);
            }
        }
    }

    public static void checkIsVisible(EntityNPCInterface npc, EntityPlayerMP playerMP) {
        if (CustomNpcs.EnableInvisibleNpcs) {
            // Если игрок НЕ подходит по условиям, не креативен и не держит жезл
            if (!npc.display.isVisibleTo(playerMP) && !playerMP.isSpectator() && playerMP.getHeldItemMainhand().getItem() != CustomItems.wand) {
                npc.setInvisible(playerMP);
            } else {
                npc.setVisible(playerMP);
            }
        }
    }
}
