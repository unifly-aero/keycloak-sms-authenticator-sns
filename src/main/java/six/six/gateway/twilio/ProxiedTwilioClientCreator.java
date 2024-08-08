package six.six.gateway.twilio;

import static com.twilio.http.HttpClient.DEFAULT_REQUEST_CONFIG;

import com.twilio.http.HttpClient;
import com.twilio.http.NetworkHttpClient;
import com.twilio.http.TwilioRestClient;
import org.apache.http.HttpHost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * copied from https://github.com/twilio/twilio-java/blob/main/advanced-examples/custom-http-client.md
 */
public class ProxiedTwilioClientCreator {

    private String username;
    private String password;
    private String proxyString;
    private HttpClient httpClient;

    /**
     * Constructor for ProxiedTwilioClientCreator
     * 
     * @param username
     * @param password
     * @param proxyHost
     * @param proxyPort
     */
    public ProxiedTwilioClientCreator(
            String username,
            String password,
            String proxy) {
        this.username = username;
        this.password = password;
        this.proxyString = proxy;
    }

    /**
     * Creates a custom HttpClient based on default config as seen on:
     * {@link com.twilio.http.NetworkHttpClient#NetworkHttpClient() constructor}
     */
    private void createHttpClient() {

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(10);
        connectionManager.setMaxTotal(10 * 2);

        HttpHost proxy = HttpHost.create(proxyString);

        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        
        /*
         * if we ever need to implement connection to proxy with authentication, we need to do something like this:
        CredentialsProvider credentialsPovider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope("example.com", 80), newUsernamePasswordCredentials("user", "mypass"));
        clientBuilder.setDefaultCredentialsProvider(credsProvider)
        */
        
        clientBuilder
                .setConnectionManager(connectionManager)
                .setProxy(proxy)
                .setDefaultRequestConfig(DEFAULT_REQUEST_CONFIG);

        // Inclusion of Twilio headers and build() is executed under this constructor
        this.httpClient = new NetworkHttpClient(clientBuilder);
    }

    /**
     * Get the custom client or builds a new one
     * 
     * @return a TwilioRestClient object
     */
    public TwilioRestClient getClient() {
        if (this.httpClient == null) {
            this.createHttpClient();
        }

        TwilioRestClient.Builder builder = new TwilioRestClient.Builder(
                username,
                password);
        return builder.httpClient(this.httpClient).build();
    }
}