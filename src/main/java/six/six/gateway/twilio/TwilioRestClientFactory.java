package six.six.gateway.twilio;

import com.twilio.Twilio;
import com.twilio.http.TwilioRestClient;
import org.apache.commons.lang.StringUtils;
import org.jboss.logging.Logger;

public class TwilioRestClientFactory {
    
    private static Logger logger = Logger.getLogger(TwilioRestClientFactory.class);
    
    private static final String HTTPS_PROXY = "HTTPS_PROXY_TWILIO";
    
    private TwilioRestClientFactory() {
        
    }

    /**
     * call Twilio.init before creating a rest client
     * @param username
     * @param password
     * @return
     */
    public static TwilioRestClient getRestClient(String username, String password) {
        String proxy = System.getProperty(HTTPS_PROXY);
        if (StringUtils.isBlank(proxy)) {
            proxy = System.getenv(HTTPS_PROXY);
        }
        logger.info("proxy for twilio client = " + proxy);
        if (StringUtils.isNotBlank(proxy)) {
            return new ProxiedTwilioClientCreator(username, password, proxy).getClient();
        } else {
            return Twilio.getRestClient();
        }
        
    }

}
