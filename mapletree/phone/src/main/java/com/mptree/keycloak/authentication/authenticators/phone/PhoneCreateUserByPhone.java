package com.mptree.keycloak.authentication.authenticators.phone;

import com.google.i18n.phonenumbers.NumberParseException;
import com.mptree.keycloak.utils.PhoneNumberUtils;
import java.util.List;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.validation.Validation;

public class PhoneCreateUserByPhone implements Authenticator {

    private static Logger logger = Logger.getLogger(PhoneCreateUserByPhone.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        if (user != null) {
            context.success();
            return;
        }
        String loginPhoneNumber = context.getAuthenticationSession().getAuthNote(
                PhoneLoginByPhoneForm.VERIFIED_LOGIN_PHONE_NUMBER);
        if (Validation.isBlank(loginPhoneNumber)) {
            logger.warn("No verified phone number found in auth note");
            context.attempted();
            return;
        }
        String canonicalPhoneNumber = "";
        try {
            canonicalPhoneNumber = PhoneNumberUtils.canonicalizePhoneNumber(
                    context.getSession(), loginPhoneNumber);
        } catch (NumberParseException e) {
            logger.warn("Invalid phone number for canonicalization: " + loginPhoneNumber);
            context.attempted();
            return;
        }
        List<UserModel> users = context.getSession()
                .users()
                .searchForUserByUserAttributeStream(
                        context.getRealm(), "phoneNumber", canonicalPhoneNumber)
                .toList();
        if (users.size() != 0) {
            // User with given phone number already exists
            logger.warn("User with phone number already exists: " +
                    canonicalPhoneNumber);
            context.attempted();
            return;
        }
        logger.info("Creating new user with phone number: " + canonicalPhoneNumber);
        UserModel phoneUser = context.getSession().users().addUser(
                context.getRealm(), canonicalPhoneNumber);
        phoneUser.setEnabled(true);
        phoneUser.setUsername(phoneUser.getId());
        phoneUser.setSingleAttribute("phoneNumber", canonicalPhoneNumber);
        phoneUser.setSingleAttribute("phoneNumberVerified", "true");
        context.setUser(phoneUser);
        context.success();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        return;
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm,
            UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm,
            UserModel user) {
    }

    @Override
    public void close() {
    }
}
