package com.djrapitops.plan.system.webserver;

import com.djrapitops.plan.api.exceptions.connection.*;
import com.djrapitops.plan.data.WebUser;
import com.djrapitops.plan.system.PlanSystem;
import com.djrapitops.plan.system.settings.Settings;
import com.djrapitops.plan.system.settings.config.PlanConfig;
import com.djrapitops.plan.utilities.Base64Util;
import com.djrapitops.plan.utilities.PassEncryptUtil;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import rules.BukkitComponentMocker;
import rules.ComponentMocker;
import utilities.HTTPConnector;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

@RunWith(MockitoJUnitRunner.Silent.class)
public class HTTPSWebServerAuthTest {

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();
    @ClassRule
    public static ComponentMocker component = new BukkitComponentMocker(temporaryFolder);

    private static PlanSystem bukkitSystem;

    private HTTPConnector connector = new HTTPConnector();

    @BeforeClass
    public static void setUpClass() throws Exception {
        URL resource = HTTPSWebServerAuthTest.class.getResource("/Cert.keystore");
        String keyStore = resource.getPath();
        String absolutePath = new File(keyStore).getAbsolutePath();

        bukkitSystem = component.getPlanSystem();

        PlanConfig config = bukkitSystem.getConfigSystem().getConfig();

        config.set(Settings.WEBSERVER_CERTIFICATE_PATH, absolutePath);
        config.set(Settings.WEBSERVER_CERTIFICATE_KEYPASS, "MnD3bU5HpmPXag0e");
        config.set(Settings.WEBSERVER_CERTIFICATE_STOREPASS, "wDwwf663NLTm73gL");
        config.set(Settings.WEBSERVER_CERTIFICATE_ALIAS, "DefaultPlanCert");

        config.set(Settings.WEBSERVER_PORT, 9005);

        bukkitSystem.enable();

        bukkitSystem.getDatabaseSystem().getDatabase().save()
                .webUser(new WebUser("test", PassEncryptUtil.createHash("testPass"), 0));
    }

    @AfterClass
    public static void tearDownClass() {
        if (bukkitSystem != null) {
            bukkitSystem.disable();
        }
    }

    /**
     * Test case against "Perm level 0 required, got 0".
     */
    @Test
    @Ignore("HTTPS Start fails due to paths being bad for some reason")
    public void testHTTPSAuthForPages() throws IOException, WebException, KeyManagementException, NoSuchAlgorithmException {
        String address = "https://localhost:9005";
        URL url = new URL(address);
        HttpURLConnection connection = connector.getConnection("HET", address);

        String user = Base64Util.encode("test:testPass");
        connection.setRequestProperty("Authorization", "Basic " + user);

        int responseCode = connection.getResponseCode();

        switch (responseCode) {
            case 200:
            case 302:
                return;
            case 400:
                throw new BadRequestException("Bad Request: " + url.toString());
            case 403:
                throw new ForbiddenException(url.toString() + " returned 403");
            case 404:
                throw new NotFoundException(url.toString() + " returned a 404, ensure that your server is connected to an up to date Plan server.");
            case 412:
                throw new UnauthorizedServerException(url.toString() + " reported that it does not recognize this server. Make sure '/plan m setup' was successful.");
            case 500:
                throw new InternalErrorException();
            default:
                throw new WebException(url.toString() + "| Wrong response code " + responseCode);
        }
    }
}