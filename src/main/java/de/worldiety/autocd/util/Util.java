package de.worldiety.autocd.util;

import de.worldiety.autocd.docker.Docker;
import de.worldiety.autocd.env.Environment;
import de.worldiety.autocd.persistence.AutoCD;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class Util {
    public static final String CLOUDIETY_DOMAIN = ".cloudiety.de";

    public static String buildSubdomain(Environment environment, String buildType, String hash) {
        if (isLocal(environment)) {
            return "local-test" + CLOUDIETY_DOMAIN;
        }

        return environment.getProjectName() +
                "-" +
                environment.getProjectNamespace().replaceAll("/", "--") +
                "-" +
                buildType +
                "-" +
                hash +
                environment.getDomainBase();
    }


    public static boolean isLocal(Environment environment) {
        var reg = environment.getRegistryUrl();
        return reg == null;
    }


    public static void pushDockerAndSetPath(Environment environment, File dockerfile, AutoCD autoCD, String buildType) {
        var dockerClient = new Docker(environment);
        var tag = dockerClient.buildAndPushImageFromFile(dockerfile, buildType);
        autoCD.setRegistryImagePath(tag);
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }


    public static String hash(String toHash) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] encodedhash = Objects.requireNonNull(digest).digest(
                toHash.getBytes(StandardCharsets.UTF_8));

        return Util.bytesToHex(encodedhash);
    }
}
