package app.crowdhub.provider.torn

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object TornSelectors {
    // VERIFY against real _samples/login-page.html when provided
    val LOGIN_USERNAME_SELECTORS = listOf(
        "input[name='player']",
        "input[name='username']",
        "input[name='email']",
        "input[type='text']",
        "input[type='email']"
    )

    // VERIFY against real _samples/login-page.html when provided
    val LOGIN_PASSWORD_SELECTORS = listOf(
        "input[name='password']",
        "input[type='password']"
    )

    // VERIFY against real _samples/login-page.html when provided
    val LOGIN_SUBMIT_SELECTORS = listOf(
        "button[type='submit']",
        "input[type='submit']",
        ".login-btn"
    )

    // VERIFY: indicators that require immediate manual user takeover
    val CAPTCHA_OR_2FA_INDICATORS = listOf(
        "iframe[src*='recaptcha']",
        "iframe[src*='turnstile']",
        "iframe[src*='hcaptcha']",
        ".g-recaptcha",
        "input[name='code']",
        "input[name='otp']",
        "input[name='totp']"
    )

    // VERIFY against real _samples/hospital-page-rows.html when provided
    val HOSPITAL_ROW_SELECTORS = listOf(
        "ul.user-info-list-wrap > li",
        ".users-list > li",
        "table.hospital-list tr"
    )

    fun buildSafeLoginFillJs(
        username: String,
        password: String,
        autoSubmit: Boolean
    ): String {
        val userJson = Json.encodeToString(username)
        val passJson = Json.encodeToString(password)
        val userSels = Json.encodeToString(LOGIN_USERNAME_SELECTORS)
        val passSels = Json.encodeToString(LOGIN_PASSWORD_SELECTORS)
        val subSels = Json.encodeToString(LOGIN_SUBMIT_SELECTORS)
        val blockSels = Json.encodeToString(CAPTCHA_OR_2FA_INDICATORS)

        return """
            (function() {
                var u = $userJson;
                var p = $passJson;
                try {
                    var blockers = $blockSels;
                    for (var i = 0; i < blockers.length; i++) {
                        if (document.querySelector(blockers[i])) {
                            return 'BLOCKED_CHALLENGE';
                        }
                    }
                    function findFirst(sels) {
                        for (var j = 0; j < sels.length; j++) {
                            var el = document.querySelector(sels[j]);
                            if (el) return el;
                        }
                        return null;
                    }
                    var uEl = findFirst($userSels);
                    var pEl = findFirst($passSels);
                    if (!uEl || !pEl) return 'NO_FORM';
                    var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
                    setter.call(uEl, u);
                    uEl.dispatchEvent(new Event('input', { bubbles: true }));
                    setter.call(pEl, p);
                    pEl.dispatchEvent(new Event('input', { bubbles: true }));
                    if ($autoSubmit) {
                        var btn = findFirst($subSels);
                        if (btn) btn.click();
                    }
                    return 'FILLED';
                } finally {
                    u = '';
                    p = '';
                }
            })();
        """.trimIndent()
    }

    fun buildHospitalCaptureJs(): String {
        val rowSels = Json.encodeToString(HOSPITAL_ROW_SELECTORS)
        return """
            (function() {
                var sels = $rowSels;
                var rows = [];
                for (var i = 0; i < sels.length; i++) {
                    var found = document.querySelectorAll(sels[i]);
                    if (found && found.length > 0) {
                        rows = Array.prototype.slice.call(found);
                        break;
                    }
                }
                return rows.map(function(row) {
                    return row.outerHTML;
                });
            })();
        """.trimIndent()
    }
}
