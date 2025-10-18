<#import "buttons.ftl" as buttons>
<#import "field.ftl" as field>
<#import "social-providers.ftl" as identityProviders>
<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('phoneNumber','verificationCode') displayInfo=true; section>
  <#if section = "header">
    ${msg("loginAccountTitle")}
  <#elseif section = "form">
    <div id="kc-form">
      <div id="kc-form-wrapper">
        <form id="kc-form-login" class="${properties.kcFormClass!}" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post" novalidate="novalidate">
          <@field.input name="phoneNumber" label=msg("phoneNumber") autofocus=true autocomplete="tel" value=phoneNumber />
          <#assign error=kcSanitize(messagesPerField.get("verificationCode"))?no_esc>
          <@field.group name="verificationCode" label=msg("phoneVerificationCode") error=error>
            <div class="${properties.kcInputGroup}">
              <div class="${properties.kcInputGroupItemClass} ${properties.kcFill}">
                <span class="${properties.kcInputClass} <#if error?has_content>${properties.kcError}</#if>">
                  <input id="verificationCode" name="verificationCode" value="" type="text" autocomplete="one-time-code" aria-invalid="<#if error?has_content>true</#if>"/>
                  <@field.errorIcon error=error/>
                </span>
              </div>
              <div class="${properties.kcInputGroupItemClass}">
                <button class="${properties.kcFormPasswordVisibilityButtonClass}" style="min-width: 5rem;" type="button" 
                    aria-label="${msg('doSendPhoneVerificationCode')}" aria-controls="verificationCode" 
                    onclick="sendPhoneVerificationCode(event)">
                  ${msg("doSendPhoneVerificationCode")}
                </button>
              </div>
            </div>
          </@field.group>
          <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
          <@buttons.loginButton />
        </form>
      </div>
    </div>
    <script type="text/javascript">
        function countExpires(target, seconds) {
            if (seconds <= 0) {
                target.innerText = "${msg('doSendPhoneVerificationCode')}";
                target.disabled = false;
            } else {
                target.innerText = Math.floor(seconds).toString().padStart(2, "0");
                target.disabled = true;
                setTimeout(function () {
                  countExpires(target, seconds - 1);
                }, 1000);
            }
        }
        function sendPhoneVerificationCode(ev) {
            const phoneNumber = document.getElementById("phoneNumber").value.trim();
            if (!phoneNumber) {
                this.errorMessage = "${msg('phoneNumberMissingMessage')}";
                document.getElementById("phoneNumber").focus();
                return;
            }
            const params = new URLSearchParams();
            params.append("phoneNumber", phoneNumber);
            fetch(window.location.origin + "/realms/${realm.name}/sms/authentication-code?"+ params)
                .then(res => res.json())
                .then(res => countExpires(ev.target, res.expires_in))
                .catch(err => {
                    console.error(err)
                });
        }
    </script>
  <#elseif section = "info" >
      <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
          <div id="kc-registration-container">
              <div id="kc-registration">
                  <span>${msg("noAccount")} <a tabindex="0" href="${url.registrationUrl}">${msg("doRegister")}</a></span>
              </div>
          </div>
      </#if>
  <#elseif section = "socialProviders" >
      <#if realm.password && social.providers?? && social.providers?has_content>
          <@identityProviders.show social=social/>
      </#if>
  </#if>
</@layout.registrationLayout>
