// Automatically attach the CSRF token to every state-changing fetch() call so
// existing AJAX code (GPX upload, Garmin import, comments, ...) keeps working
// now that CSRF protection is enabled server-side (see SecurityConfig.java).
//
// The token is read from an explicit <input id="csrf-token"> rendered by the
// shared navbar fragment (navbars.html) so it is always available, falling
// back to the XSRF-TOKEN cookie that Spring Security delivers.
(function () {
    var TOKEN_HEADER = 'X-XSRF-TOKEN';
    var TOKEN_COOKIE = 'XSRF-TOKEN';

    function getCookie(name) {
        var match = document.cookie.match('(^|;)\\s*' + name + '\\s*=\\s*([^;]+)');
        return match ? decodeURIComponent(match.pop()) : '';
    }

    function getToken() {
        var el = document.getElementById('csrf-token');
        if (el && el.value) return el.value;
        return getCookie(TOKEN_COOKIE);
    }

    function attachHeader(init, token) {
        init.headers = init.headers || {};
        if (init.headers instanceof Headers) {
            init.headers.set(TOKEN_HEADER, token);
        } else {
            init.headers[TOKEN_HEADER] = token;
        }
    }

    var originalFetch = window.fetch;
    window.fetch = function (input, init) {
        init = init || {};
        // Resolve the method from fetch options, or from a Request input.
        var method = (init.method || (input && input.method) || 'GET').toUpperCase();
        if (['POST', 'PUT', 'PATCH', 'DELETE'].indexOf(method) !== -1) {
            var token = getToken();
            if (token) attachHeader(init, token);
        }
        return originalFetch(input, init);
    };
})();

// ----------------------------------------------------------------------------
// Native <form method="post"> protection
// ----------------------------------------------------------------------------
// The fetch() wrapper above only covers AJAX. Spring Security's
// XorCsrfTokenRequestAttributeHandler resolves the CSRF token exclusively from
// the X-XSRF-TOKEN header or the _csrf parameter — it NEVER reads the cookie
// for a traditional form POST, so every <form method="post"> must submit an
// explicit hidden <input name="_csrf"> field. Thymeleaf already renders one
// server-side via th:name="${_csrf.parameterName}" (its value is the masked
// token); this back-fills any _csrf field that was left empty, reading the raw
// token from the XSRF-TOKEN cookie that CookieCsrfTokenRepository sets.
(function () {
    var COOKIE_NAME = 'XSRF-TOKEN';
    var parts = document.cookie ? document.cookie.split(';') : [];
    var raw = '';
    for (var i = 0; i < parts.length; i++) {
        var kv = parts[i].trim();
        if (kv.indexOf(COOKIE_NAME + '=') === 0) {
            raw = kv.substring(COOKIE_NAME.length + 1);
            try { raw = decodeURIComponent(raw); } catch (e) { /* keep raw */ }
            break;
        }
    }
    if (!raw) return;
    var inputs = document.querySelectorAll('input[type="hidden"][name="_csrf"]');
    for (var j = 0; j < inputs.length; j++) {
        if (!inputs[j].value) {
            inputs[j].value = raw;
        }
    }
})();