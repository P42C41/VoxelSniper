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
package com.voxelplugineering.voxelsniper.sponge.world;

import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.voxelplugineering.voxelsniper.entity.AbstractPlayer;
import com.voxelplugineering.voxelsniper.entity.Entity;
import com.voxelplugineering.voxelsniper.entity.EntityType;
import com.voxelplugineering.voxelsniper.entity.Player;
import com.voxelplugineering.voxelsniper.service.registry.BiomeRegistry;
import com.voxelplugineering.voxelsniper.service.registry.MaterialRegistry;
import com.voxelplugineering.voxelsniper.sponge.VoxelSniperSponge;
import com.voxelplugineering.voxelsniper.sponge.entity.SpongeEntity;
import com.voxelplugineering.voxelsniper.sponge.entity.SpongeEntityType;
import com.voxelplugineering.voxelsniper.sponge.entity.SpongePlayer;
import com.voxelplugineering.voxelsniper.sponge.util.SpongeUtilities;
import com.voxelplugineering.voxelsniper.sponge.world.biome.SpongeBiome;
import com.voxelplugineering.voxelsniper.sponge.world.material.SpongeMaterial;
import com.voxelplugineering.voxelsniper.sponge.world.material.SpongeMaterialState;
import com.voxelplugineering.voxelsniper.util.Context;
import com.voxelplugineering.voxelsniper.util.math.Vector3d;
import com.voxelplugineering.voxelsniper.util.math.Vector3i;
import com.voxelplugineering.voxelsniper.world.AbstractWorld;
import com.voxelplugineering.voxelsniper.world.Chunk;
import com.voxelplugineering.voxelsniper.world.CommonBlock;
import com.voxelplugineering.voxelsniper.world.CommonLocation;
import com.voxelplugineering.voxelsniper.world.biome.Biome;
import com.voxelplugineering.voxelsniper.world.material.MaterialState;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A wrapper for Sponge's World.
 */
public class SpongeWorld extends AbstractWorld<org.spongepowered.api.world.World>
{

    private final Context context;
    private final MaterialRegistry<org.spongepowered.api.block.BlockType> materials;
    private final BiomeRegistry<org.spongepowered.api.world.biome.BiomeType> biomes;
    private final Thread worldThread;
    private final Map<org.spongepowered.api.world.Chunk, Chunk> chunks;
    protected final Map<org.spongepowered.api.entity.Entity, Entity> entitiesCache;

    /**
     * Creates a new {@link SpongeWorld}.
     * 
     * @param context The context
     * @param world The world to wrap
     * @param thread The main thread of this world
     */
    @SuppressWarnings("unchecked")
    public SpongeWorld(Context context, org.spongepowered.api.world.World world, Thread thread)
    {
        super(context, world);
        this.context = context;
        this.biomes = context.getRequired(BiomeRegistry.class);
        this.materials = context.getRequired(MaterialRegistry.class);
        this.chunks = new MapMaker().weakKeys().makeMap();
        this.entitiesCache = new MapMaker().weakKeys().makeMap();
        this.worldThread = thread;
    }

    @Override
    public String getName()
    {
        return getThis().getName();
    }

    @Override
    public Optional<com.voxelplugineering.voxelsniper.world.Block> getBlock(int x, int y, int z)
    {
        if (!checkAsyncBlockAccess(x, y, z))
        {
            return Optional.empty();
        }
        com.flowpowered.math.vector.Vector3i min = getThis().getBlockMin();
        com.flowpowered.math.vector.Vector3i max = getThis().getBlockMax();
        if (x < min.getX() || x > max.getX() || z < min.getZ() || z > max.getZ() || y < min.getY() || y > max.getY())
        {
            return Optional.empty();
        }
        org.spongepowered.api.world.Location<org.spongepowered.api.world.World> b = getThis().getLocation(x, y, z);
        Optional<com.voxelplugineering.voxelsniper.world.material.Material> m = this.materials.getMaterial(b.getBlockType());
        if (!m.isPresent())
        {
            return Optional.empty();
        }
        MaterialState ms = ((SpongeMaterial) m.get()).getState(b.getBlock());
        CommonLocation l = new CommonLocation(this, b.getX(), b.getY(), b.getZ());
        return Optional.<com.voxelplugineering.voxelsniper.world.Block>of(new CommonBlock(l, ms));
    }

    private boolean checkAsyncBlockAccess(int x, int y, int z)
    {
        if (Thread.currentThread() != this.worldThread)
        {
            int cx = x < 0 ? (x / 16 - 1) : x / 16;
            int cz = z < 0 ? (z / 16 - 1) : z / 16;
            if (!getThis().getChunk(new com.flowpowered.math.vector.Vector3i(cx, 0, cz)).isPresent())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setBlock(MaterialState material, int x, int y, int z, boolean update)
    {
        // TODO physics
        if (y < 0 || y >= 256)
        {
            return;
        }
        if (material instanceof SpongeMaterialState)
        {
            SpongeMaterialState spongeMaterial = (SpongeMaterialState) material;
            // TODO construct the cause with the executor of the brush or queue.
            getThis().setBlock(x, y, z, spongeMaterial.getState(), update, Cause.of(NamedCause.source(VoxelSniperSponge.instance.getContainer())));
        }
    }

    @Override
    public MaterialRegistry<org.spongepowered.api.block.BlockType> getMaterialRegistry()
    {
        return this.materials;
    }

    @Override
    public Iterable<Entity> getLoadedEntities()
    {
        List<Entity> entities = Lists.newArrayList();
        for (org.spongepowered.api.entity.Entity e : getThis().getEntities())
        {
            if (this.entitiesCache.containsKey(e))
            {
                entities.add(this.entitiesCache.get(e));
            } else
            {
                Entity ent = new SpongeEntity(this.context, e);
                this.entitiesCache.put(e, ent);
                entities.add(ent);
            }
        }
        return entities;
    }

    @Override
    public Optional<Chunk> getChunk(int x, int y, int z)
    {
        if (!checkAsyncChunkAccess(x, y, z))
        {
            return Optional.empty();
        }
        org.spongepowered.api.world.Chunk chunk = getThis().getChunk(x, y, z).get();
        if (this.chunks.containsKey(chunk))
        {
            return Optional.of(this.chunks.get(chunk));
        }
        SpongeChunk newChunk = new SpongeChunk(this.context, chunk, this);
        this.chunks.put(chunk, newChunk);
        return Optional.<Chunk>of(newChunk);
    }

    private boolean checkAsyncChunkAccess(int x, int y, int z)
    {
        if (Thread.currentThread() != this.worldThread)
        {
            if (!getThis().getChunk(new com.flowpowered.math.vector.Vector3i(x, y, z)).isPresent())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public Optional<Biome> getBiome(int x, int y, int z)
    {
        org.spongepowered.api.world.biome.BiomeType biome = getThis().getBiome(x, z);
        return this.biomes.getBiome(biome);
    }

    @Override
    public void setBiome(Biome biome, int x, int y, int z)
    {
        if (biome instanceof SpongeBiome)
        {
            SpongeBiome spongeBiome = (SpongeBiome) biome;
            getThis().setBiome(x, z, spongeBiome.getThis());
        }
    }

    @Override
    public Vector3i getChunkSize()
    {
        return SpongeChunk.CHUNK_SIZE;
    }

    @Override
    public void spawnLightning(Vector3i position, Player source)
    {
        Optional<org.spongepowered.api.entity.Entity> ent = getThis().createEntity(EntityTypes.LIGHTNING, SpongeUtilities.getSpongeVector(position));
        if(ent.isPresent()) {
            getThis().spawnEntity(ent.get(), Cause.of(VoxelSniperSponge.instance.getContainer(), ((SpongePlayer) source).getThis()));
        }
    }

    @Override
    public void spawnEntity(EntityType entityType, Vector3d position, Player source) {
        org.spongepowered.api.entity.EntityType spongeEntityType = ((SpongeEntityType) entityType).getEntityType();
        Optional<org.spongepowered.api.entity.Entity> entity = getThis().createEntity(spongeEntityType, SpongeUtilities.getSpongeVector(position));
        if (entity.isPresent()) {
            getThis().spawnEntity(entity.get(), Cause.of(NamedCause.source(VoxelSniperSponge.instance.getContainer()), NamedCause.of("Player",
                    ((SpongePlayer) source).getThis())));
        }
    }
}
