// External event listeners (no inline on* handlers, so the page runs under a CSP without 'unsafe-inline' scripts)
(function () {
    // EXIT link in the navbar: submit the logout form
    document.addEventListener('click', function (event) {
        var link = event.target.closest ? event.target.closest('#logout-link') : null;
        if (link) {
            var form = document.getElementById('logout');
            if (form) {
                form.submit();
            }
        }
    });

    // Search filters (brand, gender, price): submit the surrounding form on change
    document.addEventListener('change', function (event) {
        var target = event.target;
        if (target && target.getAttribute && target.getAttribute('data-auto-submit') === 'true' && target.form) {
            target.form.submit();
        }
    });
})();
