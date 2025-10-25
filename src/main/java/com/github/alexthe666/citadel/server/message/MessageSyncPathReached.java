package com.github.alexthe666.citadel.server.message;

import com.github.alexthe666.citadel.Citadel;
import com.github.alexthe666.citadel.client.render.pathfinding.PathfindingDebugRenderer;
import com.github.alexthe666.citadel.server.entity.pathfinding.raycoms.MNode;
import me.pepperbell.simplenetworking.SimpleChannel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;

/**
 * Message to sync the reached positions over to the client for rendering.
 * Fabric-style, same structure as PropertiesMessage and MessageSyncPath.
 */
public class MessageSyncPathReached implements CitadelPacket {

    /**
     * Set of reached positions.
     */
    public final Set<BlockPos> reached;

    public MessageSyncPathReached(final Set<BlockPos> reached) {
        this.reached = reached != null ? reached : new HashSet<>();
    }

    // === serialization ===

    public static void write(MessageSyncPathReached message, FriendlyByteBuf buf) {
        buf.writeInt(message.reached.size());
        for (BlockPos pos : message.reached) {
            buf.writeBlockPos(pos);
        }
    }

    public static MessageSyncPathReached read(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Set<BlockPos> reached = new HashSet<>();
        for (int i = 0; i < size; i++) {
            reached.add(buf.readBlockPos());
        }
        return new MessageSyncPathReached(reached);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        write(this, buf);
    }

    // === handling ===

    @Override
    public void handle(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl listener, PacketSender responseSender, SimpleChannel channel) {
        Handler.handle(this, server, player);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void handle(Minecraft client, ClientPacketListener listener, PacketSender responseSender, SimpleChannel channel) {
        Handler.handle(this, client, Citadel.PROXY.getClientSidePlayer());
    }

    public static class Handler {

        public static void handle(final MessageSyncPathReached message, BlockableEventLoop<?> loop, Player player) {
            loop.execute(() -> {
                if (player.level().isClientSide()) {
                    // Клиентская часть — обновляем узлы пути, помечая достигнутые.
                    for (MNode node : PathfindingDebugRenderer.lastDebugNodesPath) {
                        if (message.reached.contains(node.pos)) {
                            node.setReachedByWorker(true);
                        }
                    }
                }
            });
        }
    }
}
