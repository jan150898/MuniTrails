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
