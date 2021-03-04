package six.six.gateway.twilio;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.jboss.logging.Logger;
import six.six.gateway.SMSService;

public class TwilioSmsService implements SMSService {
  private static final Logger logger = Logger.getLogger(TwilioSmsService.class);

  @Override
  public boolean send(String phoneNumber, String message, String login, String pw) {
    Twilio.init(login, pw);

    Message sms = Message.creator(new PhoneNumber(phoneNumber), new PhoneNumber("+15815015124"), message).create();
    return true;
  }
}
