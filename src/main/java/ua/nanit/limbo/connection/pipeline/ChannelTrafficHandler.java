package ua.nanit.limbo.connection.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.jetbrains.annotations.NotNull;
import ua.nanit.limbo.server.Log;

import java.util.Arrays;

public class ChannelTrafficHandler extends ChannelInboundHandlerAdapter {

    private final int maxPacketSize;
    private final double maxPacketRate;
    private final double maxPacketBytesRate;
    private final PacketBucket packetBucket;

    public ChannelTrafficHandler(int maxPacketSize, double interval, double maxPacketRate, double maxPacketBytesRate) {
        this.maxPacketSize = maxPacketSize;
        this.maxPacketRate = maxPacketRate;
        this.maxPacketBytesRate = maxPacketBytesRate;
        this.packetBucket = (interval > 0.0 && (maxPacketRate > 0.0 || maxPacketBytesRate > 0.0)) ? new PacketBucket(interval * 1000.0, 150) : null;
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf in = (ByteBuf) msg;
            int bytes = in.readableBytes();

            if (maxPacketSize > 0 && bytes > maxPacketSize) {
                closeConnection(ctx, "Closed %s due to large packet size (%d bytes)", ctx.channel().remoteAddress(), bytes);
                return;
            }

            if (packetBucket != null) {
                packetBucket.recordPacket(1, bytes);
                if (packetBucket.getCurrentPacketRate() > maxPacketRate) {
                    closeConnection(ctx, "Closed %s due to many packets sent (%d in the last %.1f seconds)", ctx.channel().remoteAddress(), packetBucket.sumPackets, (packetBucket.intervalTime / 1000.0));
                    return;
                }
                if (packetBucket.getCurrentPacketBytesRate() > maxPacketBytesRate) {
                    closeConnection(ctx, "Closed %s due to many bytes sent (%d in the last %.1f seconds)", ctx.channel().remoteAddress(), packetBucket.sumBytes, (packetBucket.intervalTime / 1000.0));
                    return;
                }
            }
        }

        super.channelRead(ctx, msg);
    }

    private void closeConnection(ChannelHandlerContext ctx, String reason, Object... args) {
        ctx.close();
        Log.info(reason, args);
    }

    private static class PacketBucket {
        private static final double NANOSECONDS_TO_MILLISECONDS = 1.0e-6;
        private static final int MILLISECONDS_TO_SECONDS = 1000;

        private final double intervalTime;
        private final double intervalResolution;
        private final int[] packetsData;
        private final int[] bytesData;
        private int newestData;
        private double lastBucketTime;
        private int sumPackets;
        private int sumBytes;

        public PacketBucket(final double intervalTime, final int totalBuckets) {
            this.intervalTime = intervalTime;
            this.intervalResolution = intervalTime / totalBuckets;
            this.packetsData = new int[totalBuckets];
            this.bytesData = new int[totalBuckets];
        }

        public void recordPacket(final int packets, final int bytes) {
            double timeMs = System.nanoTime() * NANOSECONDS_TO_MILLISECONDS;
            double timeDelta = timeMs - this.lastBucketTime;

            if (timeDelta < 0.0) {
                timeDelta = 0.0;
            }

            if (timeDelta < this.intervalResolution) {
                this.packetsData[this.newestData] += packets;
                this.bytesData[this.newestData] += bytes;
                this.sumPackets += packets;
                this.sumBytes += bytes;
                return;
            }

            int bucketsToMove = (int)(timeDelta / this.intervalResolution);
            double nextBucketTime = this.lastBucketTime + bucketsToMove * this.intervalResolution;

            if (bucketsToMove >= this.packetsData.length) {
                Arrays.fill(this.packetsData, 0);
                Arrays.fill(this.bytesData, 0);
                this.packetsData[0] = packets;
                this.bytesData[0] = bytes;
                this.sumPackets = packets;
                this.sumBytes = bytes;
                this.newestData = 0;
                this.lastBucketTime = timeMs;
                return;
            }

            for (int i = 1; i < bucketsToMove; ++i) {
                int index = (this.newestData + i) % this.packetsData.length;
                this.sumPackets -= this.packetsData[index];
                this.sumBytes -= this.bytesData[index];
                this.packetsData[index] = 0;
                this.bytesData[index] = 0;
            }

            int newestDataIndex = (this.newestData + bucketsToMove) % this.packetsData.length;
            this.sumPackets += packets - this.packetsData[newestDataIndex];
            this.sumBytes += bytes - this.bytesData[newestDataIndex];
            this.packetsData[newestDataIndex] = packets;
            this.bytesData[newestDataIndex] = bytes;
            this.newestData = newestDataIndex;
            this.lastBucketTime = nextBucketTime;
        }

        public double getCurrentPacketRate() {
            return this.sumPackets / (this.intervalTime / MILLISECONDS_TO_SECONDS);
        }

        public double getCurrentPacketBytesRate() {
            return this.sumBytes / (this.intervalTime / MILLISECONDS_TO_SECONDS);
        }
    }
}
