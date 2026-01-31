package pocketutils;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class GemItem extends Item {

    private final Holder<MobEffect> effect;
    private final String custom_effect;

    public GemItem(Properties settings, Holder<MobEffect> effect, String custom_effect) {
        super(settings);
        this.effect = effect;
        this.custom_effect = custom_effect;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);

        if (world.isClientSide()) return;
        if (!(entity instanceof Player player)) return;

        if (player.getMainHandItem() == stack || player.getOffhandItem() == stack) {
            if (effect == null){
                switch (custom_effect) {
                    case "gravity" :
                        player.fallDistance = 0;
                        break;
                }
            }else{
                player.addEffect(new MobEffectInstance(effect, 1, 0, false, true));

            }


            //generic effects
        }
    }
}
