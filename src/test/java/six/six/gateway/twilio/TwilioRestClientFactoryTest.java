package six.six.gateway.twilio;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.twilio.Twilio;
import com.twilio.http.NetworkHttpClient;
import com.twilio.http.TwilioRestClient;
import org.apache.commons.lang.reflect.FieldUtils;
import org.junit.jupiter.api.Test;

class TwilioRestClientFactoryTest {

    @Test
    void testGetRestClient_returnsDefaultTwilioClient_whenNoEnvParam() throws IllegalAccessException {
        Twilio.init("twi", "lio");
        TwilioRestClient client = TwilioRestClientFactory.getRestClient("john", "secret");
        NetworkHttpClient networkHttpClient = (NetworkHttpClient) client.getHttpClient();
        
        Boolean isCustomClient = (Boolean) FieldUtils.readField(FieldUtils.getDeclaredField(NetworkHttpClient.class, "isCustomClient", true), networkHttpClient, true);
        assertFalse(isCustomClient);
    }
    @Test
    void testGetRestClient_returnsProxyTwilioClient_whenEnvParamSet() throws IllegalAccessException {
        System.setProperty("TWILIO_HTTPS_PROXY", "localhost:12345");
        Twilio.init("twi", "lio");
        TwilioRestClient client = TwilioRestClientFactory.getRestClient("john", "secret");
        NetworkHttpClient networkHttpClient = (NetworkHttpClient) client.getHttpClient();
        
        Boolean isCustomClient = (Boolean) FieldUtils.readField(FieldUtils.getDeclaredField(NetworkHttpClient.class, "isCustomClient", true), networkHttpClient, true);
        assertTrue(isCustomClient);
    }
}
