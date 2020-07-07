package six.six.gateway.openvox;

import org.jboss.logging.Logger;
import six.six.gateway.SMSService;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.net.URL;
import java.util.HashMap;


public class OpenVoxNotificationService implements SMSService {

    private String endpoint;
    private String port;
    private static Logger logger = Logger.getLogger(OpenVoxNotificationService.class);

    public OpenVoxNotificationService(String endpoint, String smsPort) {
        this.endpoint = endpoint;
        this.port = smsPort;
    }

    @Override
    public boolean send(String phoneNumber, String message, String login, String pw) {
        logger.info(String.format("Sending '%s' to %s via endpoint '%s' port: %s", message, phoneNumber, endpoint, port));
        try {
            //Prepare message
            message = message.replaceAll("\\n", "\\n").replaceAll("\\r", "");

            if(!endpoint.startsWith("https")) endpoint = "https://" + endpoint;
            URL url = new URL(endpoint);
            HashMap<String, String> parameters = new HashMap<>();
            parameters.put("username", login);
            parameters.put("password", pw);
            parameters.put("phonenumber", phoneNumber);
            parameters.put("message", message);
            parameters.put("port", port);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("GET");
            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            out.writeBytes(ParameterStringBuilder.getParamsString(parameters));
            out.flush();
            out.close();
            return connection.getResponseCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
