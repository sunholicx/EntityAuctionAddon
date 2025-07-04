package me.sunrise.entityauctionaddon;

import com.olziedev.playerauctions.api.events.auction.PlayerAuctionBuyEvent;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

public class BuyEvent implements Listener {

  FileConfiguration config;

  public BuyEvent(FileConfiguration config) {
    this.config = config;
  }


  @EventHandler
  public void onAuctionBuy(PlayerAuctionBuyEvent event) {

    if (event.getSerializableProduct().getAProduct().get() instanceof EntityData) {
      Player player = event.getBuyer();

      // Verifica se o jogador existe (só por segurança)
      if (player == null || !player.isOnline()) {
        return;
      }


      Location loc = player.getLocation();


      if (!isSafeToSpawn(loc)) {
          event.setCancelled(true);
          player.sendMessage(ChatColor.translateAlternateColorCodes('&',config.getString("message.place-not-safe-message", "")));
        }

    }
  }

  private boolean isSafeToSpawn(Location location) {

    World world = location.getWorld();
    if (world == null) return false;

    Vector direction = location.getDirection().normalize();
    Location block = location.clone().add(direction.clone().multiply(2));

    Location loc1 = block.clone().add(0, 1, 0);
    Location loc2 = block.clone().add(0, 2, 0);

    // Garante que os chunks estejam carregados
    if (!world.isChunkLoaded(loc1.getBlockX() >> 4, loc1.getBlockZ() >> 4)
      || !world.isChunkLoaded(loc2.getBlockX() >> 4, loc2.getBlockZ() >> 4)) {
      return false;
    }

    // Obtém os BlockState
    BlockState blockState1 = world.getBlockAt(loc1).getState();
    BlockState blockState2 = world.getBlockAt(loc2).getState();

    return blockState1.getType() == Material.AIR && blockState2.getType() == Material.AIR;
  }

}
