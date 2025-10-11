package com.mptree.keycloak.social.wecom;

import java.util.List;
import org.keycloak.Config;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class ConditionalWecomEmailProvidedFactory
    implements ConditionalAuthenticatorFactory {

  public static final String PROVIDER_ID = "conditional-wecom-email-provided";

  private static final AuthenticationExecutionModel.Requirement[]
      REQUIREMENT_CHOICES = {AuthenticationExecutionModel.Requirement.REQUIRED,
                             AuthenticationExecutionModel.Requirement.DISABLED};

  @Override
  public ConditionalAuthenticator getSingleton() {
    return ConditionalWecomEmailProvided.SINGLETON;
  }

  @Override
  public String getDisplayType() {
    return "Condition - Wecom Email Provided";
  }

  @Override
  public boolean isConfigurable() {
    return true;
  }

  @Override
  public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
    return REQUIREMENT_CHOICES;
  }

  @Override
  public boolean isUserSetupAllowed() {
    return false;
  }

  @Override
  public String getHelpText() {
    return "Flow is executed only if the broker email provided";
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    return null;
  }

  @Override
  public void init(Config.Scope scope) {}

  @Override
  public void postInit(KeycloakSessionFactory keycloakSessionFactory) {}

  @Override
  public void close() {}

  @Override
  public String getId() {
    return PROVIDER_ID;
  }
}
