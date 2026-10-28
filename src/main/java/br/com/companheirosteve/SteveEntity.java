package br.com.companheirosteve;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.world.World;

public class SteveEntity extends PathAwareEntity {

    public SteveEntity(
        EntityType<? extends PathAwareEntity> type,
        World world
    ) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder criarAtributos() {

        return PathAwareEntity.createMobAttributes()

            .add(
                EntityAttributes.MAX_HEALTH,
                20.0
            )

            .add(
                EntityAttributes.MOVEMENT_SPEED,
                0.30
            )

            .add(
                EntityAttributes.ATTACK_DAMAGE,
                1.0
            )

            .add(
                EntityAttributes.FOLLOW_RANGE,
                32.0
            );
    }
}
