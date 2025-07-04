package me.sunrise.entityauctionaddon;

import com.olziedev.playerauctions.api.PlayerAuctionsAPI;
import com.olziedev.playerauctions.api.auction.command.ACommand;
import com.olziedev.playerauctions.api.auction.product.AProduct;
import com.olziedev.playerauctions.api.player.APlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


public class CreatePlayerEntityAuction extends ACommand {

  private final JavaPlugin plugin;
  private final PlayerAuctionsAPI api;
  private final EntityProduct  entityProduct;
  private final FileConfiguration  config;

  // Entidades explicitamente proibidas
  private static Set<EntityType> DISALLOWED = Set.of();
  private static Set<EntityType> HOSTILES = Set.of();

  public CreatePlayerEntityAuction(JavaPlugin plugin, PlayerAuctionsAPI api, EntityProduct entityProduct, FileConfiguration config) {
    super(config.getString("sub-command", "sellanimal"));
    this.executorType = ExecutorType.PLAYER_ONLY;
    this.plugin = plugin;
    this.api = api;
    this.entityProduct = entityProduct;
    this.config = config;

    List<String> names = config.getStringList("entity-permissions.exceptions");
    DISALLOWED = names.stream()
      .map(name -> {
        try {
          return EntityType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
          return null;
        }
      })
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());

    List<String> names1 = config.getStringList("entity-permissions.hostiles");
    HOSTILES = names1.stream()
      .map(name -> {
        try {
          return EntityType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
          return null;
        }
      })
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());

  }

  @Override
  public void execute(CommandSender commandSender, String[] args) {

    Player player = (Player) commandSender;

    if (args.length < 2) {
      player.sendMessage(ChatColor
        .translateAlternateColorCodes('&',config.getString("messages.no-value", ""))
        .replace("[subcomando]", config.getString("sub-command", "sellanimal")));
      return;
    }
    double valor;
    try {
      valor = Double.parseDouble(args[1]);
    } catch (NumberFormatException e) {
      player.sendMessage(ChatColor.translateAlternateColorCodes('&',config.getString("messages.invalid-value", "")));
      return;
    }

    CompletableFuture<EntityData> future = new CompletableFuture<>();
    Bukkit.getScheduler().runTask(plugin, () -> future.complete(new EntityData(player)));

    EntityData entityData;
    try {
      entityData = future.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }

    Entity entity = entityData.getOriginalEntity();

    if (entity != null && isEntityPermitted(entity)) {

      APlayer seller = api.getAuctionPlayer(player.getUniqueId());
      AProduct<EntityData> product = entityProduct.setupProduct(1L, seller.getPlayer());

      api.createPlayerAuction(valor,
          seller,
          product,
          false,
          auction -> {
          });

      Bukkit.getScheduler().runTask(plugin, entity::remove);

    } else if (entity == null) {
      player.sendMessage(ChatColor.translateAlternateColorCodes('&',config.getString("messages.entity-not-found", "")));
    } else {
      player.sendMessage(ChatColor.translateAlternateColorCodes('&',config.getString("messages.entity-not-permitted", "")));
    }

  }

  public boolean isEntityPermitted(Entity entity) {
    EntityType type = entity.getType();

    String category = config.getString("entity-permissions.category", "");


    if (DISALLOWED.contains(type)) {
      return false;
    } else if (category.equals("Monster")){
      return HOSTILES.contains(type);
    } else if (entity instanceof Wolf wolf) {
      return !wolf.isTamed();
    } else if (category.equals("Creature")) {
      return entity instanceof Creature && !(HOSTILES.contains(type));
    }

    // Se não entrar em nenhum dos return anteriores return true
    // Porque então a entidade não pertence às exceções, não é lobo domesticado e categoria está marcada como All
    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, String[] arguments) {
    return Collections.emptyList();
  }


}
