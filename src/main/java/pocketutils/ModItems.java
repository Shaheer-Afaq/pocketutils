package pocketutils;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import java.util.function.Function;

public class ModItems {

    public static final Item SPEED_GEM = register(
            "speed_gem", props -> new GemItem(props, MobEffects.SPEED, ""), new Item.Properties().stacksTo(1));

    public static final Item STRENGTH_GEM = register(
            "strength_gem",props -> new GemItem(props, MobEffects.STRENGTH, ""), new Item.Properties().stacksTo(1));

     public static final Item FIRE_GEM = register(
            "fire_gem",props -> new GemItem(props, MobEffects.FIRE_RESISTANCE, ""), new Item.Properties().stacksTo(1));

     public static final Item HASTE_GEM = register(
            "haste_gem",props -> new GemItem(props, MobEffects.HASTE, ""), new Item.Properties().stacksTo(1));

    public static final Item GRAVITY_GEM = register(
            "gravity_gem",props -> new GemItem(props, null, "gravity"), new Item.Properties().stacksTo(1));

    static final Item GEM_FRAGMENT = register(
            "gem_fragment", props -> new GemItem(props, null, "gravity"), new Item.Properties().stacksTo(1));

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(itemGroup -> {
                    itemGroup.accept(ModItems.SPEED_GEM);
                    itemGroup.accept(ModItems.GRAVITY_GEM);
//                    itemGroup.accept(ModItems.HASTE_GEM);
//                    itemGroup.accept(ModItems.STRENGTH_GEM);
                });

    }
    public static <GenericItem extends Item> GenericItem register(String name, Function<Item.Properties, GenericItem> itemFactory, Item.Properties settings) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(PocketUtils.MOD_ID, name));
        GenericItem item = itemFactory.apply(settings.setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);
        return item;
    }

}
