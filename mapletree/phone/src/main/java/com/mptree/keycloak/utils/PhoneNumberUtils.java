package com.mptree.keycloak.utils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import java.util.Locale;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;

public class PhoneNumberUtils {

    private static final Logger logger = Logger.getLogger(PhoneNumberUtils.class);

    public static String canonicalizePhoneNumber(KeycloakSession session,
            String inputPhoneNumber)
            throws NumberParseException {
        var phoneNumberUtil = PhoneNumberUtil.getInstance();
        Locale defaultLocale = Locale.forLanguageTag(
                session.getContext().getRealm().getDefaultLocale());
        String defaultRegion = defaultLocale.getCountry();
        if (defaultRegion == null || defaultRegion.isBlank()) {
            defaultRegion = "US";
        }
        logger.infof("Default region '%s' will be used", defaultRegion);
        PhoneNumber parsedNumber = phoneNumberUtil.parse(inputPhoneNumber.trim(), defaultRegion);
        if (!phoneNumberUtil.isValidNumber(parsedNumber)) {
            throw new NumberParseException(
                    NumberParseException.ErrorType.NOT_A_NUMBER,
                    "The phone number is not valid for region.");
        }
        String result = phoneNumberUtil.format(parsedNumber, PhoneNumberFormat.E164);
        return result;
    }
}
