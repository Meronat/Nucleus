/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mob.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.argumentparsers.ImprovedEntityParser;
import io.github.nucleuspowered.nucleus.argumentparsers.PositiveIntegerArgument;
import io.github.nucleuspowered.nucleus.internal.CommandBase;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.mob.config.MobConfigAdapter;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Permissions
@RegisterCommand({"spawnmob", "spawnentity"})
public class SpawnMobCommand extends CommandBase<CommandSource> {

    private final String playerKey = "player";
    private final String amountKey = "amount";
    private final String mobTypeKey = "mob";

    @Inject private MobConfigAdapter mobConfigAdapter;

    @Override
    public CommandSpec createSpec() {
        return CommandSpec.builder().executor(this).arguments(
                GenericArguments.optionalWeak(GenericArguments.requiringPermission(GenericArguments.player(Text.of(playerKey)), permissions.getPermissionWithSuffix("others"))),
                new ImprovedEntityParser(Text.of(mobTypeKey)),
                GenericArguments.optional(new PositiveIntegerArgument(Text.of(amountKey)), 1)
        ).build();
    }

    @Override
    protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("others", new PermissionInformation(Util.getMessageWithFormat("permission.spawnmob.other"), SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        Optional<Player> opl = this.getUser(Player.class, src, playerKey, args);
        if (!opl.isPresent()) {
            return CommandResult.empty();
        }

        // Get the amount
        int amount = args.<Integer>getOne(amountKey).get();
        EntityType et = args.<EntityType>getOne(mobTypeKey).get();

        if (!Living.class.isAssignableFrom(et.getEntityClass())) {
            src.sendMessage(Util.getTextMessageWithFormat("command.spawnmob.livingonly", et.getTranslation().get()));
            return CommandResult.empty();
        }

        Location<World> loc = opl.get().getLocation();
        World w = loc.getExtent();

        // Count the number of entities spawned.
        int i = 0;
        do {
            Optional<Entity> e = w.createEntity(et, loc.getPosition());
            if (e.isPresent() && w.spawnEntity(e.get(), Cause.of(NamedCause.source(opl.get())))) {
                i++;
            }
        } while (i < Math.min(amount, mobConfigAdapter.getNodeOrDefault().getMaxMobsToSpawn()));

        if (i == 0) {
            src.sendMessage(Util.getTextMessageWithFormat("command.spawnmob.fail", et.getTranslation().get()));
            return CommandResult.empty();
        }

        if (i == 1) {
            src.sendMessage(Util.getTextMessageWithFormat("command.spawnmob.success.singular", String.valueOf(i), et.getTranslation().get()));
        } else {
            src.sendMessage(Util.getTextMessageWithFormat("command.spawnmob.success.plural", String.valueOf(i), et.getTranslation().get()));
        }
        return CommandResult.success();
    }
}