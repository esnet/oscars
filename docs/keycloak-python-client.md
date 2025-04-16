# Use Python to access Keycloak-protected APIs

## Environment
Set up your python environment and install the `python-keycloak` and `request` packages:

```shell
python3 -m venv somepath
source somepath/bin/activate
pip install python-keycloak requests
python3 python-keycloak-client.py

```


## Sample client


```python
from keycloak import KeycloakOpenID
import requests


# Configure client - coordinate these with the keycloak provider
keycloak_openid = KeycloakOpenID(server_url="https://sso-dev.es.net/auth/",
                                 client_id="local-oscars-frontend",
                                 realm_name="an_esnet")


# Get Token
token = keycloak_openid.token("your-LDAP-username", "your-LDAP-password")

# Use token to access protected API
headers = {"Authorization": "Bearer "+token['access_token']}
x = requests.get('http://localhost:8181/protected/conn/generateId', headers = headers)
print(x.text)

```
