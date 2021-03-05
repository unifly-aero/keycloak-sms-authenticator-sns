package six.six.gateway.twilio;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.Message.Status;
import com.twilio.type.PhoneNumber;
import org.jboss.logging.Logger;
import six.six.gateway.SMSService;
import six.six.keycloak.authenticator.KeycloakSmsAuthenticatorUtil;

public class TwilioSmsService implements SMSService {
  private static Logger logger = Logger.getLogger(TwilioSmsService.class);

  private final PhoneNumber twilioFromPhoneNumber;

  public TwilioSmsService(String twilioFromPhoneNumber) {
    this.twilioFromPhoneNumber = new PhoneNumber(twilioFromPhoneNumber);
  }

  @Override
  public boolean send(String phoneNumber, String message, String login, String pw) {
    Twilio.init(login, pw);

    Message sms = Message.creator(new PhoneNumber(phoneNumber), twilioFromPhoneNumber, message).create();
    if (sms.getErrorCode() != null) {
      logger.warn("Twilio can't send the SMS. It returns: " + sms);
      return false;
    } else {
      return true;
    }
  }
}
