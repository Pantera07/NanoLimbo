package ua.nanit.limbo.connection;

public record PlayerPublicKey(long expiry, byte[] key, byte[] signature) {

}

