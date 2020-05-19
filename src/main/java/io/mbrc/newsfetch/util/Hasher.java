package io.mbrc.newsfetch.util;

import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class Hasher {

    private final MessageDigest messageDigest;

    private Hasher() throws NoSuchAlgorithmException {
        this.messageDigest = MessageDigest.getInstance("SHA-256");
    }

    public String hash(String contents) {
        byte[] hash = messageDigest.digest(contents.getBytes(StandardCharsets.UTF_16));
        return Hex.encodeHexString(hash);
    }

}
