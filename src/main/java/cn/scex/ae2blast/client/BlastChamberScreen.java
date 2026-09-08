package cn.scex.ae2blast.client;
import cn.scex.ae2blast.BlastChamberMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class BlastChamberScreen extends AbstractContainerScreen<BlastChamberMenu> {
    private Button toggle;
    public BlastChamberScreen(BlastChamberMenu menu,Inventory inv,Component title) { super(menu,inv,title); imageHeight=207; inventoryLabelY=111; }
    protected void init() {
        super.init(); toggle=addRenderableWidget(Button.builder(Component.empty(),b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0)).bounds(leftPos+124,topPos+87,44,18).build()); updateButton();
    }
    private void updateButton() { toggle.setMessage(Component.translatable(menu.data.get(3)!=0?"gui.ae2blast.on":"gui.ae2blast.off")); }
    protected void containerTick() { super.containerTick(); updateButton(); }
    protected void renderBg(GuiGraphics g,float delta,int mouseX,int mouseY) {
        int x=leftPos,y=topPos;
        g.fill(x,y,x+176,y+207,0xff171821); g.fill(x+2,y+2,x+174,y+205,0xffd3d0c2); g.fill(x+5,y+20,x+171,y+83,0xff555461);
        for(int row=0;row<3;row++) for(int col=0;col<3;col++) slot(g,x+17+col*18,y+27+row*18);
        slot(g,x+89,y+62);
        for(int row=0;row<2;row++) for(int col=0;col<2;col++) slot(g,x+134+col*18,y+36+row*18);
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) slot(g,x+8+col*18,y+123+row*18);
        for(int col=0;col<9;col++) slot(g,x+8+col*18,y+181);
        g.fill(x+78,y+39,x+125,y+47,0xff20222c);
        int filled=45*menu.data.get(0)/Math.max(1,menu.data.get(1));
        g.fill(x+79,y+40,x+79+filled,y+46,0xffad6ce4);
        g.drawString(font,Component.translatable("gui.ae2blast.fuel"),x+85,y+51,0xfff2c264,false);
    }
    private static void slot(GuiGraphics g,int x,int y) { g.fill(x-1,y-1,x+17,y+17,0xff272832); g.fill(x,y,x+16,y+16,0xff85818c); }
    protected void renderLabels(GuiGraphics g,int mouseX,int mouseY) {
        super.renderLabels(g,mouseX,mouseY);
        g.drawString(font,Component.translatable("gui.ae2blast.status."+menu.data.get(4)),8,88,0xff3b2c50,false);
        g.drawString(font,Component.translatable("gui.ae2blast.charges",menu.data.get(2)),8,99,0xff514352,false);
    }
    public void render(GuiGraphics g,int mouseX,int mouseY,float delta) { super.render(g,mouseX,mouseY,delta); renderTooltip(g,mouseX,mouseY); }
}
