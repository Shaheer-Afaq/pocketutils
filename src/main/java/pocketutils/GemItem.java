package pocketutils;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.debug.DebugValueSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.*;

public class GemItem extends Item {

    private final Holder<MobEffect>  effect;
    private final int amplifier;
    private String name = "";
    public static List<String> areUsingAbility = new ArrayList<>();
    public GemItem(Properties settings, Holder<MobEffect> effect, int amplifier, String name) {
        super(settings);
        this.effect = effect;
        this.amplifier = amplifier;
        this.name = name;
    }
    static {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {

            if (source.is(DamageTypes.FALL) && (entity instanceof Player player) && isGemActive(player, "gravity")!=null && player.level() instanceof ServerLevel world) {
                world.playSound(null,  entity.getX(), entity.getY(), entity.getZ(), SoundEvents.MACE_SMASH_AIR, SoundSource.PLAYERS,1.0F,1.0F);
                world.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, entity.getX(), entity.getY(), entity.getZ(), 1, 0, 0, 0, 0);
                world.sendParticles(ParticleTypes.EXPLOSION, entity.getX(), entity.getY(), entity.getZ(), 1, 0, 0, 0, 0);

                List<LivingEntity> nearbyList = world.getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(3.0, 0.0, 3.0)
                );
                for (LivingEntity e : nearbyList) {
                    float smashDamage = (amount * 0.7f < 30)? amount * 0.7f : 30;
                    if (e != player){
                        e.hurtServer(world, new DamageSource(world.registryAccess().get(DamageTypes.MACE_SMASH).get()), smashDamage);
                        circularKnockback(e, player, smashDamage*0.04, smashDamage*0.02, smashDamage*0.04);
                    }
                }
                return false;
            }

            if ((source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MACE_SMASH)) && source.getEntity() instanceof Player attacker) {
                final Random RANDOM = new Random();

                if (entity.level() instanceof ServerLevel world){
                    if (RANDOM.nextInt(100) < 30 && isGemActive(attacker, "magic") != null) {
                        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(world, EntitySpawnReason.TRIGGERED);
                        if (lightning != null) {
                            lightning.setPos(entity.getX(), entity.getY(), entity.getZ());
                            world.addFreshEntity(lightning);
                        }
                    }
                    else if(RANDOM.nextInt(100) < 40 && isGemActive(attacker, "health")!=null) {
                        Objects.requireNonNull(source.getEntity().asLivingEntity()).heal((float) (amount*0.2));
                        world.playSound(null,  entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,1.0F,1.0F);
                    }
                }
            }

            if ((source.is(DamageTypes.LIGHTNING_BOLT) || source.is(DamageTypes.MAGIC)) && entity instanceof Player player && isGemActive(player, "magic")!=null) {
                return false;
            }

            return true;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClientSide()) return InteractionResult.PASS;
            ItemStack stack = player.getMainHandItem();
            ItemStack stackOff = player.getOffhandItem();
            if (player.isShiftKeyDown() && stackOff.getItem() instanceof GemItem itemOff && !player.getCooldowns().isOnCooldown(stackOff)) {
                areUsingAbility.add(itemOff.name);
                return InteractionResult.FAIL;

            }else if (stack.getItem() instanceof GemItem item && !player.getCooldowns().isOnCooldown(stack)){
                areUsingAbility.add(item.name);
                return InteractionResult.FAIL;

            }
            return  InteractionResult.PASS;
        });
//            if (!(projectile instanceof Snowball snowball)) return;
//            if (!(snowball.level() instanceof ServerLevel world)) return;
//
//            world.explode(
//                    snowball,
//                    snowball.getX(),
//                    snowball.getY(),
//                    snowball.getZ(),
//                    2.5F,     // explosion power
//                    Level.ExplosionInteraction.NONE // no block damage
//            );
//
//            snowball.discard(); // remove projectile
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        if (world.isClientSide()) return;
        if (!(entity instanceof Player player)) return;

        if (isGemActive(player, name)!=null) {
            if (effect != null) {
                player.addEffect(new MobEffectInstance(effect, 10, amplifier, false, true));
            }
        }

        if (!areUsingAbility.isEmpty() && areUsingAbility.contains(name)) {
            if (name.equals("fire")){
                player.getCooldowns().addCooldown(stack, 300);
                List<ParticleOptions> particleList = Arrays.asList(
                        ParticleTypes.FLAME,
                        ParticleTypes.SOUL_FIRE_FLAME,
                        ParticleTypes.SMOKE,
                        ParticleTypes.LAVA,
                        ParticleTypes.CAMPFIRE_COSY_SMOKE
                );
                spawnParticleRing(world, player, 3.0, 6, 30, particleList);
                List<LivingEntity> nearbyList = world.getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(3.0, 0.0, 3.0)
                );
                world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
                world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BLAZE_DEATH, SoundSource.PLAYERS, 1.0F, 1.0F);
                for (LivingEntity e : nearbyList) {
                    if (e != player) {
                        circularKnockback(e, player, 1.4, 0.0, 1.4);
                        e.setRemainingFireTicks(200);
                    }
                }
            }
            if (name.equals("magic")){
                player.getCooldowns().addCooldown(stack, 200);
                spawnFangs(player, 10, 2);
            }
            if (name.equals("health")){
//                player.getCooldowns().addCooldown(stack, 600);
                player.getCooldowns().addCooldown(stack, 100);
                List<ParticleOptions> particleList = List.of(
                        new DustParticleOptions(0xF81139, 4.0f)
                );
                spawnParticleRing(world, player, 3.0, 1, 100, particleList);
                world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.PLAYERS, 1.0F, 1.0F);

                player.heal(10);
                List<LivingEntity> nearbyList = world.getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(3.0, 1.0, 3.0)
                );
                for (LivingEntity e : nearbyList) {
                    if (e != player){
                        e.heal(6);
                        world.sendParticles(ParticleTypes.HEART, e.getX(), e.getBoundingBox().maxY + 0.2, e.getZ(), 1, 0, 0, 0, 0);
                    }
                }
            }
            if (name.equals("gravity")){
                world.playSound(null,  entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS,1.0F,1.0F);
                player.getCooldowns().addCooldown(stack, 100);
                Vec3 vel = player.getDeltaMovement();
                player.setDeltaMovement(vel.x, 3.0, vel.z);
                player.hurtMarked = true;
            }
            if (name.equals("mining")){
                player.getCooldowns().addCooldown(stack, 20);
                throwExplodingSnowball(player);
            }
            areUsingAbility.remove(name);
        }
    }

    public static ItemStack isGemActive(Player player, String effectId) {
        if (isGem(player.getMainHandItem(), effectId)) return player.getMainHandItem();
        if (isGem(player.getOffhandItem(), effectId)) return player.getOffhandItem();
        return null;
    }
    public static boolean isGem(ItemStack stack, String effectId) {
        return stack.getItem() instanceof GemItem gem && effectId.equals(gem.name);
    }
    public static void circularKnockback(LivingEntity e, Player player, double ax, double ay, double az){
        double dx = e.getX() - player.getX();
        double dz = e.getZ() - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        dx /= dist;
        dz /= dist;
        e.push(dx * ax, ay, dz * az);
    }
    public static void spawnParticleRing(ServerLevel world, Player player, double maxRadius, int steps, int points, List<ParticleOptions> particles) {

        Random rand = new Random();

        double centerX = player.getX();
        double centerY = player.getY() + 0.5;
        double centerZ = player.getZ();

        for (int t = 1; t <= steps; t++) {
            double radius = maxRadius * t / steps;

            for (int i = 0 ; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                double x = centerX + radius * Math.cos(angle) + ((rand.nextDouble() - 0.5) * 0.7);
                double z = centerZ + radius * Math.sin(angle) + ((rand.nextDouble() - 0.5) * 0.7);
                double y = centerY + (rand.nextDouble() - 0.5) * 0.2;

                for (ParticleOptions particle : particles) {
                    world.sendParticles(particle, x, y, z, 1, 0, 0, 0, 0.0);
                }
            }
        }
    }

    public static void spawnFangs(Player player, int rings, int tickDelay) {
        if (!(player.level() instanceof ServerLevel world)) return;
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0F, 1.0F);

        MinecraftServer server = world.getServer();

        final int[] currentRing = {1};
        final int[] tickCounter = {0};
        double direction = -Math.atan2(
                player.getHeadLookAngle().x,
                player.getHeadLookAngle().z
        ) + Math.PI / 2;
        Vector3d position = new Vector3d(player.getX(), player.getY(), player.getZ());

        ServerTickEvents.END_SERVER_TICK.register(server1 -> {
            if (server1 != server) return;

            tickCounter[0]++;

            if (tickCounter[0] < tickDelay) return;
            tickCounter[0] = 0;

            if (currentRing[0] > rings) return;

            spawnFangRing(player, currentRing[0], direction, position);
            currentRing[0]++;
        });

    }
    private static void spawnFangRing(Player player, int i, double direction, Vector3d position) {
        ServerLevel world = (ServerLevel) player.level();
        float scope = 0.3f;
        float density = 0.6f;

        for (int j = 0; j < i; j++) {
            double t = (i == 1) ? 0.5 : (double) j / (i - 1);
            double angle = direction + (t - 0.5) * scope;

            EvokerFangs fang = EntityType.EVOKER_FANGS.create(world, EntitySpawnReason.TRIGGERED);
            assert fang != null;
            fang.setPos(position.x + i * density * Math.cos(angle), position.y, position.z + i * density * Math.sin(angle));
            world.addFreshEntity(fang);
        }
    }

    public static void throwExplodingSnowball(Player player) {

        if (!(player.level() instanceof ServerLevel world)) return;
        Snowball snowball = EntityType.SNOWBALL.create(world, EntitySpawnReason.TRIGGERED);
        if (snowball != null) {
            snowball.setOwner(player);
            snowball.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            snowball.shootFromRotation(
                    player,
                    player.getXRot(),
                    player.getYRot(),
                    0.0F,
                    1.5F,
                    0.5F
            );
            world.addFreshEntity(snowball);
        }
    }

}