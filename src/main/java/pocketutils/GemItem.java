package pocketutils;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class GemItem extends Item {

    private final Holder<MobEffect>  effect;
    private final int amplifier;
    final String custom_effect;


    static {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof Player player)) return true;

            if (source.is(DamageTypes.FALL)) {
                if (isGemActive(player, "gravity")) {
                    return false;
                }
            }
            return true;
        });
    }

    public GemItem(Properties settings, Holder<MobEffect> effect, int amplifier, String custom_effect) {
        super(settings);
        this.effect = effect;
        this.amplifier = amplifier;
        this.custom_effect = custom_effect;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        if (world.isClientSide()) return;
        if (!(entity instanceof Player player)) return;

        if (stack == player.getMainHandItem() || stack == player.getOffhandItem()) {
            if (effect != null) {
                player.addEffect(new MobEffectInstance(effect, 10, amplifier, false, true));
            }
        }
    }

    public static boolean isGemActive(Player player, String effectId) {
        return isGem(player.getMainHandItem(), effectId)
                || isGem(player.getOffhandItem(), effectId);
    }
    private static boolean isGem(ItemStack stack, String effectId) {
        return stack.getItem() instanceof GemItem gem
                && effectId.equals(gem.custom_effect);
    }
}
