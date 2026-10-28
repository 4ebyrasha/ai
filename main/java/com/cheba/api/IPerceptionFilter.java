package com.cheba.api;

import net.minecraft.entity.Entity;

public interface IPerceptionFilter {
    /**
     * @param observer наблюдатель (обычно EntityPlayer — метатель/стрелок)
     * @return true — наблюдатель может воспринимать эту сущность
     */
    boolean isPerceivableBy(Entity observer);
}
