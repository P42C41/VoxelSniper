/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 The Voxel Plugineering Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.voxelplugineering.voxelsniper.forge.world;

import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.voxelplugineering.voxelsniper.entity.Entity;
import com.voxelplugineering.voxelsniper.entity.EntityType;
import com.voxelplugineering.voxelsniper.entity.Player;
import com.voxelplugineering.voxelsniper.forge.entity.ForgeEntity;
import com.voxelplugineering.voxelsniper.forge.entity.ForgeEntityType;
import com.voxelplugineering.voxelsniper.forge.world.biome.ForgeBiome;
import com.voxelplugineering.voxelsniper.forge.world.material.ForgeMaterial;
import com.voxelplugineering.voxelsniper.forge.world.material.ForgeMaterialState;
import com.voxelplugineering.voxelsniper.service.registry.BiomeRegistry;
import com.voxelplugineering.voxelsniper.service.registry.MaterialRegistry;
import com.voxelplugineering.voxelsniper.sponge.entity.SpongeEntityType;
import com.voxelplugineering.voxelsniper.util.Context;
import com.voxelplugineering.voxelsniper.util.math.Vector3d;
import com.voxelplugineering.voxelsniper.util.math.Vector3i;
import com.voxelplugineering.voxelsniper.world.AbstractWorld;
import com.voxelplugineering.voxelsniper.world.Chunk;
import com.voxelplugineering.voxelsniper.world.CommonBlock;
import com.voxelplugineering.voxelsniper.world.CommonLocation;
import com.voxelplugineering.voxelsniper.world.Location;
import com.voxelplugineering.voxelsniper.world.World;
import com.voxelplugineering.voxelsniper.world.biome.Biome;
import com.voxelplugineering.voxelsniper.world.material.Material;
import com.voxelplugineering.voxelsniper.world.material.MaterialState;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A wrapper for forge's {@link World}.
 */
public class ForgeWorld extends AbstractWorld<WorldServer>
{

    private final MaterialRegistry<net.minecraft.block.Block> materials;
    private final BiomeRegistry<net.minecraft.world.biome.BiomeGenBase> biomes;
    private final Context context;

    private final Map<net.minecraft.world.chunk.Chunk, Chunk> chunks;
    protected final Map<net.minecraft.entity.Entity, Entity> entitiesCache;

    /**
     * Creates a new {@link ForgeWorld}.
     * 
     * @param world The world to wrap
     */
    @SuppressWarnings("unchecked")
    public ForgeWorld(Context context, WorldServer world)
    {
        super(context, world);
        this.context = context;
        this.biomes = context.getRequired(BiomeRegistry.class);
        this.materials = context.getRequired(MaterialRegistry.class);
        this.chunks = new MapMaker().weakKeys().makeMap();
        this.entitiesCache = new MapMaker().weakKeys().makeMap();
    }

    @Override
    public String getName()
    {
        return getThis().getWorldInfo().getWorldName();
    }

    @Override
    public Optional<com.voxelplugineering.voxelsniper.world.Block> getBlock(int x, int y, int z)
    {
        if (!getThis().getChunkProvider().chunkExists(x < 0 ? x / 16 - 1 : x / 16, z < 0 ? z / 16 - 1 : z / 16))
        {
            return Optional.empty();
        }
        IBlockState state = getThis().getBlockState(new net.minecraft.util.BlockPos(x, y, z));
        ResourceLocation rs = Block.blockRegistry.getNameForObject(state.getBlock());
        Optional<Material> mat = this.materials
                .getMaterial((!rs.getResourceDomain().equals("minecraft") ? rs.getResourceDomain() + ":" : "") + rs.getResourcePath());
        if (mat.isPresent())
        {
            MaterialState ms = ((ForgeMaterial) mat.get()).getState(state);
            Location loc = new CommonLocation(this, x, y, z);
            return Optional.<com.voxelplugineering.voxelsniper.world.Block>of(new CommonBlock(loc, ms));
        }
        return Optional.empty();
    }

    @Override
    public void setBlock(MaterialState material, int x, int y, int z, boolean update)
    {
        if (material instanceof ForgeMaterialState)
        {
            ForgeMaterialState forgeMaterial = (ForgeMaterialState) material;
            getThis().setBlockState(new net.minecraft.util.BlockPos(x, y, z), forgeMaterial.getState(), update ? 3 : 0);
        }
    }

    @Override
    public MaterialRegistry<?> getMaterialRegistry()
    {
        return this.materials;
    }

    @Override
    public Iterable<Entity> getLoadedEntities()
    {
        List<Entity> entities = Lists.newArrayList();
        for (Object o : getThis().loadedEntityList)
        {
            net.minecraft.entity.Entity e = (net.minecraft.entity.Entity) o;
            if (this.entitiesCache.containsKey(e))
            {
                entities.add(this.entitiesCache.get(e));
            } else
            {
                Entity ent = new ForgeEntity(e, this.context);
                this.entitiesCache.put(e, ent);
                entities.add(ent);
            }
        }
        return entities;
    }

    @Override
    public Optional<Chunk> getChunk(int x, int y, int z)
    {
        if (!getThis().getChunkProvider().chunkExists(x, z))
        {
            return Optional.empty();
        }
        net.minecraft.world.chunk.Chunk chunk = getThis().getChunkFromChunkCoords(x, z);
        if (this.chunks.containsKey(chunk))
        {
            return Optional.of(this.chunks.get(chunk));
        }
        ForgeChunk newChunk = new ForgeChunk(chunk, this, this.context);
        this.chunks.put(chunk, newChunk);
        return Optional.<Chunk>of(newChunk);
    }

    @Override
    public Optional<Biome> getBiome(int x, int y, int z)
    {
        BiomeGenBase biome = getThis().getBiomeGenForCoords(new net.minecraft.util.BlockPos(x, y, z));
        return this.biomes.getBiome(biome.biomeName);
    }

    @Override
    public void setBiome(Biome biome, int x, int y, int z)
    {
        if (biome instanceof ForgeBiome)
        {
            ForgeBiome forgeBiome = (ForgeBiome) biome;
            net.minecraft.world.chunk.Chunk chunk = getThis().getChunkFromBlockCoords(new net.minecraft.util.BlockPos(x, y, z));
            byte[] biomes = chunk.getBiomeArray();
            biomes[(z & 15) << 4 | (x & 15)] = (byte) (forgeBiome.getThis().biomeID & 255);
        }
    }

    @Override
    public Vector3i getChunkSize()
    {
        return ForgeChunk.CHUNK_SIZE;
    }

    @Override
    public void spawnLightning(Vector3i position, Player source)
    {
        getThis().addWeatherEffect(new EntityLightningBolt(getThis(), position.getX(), position.getY(), position.getZ()));
    }

    @Override
    public void spawnEntity(EntityType entityType, Vector3d position, Player source) {
        final net.minecraft.entity.Entity
                entity = EntityList.createEntityByName(((ForgeEntityType) entityType).getRegistration().getEntityName(), getThis());
        entity.setPosition(position.getX(), position.getY(), position.getZ());
        getThis().spawnEntityInWorld(entity);
    }

}
