/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.datamodules;

import com.google.common.base.Preconditions;
import io.github.nucleuspowered.nucleus.api.exceptions.NoSuchWorldException;
import io.github.nucleuspowered.nucleus.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.dataservices.modular.DataKey;
import io.github.nucleuspowered.nucleus.dataservices.modular.DataModule;
import io.github.nucleuspowered.nucleus.dataservices.modular.ModularUserService;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CoreUserDataModule extends DataModule<ModularUserService> {

    // Transient
    private boolean firstPlay;

    @DataKey("lastKnownName")
    private String lastKnownName;

    @DataKey("locationOnLogin")
    @Nullable
    private LocationNode locationOnLogin;

    @DataKey("lastLocation")
    @Nullable
    private LocationNode lastLocation;

    @DataKey("lastLogin")
    private long login;

    @DataKey("lastLogout")
    private long logout;

    @DataKey("lastIP")
    @Nullable
    private String ipaddress;

    public Optional<Instant> getLastLogin() {
        if (login == 0) {
            return Optional.empty();
        }

        return Optional.of(Instant.ofEpochMilli(login));
    }

    public void setLastLogin(Instant login) {
        this.login = login.toEpochMilli();
    }

    public Optional<Instant> getLastLogout() {
        if (this.logout == 0) {
            return Optional.empty();
        }

        return Optional.of(Instant.ofEpochMilli(this.logout));
    }

    public void setLastLogout(Location<World> location) {
        this.logout = System.currentTimeMillis();
        this.lastLocation = new LocationNode(location);
    }

    public Optional<Location<World>> getLogoutLocation() {
        if (locationOnLogin != null) {
            try {
                return Optional.ofNullable(locationOnLogin.getLocation());
            } catch (NoSuchWorldException | NullPointerException e) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    public Optional<Location<World>> getLocationOnLogin() {
        if (lastLocation == null) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(lastLocation.getLocation());
        } catch (NoSuchWorldException e) {
            return Optional.empty();
        }
    }

    public void sendToLocationOnLogin(@Nonnull Location<World> worldLocation) {
        Preconditions.checkNotNull(worldLocation);
        this.locationOnLogin = new LocationNode(worldLocation);
    }

    public void removeLocationOnLogin() {
        this.locationOnLogin = null;
    }

    public Optional<String> getLastIp() {
        return Optional.ofNullable(ipaddress);
    }

    public void setLastIp(InetAddress address) {
        this.ipaddress = address.toString();
    }

    public Optional<String> getLastKnownName() {
        return Optional.ofNullable(lastKnownName);
    }

    public void setLastKnownName(String lastKnownName) {
        this.lastKnownName = lastKnownName;
    }

    public boolean isFirstPlay() {
        return firstPlay;
    }

    public void setFirstPlay(boolean firstPlay) {
        this.firstPlay = firstPlay;
    }
}