package org.gitlab4j.codereview;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gitlab4j.codereview.utils.StringUtils;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class CodeReviewConfiguration {

    private static Logger logger = LogManager.getLogger();

    public static final String PATH = "path";
    public static final String PORT = "port";
    public static final String SSL_PORT = "ssl-port";
    public static final String GITLAB_API_TOKEN = "api-token";
    public static final String GITLAB_API_URL = "gitlab-api";
    public static final String GITLAB_WEB_URL = "gitlab-web";
    public static final String SIMPLE_CR_URL = "simple-cr";
    public static final String INTERACTIVE = "interactive";
    public static final String SMTP_PORT = "smtp-port";
    public static final String SMTP_HOST = "smtp-host";
    public static final String SMTP_ENABLE_STARTTLS = "smtp-enable-starttls";
    public static final String SMTP_USERNAME = "smtp-username";
    public static final String SMTP_PASSWORD = "smtp-password";
    public static final String FROM_EMAIL = "from-email";
    public static final String FROM_NAME = "from-name";
    public static final String DEFAULT_REVIEWERS = "default-reviewers";
    public static final String DEFAULT_TARGET_BRANCHES_REGEX = "default-target-branches-regex";

    public static final String DB_NAME = "db-name";
    public static final String DB_USER = "db-user";
    public static final String DB_PASSWORD = "db-password";

    private static final String DEFAULT_DB_USER = "admin";
    private static final String DEFAULT_DB_PASSWORD = "password";
    private static final String DEFAULT_DB_NAME = "file:./dbdata/simple-cr";

    private static final int DEFAULT_PORT = 8080;
    private static final int DEFAULT_SSL_PORT = -1;
    private static final String DEFAULT_PATH = "/";
    private static final int DEFAULT_SMTP_PORT = 25;
    private static final String DEFAULT_SMTP_HOST = "localhost";

    private static final String DEFAULT_FROM_NAME = "GitLab Code Review";
    private static final String DEFAULT_FROM_EMAIL = "noreply@localhost";

    private List<String> defaultReviewers;
    private PropertiesConfiguration config;

    CodeReviewConfiguration(String filename) throws ConfigurationException {

        FileBasedConfigurationBuilder<PropertiesConfiguration> builder = 
                new FileBasedConfigurationBuilder<PropertiesConfiguration>(PropertiesConfiguration.class)
                .configure(new Parameters().properties()
                        .setFileName(filename).setThrowExceptionOnMissing(true)
                        .setListDelimiterHandler(new DefaultListDelimiterHandler(','))
                        .setIncludesAllowed(false));

        try {

            // HACK: We do this because of a known issue with getConfiguration(), an inconsequential problem is
            // dumped to standard err, we simply catch it and log it at the debug level
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setErr(new PrintStream(baos));
            config = builder.getConfiguration();
            String configOutput = baos.toString();
            if (configOutput != null && configOutput.trim().length() > 0) {
                List<String> lines = Arrays.stream(configOutput.split(System.lineSeparator())).collect(Collectors.toList());
                lines.forEach(line -> logger.debug(line.trim()));
            }

        } finally {
            // Make sure to reset standard err
            System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
        }
    }

    CodeReviewConfiguration() {
        super();
    }

    boolean processCommandLine(String[] args) {

        OptionParser parser = new OptionParser();
        OptionSpec<String> pathOption = parser.accepts(PATH).withRequiredArg().ofType(String.class).defaultsTo(DEFAULT_PATH);
        OptionSpec<Integer> portOption = parser.accepts(PORT).withRequiredArg().ofType(Integer.class).defaultsTo(DEFAULT_PORT);
        OptionSpec<Integer> sslPortOption = parser.accepts(SSL_PORT).withRequiredArg().ofType(Integer.class).defaultsTo(DEFAULT_SSL_PORT);
        OptionSpec<String> apiTokenOption = parser.accepts(GITLAB_API_TOKEN).withRequiredArg().ofType(String.class);
        OptionSpec<String> gitlabApiOption = parser.accepts(GITLAB_API_URL).withRequiredArg().ofType(String.class);
        OptionSpec<String> gitlabWebOption = parser.accepts(GITLAB_WEB_URL).withRequiredArg().ofType(String.class);
        OptionSpec<String> simpleCrOption = parser.accepts(SIMPLE_CR_URL).withRequiredArg().ofType(String.class);
        OptionSpec<Boolean> interactiveOption = parser.accepts(INTERACTIVE).withOptionalArg().ofType(Boolean.class).defaultsTo(Boolean.TRUE);
        OptionSpec<Integer> smtpPortOption = parser.accepts(SMTP_PORT).withRequiredArg().ofType(Integer.class).defaultsTo(DEFAULT_SMTP_PORT);
        OptionSpec<String> smtpHostOption = parser.accepts(SMTP_HOST).withRequiredArg().ofType(String.class).defaultsTo(DEFAULT_SMTP_HOST);
        OptionSpec<Boolean> smtpEnableStartTlsOption = parser.accepts(SMTP_ENABLE_STARTTLS).withOptionalArg().ofType(Boolean.class).defaultsTo(Boolean.TRUE);
        OptionSpec<String> defaultReviewersOption = parser.accepts(DEFAULT_REVIEWERS).withRequiredArg().ofType(String.class);
        OptionSpec<String> defaultTargetBranchesRegexOption = parser.accepts(DEFAULT_TARGET_BRANCHES_REGEX).withRequiredArg().ofType(String.class);

        OptionSet options = parser.parse(args);
        if (options.has(interactiveOption)) {
            Boolean interactive = options.valueOf(interactiveOption);
            config.setProperty(INTERACTIVE, interactive);
        }

        if (options.has(pathOption)) {
            String path = options.valueOf(pathOption);
            config.setProperty(PATH, path);
        }

        if (options.has(portOption)) {
            int port = options.valueOf(portOption);
            config.setProperty(PORT, port);
        }

        if (options.has(sslPortOption)) {
            int sslPort = options.valueOf(sslPortOption);
            config.setProperty(SSL_PORT, sslPort);
        }

        if (options.has(apiTokenOption)) {
            String apiToken = options.valueOf(apiTokenOption);
            config.setProperty(GITLAB_API_TOKEN, apiToken);
        }

        if (options.has(gitlabApiOption)) {
            String gitlabApiUrl = options.valueOf(gitlabApiOption);
            config.setProperty(GITLAB_API_URL, gitlabApiUrl);
        }

        if (options.has(gitlabWebOption)) {
            String gitlabWebUrl = options.valueOf(gitlabWebOption);
            config.setProperty(GITLAB_WEB_URL, gitlabWebUrl);
        }

        if (options.has(simpleCrOption)) {
            String simpleCrUrl = options.valueOf(simpleCrOption);
            config.setProperty(SIMPLE_CR_URL, simpleCrUrl);
        }

        if (options.has(smtpPortOption)) {
            int smtpPort = options.valueOf(smtpPortOption);
            config.setProperty(SMTP_PORT, smtpPort);
        }

        if (options.has(smtpHostOption)) {
            String smtpHost = options.valueOf(smtpHostOption);
            config.setProperty(SMTP_HOST, smtpHost);
        }
        if (options.has(smtpHostOption)) {
            String smtpHost = options.valueOf(smtpHostOption);
            config.setProperty(SMTP_HOST, smtpHost);
        }

        if (options.has(defaultTargetBranchesRegexOption)) {
            String defaultTargetBranchesRegex = options.valueOf(defaultTargetBranchesRegexOption);
            config.setProperty(DEFAULT_TARGET_BRANCHES_REGEX, defaultTargetBranchesRegex);
        }

        if (options.has(smtpEnableStartTlsOption)) {
            Boolean enableStartTls = options.valueOf(smtpEnableStartTlsOption);
            config.setProperty(SMTP_ENABLE_STARTTLS, enableStartTls);
        }

        String defaultReviewers;
        if (options.has(defaultReviewersOption)) {
            defaultReviewers = options.valueOf(defaultReviewersOption);
        } else {
            defaultReviewers = config.getString(DEFAULT_REVIEWERS);
        }

        if (defaultReviewers != null && defaultReviewers.trim().length() > 0) {
            List<String> reviewerList = StringUtils.getListFromString(defaultReviewers, ";");
            config.setProperty(DEFAULT_REVIEWERS, defaultReviewers);
            this.defaultReviewers = reviewerList;
        }

        String apiToken = getGitLabApiToken();
        String simpleCrUrl = getSimpleCrUrl();
        String gitlabApiUrl = getGitLabApiUrl();
        String gitlabWebUrl = getGitLabWebUrl();

        if (apiToken == null || simpleCrUrl == null || gitlabApiUrl == null || gitlabWebUrl == null) {

            String message = "The following argument(s) must be provided:";
            message += (apiToken == null ? " --api-token" : "");
            message += (simpleCrUrl == null ? " --simple-cr" : "");
            message += (gitlabApiUrl == null ? " --gitlab-api" : "");
            message += (gitlabWebUrl == null ? " --gitlab-web" : "");
            System.err.println(message);
            return (false);

        } else {
            return (true);
        }
    }

    public Boolean isInteractive() {
        return (config.getBoolean(INTERACTIVE, true));
    }

    public String getPath() {
        return (config.getString(PATH, "/"));
    }

    public int getPort() {
        return (config.getInt(PORT, DEFAULT_PORT));
    }

    public int getSslPort() {
        return (config.getInt(SSL_PORT, DEFAULT_SSL_PORT));
    }

    public String getGitLabApiUrl() {
        return (config.getString(GITLAB_API_URL, null));
    }

    public String getGitLabWebUrl() {
        return (config.getString(GITLAB_WEB_URL, null));
    }

    public String getSimpleCrUrl() {
        return (config.getString(SIMPLE_CR_URL, null));
    }

    public String getGitLabApiToken() {
        return (config.getString(GITLAB_API_TOKEN, null));
    }

    public int getSmtpPort() {
        return (config.getInt(SMTP_PORT, DEFAULT_SMTP_PORT));
    }

    public String getSmtpHost() {
        return (config.getString(SMTP_HOST, DEFAULT_SMTP_HOST));
    }

    public String getSmtpUsername() {
        return (config.getString(SMTP_USERNAME, null));
    }

    public String getSmtpPassword() {
        return (config.getString(SMTP_PASSWORD, null));
    }

    public Boolean getSmtpEnableStartTls() {
        return (config.getBoolean(SMTP_ENABLE_STARTTLS, Boolean.FALSE));
    }

    public String getFromEmail() {
        return (config.getString(FROM_EMAIL, DEFAULT_FROM_EMAIL));
    }

    public String getFromName() {
        return (config.getString(FROM_NAME, DEFAULT_FROM_NAME));
    }

    public List<String> getDefaultReviewers() {
        return (defaultReviewers);
    }

    public String getDefaultTargetBranchesRegex() {
        return (config.getString(FROM_NAME, DEFAULT_TARGET_BRANCHES_REGEX));
    }

    public String getDbPassword() {
        return (config.getString(DB_PASSWORD, DEFAULT_DB_PASSWORD));
    }

    public String getDbUser() {
        return (config.getString(DB_USER, DEFAULT_DB_USER));
    }

    public String getDbName() {
        return (config.getString(DB_NAME, DEFAULT_DB_NAME));
    }
}
