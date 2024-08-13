package six.six.gateway.stub;

import org.jboss.logging.Logger;
import six.six.gateway.SMSService;

public class LoggingStubSmsService implements SMSService {
    private static final Logger logger = Logger.getLogger(LoggingStubSmsService.class);

    @Override
    public boolean send(String phoneNumber, String message) {
        logger.error(message);
        return true;
    }
}
