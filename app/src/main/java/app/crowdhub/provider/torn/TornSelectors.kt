package app.crowdhub.provider.torn

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object TornSelectors {
    // Verified against _samples/login-page.html (captured 2026-11-14)
    // Real DOM: <input id="player" name="email" type="email"> — name="email" is the correct attr
    val LOGIN_USERNAME_SELECTORS = listOf(
        "input[name='email']",    // ✅ confirmed: input#player has name="email"
        "input[name='player']",   // id fallback (id="player" but name="email")
        "input[name='username']",
        "input[type='email']",
        "input[type='text']"
    )

    // Verified against _samples/login-page.html (captured 2026-11-14)
    val LOGIN_PASSWORD_SELECTORS = listOf(
        "input[name='password']",
        "input[type='password']"
    )

    // Verified against _samples/login-page.html (captured 2026-11-14)
    // Real DOM: <input type="submit" name="btnLogin" class="...loginBtn___Mn_MB">
    // NOTE: button[type='submit'] will NOT match — it's an <input>, not <button>
    val LOGIN_SUBMIT_SELECTORS = listOf(
        "input[name='btnLogin']",  // ✅ confirmed: most specific
        "input[type='submit']",    // ✅ confirmed: works for <input type="submit">
        ".loginBtn___Mn_MB",       // ✅ confirmed: CSS module class on submit input
        "button[type='submit']",   // fallback for other sites
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

    // Verified against _samples/hospital-page-rows.html (captured 2026-11-14)
    // Real DOM: <ul class="user-info-list-wrap"> containing <li> items
    val HOSPITAL_ROW_SELECTORS = listOf(
        "ul.user-info-list-wrap > li",  // ✅ confirmed working (3 rows captured)
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
