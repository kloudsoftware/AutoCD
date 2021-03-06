package de.worldiety.autocd.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoCD {
    private int containerPort = 8080;
    private int servicePort = 80;
    private int replicas = 1;
    private boolean publiclyAccessible = true;
    private long terminationGracePeriod = 60L;
    private String dockerImagePath;
    private String registryImagePath;
    private Map<String, String> subdomains = new HashMap<>();
    private boolean shouldHost = true;
    private List<Volume> volumes = new ArrayList<>();
    private Map<String, Map<String, String>> environmentVariables = new HashMap<>();
    private List<AutoCD> otherImages = new ArrayList<>();
    private List<String> args = new ArrayList<>();
    private String serviceName = null;
    private String subdomain;

    public AutoCD(int containerPort, int servicePort, int replicas, boolean publiclyAccessible, long terminationGracePeriod, String dockerImagePath, String registryImagePath, Map<String, String> subdomains, boolean shouldHost, List<Volume> volumes, Map<String, Map<String, String>> environmentVariables, List<AutoCD> otherImages, List<String> args, String serviceName, String subdomain) {
        this.containerPort = containerPort;
        this.servicePort = servicePort;
        this.replicas = replicas;
        this.publiclyAccessible = publiclyAccessible;
        this.terminationGracePeriod = terminationGracePeriod;
        this.dockerImagePath = dockerImagePath;
        this.registryImagePath = registryImagePath;
        this.subdomains = subdomains;
        this.shouldHost = shouldHost;
        this.volumes = volumes;
        this.environmentVariables = environmentVariables;
        this.otherImages = otherImages;
        this.args = args;
        this.serviceName = serviceName;
        this.subdomain = subdomain;
    }

    public AutoCD() {
    }

    public String getSubdomain() {
        return subdomain;
    }

    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
    }

    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }

    public List<AutoCD> getOtherImages() {
        return otherImages;
    }

    public void setOtherImages(List<AutoCD> otherImages) {
        this.otherImages = otherImages;
    }

    public List<Volume> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<Volume> volumes) {
        this.volumes = volumes;
    }

    public long getTerminationGracePeriod() {
        return terminationGracePeriod;
    }

    public void setTerminationGracePeriod(long terminationGracePeriod) {
        this.terminationGracePeriod = terminationGracePeriod;
    }

    public Map<String, String> getSubdomains() {
        return subdomains;
    }

    public void setSubdomains(Map<String, String> subdomains) {
        this.subdomains = subdomains;
    }

    public boolean isPubliclyAccessible() {
        return publiclyAccessible;
    }

    public void setPubliclyAccessible(boolean publiclyAccessible) {
        this.publiclyAccessible = publiclyAccessible;
    }

    public boolean isShouldHost() {
        return shouldHost;
    }

    public void setShouldHost(boolean shouldHost) {
        this.shouldHost = shouldHost;
    }

    public Map<String, Map<String, String>> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(Map<String, Map<String, String>> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public int getServicePort() {
        return servicePort;
    }

    public void setServicePort(int servicePort) {
        this.servicePort = servicePort;
    }

    public String getRegistryImagePath() {
        return registryImagePath;
    }

    public void setRegistryImagePath(String registryImagePath) {
        this.registryImagePath = registryImagePath;
    }

    public String getIdentifierRegistryImagePath() {
        return registryImagePath.split(":")[0];
    }

    public int getContainerPort() {
        return containerPort;
    }

    public void setContainerPort(int containerPort) {
        this.containerPort = containerPort;
    }

    public String getDockerImagePath() {
        return dockerImagePath;
    }

    public void setDockerImagePath(String dockerImagePath) {
        this.dockerImagePath = dockerImagePath;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
