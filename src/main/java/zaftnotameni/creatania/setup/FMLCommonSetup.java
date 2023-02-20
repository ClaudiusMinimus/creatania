package zaftnotameni.creatania.setup;
import com.simibubi.create.content.contraptions.fluids.tank.BoilerHeaters;
import com.simibubi.create.foundation.block.BlockStressValues;
import com.simibubi.create.foundation.config.AllConfigs;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import zaftnotameni.creatania.network.Networking;
import zaftnotameni.creatania.registry.Advancements;
import zaftnotameni.creatania.registry.Blocks;
import zaftnotameni.creatania.registry.Triggers;
import zaftnotameni.creatania.util.Log;

import static zaftnotameni.creatania.Constants.MODID;
public class FMLCommonSetup {
  public static void run(final FMLCommonSetupEvent event) {
    Log.LOGGER.info("creatania setup started");
    BlockStressValues.registerProvider(MODID, AllConfigs.SERVER.kinetics.stressValues);
    BoilerHeaters.registerHeater(Blocks.REAL_MANA_BLOCK.get(), (level, pos, state) -> 4);
    BoilerHeaters.registerHeater(Blocks.PURE_MANA_BLOCK.get(), (level, pos, state) -> 2);
    Networking.registerMessages();
    event.enqueueWork(() -> {
      Advancements.register();
      Triggers.register();
    });
    Log.LOGGER.info("creatania setup finished");
  }
}
