package six.six.keycloak.authenticator.credential;

import java.util.Optional;
import java.util.stream.Stream;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialTypeMetadata;
import org.keycloak.credential.CredentialTypeMetadataContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;
import org.keycloak.models.cache.UserCache;
import six.six.keycloak.KeycloakSmsConstants;

/**
 * Created by nickpack on 09/08/2017.
 */
public class KeycloakSmsAuthenticatorCredentialProvider implements CredentialProvider<SmsOtpCredentialModel>, CredentialInputValidator, CredentialInputUpdater, OnUserCache {
    private static final String CACHE_KEY = KeycloakSmsAuthenticatorCredentialProvider.class.getName() + "." + KeycloakSmsConstants.USR_CRED_MDL_SMS_CODE;

    private final KeycloakSession session;

    public KeycloakSmsAuthenticatorCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    private Optional<CredentialModel> getSecret(UserModel user) {
        if (user instanceof CachedUserModel) {
            CachedUserModel cached = (CachedUserModel) user;
            return Optional.of((CredentialModel) cached.getCachedWith().get(CACHE_KEY));
        } else {
            return user.credentialManager().getStoredCredentialsByTypeStream(KeycloakSmsConstants.USR_CRED_MDL_SMS_CODE).findFirst();
        }
    }


    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!KeycloakSmsConstants.USR_CRED_MDL_SMS_CODE.equals(input.getType())) return false;
        if (!(input instanceof CredentialInput)) return false;
        Optional<CredentialModel> creds = user.credentialManager().getStoredCredentialsByTypeStream(KeycloakSmsConstants.USR_CRED_MDL_SMS_CODE).findFirst();
        if (creds.isEmpty()) {
            CredentialModel secret = new CredentialModel();
            secret.setType(KeycloakSmsConstants.USR_CRED_MDL_SMS_CODE);
            secret.setSecretData(input.getChallengeResponse());
            secret.setCreatedDate(Time.currentTimeMillis());
            user.credentialManager().createStoredCredential(secret);
        } else {
            creds.get().setSecretData(input.getChallengeResponse());
            user.credentialManager().updateStoredCredential(creds.get());
        }
        session.getProvider(UserCache.class).evict(realm, user);
        return true;
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        if (!KeycloakSmsConstants.USR_CRED_MDL_SMS_CODE.equals(credentialType)) return;

        user.credentialManager().disableCredentialType(credentialType);
        session.getProvider(UserCache.class).evict(realm, user);
    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
        if (user.credentialManager().getStoredCredentialsByTypeStream(KeycloakSmsConstants.USR_CRED_MDL_SMS_CODE).findAny().isPresent()) {
            return Stream.of(KeycloakSmsConstants.USR_CRED_MDL_SMS_CODE);
        } else {
            return Stream.empty();
        }
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return KeycloakSmsConstants.USR_CRED_MDL_SMS_CODE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return KeycloakSmsConstants.USR_CRED_MDL_SMS_CODE.equals(credentialType) && getSecret(user).isPresent();
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!KeycloakSmsConstants.USR_CRED_MDL_SMS_CODE.equals(input.getType())) return false;
        if (!(input instanceof SmsOtpCredentialModel)) return false;

        String secretData = getSecret(user).map(CredentialModel::getSecretData).orElse(null);

        return secretData != null && ((SmsOtpCredentialModel) input).getCredentialData().equals(secretData);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onCache(RealmModel realm, CachedUserModel user, UserModel delegate) {
        user.credentialManager().getStoredCredentialsByTypeStream(KeycloakSmsConstants.USR_CRED_MDL_SMS_CODE).findFirst()
            .ifPresent(creds -> user.getCachedWith().put(CACHE_KEY, creds));
    }

	@Override
	public String getType() {
		return KeycloakSmsConstants.USR_CRED_MDL_SMS_CODE;
	}

	@Override
	public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
		return user.credentialManager().removeStoredCredentialById(credentialId);
	}

	@Override
	public SmsOtpCredentialModel getCredentialFromModel(CredentialModel model) {
		return SmsOtpCredentialModel.createFromCredentialModel(model);
	}

    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext credentialTypeMetadataContext) {
        return CredentialTypeMetadata.builder()
            .removeable(false)
            .category(CredentialTypeMetadata.Category.TWO_FACTOR)
            .helpText("Help")
            .displayName("SMS Authentication")
            .type("otp")
            .build(session);
    }

    @Override
	public CredentialModel createCredential(RealmModel realm, UserModel user, SmsOtpCredentialModel credentialModel) {
		if (credentialModel.getCreatedDate() == null) {
            credentialModel.setCreatedDate(Time.currentTimeMillis());
        }
        return user.credentialManager().createStoredCredential(credentialModel);
	}

}
