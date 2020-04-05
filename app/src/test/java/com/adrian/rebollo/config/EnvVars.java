package com.adrian.rebollo.config;

public class EnvVars {

    //AMQ
    static final String ACTIVEMQ_VERSION = "5.14.3";
    static final String ACTIVEMQ_USERNAME_ENV_VAR = "adapters.activemq.username";
    static final String ACTIVEMQ_PASSWORD_ENV_VAR = "adapters.activemq.password";
    static final String ACTIVEMQ_BROKER_ENV_VAR = "adapters.activemq.broker-url";

    //MariaDB
    static final String DB_URL_ENV_VAR = "adapters.mariadb.url";
    static final String DB_USER_ENV_VAR = "adapters.mariadb.username";
    static final String DB_PASSWORD_ENV_VAR = "adapters.mariadb.password";
    static final String DB_NAME = "ids";
    static final String DB_USER = "ids";
    static final String DB_PASSWORD = "pass";
}
