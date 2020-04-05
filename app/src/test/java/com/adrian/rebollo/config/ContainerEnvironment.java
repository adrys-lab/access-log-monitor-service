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
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Testcontainers
public final class ContainerEnvironment implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, AfterEachCallback {

  @Container
  private static final MariaDBContainer MARIA_DB_CONTAINER;
  @Container
  private static final GenericContainer ACTIVE_MQ_CONTAINER;

  private static File accessLogFile;

  static {
    MARIA_DB_CONTAINER =
        (MariaDBContainer)
            new MariaDBContainer()
                .withUsername(EnvVars.DB_USER)
                .withDatabaseName(EnvVars.DB_NAME)
                .withPassword(EnvVars.DB_PASSWORD)
                .waitingFor(Wait.forLogMessage(".*mysqld: ready for connections.*", 1));

    ACTIVE_MQ_CONTAINER =
        new GenericContainer("webcenter/activemq:" + EnvVars.ACTIVEMQ_VERSION)
            .withExposedPorts(8161, 61616, 61613)
            .waitingFor(Wait.forLogMessage(".*success: activemq entered RUNNING state.*", 1));
  }

  public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    public void initialize(@NotNull ConfigurableApplicationContext configurableApplicationContext) {

      setupTestAccessLogFile();

      if (!MARIA_DB_CONTAINER.isRunning()) {
        MARIA_DB_CONTAINER.start();
        setupDBEnvVars();
      }

      if (!ACTIVE_MQ_CONTAINER.isRunning()) {
        ACTIVE_MQ_CONTAINER.start();
        setupAMQEnvVars();
      }
    }
  }

  @Override
  public void afterAll(ExtensionContext extensionContext) {
    if (MARIA_DB_CONTAINER.isRunning()) {
      MARIA_DB_CONTAINER.close();
      MARIA_DB_CONTAINER.stop();
    }

    if (ACTIVE_MQ_CONTAINER.isRunning()) {
      ACTIVE_MQ_CONTAINER.close();
      ACTIVE_MQ_CONTAINER.stop();
    }
    deleteTestAccessLogFile();
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) {
    if (MARIA_DB_CONTAINER.isRunning()) {
      MARIA_DB_CONTAINER.close();
      MARIA_DB_CONTAINER.stop();
    }
    if (ACTIVE_MQ_CONTAINER.isRunning()) {
      ACTIVE_MQ_CONTAINER.close();
      ACTIVE_MQ_CONTAINER.stop();
    }
    deleteTestAccessLogFile();
  }

  @Override
  public void beforeAll(ExtensionContext extensionContext) {
    if (!MARIA_DB_CONTAINER.isRunning()) {
      MARIA_DB_CONTAINER.start();
      setupDBEnvVars();
    }

    if (!ACTIVE_MQ_CONTAINER.isRunning()) {
      ACTIVE_MQ_CONTAINER.start();
      setupAMQEnvVars();
    }

    setupTestAccessLogFile();
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) {
    if (!MARIA_DB_CONTAINER.isRunning()) {
      MARIA_DB_CONTAINER.start();
      setupDBEnvVars();
    }

    if (!ACTIVE_MQ_CONTAINER.isRunning()) {
      ACTIVE_MQ_CONTAINER.start();
      setupAMQEnvVars();
    }

    setupTestAccessLogFile();
  }

  private static void setupTestAccessLogFile() {
    accessLogFile = new File("test-access.log");
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

  private static void setupDBEnvVars() {
    System.setProperty(EnvVars.DB_URL_ENV_VAR, MARIA_DB_CONTAINER.getJdbcUrl());
    System.setProperty(EnvVars.DB_USER_ENV_VAR, MARIA_DB_CONTAINER.getUsername());
    System.setProperty(EnvVars.DB_PASSWORD_ENV_VAR, MARIA_DB_CONTAINER.getPassword());
  }

  private static void setupAMQEnvVars() {
    System.setProperty(EnvVars.ACTIVEMQ_BROKER_ENV_VAR, "tcp://" + ACTIVE_MQ_CONTAINER.getContainerIpAddress() + ":" + ACTIVE_MQ_CONTAINER.getMappedPort(61616));
    System.setProperty(EnvVars.ACTIVEMQ_USERNAME_ENV_VAR, "admin");
    System.setProperty(EnvVars.ACTIVEMQ_PASSWORD_ENV_VAR, "admin");
  }
}
