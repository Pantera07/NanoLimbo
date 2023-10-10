package ua.nanit.limbo.connection.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.jetbrains.annotations.NotNull;
import ua.nanit.limbo.server.Logger;

import java.util.HashMap;
import java.util.Map;

public class ChannelTrafficHandler extends ChannelInboundHandlerAdapter {

    private static final long NANO_IN_SEC = 1_000_000_000L;

    private final int packetSize;
    private final double maxPacketsPerInterval;
    private final double interval;
    private final Map<Class<?>, IntervalCounter> specificPacketCounters = new HashMap<>();

    private IntervalCounter globalPacketCounter;

    public ChannelTrafficHandler(int packetSize, double maxPacketsPerInterval, double interval) {
        this.packetSize = packetSize;
        this.maxPacketsPerInterval = maxPacketsPerInterval;
        this.interval = interval * NANO_IN_SEC;

        this.globalPacketCounter = new IntervalCounter(interval);
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf in = (ByteBuf) msg;
            int bytes = in.readableBytes();

            if (packetSize > 0 && bytes > packetSize) {
                closeConnection(ctx, "Closed %s due to large packet size (%d bytes)", ctx.channel().remoteAddress(), bytes);
                return;
            }

            long currentTime = System.nanoTime();

            globalPacketCounter.updateAndAdd(1, currentTime);
            if (globalPacketCounter.getRate() > maxPacketsPerInterval) {
                closeConnection(ctx, "Closed %s due to packet spamming", ctx.channel().remoteAddress());
                return;
            }

            // Further logic for specific packet types can be added similarly
        }

        super.channelRead(ctx, msg);
    }

    private void closeConnection(ChannelHandlerContext ctx, String reason, Object... args) {
        ctx.close();
        Logger.info(reason, args);
    }

    private static class IntervalCounter {
        private final long interval;
        private long lastTime;
        private int count;

        public IntervalCounter(double interval) {
            this.interval = (long) (interval * NANO_IN_SEC);
        }

        public void updateAndAdd(int increment, long currentTime) {
            if (currentTime - lastTime > interval) {
                count = 0;
                lastTime = currentTime;
            }
            count += increment;
        }

        public double getRate() {
            return count / (double) interval * NANO_IN_SEC;
        }
    }
}
