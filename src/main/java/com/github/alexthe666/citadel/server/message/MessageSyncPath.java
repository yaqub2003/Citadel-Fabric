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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Message to sync some path over to the client.
 * Переписано под структуру, аналогичную PropertiesMessage.
 */
public class MessageSyncPath implements CitadelPacket {
    public final Set<MNode> lastDebugNodesVisited;
    public final Set<MNode> lastDebugNodesNotVisited;
    public final Set<MNode> lastDebugNodesPath;

    public MessageSyncPath(final Set<MNode> lastDebugNodesVisited, final Set<MNode> lastDebugNodesNotVisited, final Set<MNode> lastDebugNodesPath) {
        this.lastDebugNodesVisited = lastDebugNodesVisited != null ? lastDebugNodesVisited : new HashSet<>();
        this.lastDebugNodesNotVisited = lastDebugNodesNotVisited != null ? lastDebugNodesNotVisited : new HashSet<>();
        this.lastDebugNodesPath = lastDebugNodesPath != null ? lastDebugNodesPath : new HashSet<>();
    }

    // === serialization helpers ===

    public static void write(MessageSyncPath message, FriendlyByteBuf packetBuffer) {
        packetBuffer.writeInt(message.lastDebugNodesVisited.size());
        for (MNode node : message.lastDebugNodesVisited) {
            node.serializeToBuf(packetBuffer);
        }

        packetBuffer.writeInt(message.lastDebugNodesNotVisited.size());
        for (MNode node : message.lastDebugNodesNotVisited) {
            node.serializeToBuf(packetBuffer);
        }

        packetBuffer.writeInt(message.lastDebugNodesPath.size());
        for (MNode node : message.lastDebugNodesPath) {
            node.serializeToBuf(packetBuffer);
        }
    }

    public static MessageSyncPath read(FriendlyByteBuf packetBuffer) {
        int size = packetBuffer.readInt();
        Set<MNode> visited = new HashSet<>();
        for (int i = 0; i < size; i++) {
            visited.add(new MNode(packetBuffer));
        }

        size = packetBuffer.readInt();
        Set<MNode> notVisited = new HashSet<>();
        for (int i = 0; i < size; i++) {
            notVisited.add(new MNode(packetBuffer));
        }

        size = packetBuffer.readInt();
        Set<MNode> path = new HashSet<>();
        for (int i = 0; i < size; i++) {
            path.add(new MNode(packetBuffer));
        }

        return new MessageSyncPath(visited, notVisited, path);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        write(this, buf);
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl listener, PacketSender responseSender, SimpleChannel channel) {
        // Запрос пришёл на сервер — сообщение ориентировано на клиента, поэтому на сервере ничего не делаем.
        Handler.handle(this, server, player);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void handle(Minecraft client, ClientPacketListener listener, PacketSender responseSender, SimpleChannel channel) {
        Handler.handle(this, client, Citadel.PROXY.getClientSidePlayer());
    }

    public static class Handler {
        /**
         * Общий обработчик: выполняется в потоке игрового цикла (loop).
         * Если вызывается на клиенте — обновляет PathfindingDebugRenderer.
         * На сервере — в текущей реализации не делает ничего (сообщение клиент-орентированное).
         */
        public static void handle(final MessageSyncPath message, BlockableEventLoop<?> loop, Player player) {
            loop.execute(() -> {
                if (player.level().isClientSide()) {
                    // клиентская сторона: обновляем отладочные наборы
                    PathfindingDebugRenderer.lastDebugNodesVisited = message.lastDebugNodesVisited;
                    PathfindingDebugRenderer.lastDebugNodesNotVisited = message.lastDebugNodesNotVisited;
                    PathfindingDebugRenderer.lastDebugNodesPath = message.lastDebugNodesPath;
                } else {
                    // серверная сторона: по исходному поведению сообщения — ничего не делаем.
                    // Если нужно — здесь можно распространить пакет другим клиентам или обработать на сервере.
                }
            });
        }
    }
}
