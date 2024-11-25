package com.mptree.keycloak.social.wecom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.UriBuilder;
import java.io.IOException;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public class WecomIdentityProvider
    extends AbstractOAuth2IdentityProvider<WecomIdentityProviderConfig>
    implements SocialIdentityProvider<WecomIdentityProviderConfig> {
  public static final String NATIVE_AUTH_URL =
      "https://open.weixin.qq.com/connect/oauth2/authorize";
  public static final String AUTH_URL =
      "https://login.work.weixin.qq.com/wwlogin/sso/login";
  public static final String TOKEN_URL =
      "https://qyapi.weixin.qq.com/cgi-bin/gettoken";
  public static final String USER_INFO_URL =
      "https://qyapi.weixin.qq.com/cgi-bin/auth/getuserinfo";
  public static final String USER_ENTITY_URL =
      "https://qyapi.weixin.qq.com/cgi-bin/user/get";
  public static final String USER_DETAIL_URL =
      "https://qyapi.weixin.qq.com/cgi-bin/auth/getuserdetail";

  public static final String HTTP_HEADER_USER_AGENT = "user-agent";
  public static final String OAUTH2_RESPONSE_TYPE_CODE = "code";

  public static final String WECOM_USER_AGENT_WXWORK = "wxwork";

  public static final String WECOM_PARAM_ACCESS_TOKEN = "access_token";
  public static final String WECOM_PARAM_AGENTID = "agentid";
  public static final String WECOM_PARAM_APPID = "appid";
  public static final String WECOM_PARAM_CODE = "code";
  public static final String WECOM_PARAM_CORPID = "corpid";
  public static final String WECOM_PARAM_CORPSECRET = "corpsecret";
  public static final String WECOM_PARAM_LOGIN_TYPE = "login_type";
  public static final String WECOM_PARAM_USER_TICKET = "user_ticket";
  public static final String WECOM_PARAM_USERID = "userid";

  public static final String WECOM_FRAGMENT_WECHAT_REDIRECT = "wechat_redirect";

  public static final String WECOM_LOGIN_TYPE_CORP_APP = "CorpApp";

  public static final String WECOM_SCOPE_SNSAPI_USERINFO = "snsapi_userinfo";
  public static final String WECOM_SCOPE_SNSAPI_PRIVATEINFO = "snsapi_privateinfo";

  public static final String WECOM_FIELD_ACCESS_TOKEN = "access_token";
  public static final String WECOM_FIELD_BIZ_MAIL = "biz_mail";
  public static final String WECOM_FIELD_ERRCODE = "errcode";
  public static final String WECOM_FIELD_ERRMSG = "errmsg";
  public static final String WECOM_FIELD_NAME = "name";
  public static final String WECOM_FIELD_USER_TICKET = "user_ticket";
  public static final String WECOM_FIELD_USERID = "userid";

  private static final ObjectMapper mapper = new ObjectMapper();

  private final WecomApiClient wecomApiClient;

  public WecomIdentityProvider(KeycloakSession session,
                               WecomIdentityProviderConfig config) {
    super(session, config);
    config.setAuthorizationUrl(AUTH_URL);
    config.setTokenUrl(TOKEN_URL);
    config.setUserInfoUrl(USER_INFO_URL);
    this.wecomApiClient = new WecomApiClient(this);
  }

  @Override
  protected String getDefaultScopes() {
    return WECOM_SCOPE_SNSAPI_USERINFO;
  }

  @Override
  public Object callback(RealmModel realm, AuthenticationCallback callback,
                         EventBuilder event) {
    return new WecomEndpoint(callback, realm, event, this);
  }

  private static class WecomApiClient {
    private final WecomIdentityProvider provider;
    private String wecomApiAccessToken = null;

    public WecomApiClient(WecomIdentityProvider provider) {
      this.provider = provider;
    }

    public JsonNode authRequest(UriBuilder uri, JsonNode body) {
      JsonNode response;
      int errcode;
      try {
        uri.queryParam(WECOM_PARAM_ACCESS_TOKEN, getWecomApiAccessToken(false));
        response =
            body == null
                ? SimpleHttp.doGet(uri.build().toString(), provider.session)
                      .asJson()
                : SimpleHttp.doPost(uri.build().toString(), provider.session)
                      .json(body)
                      .asJson();
        errcode = response.get(WECOM_FIELD_ERRCODE).asInt();
        if (errcode == 42001) { // Access token expired
          response =
              body == null
                  ? SimpleHttp.doGet(uri.build().toString(), provider.session)
                        .asJson()
                  : SimpleHttp.doPost(uri.build().toString(), provider.session)
                        .json(body)
                        .asJson();
          errcode = response.get(WECOM_FIELD_ERRCODE).asInt();
        }
        if (errcode != 0) {
          throw new IdentityBrokerException(
              response.get(WECOM_FIELD_ERRMSG).asText());
        }
      } catch (IOException err) {
        throw new IdentityBrokerException(err.getMessage());
      }
      return response;
    }

    private String getWecomApiAccessToken(boolean renew) {
      if (!renew && wecomApiAccessToken != null &&
          !wecomApiAccessToken.isEmpty()) {
        return wecomApiAccessToken;
      }
      logger.infof("Renew wecom api access token");
      JsonNode response;
      try {
        response =
            SimpleHttp.doGet(TOKEN_URL, provider.session)
                .param(WECOM_PARAM_CORPID, provider.getConfig().getCorpId())
                .param(WECOM_PARAM_CORPSECRET,
                       provider.getConfig().getClientSecret())
                .asJson();
      } catch (IOException err) {
        throw new IdentityBrokerException(
            "Failed to request wecom access token: " + err.getMessage());
      }
      int errcode = response.get(WECOM_FIELD_ERRCODE).asInt();
      if (errcode != 0) {
        throw new IdentityBrokerException(
            response.get(WECOM_FIELD_ERRMSG).asText());
      }
      JsonNode jsonAccessToken = response.get(WECOM_FIELD_ACCESS_TOKEN);
      String newAccessToken =
          jsonAccessToken != null ? jsonAccessToken.asText() : null;
      if (newAccessToken == null || newAccessToken.isEmpty()) {
        throw new IdentityBrokerException("No access_token from server.");
      }
      wecomApiAccessToken = newAccessToken;
      return wecomApiAccessToken;
    }
  }

  @Override
  protected UriBuilder createAuthorizationUrl(AuthenticationRequest request) {
    UriBuilder uriBuilder;
    String userAgent = request.getHttpRequest()
                           .getHttpHeaders()
                           .getHeaderString(HTTP_HEADER_USER_AGENT)
                           .toLowerCase();
    logger.infof("User authorization request user agent: %s", userAgent);
    if (userAgent.contains(WECOM_USER_AGENT_WXWORK)) {
      uriBuilder =
          UriBuilder.fromUri(NATIVE_AUTH_URL)
              .queryParam(OAUTH2_PARAMETER_SCOPE,
                          WECOM_SCOPE_SNSAPI_PRIVATEINFO)
              .queryParam(OAUTH2_PARAMETER_STATE,
                          request.getState().getEncoded())
              .queryParam(OAUTH2_PARAMETER_RESPONSE_TYPE,
                          OAUTH2_RESPONSE_TYPE_CODE)
              .queryParam(WECOM_PARAM_APPID, getConfig().getCorpId())
              .queryParam(WECOM_PARAM_AGENTID, getConfig().getClientId())
              .queryParam(OAUTH2_PARAMETER_REDIRECT_URI,
                          request.getRedirectUri())
              .fragment(WECOM_FRAGMENT_WECHAT_REDIRECT);
    } else {
      uriBuilder =
          UriBuilder.fromUri(getConfig().getAuthorizationUrl())
              .queryParam(WECOM_PARAM_LOGIN_TYPE, WECOM_LOGIN_TYPE_CORP_APP)
              .queryParam(WECOM_PARAM_APPID, getConfig().getCorpId())
              .queryParam(WECOM_PARAM_AGENTID, getConfig().getClientId())
              .queryParam(OAUTH2_PARAMETER_REDIRECT_URI,
                          request.getRedirectUri())
              .queryParam(OAUTH2_PARAMETER_STATE,
                          request.getState().getEncoded());
    }
    return uriBuilder;
  }

  private static class WecomEndpoint extends Endpoint {
    private final WecomIdentityProvider wecomProvider;

    public WecomEndpoint(AuthenticationCallback callback, RealmModel realm,
                         EventBuilder event, WecomIdentityProvider provider) {
      super(callback, realm, event, provider);
      this.wecomProvider = provider;
    }

    @Override
    public SimpleHttp generateTokenRequest(String authorizationCode) {
      return HackSimpleHttp.doPost(this.wecomProvider.session,
                                   authorizationCode);
    }

    private static class HackSimpleHttp extends SimpleHttp {
      private long maxConsumedResponseSize;
      private String responseBody;

      private HackSimpleHttp(HttpClient client, long maxConsumedResponseSize,
                             String responseBody) {
        super(null, HttpMethod.POST, client, maxConsumedResponseSize);
        this.maxConsumedResponseSize = maxConsumedResponseSize;
        this.responseBody = responseBody;
      }

      public static HackSimpleHttp doPost(KeycloakSession session,
                                          String responseBody) {
        HttpClientProvider provider =
            session.getProvider(HttpClientProvider.class);
        return new HackSimpleHttp(provider.getHttpClient(),
                                  provider.getMaxConsumedResponseSize(),
                                  responseBody);
      }

      @Override
      public Response asResponse() throws IOException {
        BasicHttpResponse response =
            new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, null);
        response.setEntity(new StringEntity(responseBody));
        return new Response(response, maxConsumedResponseSize);
      }
    }
  }

  @Override
  public BrokeredIdentityContext
  getFederatedIdentity(String authorizationCode) {
    JsonNode userInfo = this.wecomApiClient.authRequest(
        UriBuilder.fromUri(USER_INFO_URL)
            .queryParam(WECOM_PARAM_CODE, authorizationCode),
        null);
    JsonNode jsonUserId = userInfo.get(WECOM_FIELD_USERID);
    String userId = jsonUserId != null ? jsonUserId.asText() : null;
    JsonNode jsonUserTicket = userInfo.get(WECOM_FIELD_USER_TICKET);
    String userTicket = jsonUserTicket != null ? jsonUserTicket.asText() : null;
    JsonNode userEntity = null;
    if (userId != null && !userId.isEmpty()) {
      userEntity = this.wecomApiClient.authRequest(
          UriBuilder.fromUri(USER_ENTITY_URL)
              .queryParam(WECOM_PARAM_USERID, userId),
          null);
    }
    JsonNode userDetail = null;
    if (userTicket != null && !userTicket.isEmpty()) {
      userDetail = this.wecomApiClient.authRequest(
          UriBuilder.fromUri(USER_DETAIL_URL)
              .queryParam(WECOM_PARAM_CODE, authorizationCode),
          mapper.createObjectNode().put(WECOM_PARAM_USER_TICKET, userTicket));
    }
    BrokeredIdentityContext identity =
        new BrokeredIdentityContext(userId, getConfig());
    identity.setBrokerUserId(getConfig().getAlias() + "." + userId);
    JsonNode jsonDisplayName =
        userEntity != null ? userEntity.get(WECOM_FIELD_NAME) : null;
    identity.setFirstName(jsonDisplayName != null ? jsonDisplayName.asText()
                                                  : "");
    identity.setLastName("");
    JsonNode jsonEmail =
        userDetail != null ? userDetail.get(WECOM_FIELD_BIZ_MAIL) : null;
    identity.setEmail(jsonEmail != null ? jsonEmail.asText() : "");
    identity.setUsername(jsonEmail != null ? jsonEmail.asText() : "");
    return identity;
  }
}
