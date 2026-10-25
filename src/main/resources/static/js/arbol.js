/* =========================================================
   Dibujo del árbol sintáctico (árbol de derivación) en SVG
   Datos: window.ARBOL  →  { t: tipo, e: etiqueta, x: lexema, l: línea, h: [hijos] }
   ========================================================= */
(function () {
    const datos = window.ARBOL;
    const svg = document.getElementById('arbol-svg');
    if (!datos || !svg) return;

    const NS = 'http://www.w3.org/2000/svg';
    const MONO = "Consolas, 'Courier New', monospace";
    const ALTO_NIVEL = 86;      // distancia vertical entre niveles
    const SEP = 12;             // separación horizontal mínima entre subárboles
    const ALTO_REGLA = 30, ALTO_TOKEN = 42;

    // Colores por tipo de nodo (en atributos, para que también salgan en el PNG)
    const ESTILO = {
        regla:  { fondo: '#e0e7ff', borde: '#1e3a8a', texto: '#1e3a8a' },
        token:  { fondo: '#dcfce7', borde: '#15803d', texto: '#14532d' },
        lexico: { fondo: '#fef3c7', borde: '#b45309', texto: '#78350f' },
        error:  { fondo: '#fee2e2', borde: '#b91c1c', texto: '#7f1d1d' },
        vacio:  { fondo: '#f1f5f9', borde: '#94a3b8', texto: '#475569' }
    };

    // ---------- 1. Medidas de cada nodo ----------
    const anchoTexto = (s, px) => s.length * px * 0.62;

    function medir(n, nivel) {
        n.nivel = nivel;
        if (n.t === 'regla' || n.t === 'vacio') {
            n.w = Math.max(44, anchoTexto(n.e, 13) + 22);
            n.hgt = ALTO_REGLA;
        } else {
            n.w = Math.max(54, anchoTexto(n.e, 10.5) + 18, anchoTexto(n.x || '', 13) + 18);
            n.hgt = (n.x ? ALTO_TOKEN : ALTO_REGLA);
        }
        n.h.forEach(h => medir(h, nivel + 1));
        // Ancho del subárbol: lo que ocupan los hijos o el propio nodo, lo que sea mayor
        const anchoHijos = n.h.reduce((s, h) => s + h.anchoSub, 0) + SEP * Math.max(0, n.h.length - 1);
        n.anchoSub = Math.max(n.w, anchoHijos);
    }

    // ---------- 2. Posiciones (cada padre centrado sobre sus hijos) ----------
    function ubicar(n, izquierda) {
        n.y = n.nivel * ALTO_NIVEL + 20;
        if (n.h.length === 0) {
            n.cx = izquierda + n.anchoSub / 2;
            return;
        }
        const anchoHijos = n.h.reduce((s, h) => s + h.anchoSub, 0) + SEP * (n.h.length - 1);
        let x = izquierda + (n.anchoSub - anchoHijos) / 2;
        n.h.forEach(h => { ubicar(h, x); x += h.anchoSub + SEP; });
        n.cx = (n.h[0].cx + n.h[n.h.length - 1].cx) / 2;
    }

    medir(datos, 0);
    ubicar(datos, 20);

    let maxNivel = 0;
    (function rec(n) { maxNivel = Math.max(maxNivel, n.nivel); n.h.forEach(rec); })(datos);
    const ANCHO = datos.anchoSub + 40;
    const ALTO = maxNivel * ALTO_NIVEL + ALTO_TOKEN + 40;
    svg.setAttribute('viewBox', `0 0 ${ANCHO} ${ALTO}`);
    svg.setAttribute('xmlns', NS);

    // ---------- 3. Dibujo ----------
    function el(tag, attrs, padre) {
        const e = document.createElementNS(NS, tag);
        for (const k in attrs) e.setAttribute(k, attrs[k]);
        padre.appendChild(e);
        return e;
    }

    el('rect', { x: 0, y: 0, width: ANCHO, height: ALTO, fill: '#ffffff' }, svg);   // fondo (para el PNG)
    const capaLineas = el('g', {}, svg);
    const capaNodos = el('g', {}, svg);

    function dibujar(n) {
        // Líneas hacia los hijos
        n.h.forEach(h => {
            const x1 = n.cx, y1 = n.y + n.hgt, x2 = h.cx, y2 = h.y;
            const ym = (y1 + y2) / 2;
            el('path', {
                d: `M ${x1} ${y1} C ${x1} ${ym}, ${x2} ${ym}, ${x2} ${y2}`,
                fill: 'none', stroke: '#94a3b8', 'stroke-width': 1.4
            }, capaLineas);
            dibujar(h);
        });

        const st = ESTILO[n.t] || ESTILO.regla;
        const g = el('g', {}, capaNodos);
        const titulo = el('title', {}, g);
        titulo.textContent = (n.t === 'regla' ? 'Regla: ' : n.t === 'token' ? 'Token: ' : '') + n.e
            + (n.x ? '  →  "' + n.x + '"' : '') + '  (línea ' + n.l + ')';

        const esHoja = n.t !== 'regla';
        el('rect', {
            x: n.cx - n.w / 2, y: n.y, width: n.w, height: n.hgt,
            rx: esHoja ? 6 : 15, fill: st.fondo, stroke: st.borde, 'stroke-width': 1.5,
            'stroke-dasharray': n.t === 'vacio' || n.t === 'error' ? '4 3' : 'none'
        }, g);

        if (n.x) {
            // Token: tipo arriba (pequeño) y lexema abajo (resaltado)
            el('text', {
                x: n.cx, y: n.y + 15, 'text-anchor': 'middle', 'font-family': MONO,
                'font-size': 10.5, fill: st.texto
            }, g).textContent = n.e;
            el('text', {
                x: n.cx, y: n.y + 33, 'text-anchor': 'middle', 'font-family': MONO,
                'font-size': 13, 'font-weight': 700, fill: st.texto
            }, g).textContent = n.x;
        } else {
            el('text', {
                x: n.cx, y: n.y + n.hgt / 2 + 4.5, 'text-anchor': 'middle',
                'font-family': n.t === 'regla' ? "'Segoe UI', Arial, sans-serif" : MONO,
                'font-size': 13, 'font-weight': n.t === 'regla' ? 600 : 700, fill: st.texto
            }, g).textContent = n.e;
        }
    }
    dibujar(datos);

    // ---------- 4. Zoom ----------
    let zoom = 1;
    const lienzo = document.getElementById('arbol-lienzo');
    function aplicarZoom() {
        svg.style.width = (ANCHO * zoom) + 'px';
        svg.style.height = (ALTO * zoom) + 'px';
        const z = document.getElementById('arbol-zoom');
        if (z) z.textContent = Math.round(zoom * 100) + '%';
    }
    function ajustar() {
        zoom = Math.min(1, (lienzo.clientWidth - 20) / ANCHO);
        aplicarZoom();
    }
    const btn = id => document.getElementById(id);
    btn('arbol-mas') && (btn('arbol-mas').onclick = () => { zoom = Math.min(3, zoom * 1.25); aplicarZoom(); });
    btn('arbol-menos') && (btn('arbol-menos').onclick = () => { zoom = Math.max(0.15, zoom / 1.25); aplicarZoom(); });
    btn('arbol-ajustar') && (btn('arbol-ajustar').onclick = ajustar);
    btn('arbol-real') && (btn('arbol-real').onclick = () => { zoom = 1; aplicarZoom(); });

    // ---------- 5. Descargar como imagen PNG (para el documento) ----------
    btn('arbol-png') && (btn('arbol-png').onclick = () => {
        const copia = svg.cloneNode(true);
        copia.setAttribute('width', ANCHO);
        copia.setAttribute('height', ALTO);
        copia.removeAttribute('style');
        const xml = new XMLSerializer().serializeToString(copia);
        const img = new Image();
        img.onload = () => {
            const escala = 2;
            const canvas = document.createElement('canvas');
            canvas.width = ANCHO * escala;
            canvas.height = ALTO * escala;
            const ctx = canvas.getContext('2d');
            ctx.scale(escala, escala);
            ctx.drawImage(img, 0, 0);
            const a = document.createElement('a');
            a.download = (window.ARBOL_NOMBRE || 'arbol') + '_arbol_sintactico.png';
            a.href = canvas.toDataURL('image/png');
            a.click();
        };
        img.src = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(xml);
    });

    // Al abrir: ajustado al ancho, pero sin bajar de 60% para que se pueda leer (se desplaza con la barra)
    zoom = Math.max(0.6, Math.min(1, (lienzo.clientWidth - 20) / ANCHO));
    aplicarZoom();
    // Centrar la vista en la raíz (programa)
    lienzo.scrollLeft = Math.max(0, datos.cx * zoom - lienzo.clientWidth / 2);
})();
