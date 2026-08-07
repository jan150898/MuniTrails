// Automatically attach the CSRF token (delivered via cookie) to every
// state-changing fetch() call so existing AJAX code keeps working now
// that CSRF protection is enabled server-side (see SecurityConfig.java).
(function () {
    function getCookie(name) {
        var match = document.cookie.match('(^|;)\\s*' + name + '\\s*=\\s*([^;]+)');
        return match ? decodeURIComponent(match.pop()) : '';
    }
    var originalFetch = window.fetch;
    window.fetch = function (input, init) {
        init = init || {};
        var method = (init.method || 'GET').toUpperCase();
        if (['POST', 'PUT', 'PATCH', 'DELETE'].indexOf(method) !== -1) {
            var token = getCookie('XSRF-TOKEN');
            if (token) {
                init.headers = init.headers || {};
                if (init.headers instanceof Headers) {
                    init.headers.set('X-XSRF-TOKEN', token);
                } else {
                    init.headers['X-XSRF-TOKEN'] = token;
                }
            }
        }
        return originalFetch(input, init);
    };
})();
