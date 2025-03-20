package com.github.aiassistant.service.text.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class UserAgentGenerator {
    private final List<String> browsers;
    private final List<String> devices;

    public UserAgentGenerator() {
        browsers = getBrowsers();
        devices = getDevices();
    }

    public String generateUserAgent() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String browser = browsers.get(random.nextInt(browsers.size()));
        String device = devices.get(random.nextInt(devices.size()));
        return String.format("%s; %s", device, browser);
    }

    protected List<String> getBrowsers() {
        List<String> browsers = new ArrayList<>();
        browsers.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.150 Safari/537.36");
        browsers.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0.3 Safari/605.1.15");
        browsers.add("Mozilla/5.0 (Linux; Android 10; Pixel 3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.90 Mobile Safari/537.36");
        return browsers;
    }

    protected List<String> getDevices() {
        List<String> devices = new ArrayList<>();
        devices.add("Desktop");
        devices.add("Mobile");
        devices.add("Tablet");
        return devices;
    }
}