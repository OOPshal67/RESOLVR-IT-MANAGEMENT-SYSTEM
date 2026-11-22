(() => {
    const root = document.documentElement;
    const dark = matchMedia('(prefers-color-scheme: dark)');
    const renderTheme = theme => {
        root.dataset.bsTheme = theme;
        document.querySelectorAll('[data-theme]').forEach(button => {
            button.textContent = theme === 'dark' ? 'Light mode' : 'Dark mode';
        });
    };
    renderTheme(localStorage.getItem('resolvr-theme') || (dark.matches ? 'dark' : 'light'));
    document.querySelector('[data-theme]')?.addEventListener('click', () => {
        const theme = root.dataset.bsTheme === 'dark' ? 'light' : 'dark';
        localStorage.setItem('resolvr-theme', theme);
        renderTheme(theme);
    });
    document.querySelector('[data-password]')?.addEventListener('click', event => {
        const input = document.getElementById('password');
        input.type = input.type === 'password' ? 'text' : 'password';
        event.currentTarget.textContent = input.type === 'text' ? 'Hide' : 'Show';
    });
    const panel = document.querySelector('.signal');
    const clock = panel?.querySelector('[data-countdown]');
    if (clock && !matchMedia('(prefers-reduced-motion: reduce)').matches) {
        let remaining = 899;
        setInterval(() => {
            if (document.hidden || panel.classList.contains('paused')) return;
            remaining = remaining > 0 ? remaining - 1 : 899;
            clock.textContent = Math.floor(remaining / 60) + ':' + String(remaining % 60).padStart(2, '0');
            clock.parentNode.style.setProperty('--elapsed', remaining / 900 * 100 + '%');
        }, 1000);
    }
    panel?.querySelector('[data-motion]')?.addEventListener('click', event => {
        const paused = panel.classList.toggle('paused');
        event.currentTarget.setAttribute('aria-pressed', paused);
        event.currentTarget.textContent = paused ? 'Resume movement' : 'Pause movement';
    });

    const trigger = document.querySelector('[data-command]');
    if (trigger) {
        const dialog = document.createElement('dialog');
        dialog.className = 'command-box';
        dialog.innerHTML = '<h2>Find a page</h2><label>Search pages<input type="search" autofocus></label><nav></nav><p hidden>No matching pages.</p><button type="button">Close</button>';
        const nav = dialog.querySelector('nav');
        for (const link of document.querySelectorAll('.sidebar-nav-link')) {
            const item = document.createElement('a');
            item.href = link.href;
            item.textContent = link.textContent.trim();
            nav.append(item);
        }
        document.body.append(dialog);
        const search = dialog.querySelector('input');
        search.addEventListener('input', () => {
            for (const item of nav.children) item.hidden = !item.textContent.toLowerCase().includes(search.value.toLowerCase());
            dialog.querySelector('p').hidden = !!nav.querySelector('a:not([hidden])');
        });
        dialog.querySelector('button').onclick = () => dialog.close();
        trigger.onclick = () => { dialog.showModal(); search.focus(); };
        document.addEventListener('keydown', event => {
            if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
                event.preventDefault();
                if (dialog.open) dialog.close(); else trigger.click();
            }
        });
    }

    const queue = document.querySelector('[data-live-queue]');
    if (queue) {
        let pending = false;
        setInterval(async () => {
            if (document.hidden || pending || queue.contains(document.activeElement)) return;
            pending = true;
            try {
                const response = await fetch(location.href, {cache:'no-store'});
                if (!response.ok || response.redirected) return;
                const page = new DOMParser().parseFromString(await response.text(), 'text/html');
                const next = page.querySelector('[data-live-queue]');
                if (!next || queue.contains(document.activeElement) || queue.innerHTML === next.innerHTML) return;
                const oldRows = new Map(Array.from(queue.rows, row => [row.querySelector('a')?.href, row.textContent]));
                for (const row of next.rows) {
                    if (oldRows.get(row.querySelector('a')?.href) !== row.textContent) row.classList.add('queue-change');
                }
                queue.replaceChildren(...Array.from(next.children));
            } catch {
                // Keep the last queue visible while the connection recovers.
            } finally {
                pending = false;
            }
        }, 30000);
    }
})();
