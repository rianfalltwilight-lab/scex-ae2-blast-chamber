package cn.scex.ae2blast;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class BlastChamberMenu extends AbstractContainerMenu {
    public final BlastChamberBlockEntity machine;
    public final ContainerData data;
    public BlastChamberMenu(int id,Inventory inv,RegistryFriendlyByteBuf buf) {
        this(id,inv,(BlastChamberBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
    }
    public BlastChamberMenu(int id,Inventory inv,BlastChamberBlockEntity machine) {
        super(BlastChamberMod.MENU.get(),id); this.machine=machine;
        data=inv.player.level().isClientSide ? new SimpleContainerData(5) : machine.data;
        for(int y=0;y<3;y++) for(int x=0;x<3;x++) addSlot(new SlotItemHandler(machine.inventory,y*3+x,17+x*18,27+y*18));
        addSlot(new SlotItemHandler(machine.inventory,9,89,62));
        for(int y=0;y<2;y++) for(int x=0;x<2;x++) addSlot(new SlotItemHandler(machine.inventory,10+y*2+x,134+x*18,36+y*18) { public boolean mayPlace(ItemStack stack) { return false; } });
        for(int y=0;y<3;y++) for(int x=0;x<9;x++) addSlot(new Slot(inv,x+y*9+9,8+x*18,123+y*18));
        for(int x=0;x<9;x++) addSlot(new Slot(inv,x,8+x*18,181));
        addDataSlots(data);
    }
    public boolean stillValid(Player player) { return stillValid(ContainerLevelAccess.create(machine.getLevel(),machine.getBlockPos()),player,BlastChamberMod.CHAMBER.get()); }
    public boolean clickMenuButton(Player player,int id) { if(id==0 && stillValid(player)) { machine.toggleEnabled(); return true; } return false; }
    public ItemStack quickMoveStack(Player player,int index) {
        if(index<0 || index>=slots.size()) return ItemStack.EMPTY;
        var slot=slots.get(index); if(!slot.hasItem()) return ItemStack.EMPTY;
        var stack=slot.getItem(); var copy=stack.copy();
        if(index<14) { if(!moveItemStackTo(stack,14,slots.size(),true)) return ItemStack.EMPTY; }
        else if(BlastChamberBlockEntity.fuelValue(stack)>0) { if(!moveItemStackTo(stack,9,10,false) && !moveItemStackTo(stack,0,9,false)) return ItemStack.EMPTY; }
        else if(!moveItemStackTo(stack,0,9,false)) return ItemStack.EMPTY;
        if(stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player,stack); return copy;
    }
}
