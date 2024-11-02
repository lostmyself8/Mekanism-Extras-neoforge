package com.jerry.mekextras.client.render.tileentity;

import com.jerry.mekextras.common.tile.ExtraTileEntityFluidTank;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.ModelRenderer;
import mekanism.client.render.RenderResizableCuboid;
import mekanism.client.render.tileentity.MekanismTileEntityRenderer;
import mekanism.common.base.ProfilerConstants;
import mekanism.common.util.MekanismUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ExtraRenderFluidTank extends MekanismTileEntityRenderer<ExtraTileEntityFluidTank> {
    private static final Map<FluidStack, Int2ObjectMap<MekanismRenderer.Model3D>> cachedCenterFluids = new HashMap<>();
    private static final Map<FluidStack, Int2ObjectMap<MekanismRenderer.Model3D>> cachedValveFluids = new HashMap<>();

    private static final int stages = 1_400;

    public ExtraRenderFluidTank(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    public static void resetCachedModels() {
        cachedCenterFluids.clear();
        cachedValveFluids.clear();
    }

    @Override
    protected void render(ExtraTileEntityFluidTank tile, float partialTick, @NotNull PoseStack matrix, @NotNull MultiBufferSource renderer, int light, int overlayLight, @NotNull ProfilerFiller profiler) {
        FluidStack fluid = tile.fluidTank.getFluid();
        float fluidScale = tile.prevScale;
        VertexConsumer buffer = null;
        if (!fluid.isEmpty() && fluidScale > 0) {
            buffer = renderer.getBuffer(Sheets.translucentCullBlockSheet());
            MekanismRenderer.renderObject(getFluidModel(fluid, fluidScale), matrix, buffer, MekanismRenderer.getColorARGB(fluid, fluidScale),
                    MekanismRenderer.calculateGlowLight(light, fluid), overlayLight, RenderResizableCuboid.FaceDisplay.FRONT, getCamera(), tile.getBlockPos());
        }
        if (!tile.valveFluid.isEmpty() && !MekanismUtils.lighterThanAirGas(tile.valveFluid)) {
            if (buffer == null) {
                buffer = renderer.getBuffer(Sheets.translucentCullBlockSheet());
            }
            MekanismRenderer.renderObject(getValveModel(tile.valveFluid, fluidScale), matrix, buffer,
                    MekanismRenderer.getColorARGB(tile.valveFluid), MekanismRenderer.calculateGlowLight(light, tile.valveFluid), overlayLight, RenderResizableCuboid.FaceDisplay.FRONT,
                    getCamera(), tile.getBlockPos());
        }
    }

    @Override
    protected @NotNull String getProfilerSection() {
        return ProfilerConstants.FLUID_TANK;
    }

    private MekanismRenderer.Model3D getValveModel(@NotNull FluidStack fluid, float fluidScale) {
        Int2ObjectMap<MekanismRenderer.Model3D> modelMap = cachedValveFluids.computeIfAbsent(fluid, f -> new Int2ObjectOpenHashMap<>());
        int stage = Math.min(stages - 1, (int) (fluidScale * (stages - 1)));
        MekanismRenderer.Model3D model = modelMap.get(stage);
        if (model == null) {
            model = new MekanismRenderer.Model3D()
                    .setSideRender(side -> side.getAxis().isHorizontal())
                    .prepFlowing(fluid)
                    .xBounds(0.3225F, 0.6775F)
                    .yBounds(0.12375F + 0.7525F * (stage / (float) stages), 0.87625F)
                    .zBounds(0.3225F, 0.6775F);
            modelMap.put(stage, model);
        }
        return model;
    }

    public static MekanismRenderer.Model3D getFluidModel(@NotNull FluidStack fluid, float fluidScale) {
        Int2ObjectMap<MekanismRenderer.Model3D> modelMap = cachedCenterFluids.computeIfAbsent(fluid, f -> new Int2ObjectOpenHashMap<>());
        int stage = ModelRenderer.getStage(fluid, stages, fluidScale);
        MekanismRenderer.Model3D model = modelMap.get(stage);
        if (model == null) {
            model = new MekanismRenderer.Model3D()
                    .setTexture(MekanismRenderer.getFluidTexture(fluid, MekanismRenderer.FluidTextureType.STILL))
                    .setSideRender(Direction.DOWN, false)
                    .setSideRender(Direction.UP, stage < stages)
                    .xBounds(0.135F, 0.865F)
                    .yBounds(0.12375F, 0.124F + 0.75225F * (stage / (float) stages))
                    .zBounds(0.135F, 0.865F);
            modelMap.put(stage, model);
        }
        return model;
    }
}
