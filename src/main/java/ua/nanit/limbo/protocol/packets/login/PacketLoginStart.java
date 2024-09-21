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

package ua.nanit.limbo.protocol.packets.login;

import org.jetbrains.annotations.Nullable;
import ua.nanit.limbo.connection.ClientConnection;
import ua.nanit.limbo.protocol.ByteMessage;
import ua.nanit.limbo.protocol.PacketIn;
import ua.nanit.limbo.protocol.registry.Version;
import ua.nanit.limbo.server.LimboServer;

import java.util.UUID;

// https://github.com/jonesdevelopment/sonar/blob/main/common/src/main/java/xyz/jonesdev/sonar/common/fallback/protocol/packets/login/LoginStartPacket.java
public class PacketLoginStart implements PacketIn {

    private String username;
    private @Nullable UUID uuid;

    public String getUsername() {
        return username;
    }

    public @Nullable UUID getUuid() {
        return uuid;
    }

    @Override
    public void decode(ByteMessage msg, Version version) {
        this.username = msg.readString();

        if (version.compareTo(Version.V1_19) >= 0) {
            if (version.compareTo(Version.V1_19_3) < 0) {
                if (msg.readBoolean()) {
                    msg.readLong(); // expiry
                    msg.readBytesArray(); // key
                    msg.readBytesArray(4096); // signature
                }
            }

            if (version.compareTo(Version.V1_20_2) >= 0) {
                uuid = msg.readUuid();
                return;
            }

            if (version.compareTo(Version.V1_19_1) >= 0) {
                if (msg.readBoolean()) {
                    uuid = msg.readUuid();
                }
            }
        }
    }

    @Override
    public void handle(ClientConnection conn, LimboServer server) {
        server.getPacketHandler().handle(conn, this);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
