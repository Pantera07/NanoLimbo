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

package ua.nanit.limbo.configuration.serializers;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import ua.nanit.limbo.server.data.InfoForwarding;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class InfoForwardingSerializer implements TypeSerializer<InfoForwarding> {

    @Override
    public InfoForwarding deserialize(java.lang.reflect.Type type, ConfigurationNode node) throws SerializationException {
        InfoForwarding forwarding = new InfoForwarding();

        try {
            forwarding.setType(InfoForwarding.Type.valueOf(node.node("type").getString("").toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            throw new SerializationException("Undefined info forwarding type");
        }

        if (forwarding.getType() == InfoForwarding.Type.MODERN) {
            forwarding.setSecretKey(node.node("secret").getString("").getBytes(StandardCharsets.UTF_8));
        }

        if (forwarding.getType() == InfoForwarding.Type.BUNGEE_GUARD) {
            forwarding.setTokens(node.node("tokens").getList(String.class));
        }

        return forwarding;
    }

    @Override
    public void serialize(java.lang.reflect.Type type, InfoForwarding obj, ConfigurationNode node) {
    }

}
