package com.luckymod;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(LuckyMod.MOD_ID)
public class LuckyMod {
	public static final String MOD_ID = "luckymod";
	public static final Logger LOGGER = LogUtils.getLogger();

	public LuckyMod() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		ModParticles.PARTICLES.register(modBus);
		ModArmorMaterials.MATERIALS.register(modBus);
		ModItems.ITEMS.register(modBus);
		ModCreativeTabs.TABS.register(modBus);
		LOGGER.info("Lucky Brush chargé.");
	}
}
