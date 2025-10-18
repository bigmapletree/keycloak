package com.mptree.keycloak.authentication.authenticators.phone;

import static org.keycloak.authentication.authenticators.util.AuthenticatorUtils.getDisabledByBruteForceEventError;

import cc.coopersoft.keycloak.phone.providers.constants.TokenCodeType;
import cc.coopersoft.keycloak.phone.providers.representations.TokenCodeRepresentation;
import cc.coopersoft.keycloak.phone.providers.spi.PhoneVerificationCodeProvider;
import com.google.i18n.phonenumbers.NumberParseException;
import com.mptree.keycloak.utils.PhoneNumberUtils;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AbstractFormAuthenticator;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.events.Errors;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;

public class PhoneLoginByPhoneForm extends AbstractFormAuthenticator {

    private static final Logger logger = Logger.getLogger(PhoneLoginByPhoneForm.class);

    public static final String VERIFIED_LOGIN_PHONE_NUMBER = "VERIFIED_LOGIN_PHONE_NUMBER";

    public static final String LOGIN_PHONE_FTL = "login-phone.ftl";

    public static final String FORM_PHONE_NUMBER = "phoneNumber";

    public static final String FORM_VERIFICATION_CODE = "verificationCode";

    public static final String MESSAGE_PHONE_VERIFICATION_CODE_NOT_MATCH = "phoneVerificationCodeNotMatchMessage";

    public static final String MESSAGE_PHONE_NUMBER_MISSING = "phoneNumberMissingMessage";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        Response challengeResponse = context.form().createForm(LOGIN_PHONE_FTL);
        context.challenge(challengeResponse);
    }

    private boolean validateVerificationCode(AuthenticationFlowContext context,
            String canonicalPhoneNumber,
            String verificationCode) {
        try {
            TokenCodeRepresentation tokenCode = context.getSession()
                    .getProvider(PhoneVerificationCodeProvider.class)
                    .ongoingProcess(canonicalPhoneNumber, TokenCodeType.AUTH);
            if (Validation.isBlank(verificationCode) || tokenCode == null ||
                    !tokenCode.getCode().equals(verificationCode)) {
                context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
                Response challengeResponse = context.form()
                        .addError(
                                new FormMessage(FORM_VERIFICATION_CODE,
                                        MESSAGE_PHONE_VERIFICATION_CODE_NOT_MATCH))
                        .createForm(LOGIN_PHONE_FTL);
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                        challengeResponse);
                return false;
            }
        } catch (Exception e) {
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            Response challengeResponse = context.form()
                    .addError(
                            new FormMessage(FORM_VERIFICATION_CODE,
                                    MESSAGE_PHONE_VERIFICATION_CODE_NOT_MATCH))
                    .createForm(LOGIN_PHONE_FTL);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                    challengeResponse);
            return false;
        }
        logger.debug("verification code success!");
        context.getAuthenticationSession().setAuthNote(VERIFIED_LOGIN_PHONE_NUMBER,
                canonicalPhoneNumber);
        return true;
    }

    private boolean isUserDisabledByBruteForce(AuthenticationFlowContext context,
            UserModel user,
            String canonicalPhoneNumber) {
        String bruteForceError = getDisabledByBruteForceEventError(context, user);
        if (bruteForceError != null) {
            context.getEvent().user(user);
            context.getEvent().error(bruteForceError);
            Response challengeResponse = context.form()
                    .addError(
                            new FormMessage(FORM_PHONE_NUMBER, Messages.INVALID_USER))
                    .createForm(LOGIN_PHONE_FTL);
            context.forceChallenge(challengeResponse);
            return true;
        }
        return false;
    }

    private boolean isUserEnabled(AuthenticationFlowContext context,
            UserModel user, String canonicalPhoneNumber) {
        if (isUserDisabledByBruteForce(context, user, canonicalPhoneNumber)) {
            return false;
        }
        if (!user.isEnabled()) {
            context.getEvent().user(user);
            context.getEvent().error(Errors.USER_DISABLED);
            Response challengeResponse = context.form()
                    .setError(Messages.ACCOUNT_DISABLED)
                    .createForm(LOGIN_PHONE_FTL);
            context.forceChallenge(challengeResponse);
            return false;
        }
        return true;
    }

    private boolean validateUser(AuthenticationFlowContext context,
            UserModel user, String canonicalPhoneNumber) {
        if (!isUserEnabled(context, user, canonicalPhoneNumber)) {
            return false;
        }
        context.setUser(user);
        return true;
    }

    private boolean validatePhone(AuthenticationFlowContext context,
            String inputPhoneNumber,
            String verificationCode) {
        context.clearUser();
        String canonicalPhoneNumber = "";
        try {
            canonicalPhoneNumber = PhoneNumberUtils.canonicalizePhoneNumber(
                    context.getSession(), inputPhoneNumber);
        } catch (NumberParseException e) {
            logger.warn("canonical error: " + e.toString());
            context.getEvent().error(Errors.USERNAME_MISSING);
            Response challengeResponse = context.form()
                    .addError(new FormMessage(FORM_PHONE_NUMBER, e.getMessage()))
                    .createForm(LOGIN_PHONE_FTL);
            context.failureChallenge(AuthenticationFlowError.INVALID_USER,
                    challengeResponse);
            return false;
        }
        if (!validateVerificationCode(context, canonicalPhoneNumber,
                verificationCode)) {
            return false;
        }
        List<UserModel> users = context.getSession()
                .users()
                .searchForUserByUserAttributeStream(
                        context.getRealm(), "phoneNumber", canonicalPhoneNumber)
                .toList();
        if (users.size() > 1) {
            // Found multiple users with the same phone number
            context.getEvent().error(Errors.USERNAME_IN_USE);
            Response challengeResponse = context.form()
                    .addError(
                            new FormMessage(FORM_PHONE_NUMBER, Messages.USERNAME_EXISTS))
                    .createForm(LOGIN_PHONE_FTL);
            context.failureChallenge(AuthenticationFlowError.INVALID_USER,
                    challengeResponse);
            return false;
        } else if (users.size() == 1) {
            // Found only one user
            return validateUser(context, users.get(0), canonicalPhoneNumber);
        } else {
            // No user found
            return true;
        }
    }

    protected boolean validateForm(AuthenticationFlowContext context,
            MultivaluedMap<String, String> inputData) {
        String inputPhoneNumber = inputData.getFirst(FORM_PHONE_NUMBER);
        context.form().setAttribute(FORM_PHONE_NUMBER, inputPhoneNumber);
        if (Validation.isBlank(inputPhoneNumber)) {
            context.getEvent().error(Errors.USERNAME_MISSING);
            Response challengeResponse = context.form()
                    .addError(new FormMessage(FORM_PHONE_NUMBER,
                            MESSAGE_PHONE_NUMBER_MISSING))
                    .createForm(LOGIN_PHONE_FTL);
            context.forceChallenge(challengeResponse);
            return false;
        }
        String inputVerificationCode = inputData.getFirst(FORM_VERIFICATION_CODE);
        if (Validation.isBlank(inputVerificationCode)) {
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            Response challengeResponse = context.form()
                    .addError(
                            new FormMessage(FORM_VERIFICATION_CODE,
                                    MESSAGE_PHONE_VERIFICATION_CODE_NOT_MATCH))
                    .createForm(LOGIN_PHONE_FTL);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                    challengeResponse);
            return false;
        }
        return validatePhone(context, inputPhoneNumber,
                inputVerificationCode.trim());
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData.containsKey("cancel")) {
            context.cancelLogin();
            return;
        }
        if (!validateForm(context, formData)) {
            return;
        }
        context.success();
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
}
