package de.worldiety.autocd.env;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Optional;

public class GitlabEnvironment implements Environment {

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
    public String getProjectName() {
        return get(Environment.CI_PROJECT_NAME);
    }

    @Override
    public String getProjectNamespace() {
        return get(Environment.CI_PROJECT_NAMESPACE);
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
        return Optional.empty();
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
        return true;
    }

    @Override
    public String getStorageClass() {
        return get(Environment.K8S_STORAGE_CLASS);
    }

    @Override
    public Optional<String> getBuildType() {
        return Optional.ofNullable(get(Environment.BUILD_TYPE));
    }

    private enum Environment {
        //Populated by the CI if environment is set in .gitlab-ci.yml
        CI_REGISTRY,
        CI_REGISTRY_USER,
        CI_REGISTRY_EMAIL,
        CI_REGISTRY_PASSWORD,
        CI_PROJECT_NAME,
        CI_PROJECT_NAMESPACE,
        K8S_STORAGE_CLASS,
        //Set for the wdy namespace
        K8S_REGISTRY_USER_TOKEN,
        K8S_REGISTRY_USER_NAME,
        KUBE_URL,
        KUBE_CA_PEM_FILE,
        AUTOCD_DOMAIN_BASE,
        BUILD_TYPE
    }
}
