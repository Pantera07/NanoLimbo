/*
 * Copyright (C) 2020 Nan1t
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ua.nanit.limbo.connection.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import io.netty.util.ByteProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class VarIntFrameDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (!ctx.channel().isActive()) {
            in.clear();
            return;
        }

        // Skip any runs of 0x00 we might find
        final int packetStart = in.forEachByte(ByteProcessor.FIND_NON_NUL);
        if (packetStart == -1) {
            return;
        }
        in.readerIndex(packetStart);

        // Try to read the length of the packet
        in.markReaderIndex();
        final int preIndex = in.readerIndex();
        final int length = readRawVarInt21(in);
        if (preIndex == in.readerIndex()) {
            return;
        }
        if (length <= 0) {
            throw new DecoderException("Bad VarInt length: " + length);
        }

        if (in.readableBytes() < length) {
            in.resetReaderIndex();
        } else {
            out.add(in.readRetainedSlice(length));
        }
    }

    private static int readRawVarInt21(final @NotNull ByteBuf byteBuf) {
        if (byteBuf.readableBytes() < 4) {
            return readRawVarIntSmallBuffer(byteBuf);
        }

        // take the last three bytes and check if any of them have the high bit set
        final int wholeOrMore = byteBuf.getIntLE(byteBuf.readerIndex());
        final int atStop = ~wholeOrMore & 0x808080;
        if (atStop == 0) {
            throw new DecoderException("VarInt too big");
        }

        final int bitsToKeep = Integer.numberOfTrailingZeros(atStop) + 1;
        byteBuf.skipBytes(bitsToKeep >> 3);

        // https://github.com/netty/netty/pull/14050#issuecomment-2107750734
        int preservedBytes = wholeOrMore & (atStop ^ (atStop - 1));

        // https://github.com/netty/netty/pull/14050#discussion_r1597896639
        preservedBytes = (preservedBytes & 0x007F007F) | ((preservedBytes & 0x00007F00) >> 1);
        preservedBytes = (preservedBytes & 0x00003FFF) | ((preservedBytes & 0x3FFF0000) >> 2);
        return preservedBytes;
    }

    private static int readRawVarIntSmallBuffer(final @NotNull ByteBuf byteBuf) {
        if (!byteBuf.isReadable()) {
            return 0;
        }
        byteBuf.markReaderIndex();

        byte tmp = byteBuf.readByte();
        if (tmp >= 0) {
            return tmp;
        }
        int result = tmp & 0x7F;
        if (!byteBuf.isReadable()) {
            byteBuf.resetReaderIndex();
            return 0;
        }
        if ((tmp = byteBuf.readByte()) >= 0) {
            return result | tmp << 7;
        }
        result |= (tmp & 0x7F) << 7;
        if (!byteBuf.isReadable()) {
            byteBuf.resetReaderIndex();
            return 0;
        }
        if ((tmp = byteBuf.readByte()) >= 0) {
            return result | tmp << 14;
        }
        return result | (tmp & 0x7F) << 14;
    }
}