/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warn.datamodules;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Warning;
import io.github.nucleuspowered.nucleus.dataservices.modular.DataKey;
import io.github.nucleuspowered.nucleus.dataservices.modular.DataModule;
import io.github.nucleuspowered.nucleus.dataservices.modular.ModularUserService;
import io.github.nucleuspowered.nucleus.modules.warn.data.WarnData;

import java.util.List;

public class WarnUserDataModule extends DataModule<ModularUserService> {

    @DataKey("warnings")
    private List<WarnData> warnings = Lists.newArrayList();

    public List<WarnData> getWarnings() {
        return ImmutableList.copyOf(warnings);
    }

    public void setWarnings(List<WarnData> warnings) {
        this.warnings = warnings;
    }

    public void addWarning(WarnData warning) {
        if (warnings == null) {
            warnings = Lists.newArrayList();
        }

        warnings.add(warning);
        warnings.sort((x, y) -> Boolean.compare(x.isExpired(), y.isExpired()));
    }

    public boolean removeWarning(Warning warning) {
        return warnings.removeIf(x -> x.equals(warning));
    }

    public boolean clearWarnings() {
        if (!warnings.isEmpty()) {
            warnings.clear();
            return true;
        } else {
            return false;
        }
    }
}
