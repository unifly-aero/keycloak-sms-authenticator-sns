package six.six.keycloak;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;

/***
 * Substitutor based on Properties and Environment
 */
public class EnvSubstitutor {

    public static final StrSubstitutor envStrSubstitutor = new StrSubstitutor(new EnvLookUp());

    private EnvSubstitutor() {
        throw new IllegalStateException("Utility class");
    }
    private static class EnvLookUp extends StrLookup {

        @Override
        public String lookup(String key) {
            String value = System.getProperty(key);
            if (StringUtils.isBlank(value)) {
                value = System.getenv(key);
            }

            if (StringUtils.isBlank(value)) {
                throw new IllegalArgumentException("key " + key + " is not found in the env variables");
            }
            return value;
        }
    }
}
