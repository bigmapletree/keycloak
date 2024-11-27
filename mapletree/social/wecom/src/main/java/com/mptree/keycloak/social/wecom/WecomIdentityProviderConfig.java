package com.mptree.keycloak.social.wecom;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

public class WecomIdentityProviderConfig extends OAuth2IdentityProviderConfig {
  public WecomIdentityProviderConfig(IdentityProviderModel model) {
    super(model);
  }
  public WecomIdentityProviderConfig() {}

  public String getCorpId() {
    String corpId = getConfig().get("corpId");
    return corpId == null || corpId.isEmpty() ? null : corpId;
  }

  public void setCorpId(String corpId) { getConfig().put("corpId", corpId); }
}
