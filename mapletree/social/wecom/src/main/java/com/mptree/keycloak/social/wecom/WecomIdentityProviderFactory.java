package com.mptree.keycloak.social.wecom;

import java.util.List;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

public class WecomIdentityProviderFactory
    extends AbstractIdentityProviderFactory<WecomIdentityProvider>
    implements SocialIdentityProviderFactory<WecomIdentityProvider> {

  public static final String PROVIDER_ID = "wecom";

  @Override
  public String getName() {
    return "Wecom";
  }

  @Override
  public WecomIdentityProvider create(KeycloakSession session,
                                      IdentityProviderModel model) {
    return new WecomIdentityProvider(session,
                                     new WecomIdentityProviderConfig(model));
  }

  @Override
  public WecomIdentityProviderConfig createConfig() {
    return new WecomIdentityProviderConfig();
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    return ProviderConfigurationBuilder.create()
        .property()
        .name("corpId")
        .label("Corp ID")
        .helpText(
            "Set Corp ID to Wecom Corp ID. Set Client ID to Wecom app Agent ID. Set Client Secret to Wecom app Secret.")
        .type(ProviderConfigProperty.STRING_TYPE)
        .add()
        .build();
  }
}
