package de.worldiety.autocd;

import com.google.gson.Gson;
import de.worldiety.autocd.docker.DockerfileHandler;
import de.worldiety.autocd.env.Environment;
import de.worldiety.autocd.env.GithubEnvironment;
import de.worldiety.autocd.env.GitlabEnvironment;
import de.worldiety.autocd.k8s.K8sClient;
import de.worldiety.autocd.persistence.AutoCD;
import de.worldiety.autocd.persistence.Volume;
import de.worldiety.autocd.util.DockerconfigBuilder;
import de.worldiety.autocd.util.FileType;
import de.worldiety.autocd.util.Util;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import io.kubernetes.client.util.credentials.AccessTokenAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final int KUBERNETES_URL = 0;
    private static final int KUBERNETES_TOKEN = 1;
    private static final int CA_CERTIFICATE = 2;
    private static final int BUILD_TYPE = 3;
    private static final String ENVIRONMENT_SELECTION_VARIABLE = "AUTOCD_ENV";
    private static Map<String, de.worldiety.autocd.env.Environment> environments = Map.of(
            "GITHUB", new GithubEnvironment(),
            "GITLAB", new GitlabEnvironment()
    );

    private static de.worldiety.autocd.env.Environment getEnv() {
        var envString = System.getenv(ENVIRONMENT_SELECTION_VARIABLE);
        if (null == envString) {
            log.error("Could not read environment");
            System.exit(-1);
        }

        if (!environments.containsKey(envString)) {
            log.error("Envrionment unknown {}", envString);
            System.exit(-1);
        }

        return environments.get(envString);
    }

    public static void main(String[] args) throws IOException {
        var environment = getEnv();
        ApiClient client;
        client = environment.getK8SConfig().map(it -> {
            try {
                return Config.fromConfig(it);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }).orElse(Config.fromToken(environment.getK8SUrl(),
                environment.getK8SUserToken()).setSslCaCert(environment.getK8SCACert()));

        if (client == null) {
            log.error("Could not initialize kubernetes client, check config file");
            System.exit(-1);
        }

        Configuration.setDefaultApiClient(client);

        String name = "autocd.json";
        var autoCD = getAutoCD(name, true);
        var oldAutoCD = getAutoCD("oldautocd.json", false);
        // A new API Client is created. Docker Credentials will be obtained from the Digital Oceans cluster configuration file
        var builder = environment.getK8SConfig().map(conf -> {
            try {
                return ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(conf));
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }).orElseGet(() -> {
            try {
                return ClientBuilder.standard()
                        .setBasePath(environment.getK8SUrl())
                        .setAuthentication(new AccessTokenAuthentication(environment.getK8SUserToken()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        });

        if (null == builder) {
            log.error("could not build merge client");
            System.exit(-1);
        }

        ApiClient strategicMergePatchClient = builder
                .setVerifyingSsl(true)
                .setOverridePatchFormat(V1Patch.PATCH_FORMAT_JSON_PATCH)
                .build();

        if (environment.getK8SConfig().isEmpty()) {
                strategicMergePatchClient.setSslCaCert(environment.getK8SCACert());
        }

        var dockerCredentials = DockerconfigBuilder.getDockerConfig(
                environment.getRegistryUrl(),
                environment.getRegistryUser(),
                environment.getRegistryPassword()
        );
        // Determines the build Type
        String buildType = environment.getBuildType().orElse("dev");

        DockerfileHandler finder = new DockerfileHandler(".");


        //Creat the k8s client, CoreV1Api is needed for the client
        CoreV1Api patchApi = new CoreV1Api(strategicMergePatchClient);
        CoreV1Api api = new CoreV1Api();
        var k8sClient = new K8sClient(environment, api, finder, buildType, patchApi, dockerCredentials);


        /* This method checks for amy images which are referred in the main autoCD object
           If invalid images are found, they will be removed from the cluster.
           An image is invalid if there is no registry image path containing the name of the targeted image.
        */

        if (oldAutoCD != null) {
            var validImageNames = autoCD.getOtherImages().stream()
                    .map(AutoCD::getRegistryImagePath)
                    .collect(Collectors.toList());

            var containsInvalidImages = oldAutoCD.getOtherImages()
                    .stream()
                    .map(AutoCD::getRegistryImagePath)
                    .anyMatch(o -> !validImageNames.contains(o));

            if (containsInvalidImages) {
                oldAutoCD.getOtherImages().forEach(image -> {
                    setServiceNameForOtherImages(environment, oldAutoCD, image);
                    removeWithDependencies(environment, image, k8sClient);
                });
            }
        }

        populateRegistryImagePath(environment, autoCD, buildType, finder);
        populateSubdomain(environment, autoCD, buildType, autoCD.getSubdomains());
        populateContainerPort(autoCD, finder);

        /* Checks if the app should be hosted on the cluster, if one decides to abandon the app, this method will
            remove it if the tag 'isShouldHost' is set correctly (false)
        */

        if (!autoCD.isShouldHost()) {
            log.info("Service is being removed from k8s.");
            removeWithDependencies(environment, autoCD, k8sClient);

            log.info("Not deploying to k8s because autocd is set to no hosting");
            return;
        }

        deployWithDependencies(environment, autoCD, k8sClient, buildType);
        log.info("Deployed to k8s with subdomain: " + autoCD.getSubdomain());
    }

    /**
     * If its a vue Project, it requires Port 80. Because its simpler to check for .vue than changing the nginx
     * configuration, this method will change the container port from the default value 8080 to 80.
     *
     * @param autoCD
     * @param finder
     */
    private static void populateContainerPort(AutoCD autoCD, DockerfileHandler finder) {
        if (autoCD.getContainerPort() == 8080 && finder.getFileType().equals(FileType.VUE)) {
            autoCD.setContainerPort(80);
        }
    }

    /**
     * If there is no image on the registry, creates new Dockerfile, builds an images and pushes it to the registry
     * and sets its path in the autoCD object.
     *
     * @param autoCD
     * @param buildType
     * @param finder
     */
    private static void populateRegistryImagePath(Environment environment, AutoCD autoCD, String buildType, DockerfileHandler finder) {
        if (autoCD.getRegistryImagePath() == null || autoCD.getRegistryImagePath().isEmpty()) {
            var dockerFile = new File("Dockerfile");

            if (!dockerFile.exists()) {
                finder.findDockerConfig().ifPresent(config -> {
                    Util.pushDockerAndSetPath(environment, config, autoCD, buildType);
                });
            } else {
                Util.pushDockerAndSetPath(environment, dockerFile.getAbsoluteFile(), autoCD, buildType);
            }
        }
    }

    /**
     * If the autoCD object can not be found, this method creates a new autoCD object and returns it.
     * If there is one, the autoCD object will get its parameters from an exiting configuration file.
     *
     * @param name
     * @param createIfNotExists
     * @return
     */
    private static AutoCD getAutoCD(String name, boolean createIfNotExists) throws FileNotFoundException {
        var autocdConfigFile = new File(name);
        AutoCD autoCD;
        if (!autocdConfigFile.exists() && createIfNotExists) {
            autoCD = new AutoCD();
        } else {
            Gson gson = new Gson();
            autoCD = gson.fromJson(new FileReader(autocdConfigFile), AutoCD.class);
        }
        return autoCD;
    }

    /**
     * This method looks for subdomains within the autoCD object, if there are any, the subdomain will be the one with the
     * matching build type.
     * If autoCD does not have any subdomains, this method will build one out of the build type and an hashed version
     * of the registry image path.
     *
     * @param autoCD
     * @param buildType
     * @param subdomains
     */
    private static void populateSubdomain(Environment environment, AutoCD autoCD, String buildType, Map<String, String> subdomains) {
        if (autoCD.getSubdomains() != null && subdomains.keySet().size() != 0) {
            autoCD.setSubdomain(autoCD.getSubdomains().get(buildType));
        }

        if (autoCD.getSubdomain() == null || autoCD.getSubdomain().isEmpty()) {
            autoCD.setSubdomain(Util.buildSubdomain(environment, buildType, Util.hash(autoCD.getIdentifierRegistryImagePath()).substring(0, 5)));
        }
    }

    /**
     * If a service should be removed, this method checks for other services, depending on the one which will be removed from the
     * cluster. Dependencies can be found in the autoCD class variable called otherImages. The method proceeds recursively
     * and removes all depending services.
     *
     * @param autoCD
     * @param k8sClient
     */
    private static void removeWithDependencies(Environment environment, AutoCD autoCD, K8sClient k8sClient) {
        if (!autoCD.getOtherImages().isEmpty()) {
            autoCD.getOtherImages().forEach(config -> {
                setServiceNameForOtherImages(environment, autoCD, config);
                if (!config.getOtherImages().isEmpty()) {
                    removeWithDependencies(environment, config, k8sClient);
                }
            });
        }

        k8sClient.removeDeploymentFromK8s(autoCD);
    }

    /**
     * Creates a name for a depending service out of the project name and a hashed registry image path from the main service
     *
     * @param main
     * @param other
     */
    private static void setServiceNameForOtherImages(Environment environment, AutoCD main, AutoCD other) {
        if (other.getServiceName() == null) {
            other.setServiceName(Util.hash(
                    environment.getProjectName() + main.getIdentifierRegistryImagePath()).substring(0, 20)
            );
        }
    }

    /**
     * If there are dependencies found within the main service, those services will be deployed as well.
     *
     * @param autoCD
     * @param k8sClient
     * @param buildType
     */
    private static void deployWithDependencies(Environment environment, AutoCD autoCD, K8sClient k8sClient, String buildType) {
        validateConfig(autoCD);
        if (!autoCD.getOtherImages().isEmpty()) {
            autoCD.getOtherImages().forEach(config -> {
                populateSubdomain(environment, config, buildType, autoCD.getSubdomains());

                if (!config.getOtherImages().isEmpty()) {
                    deployWithDependencies(environment, config, k8sClient, buildType);
                }
                setServiceNameForOtherImages(environment, autoCD, config);
                k8sClient.deployToK8s(config);
            });
        }

        k8sClient.deployToK8s(autoCD);
    }

    /**
     * Note that autoCD does not support retaining volumes if the replica set is greater than 1.
     *
     * @param autoCD
     */
    private static void validateConfig(AutoCD autoCD) {
        if (autoCD.getReplicas() > 1) {
            if (autoCD.getVolumes().stream().anyMatch(Volume::isRetainVolume)) {
                throw new IllegalArgumentException("AutoCD config is invalid, if using more than 1 replica retainVolume has to be set to false");
            }
        }
    }
}



