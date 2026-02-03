package com.soft40051.app.hostmanager;

public class HostManager {

    public static void startContainer(String name) throws Exception {
        new ProcessBuilder("docker", "start", name).start();
    }

    public static void stopContainer(String name) throws Exception {
        new ProcessBuilder("docker", "stop", name).start();
    }
}