/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.qsml.module;

import com.google.inject.Inject;
import com.google.inject.Injector;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.config.CommandsConfig;
import io.github.nucleuspowered.nucleus.internal.InternalServiceManager;
import io.github.nucleuspowered.nucleus.internal.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.TaskBase;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.annotations.SkipOnError;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.CommandBuilder;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Task;
import uk.co.drnaylor.quickstart.Module;
import uk.co.drnaylor.quickstart.config.AbstractConfigAdapter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class StandardModule implements Module {

    private String packageName;
    @Inject protected Nucleus nucleus;
    @Inject protected InternalServiceManager serviceManager;

    @Inject
    private CommandsConfig commandsConfig;

    /**
     * Non-configurable module, no configuration to register.
     *
     * @return {@link Optional#empty()}
     */
    @Override
    public Optional<AbstractConfigAdapter<?>> getConfigAdapter() {
        return Optional.empty();
    }

    @Override
    public final void preEnable() {
        try {
            performPreTasks();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot enable module!", e);
        }
    }

    @Override
    public void onEnable() {
        packageName = this.getClass().getPackage().getName() + ".";

        // Construct commands
        loadCommands();
        loadEvents();
        loadRunnables();
    }

    @SuppressWarnings("unchecked")
    private void loadCommands() {
        Set<Class<? extends AbstractCommand<?>>> cmds = getStreamForModule(AbstractCommand.class)
                .filter(x -> x.isAnnotationPresent(RegisterCommand.class))
                .map(x -> (Class<? extends AbstractCommand<?>>)x) // Keeping the compiler happy...
                .collect(Collectors.toSet());

        // We all love the special injector. We just want to provide the module with more commands, in case it needs a child.
        Injector injector = nucleus.getInjector();

        Set<Class<? extends AbstractCommand>> commandBases =  cmds.stream().filter(x -> {
            RegisterCommand rc = x.getAnnotation(RegisterCommand.class);
            return (rc != null && rc.subcommandOf().equals(AbstractCommand.class));
        }).collect(Collectors.toSet());

        CommandBuilder builder = new CommandBuilder(nucleus, injector, cmds);
        commandBases.forEach(builder::buildCommand);

        try {
            commandsConfig.mergeDefaults(builder.getNodeToMerge());
            commandsConfig.save();
        } catch (IOException | ObjectMappingException e) {
            nucleus.getLogger().error("Could not save defaults.");
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadEvents() {
        Set<Class<? extends ListenerBase>> commandsToLoad = getStreamForModule(ListenerBase.class)
                .collect(Collectors.toSet());

        Injector injector = nucleus.getInjector();
        commandsToLoad.stream().map(x -> this.getInstance(injector, x)).filter(lb -> lb != null).forEach(c -> {
            // Register suggested permissions
            c.getPermissions().forEach((k, v) -> nucleus.getPermissionRegistry().registerOtherPermission(k, v));
            Sponge.getEventManager().registerListeners(nucleus, c);
        });
    }

    @SuppressWarnings("unchecked")
    private void loadRunnables() {
        Set<Class<? extends TaskBase>> commandsToLoad = getStreamForModule(TaskBase.class)
                .collect(Collectors.toSet());

        Injector injector = nucleus.getInjector();
        commandsToLoad.stream().map(x -> this.getInstance(injector, x)).filter(lb -> lb != null).forEach(c -> {
            c.getPermissions().forEach((k, v) -> nucleus.getPermissionRegistry().registerOtherPermission(k, v));
            TaskBase.TimePerRun tpr = c.interval();
            Task.Builder tb = Sponge.getScheduler().createTaskBuilder().execute(c).interval(tpr.getTime(), tpr.getUnit());
            if (c.isAsync()) {
                tb.async();
            }

            tb.submit(nucleus);
        });
    }

    @SuppressWarnings("unchecked")
    private <T> Stream<Class<? extends T>> getStreamForModule(Class<T> assignableClass) {
        return nucleus.getModuleContainer().getLoadedClasses().stream().filter(assignableClass::isAssignableFrom)
                .filter(x -> x.getPackage().getName().startsWith(packageName))
                .map(x -> (Class<? extends T>)x);
    }

    protected void performPreTasks() throws Exception { }

    private <T> T getInstance(Injector injector, Class<T> clazz) {
        try {
            return injector.getInstance(clazz);

        // I can't believe I have to do this...
        } catch (RuntimeException | NoClassDefFoundError e) {
            if (clazz.isAnnotationPresent(SkipOnError.class)) {
                nucleus.getLogger().warn(Util.getMessageWithFormat("startup.injectablenotloaded", clazz.getName()));
                return null;
            }

            throw e;
        }
    }
}