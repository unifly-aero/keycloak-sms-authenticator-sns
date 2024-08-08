package six.six.gateway.twilio;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.jboss.logging.Logger;
import six.six.gateway.SMSService;

public class TwilioSmsService implements SMSService {
    private static Logger logger = Logger.getLogger(TwilioSmsService.class);

    private final PhoneNumber twilioFromPhoneNumber;

    public TwilioSmsService(String twilioFromPhoneNumber, String login, String pw) {
        this.twilioFromPhoneNumber = new PhoneNumber(twilioFromPhoneNumber);
        Twilio.init(login, pw);
        Twilio.setRestClient(TwilioRestClientFactory.getRestClient(login, pw));
    }

    @Override
    public boolean send(String phoneNumber, String message) {

        Message sms = Message.creator(new PhoneNumber(phoneNumber), twilioFromPhoneNumber, message).create();
        if (sms.getErrorCode() != null) {
            logger.warn("Twilio can't send the SMS. It returns: " + sms);
            return false;
        } else {
            return true;
        }
    }
}
