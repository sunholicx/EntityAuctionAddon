package me.sunrise.entityauctionaddon;

import de.tr7zw.nbtapi.NBTEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.io.*;
import java.nio.charset.StandardCharsets;



// Classe que contém informações comuns para todas as entidades
public class EntityData implements Serializable {

  private final Entity originalEntity;
  private final NBTEntity nbtEntity;
  private final CompoundTag compoundTag;

  public EntityData() {
    this.originalEntity = null;
    this.nbtEntity = null;
    this.compoundTag = null;
  }

  public EntityData(Player player) {
    this.originalEntity = getEntityLookingAt(player);

    if (originalEntity != null) {
      this.nbtEntity = new NBTEntity(originalEntity);
      this.compoundTag = (CompoundTag) nbtEntity.getCompound();

      // Adiciona id se não existir
      if (!compoundTag.contains("id")) {
        String key = originalEntity.getType().getKey().toString(); // ex: "minecraft:cow"
        compoundTag.putString("id", key);
      }

    } else {
      this.nbtEntity = null;
      this.compoundTag = null;
    }

  }

  //Serializer e deserializer
  public static byte[] toBytes(EntityData data) {
    return data.toNBTString().getBytes(StandardCharsets.UTF_8);
  }

  public static EntityData fromBytes(byte[] bytes) {
    return EntityData.fromNBTString(new String(bytes, StandardCharsets.UTF_8));
  }

  public String toNBTString() {
    return compoundTag == null ? "{}" : compoundTag.toString();
  }

  public static EntityData fromNBTString(String nbt) {
    try {
      CompoundTag tag = TagParser.parseCompoundFully(nbt);
      return new EntityData(tag);
    } catch (Exception e) {
      throw new RuntimeException("Erro ao desserializar NBT", e);
    }
  }

  // Construtor para reconstruir EntityData com dados serializados
  private EntityData(CompoundTag tag) {
    this.originalEntity = null;
    this.nbtEntity = null;
    this.compoundTag = tag;
  }

  // Detecta se o player está olhando alguma entidade
  public static Entity getEntityLookingAt(Player player) {

      double range = 5.0;
      Vector direction = player.getEyeLocation().getDirection();
      RayTraceResult rayTrace = player.getWorld().rayTraceEntities(
        player.getEyeLocation(),
        direction,
        range,
        0.5,
        entity -> entity != player && entity instanceof LivingEntity
      );

    if (rayTrace != null) {
      return rayTrace.getHitEntity();
    }

    return null;
  }

  public Entity getOriginalEntity() {
    return originalEntity;
  }

  public NBTEntity getNbtEntity() {
    return nbtEntity;
  }

  public CompoundTag getCompoundTag() {
    return compoundTag;
  }

}
