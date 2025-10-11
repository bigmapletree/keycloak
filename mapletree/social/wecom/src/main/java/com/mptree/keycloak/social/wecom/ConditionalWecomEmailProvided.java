package com.mptree.keycloak.social.wecom;

import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.events.Errors;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;

public class ConditionalWecomEmailProvided
    extends AbstractIdpAuthenticator implements ConditionalAuthenticator {

  private static final Logger logger =
      Logger.getLogger(ConditionalWecomEmailProvided.class);

  static final ConditionalWecomEmailProvided SINGLETON =
      new ConditionalWecomEmailProvided();

  @Override
  protected void actionImpl(AuthenticationFlowContext context,
                            SerializedBrokeredIdentityContext serializedCtx,
                            BrokeredIdentityContext brokerContext) {
    return;
  }

  @Override
  protected void
  authenticateImpl(AuthenticationFlowContext context,
                   SerializedBrokeredIdentityContext serializedCtx,
                   BrokeredIdentityContext brokerContext) {
    return;
  }

  @Override
  public boolean matchCondition(AuthenticationFlowContext context) {
    AuthenticationSessionModel authSession = context.getAuthenticationSession();

    SerializedBrokeredIdentityContext serializedCtx =
        SerializedBrokeredIdentityContext.readFromAuthenticationSession(
            authSession, BROKERED_CONTEXT_NOTE);
    if (serializedCtx == null) {
      throw new AuthenticationFlowException(
          "Not found serialized context in clientSession",
          AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
    }
    BrokeredIdentityContext brokerContext =
        serializedCtx.deserialize(context.getSession(), authSession);

    if (!brokerContext.getIdpConfig().isEnabled()) {
      sendFailureChallenge(context, Response.Status.BAD_REQUEST,
                           Errors.IDENTITY_PROVIDER_ERROR,
                           Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR,
                           AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
    }

    logger.infof("Email for wecom user %s: %s", brokerContext.getId(),
                 brokerContext.getEmail());

    return brokerContext.getEmail() != null &&
        !brokerContext.getEmail().isBlank();
  }

  @Override
  public void action(AuthenticationFlowContext authenticationFlowContext) {
    return;
  }

  @Override
  public boolean requiresUser() {
    return false;
  }

  @Override
  public void setRequiredActions(KeycloakSession keycloakSession,
                                 RealmModel realmModel, UserModel userModel) {
    return;
  }

  @Override
  public void close() {
    return;
  }
}
