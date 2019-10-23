package de.worldiety.autocd.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.github.dockerjava.core.command.PushImageResultCallback;
import de.worldiety.autocd.env.Environment;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Set;

public class Docker {
    private static final Logger log = LoggerFactory.getLogger(Docker.class);
    private DockerClient client;
    private Environment environment;

    public Docker(Environment environment) {
        this.environment = environment;
        var reg = environment.getRegistryUrl();
        DefaultDockerClientConfig config;
        if (reg != null) {
            config = DefaultDockerClientConfig
                    .createDefaultConfigBuilder()
                    .withRegistryUrl(reg)
                    .withRegistryUsername(environment.getRegistryUser())
                    .withRegistryPassword(environment.getRegistryPassword())
                    .build();

        } else {
            config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        }
        this.client = DockerClientBuilder.getInstance(config).build();
    }

    public String buildAndPushImageFromFile(File configFile, String buildType) {
        var reg = environment.getRegistryUrl();
        var projectName = environment.getProjectName();
        var nameSpace = environment.getProjectNamespace();

        reg = reg == null ? "default" : reg;
        projectName = projectName == null ? "default" : projectName;
        nameSpace = nameSpace == null ? "default" : nameSpace;

        var tag = reg + "/" + nameSpace + "/" + projectName + ":" + buildType;

        log.info("creating image with tag " + tag);

        BuildImageResultCallback callback = new BuildImageResultCallback() {
            @Override
            public void onNext(@NotNull BuildResponseItem item) {
                if (item.getStream() != null && !item.getStream().equals(".")) {
                    log.info(item.getStream());
                }
                super.onNext(item);
            }
        };

        var staticDir = new File("static/");
        if (!staticDir.exists()) {
            if (!staticDir.mkdir()) {
                log.error("No write permissions");
            }
        }

        client.buildImageCmd(configFile)
                .withTags(Set.of(tag))
                .exec(callback)
                .awaitImageId();

        try {
            client.pushImageCmd(tag).exec(new PushImageResultCallback() {
                @Override
                public void onNext(PushResponseItem item) {
                    if (item.getStream() != null && !item.getStream().equals(".")) {
                        log.info(item.getStream());
                    }
                    super.onNext(item);
                }
            }).awaitCompletion();
        } catch (InterruptedException e) {
            log.error("pushing image failed", e);
        }

        return tag;
    }
}
