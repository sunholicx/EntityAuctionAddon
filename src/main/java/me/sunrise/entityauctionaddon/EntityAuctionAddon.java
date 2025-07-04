package me.sunrise.entityauctionaddon;

import com.olziedev.playerauctions.api.PlayerAuctionsAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class EntityAuctionAddon extends JavaPlugin {


  @Override
  public void onEnable() {
    // Plugin startup logic
    Bukkit.getConsoleSender().sendMessage("Entity Auction Addon inicializado!");

    // Carrega arquivos yml
    this.saveDefaultConfig();
    FileConfiguration config = this.getConfig();

    saveResource("translation.yml", false); // Copia se não existir
    File file = new File(getDataFolder(), "translation.yml");
    YamlConfiguration translations = YamlConfiguration.loadConfiguration(file);


    PlayerAuctionsAPI api = PlayerAuctionsAPI.getInstance();

    // CRIA e REGISTRA EntityProduct
    EntityProduct entityProduct = new EntityProduct(config, this, api, translations);
    api.getExpansionRegistry().registerExpansion(entityProduct);

    // REGISTRA o comando passando a instância
    api.getCommandRegistry().addSubCommand(new CreatePlayerEntityAuction(this, api, entityProduct, config));

    // Registra o listener
    getServer().getPluginManager().registerEvents(new BuyEvent(config), this);


  }

  @Override
  public void onDisable() {
    // Plugin shutdown logic
  }

}
