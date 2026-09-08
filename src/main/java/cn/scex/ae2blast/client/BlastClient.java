package cn.scex.ae2blast.client;
import cn.scex.ae2blast.BlastChamberMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
@EventBusSubscriber(modid=BlastChamberMod.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class BlastClient {
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent e) { e.register(BlastChamberMod.MENU.get(),BlastChamberScreen::new); }
}
