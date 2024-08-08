# keycloak-sms-authenticator-sns

## Installation
To install the SMS Authenticator one has to:

* Build and package the project:
  * `$ mvn package`

## Configuration

### Twilio

#### Proxy

To use Twilio integration over a proxy, pass environment variable "HTTPS_PROXY_TWILIO" with value the proxy url (for example http://myproxy.com:12324).
A proxy that requires authentication is not yet supported.
