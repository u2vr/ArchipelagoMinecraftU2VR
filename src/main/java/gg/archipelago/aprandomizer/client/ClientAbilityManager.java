package gg.archipelago.aprandomizer.client;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = APRandomizer.MODID, value = Dist.CLIENT)
public class ClientAbilityManager {

    private static int miningTier = 0;
    private static boolean treeFellerUnlocked = false;
    private static int jumpTier = 0;
    private static boolean itemMagnetUnlocked = false;
    private static boolean stepAssistUnlocked = false;
    private static boolean trashCanUnlocked = false;
    private static boolean lightHolderUnlocked = false;

    public static final int SLOT_TRASH_CAN = 0;
    public static final int SLOT_LIGHT_HOLDER = 1;

    private static int miningShapeIndex = 0;
    private static boolean treeFellerActive = true;
    private static boolean itemMagnetActive = true;
    private static boolean stepAssistActive = true;
    private static ItemStack trashCanItem = ItemStack.EMPTY;
    private static ItemStack lightHolderItem = ItemStack.EMPTY;

    public static void setTrashCanItem(ItemStack stack) { trashCanItem = stack != null ? stack : ItemStack.EMPTY; }
    public static void setLightHolderItem(ItemStack stack) { lightHolderItem = stack != null ? stack : ItemStack.EMPTY; }

    // Multi-jump tracking
    private static int airJumpsUsed = 0;
    private static boolean jumpKeyWasDown = false;

    public static void handleSync(SyncAbilitiesPayload payload) {
        miningTier = payload.miningTier();
        treeFellerUnlocked = payload.treeFellerUnlocked();
        jumpTier = payload.jumpTier();
        itemMagnetUnlocked = payload.itemMagnetUnlocked();
        stepAssistUnlocked = payload.stepAssistUnlocked();
        trashCanUnlocked = payload.trashCanUnlocked();
        lightHolderUnlocked = payload.lightHolderUnlocked();

        miningShapeIndex = payload.miningShapeIndex();
        treeFellerActive = payload.treeFellerActive();
        itemMagnetActive = payload.itemMagnetActive();
        stepAssistActive = payload.stepAssistActive();
        trashCanItem = payload.trashCanItem();
        lightHolderItem = payload.lightHolderItem();
    }

    public static int getMiningTier() { return miningTier; }
    public static boolean isTreeFellerUnlocked() { return treeFellerUnlocked; }
    public static int getJumpTier() { return jumpTier; }
    public static boolean isItemMagnetUnlocked() { return itemMagnetUnlocked; }
    public static boolean isStepAssistUnlocked() { return stepAssistUnlocked; }
    public static boolean isTrashCanUnlocked() { return trashCanUnlocked; }
    public static boolean isLightHolderUnlocked() { return lightHolderUnlocked; }

    public static int getMiningShapeIndex() { return miningShapeIndex; }
    public static boolean isTreeFellerActive() { return treeFellerActive; }
    public static boolean isItemMagnetActive() { return itemMagnetActive; }
    public static boolean isStepAssistActive() { return stepAssistActive; }
    public static ItemStack getTrashCanItem() { return trashCanItem; }
    public static ItemStack getLightHolderItem() { return lightHolderItem; }

    public static boolean hasAnyAbility() {
        return miningTier > 0 || treeFellerUnlocked || jumpTier > 0
                || itemMagnetUnlocked || stepAssistUnlocked || trashCanUnlocked || lightHolderUnlocked;
    }

    public static void cycleMining() {
        ClientPacketDistributor.sendToServer(new ToggleAbilityPayload(ToggleAbilityPayload.ACTION_CYCLE_MINING));
    }

    public static void toggleTreeFeller() {
        ClientPacketDistributor.sendToServer(new ToggleAbilityPayload(ToggleAbilityPayload.ACTION_TOGGLE_TREE_FELLER));
    }

    public static void toggleMagnet() {
        ClientPacketDistributor.sendToServer(new ToggleAbilityPayload(ToggleAbilityPayload.ACTION_TOGGLE_MAGNET));
    }

    public static void toggleStepAssist() {
        ClientPacketDistributor.sendToServer(new ToggleAbilityPayload(ToggleAbilityPayload.ACTION_TOGGLE_STEP_ASSIST));
    }

    public static void sendUpdateSlot(int slotType, ItemStack stack) {
        ClientPacketDistributor.sendToServer(new UpdateAbilitySlotPayload(slotType, stack));
    }

    public static void sendAltTrash(int slotId) {
        ClientPacketDistributor.sendToServer(new AltTrashItemPayload(slotId));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        boolean onGround = player.onGround();
        if (onGround || player.isInWater() || player.onClimbable() || player.getAbilities().flying || player.isPassenger()) {
            airJumpsUsed = 0;
        }

        boolean jumpKeyDown = mc.options.keyJump.isDown();
        if (jumpKeyDown && !jumpKeyWasDown) {
            // Jump key was just pressed
            if (!onGround && !player.isInWater() && !player.onClimbable() && !player.getAbilities().flying
                    && !player.isFallFlying() && !player.isPassenger()) {
                if (jumpTier > 0 && airJumpsUsed < jumpTier) {
                    airJumpsUsed++;

                    // Cancel vertical downward velocity and perform upward impulse jump
                    Vec3 motion = player.getDeltaMovement();
                    player.setDeltaMovement(motion.x, 0.45, motion.z);
                    player.fallDistance = 0.0f;

                    // Play local sound and cloud particles at feet
                    player.playSound(SoundEvents.WIND_CHARGE_BURST.value(), 0.5f, 1.4f);
                    for (int i = 0; i < 8; i++) {
                        player.level().addParticle(ParticleTypes.CLOUD,
                                player.getX() + (player.getRandom().nextDouble() - 0.5) * 0.4,
                                player.getY() + 0.1,
                                player.getZ() + (player.getRandom().nextDouble() - 0.5) * 0.4,
                                0, 0, 0);
                    }

                    // Inform server
                    ClientPacketDistributor.sendToServer(new PerformMultiJumpPayload(airJumpsUsed));
                }
            }
        }
        jumpKeyWasDown = jumpKeyDown;
    }
}
