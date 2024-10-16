package six.six.keycloak.authenticator;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.theme.Theme;
import six.six.gateway.Gateways;
import six.six.gateway.SMSService;
import six.six.gateway.stub.LoggingStubSmsService;
import six.six.gateway.twilio.TwilioSmsService;
import six.six.keycloak.EnvSubstitutor;
import six.six.keycloak.KeycloakSmsConstants;

/**
 * Created by joris on 18/11/2016.
 */
public class KeycloakSmsAuthenticatorUtil {

    private static Logger logger = Logger.getLogger(KeycloakSmsAuthenticatorUtil.class);
    private static volatile TwilioSmsService twilioSmsService;

    private KeycloakSmsAuthenticatorUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static Optional<String> getAttributeValue(UserModel user, String attributeName) {
        return user.getAttributeStream(attributeName).findFirst();
    }

    public static String getConfigString(AuthenticatorConfigModel config, String configName) {
        return getConfigString(config, configName, null);
    }

    public static String getConfigString(AuthenticatorConfigModel config, String configName, String defaultValue) {

        String value = defaultValue;

        if (config.getConfig() != null) {
            // Get value
            value = config.getConfig().get(configName);
        }

        return value;
    }

    public static Long getConfigLong(AuthenticatorConfigModel config, String configName) {
        return getConfigLong(config, configName, null);
    }

    public static Long getConfigLong(AuthenticatorConfigModel config, String configName, Long defaultValue) {

        Long value = defaultValue;

        if (config.getConfig() != null) {
            // Get value
            Object obj = config.getConfig().get(configName);
            try {
                value = Long.valueOf((String) obj); // s --> ms
            } catch (NumberFormatException nfe) {
                logger.error("Can not convert " + obj + " to a number.");
            }
        }

        return value;
    }

    public static Boolean getConfigBoolean(AuthenticatorConfigModel config, String configName) {
        return getConfigBoolean(config, configName, true);
    }

    public static Boolean getConfigBoolean(AuthenticatorConfigModel config, String configName, Boolean defaultValue) {

        Boolean value = defaultValue;

        if (config.getConfig() != null) {
            // Get value
            Object obj = config.getConfig().get(configName);
            try {
                value = Boolean.valueOf((String) obj); // s --> ms
            } catch (NumberFormatException nfe) {
                logger.error("Can not convert " + obj + " to a boolean.");
            }
        }

        return value;
    }

    public static String createMessage(String text,String code, String mobileNumber) {
        if(text !=null){
            text = text.replaceAll("%sms-code%", code);
            text = text.replaceAll("%phonenumber%", mobileNumber);
        }
        return text;
    }

    public static String setDefaultCountryCodeIfZero(String mobileNumber,String prefix ,String condition) {

        if (prefix!=null && condition!=null && mobileNumber.startsWith(condition)) {
            mobileNumber = prefix + mobileNumber.substring(1);
        }
        return mobileNumber;
    }

    /**
     * Check mobile number normative strcuture
     * @param mobileNumber
     * @return formatted mobile number
     */
    public static String checkMobileNumber(String mobileNumber) {

        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber phone = phoneUtil.parse(mobileNumber, null);
            mobileNumber = phoneUtil.format(phone,
                    PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            logger.error("Invalid phone number " + mobileNumber, e);
        }

        return mobileNumber;
    }


    public static String getMessage(AuthenticationFlowContext context, String key){
        String result=null;

        try {
            KeycloakSession session = context.getSession();
            Theme theme = session.theme().getTheme(Theme.Type.LOGIN);
            Locale locale = session.getContext().resolveLocale(context.getUser());

            logger.info("theme: " + theme != null ? theme.getName() : "null" + ", locale: "+ locale + ", key: " + key);

            result = theme.getMessages(locale).getProperty(key);
        }catch (IOException e){
            logger.warn(key + "not found in messages");
        }
        return result;
    }


    static boolean sendSmsCode(String mobileNumber, String code, AuthenticationFlowContext context) {
        final AuthenticatorConfigModel config = context.getAuthenticatorConfig();

        // Send an SMS
        KeycloakSmsAuthenticatorUtil.logger.debug("Sending " + code + "  to mobileNumber " + mobileNumber);

        String gateway = getConfigString(config, KeycloakSmsConstants.CONF_PRP_SMS_GATEWAY);

        // Create the SMS message body
        String template = getMessage(context, KeycloakSmsConstants.CONF_PRP_SMS_TEXT);
        String smsText = createMessage(template, code, mobileNumber);

        boolean result;
        SMSService smsService;
        try {
            Gateways g = Gateways.valueOf(gateway);
            switch(g) {
                case LOGGING_STUB:
                    smsService = new LoggingStubSmsService();
                    break;
              case TWILIO:
                    smsService = getTwilioSmsService(config);
                    break;
                default:
                    smsService = new LoggingStubSmsService();
            }
            result=smsService.send(checkMobileNumber(setDefaultCountryCodeIfZero(mobileNumber, getMessage(context, KeycloakSmsConstants.MSG_MOBILE_PREFIX_DEFAULT), getMessage(context, KeycloakSmsConstants.MSG_MOBILE_PREFIX_CONDITION))), smsText);
          return result;
       } catch(Exception e) {
            logger.error("Fail to send SMS " ,e );
            return false;
        }
    }

    private static synchronized SMSService getTwilioSmsService(AuthenticatorConfigModel config) {
        if (twilioSmsService == null) {
            String twilioFromPhoneNumber = EnvSubstitutor.envStrSubstitutor.replace(getConfigString(config, KeycloakSmsConstants.CONF_PRP_SMS_FROM_PHONE_NUMBER));
            String smsUsr = EnvSubstitutor.envStrSubstitutor.replace(getConfigString(config, KeycloakSmsConstants.CONF_PRP_SMS_CLIENTTOKEN));
            String smsPwd = EnvSubstitutor.envStrSubstitutor.replace(getConfigString(config, KeycloakSmsConstants.CONF_PRP_SMS_CLIENTSECRET));
            twilioSmsService = new TwilioSmsService(twilioFromPhoneNumber, smsUsr, smsPwd);
        }
        return twilioSmsService;
    }

    static String getSmsCode(long nrOfDigits) {
        if (nrOfDigits < 1) {
            throw new RuntimeException("Number of digits must be bigger than 0");
        }

        double maxValue = Math.pow(10.0, nrOfDigits); // 10 ^ nrOfDigits;
        Random r = new Random();
        long code = (long) (r.nextFloat() * maxValue);
        return Long.toString(code);
    }

    /**
     * This validation matches the registration flow's validation
     * https://github.com/UKGovernmentBEIS/beis-mspsds/blob/master/keycloak/providers/registration-form/src/main/java/uk/gov/beis/opss/keycloak/providers/RegistrationMobileNumber.java#L55
     */
    public static boolean isPhoneNumberValid(String phoneNumber) {
        String formattedPhoneNumber = convertInternationalPrefix(phoneNumber);

        String region;
        if (isPossibleNationalNumber(formattedPhoneNumber)) {
            region = "GB";
        } else if (isInternationalNumber(formattedPhoneNumber)) {
            region = null;
        } else {
            return true; // If the number cannot be interpreted as an international or possible UK phone number, do not attempt to validate it.
        }

        try {
            PhoneNumber parsedPhoneNumber = PhoneNumberUtil.getInstance().parse(formattedPhoneNumber, region);
            return PhoneNumberUtil.getInstance().isValidNumber(parsedPhoneNumber);
        } catch (NumberParseException e) {
            return false;
        }
    }

    private static String convertInternationalPrefix(String phoneNumber) {
        String trimmedPhoneNumber = phoneNumber.trim();
        if (trimmedPhoneNumber.startsWith("00")) {
            return trimmedPhoneNumber.replaceFirst("00", "+");
        }
        return trimmedPhoneNumber;
    }

    private static boolean isPossibleNationalNumber(String phoneNumber) {
        return phoneNumber.trim().startsWith("+44") || phoneNumber.trim().startsWith("07");
    }

    private static boolean isInternationalNumber(String phoneNumber) {
        return phoneNumber.trim().startsWith("+");
    }
}
