package six.six.gateway.twilio;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import six.six.gateway.SMSService;

public class TwilioSmsService implements SMSService {

  private final PhoneNumber twilioFromPhoneNumber;

  public TwilioSmsService(String twilioFromPhoneNumber) {
    this.twilioFromPhoneNumber = new PhoneNumber(twilioFromPhoneNumber);
  }

  @Override
  public boolean send(String phoneNumber, String message, String login, String pw) {
    Twilio.init(login, pw);

    Message.creator(new PhoneNumber(phoneNumber), twilioFromPhoneNumber, message).create();
    return true;
  }
}
