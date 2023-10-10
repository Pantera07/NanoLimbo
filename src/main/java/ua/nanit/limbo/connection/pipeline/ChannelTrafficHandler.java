package ua.nanit.limbo.connection.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.jetbrains.annotations.NotNull;
import ua.nanit.limbo.server.Logger;

public class ChannelTrafficHandler extends ChannelInboundHandlerAdapter {

    private static final long NANO_IN_SEC = 1_000_000_000L;

    private final int packetSize;
    private final double interval;
    private final double maxPacketRate;
    private final IntervalCounter globalPacketCounter;

    public ChannelTrafficHandler(int packetSize, double interval, double maxPacketRate) {
        this.packetSize = packetSize;
        this.interval = interval * NANO_IN_SEC;
        this.maxPacketRate = maxPacketRate;
        this.globalPacketCounter = new IntervalCounter(this.interval);
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

            if (interval > 0 && maxPacketRate > 0) {
                globalPacketCounter.updateAndAdd(1, currentTime);
                if (globalPacketCounter.getRate() > maxPacketRate) {
                    double spamPackets = globalPacketCounter.getRate();
                    closeConnection(ctx, "Closed %s due to many packets sent (%.2f per sec)", ctx.channel().remoteAddress(), spamPackets);
                    return;
                }
            }
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
            this.interval = (long) interval;
        }

        public void updateAndAdd(int increment, long currentTime) {
            if (currentTime - lastTime > interval) {
                count = 0;
                lastTime = currentTime;
            }
            count += increment;
        }

        public double getRate() {
            return (double) count / interval * NANO_IN_SEC;
        }
    }
}
