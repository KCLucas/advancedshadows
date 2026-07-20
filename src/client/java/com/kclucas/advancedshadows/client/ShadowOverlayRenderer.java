package com.kclucas.advancedshadows.client;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.joml.Matrix4f;

public class ShadowOverlayRenderer {

    private static final float OVERLAY_Y_OFFSET = 0.005f;
    private static final float ALPHA = 0.6f;

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        World world = client.world;
        if (world == null || client.player == null) return;

        if (!world.getDimension().hasSkyLight()) return;

        int renderRadius = ModConfig.get().renderRadius;
        int renderYRadius = ModConfig.get().renderYRadius;

        BlockPos playerPos = client.player.getBlockPos();
        Vec3d cam = client.gameRenderer.getCamera().getCameraPos();

        VertexConsumerProvider consumers = context.consumers();
        MatrixStack matrices = context.matrices();

        if (consumers == null) return;

        matrices.push();

        Matrix4f m = matrices.peek().getPositionMatrix();
        VertexConsumer fill = consumers.getBuffer(RenderLayers.debugFilledBox());

        for (int dx = -renderRadius; dx <= renderRadius; dx++) {
            for (int dz = -renderRadius; dz <= renderRadius; dz++) {
                for (int dy = -renderYRadius; dy <= renderYRadius; dy++) {
                    BlockPos pos = playerPos.add(dx, dy, dz);

                    var stateAt = world.getBlockState(pos);

                    boolean isAir = stateAt.isAir();
                    // Wasseroberfläche: source oder flowing, aber kein Wasser drüber
                    boolean isWaterSurface = isWater(stateAt)
                            && !isWater(world.getBlockState(pos.up()));

                    if (!isAir && !isWaterSurface) continue;

                    // Luft direkt über Wasser überspringen — Wasser rendert sich selbst
                    if (isAir && isWater(world.getBlockState(pos.down()))) continue;

                    if (!isWalkableSurface(world, pos.down())) continue;

                    BlockPos lightPos = isWaterSurface ? pos.up() : pos;
                    int skyLight = world.getLightLevel(LightType.SKY, lightPos);

                    float r, g, b;

                    if (skyLight == 0) {
                        r = 1.0f; g = 0.1f; b = 0.1f;
                    } else if (skyLight <= 7) {
                        r = 1.0f; g = 0.5f; b = 0.0f;
                    } else if (skyLight <= 14) {
                        r = 1.0f; g = 1.0f; b = 0.0f;
                    } else {
                        continue;
                    }

                    double x1 = pos.getX() - cam.x;
                    double surfaceY = isWaterSurface ? pos.getY() + 1.0 : pos.getY();
                    double y1 = surfaceY + OVERLAY_Y_OFFSET - cam.y;
                    double x2 = x1 + 1.0;
                    double z1 = pos.getZ() - cam.z;
                    double z2 = z1 + 1.0;

                    quad(fill, m, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, ALPHA);
                    quad(fill, m, x1, y1, z2, x2, y1, z2, x2, y1, z1, x1, y1, z1, r, g, b, ALPHA);
                }
            }
        }

        matrices.pop();
    }

    private static boolean isWater(BlockState state) {
        return !state.getFluidState().isEmpty()
                && state.getFluidState().isOf(net.minecraft.fluid.Fluids.WATER)
                || state.getFluidState().isOf(net.minecraft.fluid.Fluids.FLOWING_WATER);
    }

    private static boolean isWalkableSurface(World world, BlockPos pos) {
        var state = world.getBlockState(pos);
        return state.isSolidBlock(world, pos)
                || state.getBlock() instanceof LeavesBlock
                || isWater(state);
    }

    private static void quad(VertexConsumer v, Matrix4f m,
                             double ax, double ay, double az,
                             double bx, double by, double bz,
                             double cx, double cy, double cz,
                             double dx, double dy, double dz,
                             float r, float g, float b, float a) {
        v.vertex(m, (float) ax, (float) ay, (float) az).color(r, g, b, a);
        v.vertex(m, (float) bx, (float) by, (float) bz).color(r, g, b, a);
        v.vertex(m, (float) cx, (float) cy, (float) cz).color(r, g, b, a);
        v.vertex(m, (float) dx, (float) dy, (float) dz).color(r, g, b, a);
    }
}