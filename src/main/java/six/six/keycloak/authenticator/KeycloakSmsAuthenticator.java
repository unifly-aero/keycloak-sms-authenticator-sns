package six.six.keycloak.authenticator;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.CredentialValidator;
import org.keycloak.authentication.authenticators.challenge.BasicAuthAuthenticator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import six.six.keycloak.KeycloakSmsConstants;
import six.six.keycloak.MobileNumberHelper;
import six.six.keycloak.authenticator.credential.KeycloakSmsAuthenticatorCredentialProvider;
import six.six.keycloak.requiredaction.action.required.KeycloakSmsMobilenumberRequiredAction;

/**
 * Created by joris on 11/11/2016.
 */
public class KeycloakSmsAuthenticator extends BasicAuthAuthenticator implements CredentialValidator<KeycloakSmsAuthenticatorCredentialProvider> {

    private static final String SMS_VALIDATION_FTL = "sms-validation.ftl";
    private static final String MOBILE_NUMBER_ATTRIBUTE = "mobileNumber";

    private static Logger logger = Logger.getLogger(KeycloakSmsAuthenticator.class);

    public static final String CREDENTIAL_TYPE = "sms_validation";

    private enum CODE_STATUS {
        VALID,
        INVALID,
        EXPIRED
    }


    private boolean isOnlyForVerificationMode(boolean onlyForVerification,String mobileNumber,String mobileNumberVerified){
        return (mobileNumber ==null || onlyForVerification==true && !mobileNumber.equals(mobileNumberVerified) );
    }

    private String getMobileNumber(UserModel user){
        return MobileNumberHelper.getMobileNumber(user);
    }

    private Optional<String> getMobileNumberVerified(UserModel user){
        return user.getAttributeStream(KeycloakSmsConstants.ATTR_MOBILE_VERIFIED).findFirst();
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.debug("authenticate called ... context = " + context);
        UserModel user = context.getUser();

        if (!"SMS".equals(user.getFirstAttribute("mfa"))) {
          context.success();
          return;
        }

        AuthenticatorConfigModel config = context.getAuthenticatorConfig();

        boolean onlyForVerification = KeycloakSmsAuthenticatorUtil.getConfigBoolean(config, KeycloakSmsConstants.MOBILE_VERIFICATION_ENABLED);

        String mobileNumber =getMobileNumber(user);
        String mobileNumberVerified = getMobileNumberVerified(user).orElse(null);

        if (onlyForVerification == false || isOnlyForVerificationMode(onlyForVerification, mobileNumber, mobileNumberVerified)){
            if (mobileNumber != null) {
                // The mobile number is configured --> send an SMS
                long nrOfDigits = KeycloakSmsAuthenticatorUtil.getConfigLong(config, KeycloakSmsConstants.CONF_PRP_SMS_CODE_LENGTH, 8L);
                logger.info("Using nrOfDigits " + nrOfDigits);


                long ttl = KeycloakSmsAuthenticatorUtil.getConfigLong(config, KeycloakSmsConstants.CONF_PRP_SMS_CODE_TTL, 10 * 60L); // 10 minutes in s

                logger.info("Using ttl " + ttl + " (s)");

                String code = KeycloakSmsAuthenticatorUtil.getSmsCode(nrOfDigits);

                storeSMSCode(context, code, new Date().getTime() + (ttl * 1000)); // s --> ms

                if (KeycloakSmsAuthenticatorUtil.sendSmsCode(mobileNumber, code, context)) {

                    context.challenge(context.form()
                        .setAttribute(MOBILE_NUMBER_ATTRIBUTE, getObfuscatedMobile(mobileNumber))
                        .createForm(SMS_VALIDATION_FTL));

                } else {
                    Response challenge2 = context.form()
                            .setError("sms-auth.not.send")
                            .createForm("sms-validation-error.ftl");
                    context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR, challenge2);
                }

            } else {
                boolean isAskingFor=KeycloakSmsAuthenticatorUtil.getConfigBoolean(config, KeycloakSmsConstants.MOBILE_ASKFOR_ENABLED);
                if(isAskingFor){
                    //Enable access and ask for mobilenumber
                    user.addRequiredAction(KeycloakSmsMobilenumberRequiredAction.PROVIDER_ID);
                    context.success();
                }else {
                    // The mobile number is NOT configured --> complain
                    Response challenge = context.form()
                            .setError("sms-auth.not.mobile")
                            .createForm("sms-validation-error.ftl");
                    context.failureChallenge(AuthenticationFlowError.CLIENT_CREDENTIALS_SETUP_REQUIRED, challenge);
                }
            }
        }else{
            logger.debug("Skip SMS code because onlyForVerification " + onlyForVerification + " or  mobileNumber==mobileNumberVerified");
            context.success();

        }
    }

    private String getObfuscatedMobile(String mobileNumber) {
        if (mobileNumber == null) {
            return "";
        }
        return mobileNumber.replaceAll("(?<=.{3}).(?=.{2})", "*");
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        logger.debug("action called ... context = " + context);
        CODE_STATUS status = validateCode(context);
        Response challenge = null;
        String mobileNumber = getMobileNumber(context.getUser());
        String obfuscatedMobile = getObfuscatedMobile(mobileNumber);
        switch (status) {
            case EXPIRED:
                challenge = context.form()
                        .setError("sms-auth.code.expired")
                        .setAttribute(MOBILE_NUMBER_ATTRIBUTE, obfuscatedMobile)
                        .createForm(SMS_VALIDATION_FTL);
                context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE, challenge);
                break;

            case INVALID:
                if (context.getExecution().getRequirement() == AuthenticationExecutionModel.Requirement.CONDITIONAL ||
                        context.getExecution().getRequirement() == AuthenticationExecutionModel.Requirement.ALTERNATIVE) {
                    logger.debug("Calling context.attempted()");
                    context.attempted();
                } else if (context.getExecution().getRequirement() == AuthenticationExecutionModel.Requirement.REQUIRED) {
                    challenge = context.form()
                            .setError("sms-auth.code.invalid")
                            .setAttribute(MOBILE_NUMBER_ATTRIBUTE, obfuscatedMobile)
                            .createForm(SMS_VALIDATION_FTL);
                    context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
                } else {
                    // Something strange happened
                    logger.warn("Undefined execution ...");
                }
                break;

            case VALID:
                context.success();
                updateVerifiedMobilenumber(context);
                break;

        }
    }

    /**
     * If necessary update verified mobilenumber
     * @param context
     */
    private void updateVerifiedMobilenumber(AuthenticationFlowContext context){
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        UserModel user = context.getUser();
        boolean onlyForVerification=KeycloakSmsAuthenticatorUtil.getConfigBoolean(config, KeycloakSmsConstants.MOBILE_VERIFICATION_ENABLED);

        if(onlyForVerification){
            //Only verification mode
            List<String> mobileNumberCreds = user.getAttributeStream(KeycloakSmsConstants.ATTR_MOBILE).collect(Collectors.toList());
            if (mobileNumberCreds != null && !mobileNumberCreds.isEmpty()) {
                user.setAttribute(KeycloakSmsConstants.ATTR_MOBILE_VERIFIED,mobileNumberCreds);
            }
        }
    }

    // Store the code + expiration time in a UserCredential. Keycloak will persist these in the DB.
    // When the code is validated on another node (in a clustered environment) the other nodes have access to it's values too.
    private void storeSMSCode(AuthenticationFlowContext context, String code, Long expiringAt) {

        //Delete previously stored credentials Data of SMS OPT for this user
        SubjectCredentialManager credentialManager = context.getUser().credentialManager();
        credentialManager.getStoredCredentialsByTypeStream(KeycloakSmsConstants.USR_CRED_MDL_SMS_CODE)
            .forEach(credential -> credentialManager.removeStoredCredentialById(credential.getId()));
        credentialManager.getStoredCredentialsByTypeStream(KeycloakSmsConstants.USR_CRED_MDL_SMS_EXP_TIME)
        .forEach(credential -> credentialManager.removeStoredCredentialById(credential.getId()));

        CredentialModel credentials = new CredentialModel();
        credentials.setType(KeycloakSmsConstants.USR_CRED_MDL_SMS_CODE);
        credentials.setCredentialData(code);
        credentialManager.createStoredCredential(credentials);

        credentials = new CredentialModel();
        credentials.setType(KeycloakSmsConstants.USR_CRED_MDL_SMS_EXP_TIME);
        credentials.setSecretData((expiringAt).toString());
        credentialManager.createStoredCredential(credentials);
    }


    protected CODE_STATUS validateCode(AuthenticationFlowContext context) {
        CODE_STATUS result = CODE_STATUS.INVALID;

        logger.debug("validateCode called ... ");
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String enteredCode = formData.getFirst(KeycloakSmsConstants.ANSW_SMS_CODE);

        SubjectCredentialManager credentialManager = context.getUser().credentialManager();

        Stream<CredentialModel> codeCreds = credentialManager.getStoredCredentialsByTypeStream(KeycloakSmsConstants.USR_CRED_MDL_SMS_CODE);
        Stream<CredentialModel> timeCreds = credentialManager.getStoredCredentialsByTypeStream(KeycloakSmsConstants.USR_CRED_MDL_SMS_EXP_TIME);

        CredentialModel expectedCode = codeCreds.findFirst().orElse(null);
        CredentialModel expTimeString = timeCreds.findFirst().orElse(null);

        logger.debug("Expected code = " + expectedCode != null ? expectedCode.getCredentialData() : "null" + ", entered code = " + enteredCode);

        if (expectedCode != null && expTimeString != null) {
            result = enteredCode.equals(expectedCode.getCredentialData()) ? CODE_STATUS.VALID : CODE_STATUS.INVALID;
            long now = new Date().getTime();

            logger.debug("Valid code expires in " + (Long.parseLong(expTimeString.getSecretData()) - now) + " ms");
            if (result == CODE_STATUS.VALID) {
                if (Long.parseLong(expTimeString.getSecretData()) < now) {
                    logger.info("Code is expired !!");
                    result = CODE_STATUS.EXPIRED;
                }
            }
        }
        logger.debug("result : " + result);
        return result;
    }
    @Override
    public boolean requiresUser() {
        logger.debug("requiresUser called ... returning true");
        return true;
    }
    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        logger.debug("configuredFor called ... session=" + session + ", realm=" + realm + ", user=" + user);
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        user.addRequiredAction(KeycloakSmsMobilenumberRequiredAction.PROVIDER_ID);
    }
    @Override
    public void close() {
        logger.debug("close called ...");
    }

	@Override
	public KeycloakSmsAuthenticatorCredentialProvider getCredentialProvider(KeycloakSession session) {
		return (KeycloakSmsAuthenticatorCredentialProvider)session.getProvider(CredentialProvider.class, "smsCode");
	}

}
