package de.worldiety.autocd.env;

import java.io.InputStream;
import java.io.Reader;
import java.util.Optional;

public interface Environment {
    String getRegistryUrl();

    String getRegistryEMail();

    String getRegistryUser();

    String getRegistryPassword();

    String getProjectName();

    String getProjectNamespace();

    String getK8SUrl();

    InputStream getK8SCACert();

    Optional<Reader> getK8SConfig();

    String getK8SUserToken();

    String getK8SUserName();

    String getDomainBase();

    boolean needsSecret();

    String getStorageClass();

    Optional<String> getBuildType();
}
