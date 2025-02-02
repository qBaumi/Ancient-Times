package com.delu.ancienttimes.client.model;

import com.delu.ancienttimes.AncientTimes;
import com.delu.ancienttimes.common.entity.Scalemouflis;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ScalemouflisModel extends GeoModel<Scalemouflis> {
    @Override
    public ResourceLocation getModelResource(Scalemouflis scalemouflis) {
        return AncientTimes.geo("scalemouflis/scalemouflis.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Scalemouflis scalemouflis) {
        if (scalemouflis.hasCustomName() && "Rudolph".equals(scalemouflis.getCustomName().getString())) {
            // Use the Rudolph texture if the name is "Rudolph"
            return AncientTimes.entityTexture("scalemouflis/scalemouflis_rudolph.png");
        }
        return scalemouflis.getVariant().getTexture();
    }

    @Override
    public ResourceLocation getAnimationResource(Scalemouflis scalemouflis) {
        return AncientTimes.animations("scalemouflis.animation.json");
    }
}
