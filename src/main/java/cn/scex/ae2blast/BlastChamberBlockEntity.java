package cn.scex.ae2blast;

import java.util.*;
import cn.scex.ae2blast.recipe.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.*;

public final class BlastChamberBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUTS = 9, FUEL = 9, OUTPUT = 10, SLOTS = 14;
    private boolean searchDirty = true, enabled = true;
    private int cursor, progress, duration = 60, charges, status;
    private long catalogRevision = -1;
    private ItemStack pending = ItemStack.EMPTY;
    private final ItemStack[] escrow = new ItemStack[9];
    public final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
        protected void onContentsChanged(int slot) { searchDirty = true; cursor = 0; setChanged(); }
        public boolean isItemValid(int slot, ItemStack stack) { return slot < INPUTS || slot == FUEL && fuelValue(stack) > 0; }
    };
    private final IItemHandler top = new Port(FUEL, 1, false), sides = new Port(0, 9, false), bottom = new Port(OUTPUT, 4, true);
    public final ContainerData data = new ContainerData() {
        public int get(int i) { return switch(i) { case 0 -> progress; case 1 -> duration; case 2 -> charges; case 3 -> enabled ? 1 : 0; case 4 -> status; default -> 0; }; }
        public void set(int i, int v) { switch(i) { case 0 -> progress=v; case 1 -> duration=v; case 2 -> charges=v; case 3 -> enabled=v!=0; case 4 -> status=v; } }
        public int getCount() { return 5; }
    };
    public BlastChamberBlockEntity(BlockPos pos, BlockState state) { super(BlastChamberMod.ENTITY.get(), pos, state); Arrays.fill(escrow, ItemStack.EMPTY); }
    public IItemHandler automation(Direction side) { return side == Direction.UP ? top : side == Direction.DOWN ? bottom : sides; }
    public static int fuelValue(ItemStack stack) { return stack.is(Items.GUNPOWDER) ? 1 : stack.is(Items.TNT) ? 5 : 0; }
    public Component getDisplayName() { return Component.translatable("block.ae2blast.blast_chamber"); }
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new BlastChamberMenu(id, inv, this); }
    public boolean enabled() { return enabled; }
    public boolean hasPending() { return !pending.isEmpty(); }
    public void toggleEnabled() { enabled = !enabled; setChanged(); }
    public static void tick(Level level, BlockPos pos, BlockState state, BlastChamberBlockEntity be) { be.serverTick((ServerLevel) level); }

    private void serverTick(ServerLevel server) {
        if (!enabled) { status = 4; active(false); return; }
        if (server.hasNeighborSignal(worldPosition)) { status = 3; active(false); return; }
        if (!pending.isEmpty()) {
            if (progress < duration) { progress++; status = 1; active(true); setChanged(); }
            if (progress >= duration) {
                if (canOutput(pending)) {
                    insertOutput(pending); pending = ItemStack.EMPTY; Arrays.fill(escrow, ItemStack.EMPTY); progress=0;
                    searchDirty = true; cursor=0; setChanged();
                    server.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.25f, 1.6f);
                    var facing = getBlockState().getValue(BlastChamberBlock.FACING);
                    server.sendParticles(ParticleTypes.SMOKE, worldPosition.getX()+.5+facing.getStepX()*.55, worldPosition.getY()+.55, worldPosition.getZ()+.5+facing.getStepZ()*.55, 3, .08, .08, .08, .01);
                    active(false); status=0;
                } else { status=2; active(false); }
            }
            return;
        }
        active(false);
        if ((server.getGameTime() + worldPosition.asLong()) % 10 != 0) return;
        var catalog = RecipeCatalog.get(server);
        if (catalogRevision != catalog.revision) { catalogRevision=catalog.revision; searchDirty=true; cursor=0; }
        if (!searchDirty) return;
        var recipes = catalog.recipes();
        if (cursor == 0) status=0;
        int tested = 0;
        // Eight bounded checks per scheduled search, with full negative-result caching until inputs/reload change.
        while (cursor < recipes.size() && tested++ < 8) {
            var recipe = recipes.get(cursor++);
            var consumed = IngredientAllocator.allocate(inventory, recipe.ingredients());
            if (consumed == null) continue;
            var fuel = inventory.getStackInSlot(FUEL);
            int value = fuelValue(fuel), deficit = Math.max(0, recipe.charges() - charges);
            int fuelCount = value == 0 ? (deficit == 0 ? 0 : Integer.MAX_VALUE) : (deficit + value - 1) / value;
            if (fuelCount > fuel.getCount()) { status=5; continue; }
            // Dynamic outputs such as entangled singularities may have unique components: require empty room
            // before assembling, so rejected searches cannot continually allocate quantum frequencies.
            int emptyCapacity=0;
            for (int s=OUTPUT;s<SLOTS;s++) if (inventory.getStackInSlot(s).isEmpty()) emptyCapacity += Math.min(64, recipe.preview().getMaxStackSize());
            boolean unique = appeng.core.definitions.AEItems.QUANTUM_ENTANGLED_SINGULARITY.is(recipe.preview());
            if (unique ? emptyCapacity < recipe.preview().getCount() : !canOutput(recipe.preview())) { status=2; continue; }
            var input = new ArrayList<ItemStack>();
            for (int s=0;s<INPUTS;s++) input.add(inventory.getStackInSlot(s).copyWithCount(consumed[s]));
            ItemStack output;
            try { output=recipe.assembler().apply(List.copyOf(input), server.registryAccess()); }
            catch (RuntimeException ex) { org.slf4j.LoggerFactory.getLogger(getClass()).error("Could not assemble blast recipe {}", recipe.id(), ex); continue; }
            if (output.isEmpty() || output.getCount() > 256 || !canOutput(output)) { status=2; continue; }
            for (int s=0;s<INPUTS;s++) escrow[s] = inventory.extractItem(s, consumed[s], false);
            inventory.extractItem(FUEL, fuelCount, false);
            charges += fuelCount * value - recipe.charges();
            pending=output.copy(); progress=0; duration=recipe.ticks(); status=1;
            setChanged(); active(true); return;
        }
        if (cursor >= recipes.size()) { searchDirty=false; if (status != 2 && status != 5) status=0; }
        else status=6;
    }
    private void active(boolean value) {
        if (getBlockState().getValue(BlastChamberBlock.ACTIVE) != value) level.setBlock(worldPosition, getBlockState().setValue(BlastChamberBlock.ACTIVE, value), 3);
    }
    private boolean canOutput(ItemStack output) {
        int left=output.getCount();
        for (int s=OUTPUT;s<SLOTS;s++) {
            var stored=inventory.getStackInSlot(s);
            if (stored.isEmpty()) left-=Math.min(64, output.getMaxStackSize());
            else if (ItemStack.isSameItemSameComponents(stored,output)) left-=Math.max(0,Math.min(64,stored.getMaxStackSize())-stored.getCount());
        }
        return left<=0;
    }
    private void insertOutput(ItemStack output) {
        var remaining=output.copy();
        for (int s=OUTPUT;s<SLOTS && !remaining.isEmpty();s++) {
            var stored=inventory.getStackInSlot(s);
            if (!stored.isEmpty() && !ItemStack.isSameItemSameComponents(stored,remaining)) continue;
            int take=Math.min(remaining.getCount(), Math.min(64,remaining.getMaxStackSize())-stored.getCount());
            if (take>0) { inventory.setStackInSlot(s, remaining.copyWithCount(stored.getCount()+take)); remaining.shrink(take); }
        }
        if (!remaining.isEmpty()) throw new IllegalStateException("Output transaction overflow");
    }
    public void dropContents() {
        if (level == null || level.isClientSide) return;
        for (int s=0;s<SLOTS;s++) { Containers.dropItemStack(level, worldPosition.getX(),worldPosition.getY(),worldPosition.getZ(),inventory.getStackInSlot(s)); inventory.setStackInSlot(s,ItemStack.EMPTY); }
        // Breaking an unfinished machine returns its committed raw ingredients, never an early crafted result.
        for (int s=0;s<9;s++) { Containers.dropItemStack(level,worldPosition.getX(),worldPosition.getY(),worldPosition.getZ(),escrow[s]); escrow[s]=ItemStack.EMPTY; }
        pending=ItemStack.EMPTY; charges=0;
    }
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries); tag.put("Inventory",inventory.serializeNBT(registries));
        tag.putInt("Schema",1); tag.putInt("Progress",progress); tag.putInt("Duration",duration); tag.putInt("Charges",charges); tag.putBoolean("Enabled",enabled);
        if (!pending.isEmpty()) tag.put("Pending",pending.save(registries));
        for (int s=0;s<9;s++) if (!escrow[s].isEmpty()) tag.put("Escrow"+s,escrow[s].save(registries));
    }
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries); inventory.deserializeNBT(registries,tag.getCompound("Inventory"));
        duration=Math.clamp(tag.getInt("Duration"),1,1200); progress=Math.clamp(tag.getInt("Progress"),0,duration);
        charges=Math.clamp(tag.getInt("Charges"),0,64); enabled=!tag.contains("Enabled") || tag.getBoolean("Enabled");
        pending=ItemStack.parseOptional(registries,tag.getCompound("Pending"));
        for (int s=0;s<9;s++) escrow[s]=ItemStack.parseOptional(registries,tag.getCompound("Escrow"+s));
        searchDirty=true; cursor=0; catalogRevision=-1;
    }
    private final class Port implements IItemHandler {
        private final int start, count; private final boolean output;
        Port(int start,int count,boolean output) { this.start=start; this.count=count; this.output=output; }
        public int getSlots() { return count; }
        private int slot(int s) { if(s<0 || s>=count) throw new IndexOutOfBoundsException(s); return start+s; }
        public ItemStack getStackInSlot(int s) { return inventory.getStackInSlot(slot(s)); }
        public ItemStack insertItem(int s,ItemStack stack,boolean simulate) { int index=slot(s); return output ? stack : inventory.insertItem(index,stack,simulate); }
        public ItemStack extractItem(int s,int amount,boolean simulate) { int index=slot(s); return output ? inventory.extractItem(index,amount,simulate) : ItemStack.EMPTY; }
        public int getSlotLimit(int s) { return inventory.getSlotLimit(slot(s)); }
        public boolean isItemValid(int s,ItemStack stack) { return !output && inventory.isItemValid(slot(s),stack); }
    }
}
