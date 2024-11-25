# Mapletree forked Keycloak

We extended the keycloak with popular China's social media identifier providers,
including:

- Wecom

## Build

```bash
./mvnw -pl mapletree/social/wecom clean install 
```

## Implement Details

### Wecom

The keycloak standard OAuth2 (OIDC is extended from OAuth2) authenticate flow is

1. User click on social login button
   1. AbstractOAuth2IdentityProvider.performLogin
      1. AbstractOAuth2IdentityProvider.createAuthorizationUrl
      1. 302 redirect
1. Redirect user to IdP
   1. login at IdP
1. Redirect user back to keycloak
   1. AbstractOAuth2IdentityProvider.callback
      1. AbstractOAuth2IdentityProvider.EndPoint.authResponse@Get
         1. AbstractOAuth2IdentityProvider.EndPoint.generateTokenRequest
            `Get access_token from code`
         1. AbstractOAuth2IdentityProvider.getFederatedIdentity.
            `Get userinfo form access_token Generate federatedIdentity for account search`
   1. IdentityBrokerService.autenticated
      1. UserStorageManager.getUserByFederatedIdentity

However, the wecom authenticate flow is incompatiple to the standard flow.

1. Background service retrives agent access_token every two hours.
1. Redirect user to IdP
1. Redirect user back to service with code
1. Get wecom userid with agent access_token and user code
1. Get user name with agent access_token and wecom userid
1. (Only in wecom app) Get user email and phone with agent access_token and
   wecom userid

The key difference is that wecom asks the service to store access_token on its
own and use three http requests to get the user info. And if any request fails
with access_token expired. Refresh the agent access_token and retry the fetch.
To make wecom compatible with keycloak, every plugin choose different point to
override (and hack) the keycloak social provider.

Our choise is to block the "generateTokenRequest" with fake http response. And do all the dirty stuff with overriding the "getFederatedIdentity" call.

## Prior Arts

We thank the following great works for inspiring our project. However, the
reason why we decided to rewrite the code is that non of the projects are
actively under maintained. We cannot finish the prject such easily without these
prior works.

https://github.com/jyqq163/keycloak-services-social-weixin

https://github.com/Jeff-Tian/keycloak-services-social-weixin ( was removed by
author. A backup mirror at
https://gitee.com/zizhujy/keycloak-services-social-weixin )

https://github.com/kkzxak47/keycloak-services-social-wechatworkr
