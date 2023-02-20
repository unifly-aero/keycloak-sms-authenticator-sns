package six.six.keycloak.requiredaction.credential;

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
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;
import org.keycloak.models.cache.UserCache;

/**
 * Created by nickpack on 15/08/2017.
 */
public class KeycloakSmsMobilenumberCredentialProvider implements CredentialProvider, CredentialInputValidator, CredentialInputUpdater, OnUserCache {
    public static final String MOBILE_NUMBER = "mobile_number";
    public static final String CACHE_KEY = KeycloakSmsMobilenumberCredentialProvider.class.getName() + "." + MOBILE_NUMBER;

    protected KeycloakSession session;

    public KeycloakSmsMobilenumberCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    public Optional<CredentialModel> getSecret(UserModel user) {
        if (user instanceof CachedUserModel) {
            CachedUserModel cached = (CachedUserModel)user;
            return Optional.of((CredentialModel)cached.getCachedWith().get(CACHE_KEY));
        } else {
            return user.credentialManager().getStoredCredentialsByTypeStream(MOBILE_NUMBER).findFirst();
        }
    }

    @Override
	public CredentialModel createCredential(RealmModel realm, UserModel user, CredentialModel credInput) {
    	if (!MOBILE_NUMBER.equals(credInput.getType())) return null;

    	CredentialModel secret = new CredentialModel();
        Optional<CredentialModel> creds = user.credentialManager().getStoredCredentialsByTypeStream(MOBILE_NUMBER).findFirst();
        if (creds.isEmpty()) {
            secret.setType(MOBILE_NUMBER);
            secret.setSecretData(credInput.getSecretData());
            secret.setCreatedDate(Time.currentTimeMillis());
            user.credentialManager().createStoredCredential(secret);
        } else {
            creds.get().setSecretData(credInput.getSecretData());
            user.credentialManager().updateStoredCredential(creds.get());
        }
        session.getProvider(UserCache.class).evict(realm, user);
        return secret;
	}

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!MOBILE_NUMBER.equals(input.getType())) return false;
        if (!(input instanceof UserCredentialModel)) return false;
        UserCredentialModel credInput = (UserCredentialModel) input;
        Optional<CredentialModel> creds = user.credentialManager().getStoredCredentialsByTypeStream(MOBILE_NUMBER).findFirst();
        if (creds.isEmpty()) {
            CredentialModel secret = new CredentialModel();
            secret.setType(MOBILE_NUMBER);
            secret.setSecretData(credInput.getChallengeResponse());
            secret.setCreatedDate(Time.currentTimeMillis());
            user.credentialManager().createStoredCredential(secret);
        } else {
            creds.get().setSecretData(credInput.getChallengeResponse());
            user.credentialManager().updateStoredCredential(creds.get());
        }
        session.getProvider(UserCache.class).evict(realm, user);
        return true;
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        if (!MOBILE_NUMBER.equals(credentialType)) return;
        user.credentialManager().disableCredentialType(credentialType);
        session.getProvider(UserCache.class).evict(realm, user);

    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
        if (user.credentialManager().getStoredCredentialsByTypeStream(MOBILE_NUMBER).findAny().isPresent()) {
            return Stream.of(MOBILE_NUMBER);
        } else {
            return Stream.empty();
        }
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return MOBILE_NUMBER.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        if (!MOBILE_NUMBER.equals(credentialType)) return false;
        return getSecret(user).isPresent();
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!MOBILE_NUMBER.equals(input.getType())) return false;
        if (!(input instanceof UserCredentialModel)) return false;

        String secretData = getSecret(user).map(CredentialModel::getSecretData).orElse(null);

        return secretData != null && ((UserCredentialModel)input).getChallengeResponse().equals(secretData);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onCache(RealmModel realm, CachedUserModel user, UserModel delegate) {
        user.credentialManager().getStoredCredentialsByTypeStream(MOBILE_NUMBER).findFirst()
            .ifPresent(creds -> user.getCachedWith().put(CACHE_KEY, creds));
    }

    @Override
    public void close() {

    }

    @Override
	public String getType() {
		return MOBILE_NUMBER;
	}


	@Override
	public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
		return user.credentialManager().removeStoredCredentialById(credentialId);
    }

	@Override
	public CredentialModel getCredentialFromModel(CredentialModel model) {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
    public CredentialModel getDefaultCredential(KeycloakSession session, RealmModel realm, UserModel user) {
        return null;
    }

    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext credentialTypeMetadataContext) {
        return CredentialTypeMetadata.builder()
            .removeable(false)
            .category(CredentialTypeMetadata.Category.TWO_FACTOR)
            .displayName("SMS Authentication")
            .type("OTP")
            .helpText("Help")
            .build(session);
    }

}
