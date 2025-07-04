package me.sunrise.entityauctionaddon;

import com.olziedev.playerauctions.api.PlayerAuctionsAPI;
import com.olziedev.playerauctions.api.auction.ACategory;
import com.olziedev.playerauctions.api.auction.Auction;
import com.olziedev.playerauctions.api.auction.product.AProduct;
import com.olziedev.playerauctions.api.auction.product.ASerializableProduct;
import com.olziedev.playerauctions.api.expansion.AProductProvider;
import com.olziedev.playerauctions.api.player.APlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;


import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class EntityProduct extends AProductProvider<EntityData> {

  private final FileConfiguration config;
  private final JavaPlugin plugin;
  private final PlayerAuctionsAPI api;
  private final YamlConfiguration translations;

  public EntityProduct(FileConfiguration config, JavaPlugin plugin, PlayerAuctionsAPI api,  YamlConfiguration translations) {
    this.plugin = plugin;
    this.config = config;
    this.api = api;
    this.translations = translations;
  }


  @Override
  public void giveProduct(AProduct<EntityData> aProduct, Player player) {
    Vector direction = player.getLocation().getDirection().normalize();
    Location base = player.getLocation();
    Location block = base.clone().add(direction.clone().multiply(2));

    EntityData entityData = aProduct.get();
    CompoundTag compoundTag = entityData.getCompoundTag();

    double x = block.getX();
    double y = block.getY() + 1;
    double z = block.getZ();

    // Criar a nova lista Pos com coordenadas
    ListTag posList = new ListTag();
    posList.add(DoubleTag.valueOf(x));
    posList.add(DoubleTag.valueOf(y));
    posList.add(DoubleTag.valueOf(z));
    compoundTag.put("Pos", posList);

    Level nmsWorld = ((CraftWorld) block.getWorld()).getHandle();
    Entity nmsEntity = EntityType.loadEntityRecursive(
      compoundTag,
      nmsWorld,
      EntitySpawnReason.COMMAND,
      (entity) -> {
        Vec3 pos = new Vec3(x, y, z);
        entity.moveOrInterpolateTo(pos, entity.getYRot(), entity.getXRot());
        return entity;
      }
    );

    Bukkit.getScheduler().runTask(plugin, () -> nmsWorld.addFreshEntity(nmsEntity));

  }

  @Override
  public boolean isInvalidProduct(AProduct<EntityData> aProduct, Player player) {
    return false;
  }

  @Override
  public boolean isDamagedProduct(AProduct<EntityData> aProduct, Player player) {
    return false;
  }

  @Override
  public boolean isCorrect(AProduct<EntityData> aProduct, Player player) {
    return false;
  }

  @Override
  public boolean isSimilarProduct(ASerializableProduct<?> aSerializableProduct, ASerializableProduct<?> aSerializableProduct1) {
    return false;
  }

  @Override
  public boolean takeProduct(AProduct<EntityData> aProduct, Player player) {
    return false;
  }

  @Override
  public AProduct<EntityData> setupProduct(Long aLong, Player player) {
    CompletableFuture<EntityData> future = new CompletableFuture<>();
    Bukkit.getScheduler().runTask(plugin, () -> future.complete(new EntityData(player)));

    EntityData entity;
    try {
      entity = future.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }

    ASerializableProduct<EntityData> serializableProduct = getSerializableProduct(EntityData.toBytes(entity));

    return new AProduct<>(aLong, entity,0L, 1L, EntityData::new, serializableProduct);
  }

  @Override
  public AProduct<EntityData> setupProduct(Long aLong, EntityData entityData) {
    ASerializableProduct<EntityData> serializableProduct = getSerializableProduct(EntityData.toBytes(entityData));

    return new AProduct<>(aLong, entityData,0L, 1L, EntityData::new, serializableProduct);
  }

  @Override
  public ASerializableProduct<EntityData> getSerializableProduct(byte[] bytes) {
    ASerializableProduct.SerializableFunction<byte[], EntityData> function = EntityData::fromBytes;
    return new ASerializableProduct<>(getName(), bytes, function);
  }

  @Override
  public ItemStack getIcon(AProduct<EntityData> aProduct) {
    EntityData entityData = aProduct.get();
    CompoundTag tag = entityData.getCompoundTag();

    String type = getType(tag);

    try {
      Material mat = Material.getMaterial(type + "_SPAWN_EGG");
      return customItemStack(new ItemStack(mat), type);

    } catch (IllegalArgumentException e) {
      // O tipo não tem spawn egg
      Bukkit.getLogger().warning("EntityType desconhecido: " + type);
      return customItemStack(new ItemStack(Material.CHICKEN_SPAWN_EGG), type);
    }
  }

  @Override
  public List<ACategory> getCategories(AProduct<EntityData> aProduct) {
    List<String> categoryNames = config.getStringList("categories");

    List<ACategory> categories = new ArrayList<>();

    for (String name : categoryNames) {
      // Certifique-se de criar ACategory do nome
      ACategory category = api.getAuctionCategory(name);
      categories.add(category);
    }

    return categories;
  }


  @Override
  public List<ACategory> getCategories(EntityData entityData) {
    return getCategories(setupProduct(1L, entityData));
  }

  @Override
  public String getProductName(AProduct<EntityData> aProduct, FileConfiguration fileConfiguration, FileConfiguration fileConfiguration1, boolean b) {
    EntityData data = aProduct.get();
    CompoundTag tag = data.getCompoundTag();

    String type = getType(tag);

    return translations.getString(type, type.replace("_", " "));
  }

  @Override
  public List<String> getItemLore(Auction auction, APlayer aPlayer, ConfigurationSection configurationSection) {

    List<String> rawLore;
    if (!auction.isBidding()) {
      rawLore = configurationSection.getStringList("icon.lore");
    } else {
      rawLore = configurationSection.getStringList("icon.lore-bidding");
    }

    if (rawLore.isEmpty()) {
      return Collections.singletonList(ChatColor.GRAY + "Sem descrição disponível.");
    }

    List<String> lore = new ArrayList<>();

    for (String line : rawLore) {
      line = line
        .replace("[id]", String.valueOf(auction.getID()))
        .replace("[seller]", auction.getAuctionPlayer().getName())
        .replace("[amount]", "1")
        .replace("[price]", String.format("%.2f", auction.getPrice()))
        .replace("[expire]", formatExpireTime(auction.getExpireTime()))
        .replace("[date]", formatAuctionDate(auction.getAuctionDate()))
        .replace("[categories]", listCategories(auction.getAuctionCategories()))
        .replace("[price_item]", "") // Ou algum cálculo seu
        .replace("[delauction]", "") // Ou algum texto seu
        .replace("[repair_penalty]", "") // Se tiver suporte
        .replace("[lore]", ""); // Se quiser mostrar lore do item

      if (auction.isBidding()) {
        line = line.replace("[bidder]", auction.getBidder().getName());
      }

      lore.add(ChatColor.translateAlternateColorCodes('&', line));
    }



    return lore;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public String getName() {
    return "EntityProduct";
  }

  @Override
  public void onLoad() {
  }

  // Métodos privados da classe
  private String getType(CompoundTag tag){
    // Pega o id da entidade
    String id = tag.getString("id").toString();
    // Extrai só o nome (ex.: "minecraft:cow" -> "COW")
    String[] split = id.split(":");
    String type = split.length > 1 ? split[1].toUpperCase() : id.toUpperCase();
    type = type.replace("]", "");

    return type;
  }

  private ItemStack customItemStack(ItemStack item, String type){
    // Obtém o ItemMeta
    ItemMeta meta = item.getItemMeta();

    // Altera o display name
    meta.setDisplayName(translations.getString(type, type.replace("_", " ")));

    // Reatribui o ItemMeta no ItemStack
    item.setItemMeta(meta);

    return item;
  }

  private String formatExpireTime(long expireTime) {
    long totalSeconds = expireTime / 1000;

    long days = totalSeconds / (24 * 60 * 60);
    long hours = (totalSeconds % (24 * 60 * 60)) / (60 * 60);
    long minutes = (totalSeconds % (60 * 60)) / 60;

    return String.format("%dd %dh %dm", days, hours, minutes);
  }

  private String formatAuctionDate(long auctionDate) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
      .withZone(ZoneId.systemDefault());

    return formatter.format(Instant.ofEpochMilli(auctionDate));
  }

  private String listCategories(List<ACategory> categories) {

    String categoryNames = (categories == null || categories.isEmpty())
      ? "None"
      : categories.stream()
      // Filtra categorias cujo nome NÃO é "all"
      .map(ACategory::getName)
      .filter(name -> !name.equalsIgnoreCase("all"))
      // Capitaliza a primeira letra
      .map(name -> name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase())
      // Junta tudo separado por vírgula
      .collect(Collectors.joining(", "));

// Caso depois de filtrar tudo fique vazio, mostrar "None"
    if (categoryNames.isEmpty()) {
      categoryNames = "None";
    }

    return categoryNames;
  }

}


