/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public final class ControllerOptions {

    private static final String SERVICEACCOUNT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount";

    private final String masterUrl;
    private final String namespace;
    private final String token;

    private final File templateDir;
    private final NoneAuthServiceInfo noneAuthService;
    private final StandardAuthServiceInfo standardAuthService;
    private final boolean enableRbac;
    private final boolean enableEventLogger;

    private final String environment;
    private final String addressControllerSa;
    private final String addressSpaceAdminSa;

    private final String wildcardCertSecret;

    private final Duration resyncInterval;
    private final Duration recheckInterval;

    private final String impersonateUser;

    private ControllerOptions(String masterUrl, String namespace, String token,
                              File templateDir, NoneAuthServiceInfo noneAuthService, StandardAuthServiceInfo standardAuthService, boolean enableRbac, boolean enableEventLogger, String environment, String addressControllerSa, String addressSpaceAdminSa, String wildcardCertSecret, Duration resyncInterval, Duration recheckInterval, String impersonateUser) {
        this.masterUrl = masterUrl;
        this.namespace = namespace;
        this.token = token;
        this.templateDir = templateDir;
        this.noneAuthService = noneAuthService;
        this.standardAuthService = standardAuthService;
        this.enableRbac = enableRbac;
        this.enableEventLogger = enableEventLogger;
        this.environment = environment;
        this.addressControllerSa = addressControllerSa;
        this.addressSpaceAdminSa = addressSpaceAdminSa;
        this.wildcardCertSecret = wildcardCertSecret;
        this.resyncInterval = resyncInterval;
        this.recheckInterval = recheckInterval;
        this.impersonateUser = impersonateUser;
    }

    public String getMasterUrl() {
        return masterUrl;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getToken() {
        return token;
    }

    public File getTemplateDir() {
        return templateDir;
    }

    public Optional<NoneAuthServiceInfo> getNoneAuthService() {
        return Optional.ofNullable(noneAuthService);
    }

    public Optional<StandardAuthServiceInfo> getStandardAuthService() {
        return Optional.ofNullable(standardAuthService);
    }

    public boolean isEnableRbac() {
        return enableRbac;
    }

    public boolean isEnableEventLogger() {
        return enableEventLogger;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getAddressControllerSa() {
        return addressControllerSa;
    }

    public String getAddressSpaceAdminSa() {
        return addressSpaceAdminSa;
    }

    public String getWildcardCertSecret() {
        return wildcardCertSecret;
    }

    public Duration getResyncInterval() {
        return resyncInterval;
    }

    public Duration getRecheckInterval() {
        return recheckInterval;
    }

    public String getImpersonateUser() {
        return impersonateUser;
    }


    public static ControllerOptions fromEnv(Map<String, String> env) throws IOException {

        String masterHost = getEnvOrThrow(env, "KUBERNETES_SERVICE_HOST");
        String masterPort = getEnvOrThrow(env, "KUBERNETES_SERVICE_PORT");

        String namespace = getEnv(env, "NAMESPACE")
                .orElseGet(() -> readFile(new File(SERVICEACCOUNT_PATH, "namespace")));

        String token = getEnv(env, "TOKEN")
                .orElseGet(() -> readFile(new File(SERVICEACCOUNT_PATH, "token")));

        File templateDir = new File(getEnvOrThrow(env, "TEMPLATE_DIR"));

        if (!templateDir.exists()) {
            throw new IllegalArgumentException("Template directory " + templateDir.getAbsolutePath() + " not found");
        }

        NoneAuthServiceInfo noneAuthService = getNoneAuthService(env, "NONE_AUTHSERVICE_SERVICE_HOST", "NONE_AUTHSERVICE_SERVICE_PORT").orElse(null);
        StandardAuthServiceInfo standardAuthService = getStandardAuthService(env, "STANDARD_AUTHSERVICE_CONFIG").orElse(null);

        boolean enableRbac = getEnv(env, "ENABLE_RBAC").map(Boolean::parseBoolean).orElse(false);

        boolean enableEventLogger = getEnv(env, "ENABLE_EVENT_LOGGER").map(Boolean::parseBoolean).orElse(false);

        String environment = getEnv(env, "ENVIRONMENT").orElse("development");

        String addressControllerSa = getEnv(env, "ADDRESS_CONTROLLER_SA").orElse("enmasse-admin");

        String addressSpaceAdminSa = getEnv(env, "ADDRESS_SPACE_ADMIN_SA").orElse("address-space-admin");

        String wildcardCertSecret = getEnv(env, "WILDCARD_ENDPOINT_CERT_SECRET").orElse(null);

        Duration resyncInterval = getEnv(env, "RESYNC_INTERVAL")
                .map(i -> Duration.ofSeconds(Long.parseLong(i)))
                .orElse(Duration.ofMinutes(5));

        Duration recheckInterval = getEnv(env, "CHECK_INTERVAL")
                .map(i -> Duration.ofSeconds(Long.parseLong(i)))
                .orElse(Duration.ofSeconds(30));

        String impersonateUser = getEnv(env, "IMPERSONATE_USER").orElse("");
        if (impersonateUser.isEmpty()) {
            impersonateUser = null;
        }

        return new ControllerOptions(String.format("https://%s:%s", masterHost, masterPort),
                namespace,
                token,
                templateDir,
                noneAuthService,
                standardAuthService,
                enableRbac,
                enableEventLogger, environment,
                addressControllerSa,
                addressSpaceAdminSa,
                wildcardCertSecret,
                resyncInterval,
                recheckInterval,
                impersonateUser);
    }


    private static Optional<NoneAuthServiceInfo> getNoneAuthService(Map<String, String> env, String hostEnv, String portEnv) {

        return getEnv(env, hostEnv)
                .map(host -> new NoneAuthServiceInfo(host, Integer.parseInt(getEnvOrThrow(env, portEnv))));
    }

    private static Optional<StandardAuthServiceInfo> getStandardAuthService(Map<String, String> env, String configMapEnv) {
        return getEnv(env, configMapEnv).map(e -> {
            if (e.isEmpty()) {
                return null;
            } else {
                return new StandardAuthServiceInfo(e);
            }
        });
    }

    private static Optional<String> getEnv(Map<String, String> env, String envVar) {
        return Optional.ofNullable(env.get(envVar));
    }

    private static String getEnvOrThrow(Map<String, String> env, String envVar) {
        String var = env.get(envVar);
        if (var == null) {
            throw new IllegalArgumentException(String.format("Unable to find value for required environment var '%s'", envVar));
        }
        return var;
    }

    private static String readFile(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
