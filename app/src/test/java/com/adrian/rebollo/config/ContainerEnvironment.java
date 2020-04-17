package com.adrian.rebollo.config;

import java.io.File;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Testcontainers
public final class ContainerEnvironment implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, AfterEachCallback {

  @Container
  private static final GenericContainer ACTIVE_MQ_CONTAINER;

  private static File accessLogFile;

  static {
    ACTIVE_MQ_CONTAINER =
        new GenericContainer("webcenter/activemq:" + EnvVars.ACTIVEMQ_VERSION)
            .withExposedPorts(8161, 61616, 61613)
            .waitingFor(Wait.forLogMessage(".*success: activemq entered RUNNING state.*", 1));
  }

  public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    public void initialize(@NotNull ConfigurableApplicationContext configurableApplicationContext) {

      setupTestAccessLogFile();

      if (!ACTIVE_MQ_CONTAINER.isRunning()) {
        ACTIVE_MQ_CONTAINER.start();
        setupAMQEnvVars();
      }
    }
  }

  @Override
  public void afterAll(ExtensionContext extensionContext) {

    if (ACTIVE_MQ_CONTAINER.isRunning()) {
      ACTIVE_MQ_CONTAINER.close();
      ACTIVE_MQ_CONTAINER.stop();
    }
    deleteTestAccessLogFile();
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) {
    if (ACTIVE_MQ_CONTAINER.isRunning()) {
      ACTIVE_MQ_CONTAINER.close();
      ACTIVE_MQ_CONTAINER.stop();
    }
    deleteTestAccessLogFile();
  }

  @Override
  public void beforeAll(ExtensionContext extensionContext) {
    if (!ACTIVE_MQ_CONTAINER.isRunning()) {
      ACTIVE_MQ_CONTAINER.start();
      setupAMQEnvVars();
    }

    setupTestAccessLogFile();
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) {
    if (!ACTIVE_MQ_CONTAINER.isRunning()) {
      ACTIVE_MQ_CONTAINER.start();
      setupAMQEnvVars();
    }

    setupTestAccessLogFile();
  }

  private static void setupTestAccessLogFile() {
    accessLogFile = new File("/tmp/access.log");
    try {
      if(accessLogFile.createNewFile()) {
        accessLogFile.setWritable(true);
        accessLogFile.setReadable(true);
        System.setProperty("service.reader.file-name", accessLogFile.getAbsolutePath());
      }
    } catch (Exception e) {
      LOG.error("Impossible create new file");
    }
  }

  private static void deleteTestAccessLogFile() {
    try {
      FileUtils.forceDelete(accessLogFile);
    } catch (Exception e) {
      LOG.error("Impossible forceDelete file");
    }
  }

  private static void setupAMQEnvVars() {
    System.setProperty(EnvVars.ACTIVEMQ_BROKER_ENV_VAR, "tcp://" + ACTIVE_MQ_CONTAINER.getContainerIpAddress() + ":" + ACTIVE_MQ_CONTAINER.getMappedPort(61616));
    System.setProperty(EnvVars.ACTIVEMQ_USERNAME_ENV_VAR, "admin");
    System.setProperty(EnvVars.ACTIVEMQ_PASSWORD_ENV_VAR, "admin");
  }
}
