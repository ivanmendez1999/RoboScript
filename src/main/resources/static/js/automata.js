/* =========================================================
   Dibujo de autómatas finitos en SVG y animación de la simulación
   Datos: window.AUTOMATA (estados y transiciones) y window.SIMULACION (pasos)
   ========================================================= */
(function () {
    const R = 24;                      // radio de cada estado
    const NS = 'http://www.w3.org/2000/svg';
    const svg = document.getElementById('diagrama');
    if (!svg || !window.AUTOMATA) return;

    const A = window.AUTOMATA;
    const porNombre = {};
    A.estados.forEach(e => porNombre[e.nombre] = e);

    function el(tag, attrs, padre) {
        const n = document.createElementNS(NS, tag);
        for (const k in attrs) n.setAttribute(k, attrs[k]);
        (padre || svg).appendChild(n);
        return n;
    }

    // ---------- Tamaño del dibujo ----------
    const xs = A.estados.map(e => e.x), ys = A.estados.map(e => e.y);
    const minX = Math.min(...xs) - R - 75, maxX = Math.max(...xs) + R + 30;
    const minY = Math.min(...ys) - R - 70, maxY = Math.max(...ys) + R + 40;
    svg.setAttribute('viewBox', `${minX} ${minY} ${maxX - minX} ${maxY - minY}`);


    // Punta de flecha
    const defs = el('defs', {});
    ['normal', 'activa'].forEach(tipo => {
        const m = el('marker', {
            id: 'flecha-' + tipo, viewBox: '0 0 10 10', refX: '9', refY: '5',
            markerUnits: 'userSpaceOnUse', markerWidth: '11', markerHeight: '11', orient: 'auto-start-reverse'
        }, defs);
        el('path', { d: 'M0,0 L10,5 L0,10 z', class: 'punta-' + tipo }, m);
    });

    const capaAristas = el('g', {});
    const capaEtiquetas = el('g', {});
    const capaEstados = el('g', {});

    // ---------- Geometría ----------
    const dist = (p, q) => Math.hypot(p.x - q.x, p.y - q.y);
    const bez = (a, c, b, t) => ({
        x: (1 - t) * (1 - t) * a.x + 2 * (1 - t) * t * c.x + t * t * b.x,
        y: (1 - t) * (1 - t) * a.y + 2 * (1 - t) * t * c.y + t * t * b.y
    });

    /** ¿La curva a→b (con control c) pasa demasiado cerca de otro estado? */
    function choca(a, c, b, origen, destino) {
        for (let t = 0.05; t <= 0.95; t += 0.05) {
            const p = bez(a, c, b, t);
            for (const e of A.estados) {
                if (e.nombre === origen || e.nombre === destino) continue;
                if (dist(p, e) < R + 14) return true;
            }
        }
        return false;
    }

    const existe = (o, d) => A.transiciones.some(t => t.origen === o && t.destino === d);

    // ---------- Aristas ----------
    A.transiciones.forEach(t => {
        const a = porNombre[t.origen], b = porNombre[t.destino];
        const id = 'ar-' + t.origen + '-' + t.destino;
        let etiquetaPos;

        if (a === b) {
            // Lazo sobre el estado
            const x = a.x, y = a.y;
            const d = `M ${x - 12} ${y - R + 3} C ${x - 40} ${y - 85}, ${x + 40} ${y - 85}, ${x + 12} ${y - R + 3}`;
            el('path', { d, id, class: 'arista', 'marker-end': 'url(#flecha-normal)' }, capaAristas);
            etiquetaPos = { x, y: y - 70 };
        } else {
            const mx = (a.x + b.x) / 2, my = (a.y + b.y) / 2;
            const len = dist(a, b);
            const nx = -(b.y - a.y) / len, ny = (b.x - a.x) / len;     // normal unitaria
            const desplazamientos = existe(t.destino, t.origen)
                ? [35, 60, 90, 130] : [0, 45, -45, 80, -80, 120, -120, 160, -160];
            let c = { x: mx, y: my };
            for (const k of desplazamientos) {
                const prueba = { x: mx + nx * k, y: my + ny * k };
                c = prueba;
                if (!choca(a, prueba, b, t.origen, t.destino)) break;
            }
            // Recortar en el borde de los círculos
            const ua = { x: c.x - a.x, y: c.y - a.y }, la = Math.hypot(ua.x, ua.y) || 1;
            const ub = { x: c.x - b.x, y: c.y - b.y }, lb = Math.hypot(ub.x, ub.y) || 1;
            const p0 = { x: a.x + ua.x / la * R, y: a.y + ua.y / la * R };
            const p1 = { x: b.x + ub.x / lb * R, y: b.y + ub.y / lb * R };
            el('path', {
                d: `M ${p0.x} ${p0.y} Q ${c.x} ${c.y} ${p1.x} ${p1.y}`,
                id, class: 'arista', 'marker-end': 'url(#flecha-normal)'
            }, capaAristas);
            const medio = bez(p0, c, p1, 0.5);
            const recta = Math.abs(c.x - mx) < 1 && Math.abs(c.y - my) < 1;
            etiquetaPos = recta ? { x: medio.x, y: medio.y - 10 } : medio;
        }

        // Etiqueta con fondo blanco para que se lea sobre las líneas
        const g = el('g', { id: 'et-' + t.origen + '-' + t.destino, class: 'etiqueta-arista' }, capaEtiquetas);
        const txt = el('text', { x: etiquetaPos.x, y: etiquetaPos.y, 'text-anchor': 'middle', 'dominant-baseline': 'middle' }, g);
        txt.textContent = t.etiqueta;
        const bb = txt.getBBox();
        const fondo = el('rect', { x: bb.x - 4, y: bb.y - 2, width: bb.width + 8, height: bb.height + 4, rx: 4 }, g);
        g.insertBefore(fondo, txt);
    });

    // ---------- Estados ----------
    A.estados.forEach(e => {
        const g = el('g', { id: 'es-' + e.nombre, class: 'estado' }, capaEstados);
        if (e.inicial) {
            el('path', {
                d: `M ${e.x - R - 38} ${e.y} L ${e.x - R - 1} ${e.y}`,
                class: 'arista', 'marker-end': 'url(#flecha-normal)'
            }, capaAristas);
            const ini = el('text', { x: e.x - R - 42, y: e.y - 10, class: 'texto-inicio', 'text-anchor': 'middle' }, capaEtiquetas);
            ini.textContent = 'inicio';
        }
        el('circle', { cx: e.x, cy: e.y, r: R, class: 'circulo' }, g);
        if (e.aceptacion) el('circle', { cx: e.x, cy: e.y, r: R - 5, class: 'circulo-interno' }, g);
        const t = el('text', { x: e.x, y: e.y, 'text-anchor': 'middle', 'dominant-baseline': 'central', class: 'nombre-estado' }, g);
        t.textContent = e.nombre;
    });

    // ---------- Ajustar el área visible a todo lo dibujado (incluye etiquetas de lazos) ----------
    const caja = svg.getBBox();
    const pad = 16;
    svg.setAttribute('viewBox', `${caja.x - pad} ${caja.y - pad} ${caja.width + 2 * pad} ${caja.height + 2 * pad}`);
    // Tamaño natural: los autómatas pequeños no se estiran; los grandes se pueden desplazar en celular
    const ancho = caja.width + 2 * pad;
    svg.style.maxWidth = (ancho * 1.25) + 'px';
    svg.style.minWidth = Math.min(ancho * 0.8, 720) + 'px';

    // =========================================================
    //  Animación de la simulación
    // =========================================================
    const S = window.SIMULACION;
    if (!S) return;

    const cadena = document.querySelectorAll('#cinta .celda');
    const filas = document.querySelectorAll('#tabla-pasos tbody tr');
    const info = document.getElementById('paso-info');
    let paso = 0;                                   // 0 = antes de leer el primer símbolo
    const total = S.pasos.length;
    let timer = null;

    function limpiar() {
        svg.querySelectorAll('.estado').forEach(g => g.classList.remove('actual', 'ok', 'error'));
        svg.querySelectorAll('.arista').forEach(p => { p.classList.remove('activa'); if (p.id) p.setAttribute('marker-end', 'url(#flecha-normal)'); });
        svg.querySelectorAll('.etiqueta-arista').forEach(g => g.classList.remove('activa'));
        cadena.forEach(c => c.classList.remove('leida', 'actual', 'fallo'));
        filas.forEach(f => f.classList.remove('resaltada'));
    }

    function marcarEstado(nombre, clase) {
        const g = document.getElementById('es-' + nombre);
        if (g) g.classList.add(clase);
    }

    function dibujar() {
        limpiar();
        const inicial = A.estados[0].nombre;
        if (paso === 0) {
            marcarEstado(inicial, 'actual');
            info.textContent = `Inicio: el autómata está en ${inicial}` + (total ? '. Presione ▶ para leer el primer símbolo.' : '.');
            if (total === 0) terminar();
            return;
        }
        const p = S.pasos[paso - 1];
        for (let i = 0; i < paso - 1; i++) cadena[i] && cadena[i].classList.add('leida');
        filas[paso - 1] && filas[paso - 1].classList.add('resaltada');
        const simbolo = String.fromCharCode(p.simbolo);

        if (p.hacia === null) {
            cadena[paso - 1] && cadena[paso - 1].classList.add('fallo');
            marcarEstado(p.desde, 'error');
            info.textContent = `Paso ${paso}: desde ${p.desde} no hay transición con '${simbolo}'. La cadena se rechaza.`;
            return;
        }
        cadena[paso - 1] && cadena[paso - 1].classList.add('actual');
        const arista = document.getElementById('ar-' + p.desde + '-' + p.hacia);
        if (arista) { arista.classList.add('activa'); arista.setAttribute('marker-end', 'url(#flecha-activa)'); }
        const et = document.getElementById('et-' + p.desde + '-' + p.hacia);
        if (et) et.classList.add('activa');
        info.textContent = `Paso ${paso}: δ(${p.desde}, '${simbolo}') = ${p.hacia}`;

        if (paso === total) terminar(p.hacia);
        else marcarEstado(p.hacia, 'actual');
    }

    function terminar(ultimo) {
        const estado = ultimo || A.estados[0].nombre;
        cadena.forEach(c => c.classList.add('leida'));
        marcarEstado(estado, S.aceptada ? 'ok' : 'error');
        info.textContent += S.aceptada
            ? `  →  Terminó en ${estado}, estado de aceptación: ACEPTADA.`
            : `  →  Terminó en ${estado}, que no es de aceptación: RECHAZADA.`;
    }

    function ir(n) { paso = Math.max(0, Math.min(total, n)); dibujar(); }
    function parar() { clearInterval(timer); timer = null; document.getElementById('btn-play').textContent = '▶▶ Reproducir'; }

    document.getElementById('btn-inicio').onclick = () => { parar(); ir(0); };
    document.getElementById('btn-atras').onclick = () => { parar(); ir(paso - 1); };
    document.getElementById('btn-adelante').onclick = () => { parar(); ir(paso + 1); };
    document.getElementById('btn-fin').onclick = () => { parar(); ir(total); };
    document.getElementById('btn-play').onclick = function () {
        if (timer) { parar(); return; }
        if (paso >= total) ir(0);
        this.textContent = '❚❚ Pausa';
        timer = setInterval(() => { if (paso >= total) { parar(); return; } ir(paso + 1); }, 900);
    };

    dibujar();
})();
