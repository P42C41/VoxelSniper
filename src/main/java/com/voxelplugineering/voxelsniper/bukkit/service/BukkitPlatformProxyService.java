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
package com.voxelplugineering.voxelsniper.bukkit.service;

import com.voxelplugineering.voxelsniper.CommonPlatformProxyService;
import com.voxelplugineering.voxelsniper.bukkit.config.BukkitConfiguration;
import com.voxelplugineering.voxelsniper.util.Context;

import java.io.File;

/**
 * A proxy for Bukkit's platform.
 */
public class BukkitPlatformProxyService extends CommonPlatformProxyService
{

    private File metricsConf;

    /**
     * Creates a new {@link BukkitPlatformProxyService}.
     * 
     * @param data The data folder
     */
    public BukkitPlatformProxyService(Context context, File data)
    {
        super(context, data);
        this.metricsConf = new File(this.rootDir.getParentFile(), BukkitConfiguration.metricsConf);
    }

    @Override
    public String getPlatformName()
    {
        return org.bukkit.Bukkit.getName();
    }

    @Override
    public String getVersion()
    {
        return org.bukkit.Bukkit.getVersion();
    }

    @Override
    public String getFullVersion()
    {
        return org.bukkit.Bukkit.getBukkitVersion();
    }

    @Override
    public int getNumberOfPlayersOnline()
    {
        return org.bukkit.Bukkit.getOnlinePlayers().size();
    }

    @Override
    public File getMetricsFile()
    {
        return this.metricsConf;
    }

}
