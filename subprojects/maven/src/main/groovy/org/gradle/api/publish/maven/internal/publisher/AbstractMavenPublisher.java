/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.publish.maven.internal.publisher;

import org.gradle.api.internal.artifacts.mvnsettings.LocalMavenRepositoryLocator;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.publication.maven.internal.ant.MavenPublishAction;
import org.gradle.api.publish.maven.MavenArtifact;
import org.gradle.internal.Factory;
import org.gradle.internal.artifacts.repositories.MavenArtifactRepositoryInternal;
import org.gradle.logging.LoggingManagerInternal;
import org.gradle.util.GUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

abstract public class AbstractMavenPublisher<T extends MavenPublishAction> implements MavenPublisher {
    private final Factory<LoggingManagerInternal> loggingManagerFactory;

    private static Logger logger = LoggerFactory.getLogger(AbstractMavenPublisher.class);
    private final LocalMavenRepositoryLocator mavenRepositoryLocator;

    public AbstractMavenPublisher(Factory<LoggingManagerInternal> loggingManagerFactory, LocalMavenRepositoryLocator mavenRepositoryLocator) {
        this.loggingManagerFactory = loggingManagerFactory;
        this.mavenRepositoryLocator = mavenRepositoryLocator;
    }

    public void publish(MavenNormalizedPublication publication, MavenArtifactRepositoryInternal artifactRepository) {
        logger.info("Publishing to repository {}", artifactRepository);
        T deployTask = createDeployTask(publication.getPomFile(), mavenRepositoryLocator, artifactRepository);
        addPomAndArtifacts(deployTask, publication);
        execute(deployTask);
    }

    abstract protected T createDeployTask(File pomFile, LocalMavenRepositoryLocator mavenRepositoryLocator, MavenArtifactRepositoryInternal artifactRepository);

    private void addPomAndArtifacts(MavenPublishAction publishAction, MavenNormalizedPublication publication) {
        MavenArtifact mainArtifact = publication.getMainArtifact();
        if (mainArtifact != null) {
            publishAction.setMainArtifact(mainArtifact.getFile());
        }

        for (MavenArtifact mavenArtifact : publication.getArtifacts()) {
            if (mavenArtifact == mainArtifact) {
                continue;
            }
            publishAction.addAdditionalArtifact(mavenArtifact.getFile(), GUtil.elvis(mavenArtifact.getExtension(), ""), GUtil.elvis(mavenArtifact.getClassifier(), ""));
        }
    }

    private void execute(MavenPublishAction publishAction) {
        LoggingManagerInternal loggingManager = loggingManagerFactory.create();
        loggingManager.captureStandardOutput(LogLevel.INFO).start();
        try {
            publishAction.publish();
        } finally {
            loggingManager.stop();
        }
    }

}
