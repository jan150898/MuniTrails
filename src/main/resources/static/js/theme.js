(function () {
    const storageKey = 'muni-trails-theme';
    const root = document.documentElement;
    const savedTheme = localStorage.getItem(storageKey);
    const preferredTheme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';

    function setTheme(theme) {
        root.dataset.theme = theme;
        localStorage.setItem(storageKey, theme);
        document.querySelectorAll('[data-theme-toggle]').forEach((button) => {
            const isDark = theme === 'dark';
            button.setAttribute('aria-pressed', String(isDark));
            button.setAttribute('aria-label', isDark ? 'Switch to light mode' : 'Switch to dark mode');
            button.querySelector('[data-theme-label]').textContent = isDark ? 'Light' : 'Dark';
            button.querySelector('[data-theme-icon]').textContent = isDark ? '☀' : '☾';
        });
    }

    setTheme(savedTheme || preferredTheme);
    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-theme-toggle]').forEach((button) => {
            button.addEventListener('click', () => setTheme(root.dataset.theme === 'dark' ? 'light' : 'dark'));
        });
        setTheme(root.dataset.theme);
    });
}());
