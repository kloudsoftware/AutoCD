package de.worldiety.autocd.env;

import java.io.InputStream;

public interface Environment {
    String getRegistryUrl();

    String getRegistryEMail();

    String getRegistryUser();

    String getRegistryPassword();

    String getProjectName();

    String getProjectNamespace();

    String getK8SUrl();

    InputStream getK8SCACert();

    String getK8SUserToken();

    String getK8SUserName();

    String getDomainBase();
}
