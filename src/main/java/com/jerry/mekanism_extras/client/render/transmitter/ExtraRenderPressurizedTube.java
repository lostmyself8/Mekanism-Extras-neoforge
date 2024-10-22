package com.jerry.mekanism_extras.client.render.transmitter;

import com.jerry.mekanism_extras.common.content.network.transmitter.ExtraPressurizedTube;
import com.jerry.mekanism_extras.common.tile.transmitter.ExtraTileEntityPressurizedTube;
import com.mojang.blaze3d.vertex.PoseStack;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.Chemical;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.transmitter.RenderTransmitterBase;
import mekanism.common.base.ProfilerConstants;
import mekanism.common.content.network.ChemicalNetwork;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

@NothingNullByDefault
public class ExtraRenderPressurizedTube extends RenderTransmitterBase<ExtraTileEntityPressurizedTube> {
    public ExtraRenderPressurizedTube(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void render(@NotNull ExtraTileEntityPressurizedTube tile, float partialTick, PoseStack matrix, MultiBufferSource renderer, int light, int overlayLight, ProfilerFiller profiler) {
        ChemicalNetwork network = tile.getTransmitter().getTransmitterNetwork();
        matrix.pushPose();
        matrix.translate(0.5, 0.5, 0.5);
        Chemical chemical = network.lastChemical.getChemical();
        renderModel(tile, matrix, renderer.getBuffer(Sheets.translucentCullBlockSheet()), chemical.getTint(), Math.max(0.2F, network.currentScale),
                LightTexture.FULL_BRIGHT, overlayLight, MekanismRenderer.getChemicalTexture(chemical));
        matrix.popPose();
    }

    @Override
    protected @NotNull String getProfilerSection() {
        return ProfilerConstants.PRESSURIZED_TUBE;
    }

    @Override
    protected boolean shouldRenderTransmitter(@NotNull ExtraTileEntityPressurizedTube tile, @NotNull Vec3 camera) {
        if (super.shouldRenderTransmitter(tile, camera)) {
            ExtraPressurizedTube tube = tile.getTransmitter();
            if (tube.hasTransmitterNetwork()) {
                ChemicalNetwork network = tube.getTransmitterNetwork();
                return !network.lastChemical.isEmptyType() && !network.isEmpty() && network.currentScale > 0;
            }
        }
        return false;
    }
}
