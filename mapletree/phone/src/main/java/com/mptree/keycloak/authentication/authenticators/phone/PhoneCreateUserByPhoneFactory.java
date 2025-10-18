package com.mptree.keycloak.authentication.authenticators.phone;

import java.util.List;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class PhoneCreateUserByPhoneFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "phone-create-user-by-phone";

    public static final PhoneCreateUserByPhone SINGLETON = new PhoneCreateUserByPhone();

    // Implements ProviderFactory

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    // Implements ConfigurableAuthenticatorFactory

    @Override
    public String getDisplayType() {
        return "Phone - Create User By Phone";
    }

    @Override
    public String getReferenceCategory() {
        return "create-user";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED };

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    // Implements ConfiguredProvider

    @Override
    public String getHelpText() {
        return "Create new keycloak user if there is no existing Keycloak account with verified login phone number";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return null;
    }
}
