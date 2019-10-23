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

    private enum Environment {
        //Populated by the CI if environment is set in .gitlab-ci.yml
        CI_REGISTRY,
        CI_REGISTRY_USER,
        CI_REGISTRY_EMAIL,
        CI_REGISTRY_PASSWORD,
        CI_PROJECT_NAME,
        CI_PROJECT_NAMESPACE,
        //Set for the wdy namespace
        K8S_REGISTRY_USER_TOKEN,
        K8S_REGISTRY_USER_NAME,
        KUBE_URL,
        KUBE_CA_PEM_FILE,
        AUTOCD_DOMAIN_BASE,
        KUBE_CONFIG
    }
}
