package de.worldiety.autocd.env;

import java.io.*;
import java.util.Optional;

public class GithubEnvironment implements Environment {

    private String get(Environment what) {
        return System.getenv(what.toString());
    }

    @Override
    public String getRegistryUrl() {
        return get(Environment.CI_REGISTRY);
    }

    @Override
    public String getRegistryEMail() {
        return get(Environment.CI_REGISTRY_EMAIL);
    }

    @Override
    public String getRegistryUser() {
        return get(Environment.CI_REGISTRY_USER);
    }

    @Override
    public String getRegistryPassword() {
        return get(Environment.CI_REGISTRY_PASSWORD);
    }

    @Override
    public Optional<String> getDockerConfig() {
        return Optional.of(get(Environment.DOCKERCONFIG));
    }

    @Override
    public String getProjectName() {
        var split = get(Environment.GITHUB_REPOSITORY).split("/");
        return split[split.length - 1];
    }

    @Override
    public String getProjectNamespace() {
        var split = get(Environment.GITHUB_REPOSITORY).split("/");
        return split[split.length - 1];
    }

    @Override
    public String getK8SUrl() {
        return get(Environment.KUBE_URL);
    }

    @Override
    public InputStream getK8SCACert() {
        try {
            return new FileInputStream(get(Environment.KUBE_CA_PEM_FILE));
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    public Optional<Reader> getK8SConfig() {
        return Optional.of(new StringReader(get(Environment.KUBE_CONFIG)));
    }

    @Override
    public String getK8SUserToken() {
        return get(Environment.K8S_REGISTRY_USER_TOKEN);
    }

    @Override
    public String getK8SUserName() {
        return get(Environment.K8S_REGISTRY_USER_NAME);
    }

    @Override
    public String getDomainBase() {
        return get(Environment.AUTOCD_DOMAIN_BASE);
    }

    @Override
    public boolean needsSecret() {
        return Boolean.parseBoolean(get(Environment.K8S_SECRET_NEEDED));
    }


    @Override
    public String getStorageClass() {
        return get(Environment.K8S_STORAGE_CLASS);
    }

    @Override
    public Optional<String> getBuildType() {
        return Optional.ofNullable(get(Environment.BUILD_TYPE));
    }

    @Override
    public String getOrgName() {
        return get(Environment.ORG_NAME);
    }

    private enum Environment {
        //Populated by the CI if environment is set in .gitlab-ci.yml
        CI_REGISTRY,
        CI_REGISTRY_USER,
        CI_REGISTRY_EMAIL,
        CI_REGISTRY_PASSWORD,
        GITHUB_REPOSITORY,
        K8S_SECRET_NEEDED,
        K8S_STORAGE_CLASS,
        K8S_REGISTRY_USER_TOKEN,
        K8S_REGISTRY_USER_NAME,
        KUBE_URL,
        KUBE_CA_PEM_FILE,
        AUTOCD_DOMAIN_BASE,
        KUBE_CONFIG,
        DOCKERCONFIG,
        BUILD_TYPE,
        ORG_NAME
    }
}
