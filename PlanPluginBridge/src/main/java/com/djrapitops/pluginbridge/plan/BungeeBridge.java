/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package com.djrapitops.pluginbridge.plan;

import com.djrapitops.plan.system.settings.config.PlanConfig;
import com.djrapitops.plugin.logging.error.ErrorHandler;
import com.djrapitops.pluginbridge.plan.advancedban.AdvancedBanHook;
import com.djrapitops.pluginbridge.plan.buycraft.BuyCraftHook;
import com.djrapitops.pluginbridge.plan.litebans.LiteBansBungeeHook;
import com.djrapitops.pluginbridge.plan.luckperms.LuckPermsHook;
import com.djrapitops.pluginbridge.plan.viaversion.ViaVersionBungeeHook;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Plugin bridge for BungeeCord plugins.
 *
 * @author Rsl1122
 */
@Singleton
public class BungeeBridge extends Bridge {

    private final AdvancedBanHook advancedBanHook;
    private final BuyCraftHook buyCraftHook;
    private final LiteBansBungeeHook liteBansHook;
    private final LuckPermsHook luckPermsHook;
    private final ViaVersionBungeeHook viaVersionHook;

    @Inject
    public BungeeBridge(
            PlanConfig config,
            ErrorHandler errorHandler,

            AdvancedBanHook advancedBanHook,
            BuyCraftHook buyCraftHook,
            LiteBansBungeeHook liteBansHook,
            LuckPermsHook luckPermsHook,
            ViaVersionBungeeHook viaVersionHook
    ) {
        super(config, errorHandler);
        this.advancedBanHook = advancedBanHook;
        this.buyCraftHook = buyCraftHook;
        this.liteBansHook = liteBansHook;
        this.luckPermsHook = luckPermsHook;
        this.viaVersionHook = viaVersionHook;
    }

    @Override
    Hook[] getHooks() {
        return new Hook[]{
                advancedBanHook,
                buyCraftHook,
                liteBansHook,
                luckPermsHook,
                viaVersionHook
        };
    }
}