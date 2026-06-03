/**
 * CineCritik — Frontend SPA (Vanilla JS)
 * Flujo obligatorio: Login → Dashboard → Póster → Modal de detalles.
 * Login se valida contra backend Java (/api/login).
 */
(function(){
  "use strict";

  // Configuración del Backend (Local).
  var API_BASE = "http://127.0.0.1:7071";

  var _origFetch = window.fetch;
  window.fetch = function(url, options) {
    if (typeof url === "string" && url.startsWith("/api/")) {
      url = API_BASE + url;
    }
    return _origFetch(url, options);
  };

  function $(id){ return document.getElementById(id); }
  function clamp(n,min,max){ return Math.max(min, Math.min(max,n)); }
  function now(){ return (typeof performance!=="undefined") ? performance.now() : Date.now(); }

  var viewLogin = $("view-login");
  var viewDash = $("view-dashboard");
  var viewRegistro = $("view-registro");
  var viewGaleria = $("view-galeria");
  var viewPoster = $("view-poster");
  var viewMyList = $("view-milista");
  var viewForo = $("view-foro");

  // Audio ambiente
  var bgAudio = new Audio();
  bgAudio.loop = true;
  bgAudio.volume = 0.25;

  function setBackgroundAudio(dataUrl){
    try{
      if (!dataUrl){
        bgAudio.pause();
        bgAudio.removeAttribute("src");
        bgAudio.load();
        return;
      }
      if (bgAudio.src === dataUrl) return;
      bgAudio.src = dataUrl;
      bgAudio.play().catch(function(){ /* requiere interacción previa */ });
    }catch(_){}
  }

  var session = { token: "", username: "" };
  var globalForumData = []; // Array of { id, multimediaId, likes, dislikes, etc }
  var lastView = null; // Para volver a la vista anterior
  var selectedItem = null;

  /* ================= TOAST NOTIFICATION SYSTEM ================= */
  function showToast(message, type) {
    type = type || "success";
    var container = $("toast-container");
    if (!container) return;
    var t = document.createElement("div");
    t.className = "toast toast--" + type;
    t.innerHTML = "<span>" + (type === "success" ? "✓" : "✗") + "</span> " + escapeHtml(message);
    container.appendChild(t);
    setTimeout(function() {
      t.classList.add("fade-out");
      setTimeout(function() { t.remove(); }, 300);
    }, 3500);
  }

  /* ================= PARTICLE BACKGROUND CANVAS ================= */
  function initParticles() {
    var canvas = $("login-particles");
    if (!canvas) return;
    var ctx = canvas.getContext("2d");
    var particles = [];
    function resize() {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
    }
    window.addEventListener("resize", resize);
    resize();
    for (var i = 0; i < 60; i++) {
      particles.push({
        x: Math.random() * canvas.width,
        y: Math.random() * canvas.height,
        baseRadius: Math.random() * 1.0 + 1.5, // 1.5px to 2.5px
        radius: 0,
        opacity: Math.random() * 0.4 + 0.3, // 0.3 to 0.7
        speedY: Math.random() * 0.15 + 0.05, // slow motion
        speedX: Math.random() * 0.1 - 0.05,
        pulseSpeed: Math.random() * 0.02 + 0.01,
        pulseOffset: Math.random() * Math.PI * 2
      });
    }
    function draw() {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      var time = now();
      for (var i = 0; i < particles.length; i++) {
        var p = particles[i];
        p.radius = p.baseRadius + Math.sin(time * p.pulseSpeed + p.pulseOffset) * 0.5; // pulsing
        ctx.fillStyle = "rgba(255, 255, 255, " + p.opacity + ")";
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2, true);
        ctx.fill();

        p.y -= p.speedY;
        p.x += p.speedX;
        
        if (p.y < 0) {
          p.y = canvas.height;
          p.x = Math.random() * canvas.width;
        }
        if (p.x < 0 || p.x > canvas.width) {
          p.speedX *= -1;
        }
      }
      requestAnimationFrame(draw);
    }
    draw();
  }

  /* ================= PARALLAX EFFECT FOR LOGIN/REGISTER BACKGROUNDS ================= */
  var loginBg = $("login-bg");
  var regBg = $("register-bg");
  document.addEventListener("mousemove", function(e) {
    var x = (e.clientX - window.innerWidth / 2) / 50;
    var y = (e.clientY - window.innerHeight / 2) / 50;
    if (loginBg && viewLogin && viewLogin.classList.contains("view--active")) {
      loginBg.style.transform = "translate(" + x + "px, " + y + "px) scale(1.05)";
    }
    if (regBg && viewUserRegister && viewUserRegister.classList.contains("view--active")) {
      regBg.style.transform = "translate(" + x + "px, " + y + "px) scale(1.05)";
    }
  });

  /* ================= STICKY NAVBAR SCROLL ================= */
  var navbar = $("main-navbar");
  window.addEventListener("scroll", function() {
    if (!navbar) return;
    if (window.scrollY > 40) {
      navbar.classList.add("scrolled");
    } else {
      navbar.classList.remove("scrolled");
    }
  });

  /* ================= CAROUSEL SLIDE HELPER ================= */
  window.slideCarousel = function(id, direction) {
    var list = $(id);
    if (list) {
      list.scrollLeft += direction * 320;
    }
  };

  /* ================= ACTIVE LINK UNDERLINE ================= */
  function updateActiveNavLink(activeButtonId) {
    var links = document.querySelectorAll(".nav-links button");
    links.forEach(function(btn) {
      if (btn.id === activeButtonId) {
        btn.classList.add("active");
      } else {
        btn.classList.remove("active");
      }
    });
  }

  function updateGlobalMatchScores() {
    if (viewDash && viewDash.classList.contains("view--active")) {
      loadDashboardHero();
    }
    if (viewGaleria && viewGaleria.classList.contains("view--active")) {
       renderGallery();
    }
    if (viewPoster && viewPoster.classList.contains("view--active") && selectedItem) {
      var infoMatch = $("poster-info-match");
      if (infoMatch) infoMatch.textContent = getMatchScore(selectedItem);
    }
  }

  /* ================= FALLBACK LOCALSTORAGE MYLIST LOGIC ================= */
  function getMyListKey() {
    return "miLista_" + (session.username || "guest");
  }
  function getMyList() {
    try {
      var raw = localStorage.getItem(getMyListKey());
      return raw ? JSON.parse(raw) : [];
    } catch(e) {
      return [];
    }
  }
  function setMyList(arr) {
    try {
      localStorage.setItem(getMyListKey(), JSON.stringify(arr));
    } catch(e) {}
  }
  async function apiListGet() {
    try {
      var res = await fetch("/api/lista", { headers: authHeaders() });
      if (res.ok) {
        var data = await res.json();
        return data.ids || [];
      }
      if (res.status === 404) return getMyList();
    } catch(e) {
      return getMyList();
    }
    return getMyList();
  }
  async function apiListAdd(id) {
    try {
      var res = await fetch("/api/lista/agregar", {
        method: "POST",
        headers: Object.assign({ "Content-Type": "application/json" }, authHeaders()),
        body: JSON.stringify({ tituloId: Number(id) })
      });
      if (res.ok) return true;
      if (res.status === 404) {
        var list = getMyList();
        if (list.indexOf(Number(id)) === -1) {
          list.push(Number(id));
          setMyList(list);
        }
        return true;
      }
    } catch(e) {
      var list = getMyList();
      if (list.indexOf(Number(id)) === -1) {
        list.push(Number(id));
        setMyList(list);
      }
      return true;
    }
    return false;
  }
  async function apiListRemove(id) {
    try {
      var res = await fetch("/api/lista/eliminar", {
        method: "DELETE",
        headers: Object.assign({ "Content-Type": "application/json" }, authHeaders()),
        body: JSON.stringify({ id: Number(id) })
      });
      if (res.ok) return true;
      if (res.status === 404) {
        var list = getMyList();
        var idx = list.indexOf(Number(id));
        if (idx !== -1) {
          list.splice(idx, 1);
          setMyList(list);
        }
        return true;
      }
    } catch(e) {
      var list = getMyList();
      var idx = list.indexOf(Number(id));
      if (idx !== -1) {
        list.splice(idx, 1);
        setMyList(list);
      }
      return true;
    }
    return false;
  }

  /* ================= CORRECCIÓN 1: RENDER TOP 10 CON NÚMEROS SUPERPUESTOS ================= */
  async function renderTop10() {
    var container = $("top10-container");
    var section = $("top10-section");
    if (!container || !section) return;

    var ranked = globalForumData.slice().sort(function(a, b) {
        var al = parseInt(a.likes) || 0, ad = parseInt(a.dislikes) || 0;
        var bl = parseInt(b.likes) || 0, bd = parseInt(b.dislikes) || 0;
        
        var at = al + ad, bt = bl + bd;
        var ap = at === 0 ? 0 : (al / at);
        var bp = bt === 0 ? 0 : (bl / bt);
        
        if (bp !== ap) return bp - ap;
        return bt - at;
    }).slice(0, 10);

    if (ranked.length > 0) {
        section.hidden = false;
        container.innerHTML = "";
        ranked.forEach(function(item, index) {
            var card = document.createElement("div");
            card.className = "top10-card";
            
            var prom = 0;
            if (item.promedio) {
              prom = Math.round(item.promedio);
            }
            var stars = "";
            for (var s = 1; s <= 5; s++) {
              stars += s <= prom ? "★" : "☆";
            }

            var imgStyle = item.posterDataUrl ? "background-image:url('" + String(item.posterDataUrl).replace(/'/g, "%27") + "');" : "";

            card.innerHTML = `
              <div class="top10-img-container">
                <div class="top10-rank">${index + 1}</div>
                <div class="top10-img" style="${imgStyle}"></div>
              </div>
              <div class="top10-details">
                <div class="top10-title">${escapeHtml(item.titulo || "")}</div>
                <div class="top10-meta">${escapeHtml(item.tipo || "")} • ${escapeHtml(item.genero || "")}</div>
                <div class="top10-stars">${stars}</div>
              </div>
            `;
            
            card.onclick = function() {
                var el = document.getElementById("forum-post-" + item.id);
                if (el) {
                    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    el.classList.add("glitch-highlight");
                    setTimeout(function(){ el.classList.remove("glitch-highlight"); }, 2000);
                } else {
                    openPoster({
                      id: item.multimediaId || item.id,
                      titulo: item.titulo,
                      tipo: item.tipo,
                      anio: item.anio,
                      genero: item.genero,
                      descripcion: item.descripcion,
                      posterDataUrl: item.posterDataUrl,
                      audioDataUrl: ""
                    });
                }
            };
            container.appendChild(card);
        });
    } else {
        section.hidden = true;
    }
  }

  function authHeaders(){
    return session.token ? { "X-Auth": session.token } : {};
  }

  async function syncForumData() {
    try {
      var res = await fetch("/api/forum/list", { headers: authHeaders() });
      if (res.ok) {
        var data = await res.json();
        globalForumData = data.posts || [];
      }
    } catch(e) { console.error("Sync fail", e); }
  }

  function setView(next){
    var current = [viewLogin, viewUserRegister, viewDash, viewRegistro, viewGaleria, viewForo, viewMyList].find(function(v){
      return v && v.classList.contains("view--active");
    });
    if (current && next === viewPoster) lastView = current;

    [viewLogin, viewUserRegister, viewDash, viewRegistro, viewGaleria, viewForo, viewMyList, viewPoster].filter(Boolean).forEach(function(v){
      v.classList.remove("view--active","fade-in","fade-out");
      v.style.display = "none";
    });
    next.style.display = "block";
    next.classList.add("view--active","fade-in");

    if (next === viewLogin || next === viewDash){
      setBackgroundAudio("");
    }
    
    if (next === viewDash && typeof loadDashboardHero === "function") {
      loadDashboardHero();
    }
    if (next === viewMyList) {
      renderMyList();
    }
  }

  function flashGlitch(){
    var glitch = $("screen-glitch");
    if (!glitch) return;
    glitch.classList.remove("is-on");
    void glitch.offsetWidth;
    glitch.classList.add("is-on");
  }

  function getMatchScore(item) {
    var l = 0, d = 0;
    if (typeof item.likes !== "undefined") {
      l = parseInt(item.likes) || 0;
      d = parseInt(item.dislikes) || 0;
    } else {
      var match = globalForumData.find(function(p){ 
        return Number(p.multimediaId) == Number(item.id); 
      });
      if (match) {
        l = parseInt(match.likes) || 0;
        d = parseInt(match.dislikes) || 0;
      }
    }
    var t = l + d;
    return t === 0 ? "Nuevo" : Math.round((l/t)*100) + "% de coincidencia";
  }

  async function apiCalificar(id, puntaje) {
    var res = await fetch("/api/catalogo/calificar", {
      method: "POST",
      headers: Object.assign({ "Content-Type": "application/json" }, authHeaders()),
      body: JSON.stringify({ id: Number(id), puntaje: Number(puntaje) })
    });
    if (!res.ok) throw new Error("Error al calificar");
    return await res.json();
  }

  /* ================= CORRECCIÓN 2: VISOR DE DETALLES DISPONIBILIDAD BADGE ================= */
  function openPoster(item) {
    selectedItem = item;
    var pCenter = $("poster-center");
    var pCard = $("poster-card-detail");
    var btnPlayTitle = $("btn-play-title");

    if(pCenter) { 
      pCenter.style.opacity = "1"; 
      pCenter.style.transform = "scale(1)"; 
      pCenter.style.pointerEvents = "auto"; 
      pCenter.style.display = "flex";
    }
    if(pCard) { 
      pCard.style.display = "none";
      pCard.style.opacity = "0"; 
      pCard.style.transform = "translateY(20px)"; 
      pCard.style.pointerEvents = "none"; 
    }
    
    var pTitle = $("poster-title");
    if (pTitle) {
      pTitle.textContent = (item.titulo || "CINECRITIK").toUpperCase();
      pTitle.style.display = "block";
      pTitle.style.opacity = "1";
    }
    
    var infoMatch = $("poster-info-match"); if(infoMatch) infoMatch.textContent = getMatchScore(item);
    var infoYear = $("poster-info-year"); if(infoYear) infoYear.textContent = item.anio || "";
    var infoGenre = $("poster-info-genre"); if(infoGenre) infoGenre.textContent = item.genero || "";
    var infoDesc = $("poster-info-desc"); if(infoDesc) infoDesc.textContent = item.descripcion || "Sin descripción.";

    // CORRECCIÓN 2: Badge e Indicador de Enlace asociado
    var hasUrl = item.streamingUrl && item.streamingUrl.trim().length > 0;
    var badge = $("poster-availability-badge");
    if (badge) {
      if (hasUrl) {
        badge.textContent = "● Disponible";
        badge.className = "availability-badge available";
      } else {
        badge.textContent = "● No disponible";
        badge.className = "availability-badge unavailable";
      }
    }

    // Play Splash cover button
    if (btnPlayTitle) {
      btnPlayTitle.style.display = "block";
      btnPlayTitle.style.opacity = "1";
      if (hasUrl) {
        btnPlayTitle.textContent = "▶ Reproducir Título";
      } else {
        btnPlayTitle.textContent = "Ver Detalles";
      }

      btnPlayTitle.onclick = function() {
          btnPlayTitle.style.opacity = "0";
          if(pTitle) pTitle.style.opacity = "0";
          if(stage) stage.style.backgroundPosition = "center 20%"; // Shift image up
          setTimeout(function(){
            if(pTitle) pTitle.style.display = "none";
            if(btnPlayTitle) btnPlayTitle.style.display = "none";
            if(pCard) { 
              pCard.style.display = "block";
              void pCard.offsetWidth;
              pCard.style.opacity = "1"; 
              pCard.style.transform = "translateY(0)"; 
              pCard.style.pointerEvents = "auto"; 
            }
          }, 400);
      };
    }

    // Play action button inside details card
    var btnPlayMovie = $("btn-play-movie");
    if (btnPlayMovie) {
      if (hasUrl) {
        btnPlayMovie.textContent = "▶ Reproducir Ahora";
        btnPlayMovie.style.opacity = "1";
        btnPlayMovie.style.cursor = "pointer";
        btnPlayMovie.style.pointerEvents = "auto";
        btnPlayMovie.removeAttribute("title");
        btnPlayMovie.onclick = function(e) {
          e.stopPropagation();
          window.open(item.streamingUrl, "_blank");
        };
      } else {
        btnPlayMovie.textContent = "No disponible";
        btnPlayMovie.style.opacity = "0.4";
        btnPlayMovie.style.cursor = "not-allowed";
        btnPlayMovie.style.pointerEvents = "none";
        btnPlayMovie.setAttribute("title", "Enlace no disponible");
        btnPlayMovie.onclick = null;
      }
    }

    // CORRECCIÓN 5: Botón "+ Mi Lista" / "- Mi Lista" logic
    var btnAddList = $("btn-detail-add-list");
    if (btnAddList) {
      apiListGet().then(function(ids) {
        var inList = ids.indexOf(Number(item.id)) !== -1;
        updateAddListBtnState(inList);
      });

      function updateAddListBtnState(inList) {
        if (inList) {
          btnAddList.innerHTML = "✓ En Mi Lista";
          btnAddList.classList.add("in-list");
        } else {
          btnAddList.innerHTML = "+ Mi Lista";
          btnAddList.classList.remove("in-list");
        }
      }

      btnAddList.onclick = async function(e) {
        e.stopPropagation();
        var inList = btnAddList.classList.contains("in-list");
        if (inList) {
          var ok = await apiListRemove(item.id);
          if (ok) {
            updateAddListBtnState(false);
            showToast("Eliminado de Mi Lista");
            if (viewMyList && viewMyList.classList.contains("view--active")) {
              renderMyList();
            }
          }
        } else {
          var ok = await apiListAdd(item.id);
          if (ok) {
            updateAddListBtnState(true);
            showToast("Añadido a Mi Lista");
            if (viewMyList && viewMyList.classList.contains("view--active")) {
              renderMyList();
            }
          }
        }
      };
    }

    // Render left side poster image inside detail card
    var detailLeft = $("detail-left-poster");
    if (detailLeft && item.posterDataUrl) {
      detailLeft.style.backgroundImage = "url('" + String(item.posterDataUrl).replace(/'/g, "%27") + "')";
    } else if (detailLeft) {
      detailLeft.style.backgroundImage = "";
    }

    // Set star rating selection states & handlers
    var stars = document.querySelectorAll("#detail-rating-stars span");
    var currentRating = Math.round(item.promedio || 0);
    stars.forEach(function(s, idx) {
      if (idx < currentRating) s.classList.add("selected");
      else s.classList.remove("selected");

      s.onclick = async function(e) {
        e.stopPropagation();
        var val = Number(s.getAttribute("data-value"));
        try {
          var res = await apiCalificar(item.id, val);
          if (res && res.ok) {
            showToast("Calificado con " + val + " estrellas! Promedio actual: " + res.promedio);
            stars.forEach(function(st, idx2) {
              if (idx2 < val) st.classList.add("selected");
              else st.classList.remove("selected");
            });
            item.promedio = res.promedio;
            syncForumData().then(() => {
              updateGlobalMatchScores();
            });
          } else {
            showToast(res.error || "No se pudo registrar la calificación", "error");
          }
        } catch(err) {
          showToast("Error de conexión al servidor", "error");
        }
      };
    });

    var stage = $("poster-stage");
    if (stage && item.posterDataUrl){
      stage.style.backgroundImage = "linear-gradient(180deg, rgba(0,0,0,.2), rgba(0,0,0,.8)), url('" + String(item.posterDataUrl).replace(/'/g, "%27") + ")";
      stage.style.backgroundSize = "cover";
      stage.style.backgroundPosition = "center";
    } else if (stage) {
      stage.style.backgroundImage = "";
    }
    setBackgroundAudio(item.audioDataUrl || "");
    setView($("view-poster"));
  }

  /* RENDER CAROUSELS DINÁMICAMENTE EN DASHBOARD */
  function renderDashboardCarousels(items) {
    var trendsList = $("trends-list");
    var recList = $("rec-list");
    if (!trendsList || !recList) return;

    trendsList.innerHTML = "";
    recList.innerHTML = "";

    if (!items.length) {
      trendsList.innerHTML = "<p class=\"mono\" style=\"padding: 10px; color: var(--fg-mut)\">No hay tendencias disponibles.</p>";
      recList.innerHTML = "<p class=\"mono\" style=\"padding: 10px; color: var(--fg-mut)\">No hay recomendaciones todavía.</p>";
      return;
    }

    // Tendencias: últimos agregados
    var trends = items.slice().reverse().slice(0, 10);
    trends.forEach(function(item) {
      var el = document.createElement("div");
      el.className = "carousel-item";
      var img = item.posterDataUrl ? "background-image:url('" + String(item.posterDataUrl).replace(/'/g, "%27") + "')" : "";
      el.innerHTML = 
        "<div class=\"carousel-item-img\" style=\"" + img + "\"></div>" +
        "<div class=\"carousel-item-overlay\">" +
          "<div class=\"carousel-item-title\">" + escapeHtml(item.titulo) + "</div>" +
          "<div class=\"carousel-item-meta\">" + escapeHtml(item.tipo) + " • " + (item.anio || "") + "</div>" +
        "</div>";
      el.onclick = function() { openPoster(item); };
      trendsList.appendChild(el);
    });

    // Recomendado: Ordenado por match o calificación
    var recommended = items.slice().sort(function(a, b) {
      return (b.promedio || 0) - (a.promedio || 0);
    }).slice(0, 10);

    recommended.forEach(function(item) {
      var el = document.createElement("div");
      el.className = "carousel-item";
      var img = item.posterDataUrl ? "background-image:url('" + String(item.posterDataUrl).replace(/'/g, "%27") + "')" : "";
      el.innerHTML = 
        "<div class=\"carousel-item-img\" style=\"" + img + "\"></div>" +
        "<div class=\"carousel-item-overlay\">" +
          "<div class=\"carousel-item-title\">" + escapeHtml(item.titulo) + "</div>" +
          "<div class=\"carousel-item-meta\">⭐ " + (item.promedio ? Number(item.promedio).toFixed(1) : "Nuevo") + "</div>" +
        "</div>";
      el.onclick = function() { openPoster(item); };
      recList.appendChild(el);
    });
  }

  async function loadDashboardHero() {
    var heroHeader = $("hero-header");
    if (!heroHeader) return;
    
    // Inyectar skeleton shimmer en los carousels
    var trendsList = $("trends-list");
    var recList = $("rec-list");
    var sk = "<div class=\"skeleton-card\"></div>".repeat(6);
    if(trendsList) trendsList.innerHTML = sk;
    if(recList) recList.innerHTML = sk;

    try {
      await syncForumData();
      var data = await apiCatalogo();
      var items = (data && data.items) || [];
      
      renderDashboardCarousels(items);

      if (items.length > 0) {
        var last = items[items.length - 1];
        
        var titleEl = $("hero-title"); if(titleEl) titleEl.textContent = last.titulo;
        var infoMatch = $("hero-match"); if(infoMatch) infoMatch.textContent = getMatchScore(last);
        var yearEl = $("hero-year"); if(yearEl) yearEl.textContent = last.anio;
        var genreEl = $("hero-genre"); if(genreEl) genreEl.textContent = last.genero;
        var descEl = $("hero-desc"); if(descEl) descEl.textContent = last.descripcion || "Sin descripción.";
        if (last.posterDataUrl) {
          heroHeader.style.backgroundImage = "linear-gradient(to top, var(--bg-main) 0%, transparent 60%), url('" + String(last.posterDataUrl).replace(/'/g, "%27") + "')";
        } else {
          heroHeader.style.backgroundImage = "linear-gradient(to top, var(--bg-main) 0%, transparent 60%), linear-gradient(135deg, #111, #333)";
        }
        
        var clickHandler = function() { openPoster(last); };
        var playBtn = $("hero-play");
        if(playBtn) playBtn.onclick = clickHandler;
        if(titleEl) { titleEl.style.cursor = "pointer"; titleEl.onclick = clickHandler; }

        // Watchlist hero list button
        var heroListBtn = $("hero-list");
        if (heroListBtn) {
          apiListGet().then(function(ids) {
            var inList = ids.indexOf(Number(last.id)) !== -1;
            updateHeroListBtnState(inList);
          });
          
          function updateHeroListBtnState(inList) {
            if (inList) {
              heroListBtn.textContent = "✓ En Mi Lista";
              heroListBtn.classList.add("in-list");
            } else {
              heroListBtn.textContent = "+ Mi Lista";
              heroListBtn.classList.remove("in-list");
            }
          }

          heroListBtn.onclick = async function(e) {
            e.stopPropagation();
            var inList = heroListBtn.classList.contains("in-list");
            if (inList) {
              var ok = await apiListRemove(last.id);
              if (ok) {
                updateHeroListBtnState(false);
                showToast("Eliminado de Mi Lista");
              }
            } else {
              var ok = await apiListAdd(last.id);
              if (ok) {
                updateHeroListBtnState(true);
                showToast("Añadido a Mi Lista");
              }
            }
          };
        }
      } else {
        var titleEl = $("hero-title"); if(titleEl) titleEl.innerHTML = "¡Aún no hay<br>contenido!";
        var descEl = $("hero-desc"); if(descEl) descEl.textContent = "Sube tu primera película o serie al catálogo para verla aquí.";
        heroHeader.style.backgroundImage = "linear-gradient(to top, var(--bg-main) 0%, transparent 60%), linear-gradient(135deg, #1f222d, #0b0c10)";
        var playBtn = $("hero-play");
        if(playBtn) { playBtn.onclick = function() { setView($("view-registro")); }; playBtn.textContent = "Subir Contenido"; }
      }
    } catch(e) {
      if(trendsList) trendsList.innerHTML = "<p class='mono' style='color:var(--accent)'>Error al cargar tendencias</p>";
      if(recList) recList.innerHTML = "<p class='mono' style='color:var(--accent)'>Error al cargar recomendaciones</p>";
    }
  }

  /* ================= Login ================= */
  var loginForm = $("login-form");
  var loginUser = $("login-username");
  var loginPass = $("login-password");
  var btnGoRegister = $("btn-go-register");
  var btnFillHint = $("btn-fill-hint");
  var verify = $("verify");
  var fill = $("retro-fill") || document.createElement("div");
  var status = $("verify-status");

  var verifying = false;

  function runRetro(ok, done){
    verifying = true;
    verify.hidden = false;
    status.textContent = "[ estableciendo enlace ]";
    fill.style.width = "0%";
    var t0 = now();
    var dur = 1150;
    function tick(){
      var p = clamp((now()-t0)/dur, 0, 1);
      fill.style.width = Math.round(p*100) + "%";
      if (p < 1){ requestAnimationFrame(tick); return; }
      status.textContent = ok ? "[ acceso concedido ]" : "[ acceso denegado ]";
      verifying = false;
      setTimeout(done, ok ? 320 : 650);
    }
    requestAnimationFrame(tick);
  }

  async function apiLogin(username, password){
    var res = await fetch("/api/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username: username, password: password })
    });
    if (!res.ok){
      return { ok: false };
    }
    return await res.json();
  }

  async function apiRegister(username, password){
    var res = await fetch("/api/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username: username, password: password })
    });
    var data;
    try { data = await res.json(); } catch (_) { data = { ok:false }; }
    if (!res.ok) return data || { ok:false };
    return data;
  }

  // Navegación a registro de usuario
  var viewUserRegister = $("view-user-register");
  var btnBackLogin = $("btn-back-login");
  var btnUseCreated = $("btn-use-created");
  var userRegForm = $("user-register-form");
  var regUser = $("reg-user");
  var regPass = $("reg-pass");
  var regPass2 = $("reg-pass2");
  var userRegisterStatus = $("user-register-status");
  var lastCreated = { username:"", password:"" };

  if (btnGoRegister){
    btnGoRegister.addEventListener("click", function(){
      if (viewUserRegister) setView(viewUserRegister);
    });
  }
  if (btnBackLogin){
    btnBackLogin.addEventListener("click", function(){
      setView(viewLogin);
    });
  }
  if (btnUseCreated){
    btnUseCreated.addEventListener("click", function(){
      if (!lastCreated.username){
        flashGlitch();
        if (userRegisterStatus) userRegisterStatus.textContent = "Primero crea un acceso.";
        return;
      }
      if (loginUser) loginUser.value = lastCreated.username;
      if (loginPass) loginPass.value = lastCreated.password;
      setView(viewLogin);
    });
  }

  if (userRegForm){
    userRegForm.addEventListener("submit", async function(e){
      e.preventDefault();
      var u = (regUser.value || "").trim();
      var p1 = String(regPass.value || "");
      var p2 = String(regPass2.value || "");
      if (u.length < 3){
        flashGlitch();
        userRegisterStatus.textContent = "El usuario debe tener al menos 3 caracteres.";
        return;
      }
      if (p1.length < 4){
        flashGlitch();
        userRegisterStatus.textContent = "La contraseña debe tener al menos 4 caracteres.";
        return;
      }
      if (p1 !== p2){
        flashGlitch();
        userRegisterStatus.textContent = "Las contraseñas no coinciden.";
        return;
      }
      userRegisterStatus.textContent = "Registrando…";
      try{
        var r = await apiRegister(u, p1);
        if (!r || !r.ok){
          flashGlitch();
          userRegisterStatus.textContent = (r && r.error) ? r.error : "No se pudo registrar.";
          return;
        }
        flashGlitch();
        lastCreated.username = u;
        lastCreated.password = p1;
        userRegisterStatus.textContent = "Acceso creado. Puedes usarlo para iniciar sesión.";
        showToast("Cuenta de usuario creada con éxito!");
      }catch(_){
        flashGlitch();
        userRegisterStatus.textContent = "No se pudo registrar. (¿Servidor Java corriendo?)";
      }
    });
  }

  if (btnFillHint){
    btnFillHint.addEventListener("click", function(){
      loginUser.value = "andres";
      loginPass.value = "cine";
    });
  }

  if (loginForm){
    loginForm.addEventListener("submit", async function(e){
      e.preventDefault();
      if (verifying) return;
      var u = (loginUser.value||"").trim();
      var p = String(loginPass.value||"");
      var ok = false;
      var errText = "";
      
      var errorContainer = $("login-error-msg");
      if (errorContainer) errorContainer.style.display = "none";

      try{
        var r = await apiLogin(u,p);
        ok = !!(r && r.ok);
        if (ok && r.token){
          session.token = r.token;
          session.username = r.username || u;
          // Actualizar nombre en el dropdown de perfil
          var dropUser = $("dropdown-username");
          if (dropUser) dropUser.textContent = session.username;
          var avatar = $("nav-avatar");
          if (avatar) avatar.textContent = session.username.substring(0,1).toUpperCase();
        } else {
          errText = (r && r.error) ? r.error : "Credenciales inválidas.";
        }
      }catch(_){
        ok = false;
        errText = "No se pudo establecer conexión con el servidor.";
      }

      runRetro(ok, function(){
        if (!ok){
          flashGlitch();
          status.textContent = "[ acceso denegado ]";
          fill.style.width = "0%";
          if (errorContainer) {
            errorContainer.textContent = errText;
            errorContainer.style.display = "block";
          }
          showToast("Error de inicio de sesión", "error");
          return;
        }
        showToast("¡Enlace establecido! Bienvenido " + session.username);
        viewLogin.classList.add("fade-out");
        setTimeout(function(){ setView(viewDash); }, 450);
      });
    });
  }

  /* ================= Dashboard ================= */
  var btnRegistro = $("btn-registro");
  var btnGaleria = $("btn-galeria");
  var btnForo = $("btn-foro");
  var btnForoBack = $("btn-foro-back");
  var btnLogout = $("btn-logout");
  var forum = $("forum");

  async function apiInfo(){
    var res = await fetch("/api/info", { method:"GET", headers: authHeaders() });
    if (!res.ok) throw new Error("info fail");
    return await res.json();
  }

  async function apiCatalogo(){
    var res = await fetch("/api/catalogo", { method:"GET", headers: authHeaders() });
    if (!res.ok) throw new Error("catalogo fail");
    return await res.json();
  }

  async function apiAgregar(payload){
    var res = await fetch("/api/catalogo/agregar", {
      method:"POST",
      headers: Object.assign({ "Content-Type":"application/json" }, authHeaders()),
      body: JSON.stringify(payload)
    });
    if (!res.ok) throw new Error("agregar fail");
    return await res.json();
  }

  async function apiForumPublish(id){
    var res = await fetch("/api/forum/publicar", {
      method:"POST",
      headers: Object.assign({ "Content-Type":"application/json" }, authHeaders()),
      body: JSON.stringify({ id: id })
    });
    var data;
    try { data = await res.json(); } catch (_) { data = { ok:false }; }
    if (!res.ok) throw new Error(data && data.error ? data.error : "publicar fail");
    return data;
  }

  async function apiForumList(){
    var res = await fetch("/api/forum/list", { method:"GET" });
    if (!res.ok) throw new Error("forum list fail");
    return await res.json();
  }

  async function apiForumComments(postId){
    var res = await fetch("/api/forum/comments?postId=" + encodeURIComponent(postId), { method:"GET" });
    if (!res.ok) throw new Error("forum comments fail");
    return await res.json();
  }

  async function apiForumComment(postId, texto){
    var res = await fetch("/api/forum/comment", {
      method:"POST",
      headers: Object.assign({ "Content-Type":"application/json" }, authHeaders()),
      body: JSON.stringify({ postId: postId, texto: texto })
    });
    if (!res.ok) throw new Error("forum comment fail");
    return res.json();
  }
  async function apiForumVote(postId, isLike){
    var res = await fetch("/api/forum/vote", {
      method:"POST",
      headers: Object.assign({ "Content-Type":"application/json" }, authHeaders()),
      body: JSON.stringify({ postId: postId, isLike: isLike })
    });
    if (!res.ok) throw new Error("vote fail");
    return res.json();
  }
  async function apiForumDelete(postId){
    var res = await fetch("/api/forum/delete", {
      method:"POST",
      headers: Object.assign({ "Content-Type":"application/json" }, authHeaders()),
      body: JSON.stringify({ postId: postId })
    });
    if (!res.ok) throw new Error("delete fail");
    return res.json();
  }

  /* ================= CORRECCIÓN 4: FORMULARIO DE SUBIR CONTENIDO LIMPIAR AL ENTRAR/ENVIAR ================= */
  function resetUploadForm() {
    var form = $("registro-form");
    if (form) {
      form.reset();
    }
    editMode.id = 0;
    editMode.original = null;
    clearPoster();
    clearAudio();
    fillYearOptions();
    syncTipo();
    if (regStatus) regStatus.textContent = "";
  }

  if (btnRegistro){
    btnRegistro.addEventListener("click", function(){
      resetUploadForm();
      setView(viewRegistro);
      updateActiveNavLink("btn-registro");
    });
  }
  if (btnGaleria){
    btnGaleria.addEventListener("click", async function(){
      setView(viewGaleria);
      updateActiveNavLink("btn-galeria");
      await renderGallery();
    });
  }

  // Back button for Mi Lista
  var btnMyListBack = $("btn-milista-back");
  if (btnMyListBack) {
    btnMyListBack.addEventListener("click", function() {
      setView(viewDash);
      updateActiveNavLink("");
    });
  }

  // Mi Lista Navigation Link in Profile Dropdown
  var btnDropdownMyList = $("btn-dropdown-milista");
  if (btnDropdownMyList) {
    btnDropdownMyList.addEventListener("click", function() {
      var dropdown = $("nav-dropdown");
      if (dropdown) dropdown.classList.remove("active");
      setView(viewMyList);
      updateActiveNavLink("");
    });
  }

  if (btnForo){
    btnForo.addEventListener("click", async function(){
      if (viewForo) setView(viewForo);
      updateActiveNavLink("btn-foro");
      await renderForum();
    });
  }
  if (btnForoBack){
    btnForoBack.addEventListener("click", function(){
      setView(viewDash);
      updateActiveNavLink("");
    });
  }
  if (btnLogout){
    btnLogout.addEventListener("click", function(){
      verify.hidden = true;
      fill.style.width = "0%";
      status.textContent = "[ esperando respuesta ]";
      loginPass.value = "";
      var errorContainer = $("login-error-msg");
      if (errorContainer) errorContainer.style.display = "none";
      setView(viewLogin);
      showToast("Sesión cerrada con éxito");
    });
  }

  /* ================= Registro de archivos ================= */
  var btnRegistroBack = $("btn-registro-back");
  var regForm = $("registro-form");
  var regTipo = $("reg-tipo");
  var regTitulo = $("reg-titulo");
  var regAnio = $("reg-anio");
  var regGenero = $("reg-genero");
  var regDesc = $("reg-descripcion");
  var regDur = $("reg-duracion");
  var regTemps = $("reg-temporadas");
  var regEpis = $("reg-episodios");
  var regStreamUrl = $("reg-streaming-url");
  var lblStreamUrl = $("lbl-streaming-url");
  var wrapDur = $("duracion-wrap");
  var wrapTemps = $("temporadas-wrap");
  var wrapEpis = $("episodios-wrap");
  var regStatus = $("registro-status");
  var btnPosterFile = $("btn-poster-file");
  var btnClearFile = $("btn-clear-file");
  var posterFile = $("poster-file");
  var posterPreview = $("poster-preview");
  var posterFit = $("poster-fit");
  var posterFocus = $("poster-focus");
  var selectedPosterDataUrl = "";
  var btnAudioFile = $("btn-audio-file");
  var btnClearAudio = $("btn-clear-audio");
  var audioFile = $("audio-file");
  var audioHint = $("audio-hint");
  var audioPreview = $("audio-preview");
  var selectedAudioDataUrl = "";

  function fillYearOptions(){
    if (!regAnio) return;
    regAnio.innerHTML = "";
    var current = new Date().getFullYear();
    var start = 1970;
    var end = current + 2;
    for (var y = end; y >= start; y--){
      var opt = document.createElement("option");
      opt.value = String(y);
      opt.textContent = String(y);
      if (y === current) opt.selected = true;
      regAnio.appendChild(opt);
    }
  }
  fillYearOptions();

  function syncTipo(){
    var t = (regTipo && regTipo.value) || "Pelicula";
    var tNormal = t.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");
    var isSerieOrAnime = (tNormal === "serie" || tNormal === "anime");
    if (wrapTemps) wrapTemps.hidden = !isSerieOrAnime;
    if (wrapEpis) wrapEpis.hidden = !isSerieOrAnime;
    if (wrapDur) wrapDur.hidden = isSerieOrAnime;
    
    if (lblStreamUrl) {
      if (tNormal === "serie") lblStreamUrl.textContent = "Link de la serie";
      else if (tNormal === "anime") lblStreamUrl.textContent = "Link del anime";
      else lblStreamUrl.textContent = "Link de la película";
    }
  }
  if (regTipo) regTipo.addEventListener("change", syncTipo);
  syncTipo();

  if (btnRegistroBack){
    btnRegistroBack.addEventListener("click", function(){
      setView(viewDash);
      updateActiveNavLink("");
    });
  }

  if (regForm){
    regForm.addEventListener("submit", async function(e){
      e.preventDefault();
      try{
        var tNormal = String(regTipo.value).toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");
        var isSerieOrAnime = (tNormal === "serie" || tNormal === "anime");
        var payload = {
          id: editMode.id || 0,
          tipo: regTipo.value,
          titulo: (regTitulo.value||"").trim(),
          anio: Number(regAnio.value||2026),
          genero: (regGenero.value||"").trim(),
          descripcion: (regDesc.value||"").trim(),
          streamingUrl: (regStreamUrl ? regStreamUrl.value : "").trim(),
          duracionMin: isSerieOrAnime ? 0 : Number(regDur.value||92),
          temporadas: isSerieOrAnime ? Number(regTemps.value||1) : 0,
          episodios: isSerieOrAnime ? Number(regEpis.value||12) : 0,
          posterDataUrl: selectedPosterDataUrl,
          audioDataUrl: selectedAudioDataUrl
        };
        regStatus.textContent = "Registrando…";
        var r = editMode.id ? await apiEditar(payload) : await apiAgregar(payload);
        flashGlitch();
        regStatus.textContent = r && r.ok ? ((editMode.id ? "Actualizado: " : "Registrado en catálogo: ") + r.item.titulo) : "Error al registrar.";
        if (r && r.ok){
          showToast(editMode.id ? "Título actualizado con éxito!" : "¡Título añadido al catálogo!");
          var fit = (posterFit && posterFit.value) || "cover";
          var focus = (posterFocus && posterFocus.value) || "center";
          var idToSave = editMode.id || (r.item && r.item.id) || 0;
          if (idToSave) saveImagePrefs(idToSave, fit, focus);

          resetUploadForm();
          if (editSourceView === viewMyList) {
            setView(viewMyList);
            renderMyList();
          } else {
            setView(viewGaleria);
            renderGallery();
          }
        }
      }catch(_){
        flashGlitch();
        regStatus.textContent = "No se pudo registrar. (¿Servidor Java corriendo?)";
        showToast("Error al guardar multimedia", "error");
      }
    });
  }

  function clearPoster(){
    selectedPosterDataUrl = "";
    if (posterPreview){
      posterPreview.style.backgroundImage = "";
      posterPreview.style.backgroundSize = "";
      posterPreview.style.backgroundPosition = "";
      posterPreview.style.backgroundRepeat = "";
      posterPreview.style.backgroundColor = "";
    }
    if (posterFile) posterFile.value = "";
  }
  function clearAudio(){
    selectedAudioDataUrl = "";
    if (audioFile) audioFile.value = "";
    if (audioHint) audioHint.textContent = "Sin audio cargado.";
    if (audioPreview) audioPreview.style.backgroundImage = "";
  }

  if (btnPosterFile && posterFile){
    btnPosterFile.addEventListener("click", function(){ posterFile.click(); });
  }
  if (btnClearFile){
    btnClearFile.addEventListener("click", clearPoster);
  }
  if (posterFile){
    posterFile.addEventListener("change", function(){
      var f = posterFile.files && posterFile.files[0];
      if (!f) return;
      if (f.size > 700 * 1024){
        flashGlitch();
        regStatus.textContent = "Imagen demasiado grande. Usa una menor a 700KB.";
        clearPoster();
        return;
      }
      var reader = new FileReader();
      reader.onload = function(){
        selectedPosterDataUrl = String(reader.result || "");
        if (posterPreview){
          posterPreview.style.backgroundImage = "url('" + selectedPosterDataUrl.replace(/'/g, "%27") + "')";
          applyPosterPreviewFit();
        }
      };
      reader.readAsDataURL(f);
    });
  }

  function applyPosterPreviewFit(){
    if (!posterPreview) return;
    var fit = (posterFit && posterFit.value) || "cover";
    var focus = (posterFocus && posterFocus.value) || "center";
    if (fit === "contain"){
      posterPreview.style.backgroundSize = "contain";
      posterPreview.style.backgroundRepeat = "no-repeat";
      posterPreview.style.backgroundColor = "rgba(0,0,0,.35)";
    }else{
      posterPreview.style.backgroundSize = "cover";
      posterPreview.style.backgroundRepeat = "no-repeat";
      posterPreview.style.backgroundColor = "";
    }
    if (focus === "top") posterPreview.style.backgroundPosition = "50% 15%";
    else if (focus === "bottom") posterPreview.style.backgroundPosition = "50% 85%";
    else posterPreview.style.backgroundPosition = "center";
  }
  if (posterFit) posterFit.addEventListener("change", applyPosterPreviewFit);
  if (posterFocus) posterFocus.addEventListener("change", applyPosterPreviewFit);

  if (btnAudioFile && audioFile){
    btnAudioFile.addEventListener("click", function(){ audioFile.click(); });
  }
  if (btnClearAudio){
    btnClearAudio.addEventListener("click", clearAudio);
  }
  if (audioFile){
    audioFile.addEventListener("change", function(){
      var f = audioFile.files && audioFile.files[0];
      if (!f) return;
      if (f.size > 2.2 * 1024 * 1024){
        flashGlitch();
        regStatus.textContent = "Audio demasiado grande. Usa uno menor a 2.2MB.";
        clearAudio();
        return;
      }
      var reader = new FileReader();
      reader.onload = function(){
        selectedAudioDataUrl = String(reader.result || "");
        if (audioHint) audioHint.textContent = "Audio cargado: " + f.name;
        if (audioPreview) audioPreview.style.backgroundImage = "radial-gradient(circle at 30% 40%, rgba(120,255,180,.15), transparent 55%), radial-gradient(circle at 70% 70%, rgba(139,0,0,.14), transparent 62%)";
      };
      reader.readAsDataURL(f);
    });
  }

  var gallery = $("gallery");
  var btnGaleriaBack = $("btn-galeria-back");
  var gallerySize = $("gallery-size");
  var galleryFit = $("gallery-fit");
  var editMode = { id: 0, original: null };
  var currentFilter = "all";

  function getImagePrefs(id){
    try{
      var raw = localStorage.getItem("lm.img." + id);
      if (!raw) return { fit:"cover", focus:"center" };
      var obj = JSON.parse(raw);
      return {
        fit: (obj.fit === "contain" ? "contain" : "cover"),
        focus: (obj.focus === "top" || obj.focus === "bottom") ? obj.focus : "center"
      };
    }catch(_){
      return { fit:"cover", focus:"center" };
    }
  }
  function saveImagePrefs(id, fit, focus){
    try{
      localStorage.setItem("lm.img." + id, JSON.stringify({
        fit: fit || "cover",
        focus: focus || "center"
      }));
    }catch(_){}
  }

  function applyGalleryPrefs(){
    if (!gallery) return;
    var size = localStorage.getItem("lm.gallery.size") || "m";
    var fit = localStorage.getItem("lm.gallery.fit") || "cover";
    gallery.classList.remove("gallery--s","gallery--m","gallery--l","fit--contain");
    gallery.classList.add("gallery--" + (size === "s" ? "s" : size === "l" ? "l" : "m"));
    if (fit === "contain") gallery.classList.add("fit--contain");
    if (gallerySize) gallerySize.value = size;
    if (galleryFit) galleryFit.value = fit;
  }
  if (gallerySize){
    gallerySize.addEventListener("change", function(){
      localStorage.setItem("lm.gallery.size", gallerySize.value);
      applyGalleryPrefs();
    });
  }
  if (galleryFit){
    galleryFit.addEventListener("change", function(){
      localStorage.setItem("lm.gallery.fit", galleryFit.value);
      applyGalleryPrefs();
    });
  }
  applyGalleryPrefs();

  async function apiEditar(payload){
    var res = await fetch("/api/catalogo/editar", {
      method:"POST",
      headers: Object.assign({ "Content-Type":"application/json" }, authHeaders()),
      body: JSON.stringify(Object.assign({ id: editMode.id }, payload))
    });
    var data;
    try { data = await res.json(); } catch (_) { data = { ok:false }; }
    if (!res.ok) throw new Error(data && data.error ? data.error : "editar fail");
    return data;
  }
  async function apiEliminar(id){
    var res = await fetch("/api/catalogo/eliminar", {
      method:"POST",
      headers: Object.assign({ "Content-Type":"application/json" }, authHeaders()),
      body: JSON.stringify({ id: id })
    });
    var data;
    try { data = await res.json(); } catch (_) { data = { ok:false }; }
    if (!res.ok) throw new Error(data && data.error ? data.error : "eliminar fail");
    return data;
  }
  if (btnGaleriaBack){
    btnGaleriaBack.addEventListener("click", function(){
      setView(viewDash);
      updateActiveNavLink("");
    });
  }

  function escapeHtml(s){
    return String(s||"")
      .replace(/&/g,"&amp;")
      .replace(/</g,"&lt;")
      .replace(/>/g,"&gt;")
      .replace(/\"/g,"&quot;");
  }

  function cardHtml(item){
    var meta = (item.anio||"") + " • " + (item.genero||"");
    var styleAttr = "";
    if (item.posterDataUrl){
      styleAttr = " style=\"background-image:url('" + String(item.posterDataUrl).replace(/'/g, "%27") + "');\"";
    }

    var prom = 0;
    if (item.promedio) {
      prom = Math.round(item.promedio);
    }
    var stars = "";
    for (var s = 1; s <= 5; s++) {
      stars += s <= prom ? "★" : "☆";
    }

    return (
      "<div class=\"poster-card__thumb\" aria-hidden=\"true\"" + styleAttr + "></div>" +
      "<div class=\"poster-card__overlay\">" +
        "<div class=\"poster-card__title\">" + escapeHtml(item.titulo||"") + "</div>" +
        "<div style=\"color: var(--rating-color); font-size:12px; margin-bottom:4px;\">" + stars + "</div>" +
        "<div class=\"poster-card__meta\">" + escapeHtml(item.tipo||"") + " • " + escapeHtml(meta) + "</div>" +
        "<div class=\"poster-card__actions\">" +
          "<button class=\"mini-btn\" data-action=\"publish\" type=\"button\">Compartir</button>" +
          "<button class=\"mini-btn\" data-action=\"edit\" type=\"button\">Editar</button>" +
          "<button class=\"mini-btn mini-btn--danger\" data-action=\"delete\" type=\"button\">Eliminar</button>" +
        "</div>" +
      "</div>"
    );
  }

  /* RENDER GALLERY CON FILTROS Y CONTADOR */
  async function renderGallery(){
    if (!gallery) return;
    applyGalleryPrefs();

    gallery.innerHTML = "<div class=\"skeleton-card\"></div>".repeat(6);
    
    try{
      await syncForumData();
      var data = await apiCatalogo();
      var items = (data && data.items) || [];
      
      var filtered = items;
      if (currentFilter !== "all") {
        filtered = items.filter(function(item) {
          var tNormal = String(item.tipo).toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");
          var filterNormal = currentFilter.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");
          return tNormal === filterNormal;
        });
      }

      var counterEl = $("gallery-counter");
      if (counterEl) {
        counterEl.textContent = filtered.length + " títulos en tu colección";
      }

      gallery.innerHTML = "";

      if (!filtered.length){
        var placeholder = document.createElement("button");
        placeholder.type = "button";
        placeholder.className = "poster-card poster-card-placeholder";
        placeholder.innerHTML = 
          "<div class=\"placeholder-icon\">+</div>" +
          "<div style=\"font-weight:700\">Añadir contenido</div>" +
          "<div style=\"font-size:0.8rem; margin-top:4px;\">No hay títulos en esta sección</div>";
        placeholder.onclick = function() {
          setView(viewRegistro);
          updateActiveNavLink("btn-registro");
        };
        gallery.appendChild(placeholder);
        return;
      }

      filtered.forEach(function(item){
        var btn = document.createElement("button");
        btn.type = "button";
        btn.className = "poster-card";
        btn.innerHTML = cardHtml(item);
        btn.addEventListener("click", function(e){
          var target = e.target;
          var action = target && target.getAttribute ? target.getAttribute("data-action") : null;
          if (action === "edit"){
            e.preventDefault(); e.stopPropagation();
            startEdit(item); return;
          }
          if (action === "delete"){
            e.preventDefault(); e.stopPropagation();
            confirmDelete(item); return;
          }
          if (action === "publish"){
            e.preventDefault(); e.stopPropagation();
            publishToForum(item); return;
          }
          openPoster(item);
        });
        gallery.appendChild(btn);
      });
    }catch(_){
      gallery.innerHTML = "<p class=\"mono\" style=\"color:rgba(209,209,209,.65)\">No se pudo cargar el catálogo. (¿Servidor Java corriendo?)</p>";
    }
  }

  /* ASIGNAR HANDLERS A FILTROS CHIPS */
  var filterChips = document.querySelectorAll(".filter-chip:not(.btn-export)");
  filterChips.forEach(function(chip) {
    chip.addEventListener("click", function() {
      filterChips.forEach(function(c) { c.classList.remove("active"); });
      chip.classList.add("active");
      currentFilter = chip.getAttribute("data-filter");
      renderGallery();
    });
  });

  /* EXPORTAR CATÁLOGO CSV */
  var btnExport = $("btn-export-csv");
  if (btnExport) {
    btnExport.addEventListener("click", async function(e) {
      e.stopPropagation();
      try {
        var res = await fetch("/api/catalogo/exportar", { headers: authHeaders() });
        var data = await res.json();
        if (data && data.ok) {
          showToast(data.message || "Exportado con éxito!");
        } else {
          showToast(data.error || "No se pudo exportar", "error");
        }
      } catch (e) {
        showToast("Error de comunicación", "error");
      }
    });
  }

  /* ================= CORRECCIÓN 5: SECCIÓN MI LISTA VIEW Y DELETION ================= */
  var mylistGallery = $("milista-gallery");
  var mylistCounter = $("milista-counter");

  async function renderMyList() {
    if (!mylistGallery) return;
    mylistGallery.innerHTML = "<div class=\"skeleton-card\"></div>".repeat(3);
    
    try {
      var ids = await apiListGet();
      var data = await apiCatalogo();
      var items = (data && data.items) || [];
      
      var filtered = items.filter(function(item) {
        return ids.indexOf(Number(item.id)) !== -1;
      });

      if (mylistCounter) {
        mylistCounter.textContent = filtered.length + " títulos guardados";
      }

      mylistGallery.innerHTML = "";

      if (!filtered.length) {
        mylistGallery.innerHTML = `
          <div class="empty-list-state" style="grid-column: 1/-1; text-align: center; padding: 60px 20px; color: var(--fg-mut);">
            <div style="font-size: 3.5rem; margin-bottom: 16px;">🔖</div>
            <h3 style="font-size: 1.5rem; color: #fff; margin-bottom: 8px;">Tu lista está vacía</h3>
            <p style="font-size: 0.95rem;">Añade títulos con el botón + Mi Lista</p>
          </div>
        `;
        return;
      }

      filtered.forEach(function(item) {
        var wrapper = document.createElement("div");
        wrapper.className = "poster-card-wrapper";
        wrapper.style.position = "relative";
        
        var card = document.createElement("button");
        card.type = "button";
        card.className = "poster-card";
        card.style.width = "100%";
        card.style.height = "100%";
        card.innerHTML = cardHtml(item);
        
        card.addEventListener("click", function(e) {
          var target = e.target;
          var action = target && target.getAttribute ? target.getAttribute("data-action") : null;
          if (action === "edit"){
            e.preventDefault(); e.stopPropagation();
            startEdit(item); return;
          }
          if (action === "delete"){
            e.preventDefault(); e.stopPropagation();
            confirmDelete(item); return;
          }
          if (action === "publish"){
            e.preventDefault(); e.stopPropagation();
            publishToForum(item); return;
          }
          openPoster(item);
        });

        // Hover deletion cross button
        var delBtn = document.createElement("button");
        delBtn.type = "button";
        delBtn.className = "milista-delete-card-btn";
        delBtn.innerHTML = "×";
        delBtn.title = "Eliminar de Mi Lista";
        delBtn.addEventListener("click", async function(e) {
          e.stopPropagation();
          var ok = await apiListRemove(item.id);
          if (ok) {
            showToast("Eliminado de Mi Lista");
            renderMyList();
          }
        });

        wrapper.appendChild(card);
        wrapper.appendChild(delBtn);
        mylistGallery.appendChild(wrapper);
      });
    } catch(e) {
      mylistGallery.innerHTML = "<p class=\"mono\" style=\"color:var(--accent)\">Error al cargar Mi Lista</p>";
    }
  }

  async function renderForum(){
    if (!forum) return;
    forum.innerHTML = "";
    try{
      var data = await apiForumList();
      globalForumData = data.posts || [];

      await renderTop10();

      var posts = (data && data.posts) || [];
      if (!posts.length){
        forum.innerHTML = "<p class=\"mono\" style=\"color:rgba(209,209,209,.65)\">Aún no hay galerías publicadas en el foro.</p>";
        return;
      }
      posts.slice().reverse().forEach(function(item){
        var post = document.createElement("article");
        post.className = "forum-post";
        post.id = "forum-post-" + item.id;
        var img = item.posterDataUrl ? ("background-image:linear-gradient(180deg, rgba(0,0,0,.12), rgba(0,0,0,.72)), url('" + String(item.posterDataUrl).replace(/'/g, "%27") + "');") : "";
        var isOwner = session.username && (item.usuario || "").toLowerCase() === session.username.toLowerCase();
        var deleteBtn = isOwner ? "<button class=\"mini-btn mini-btn--danger\" type=\"button\" data-action=\"delete-post\" title=\"Eliminar mi publicación\" style=\"margin-left:auto; padding:6px 10px\">Eliminar</button>" : "";

        post.innerHTML =
          "<div class=\"forum-post__media\" style=\"cursor:pointer;" + img + "\" title=\"Clic para ver detalles\"></div>" +
          "<div class=\"forum-post__body\">" +
            "<div style=\"display:flex; align-items:flex-start; justify-content:space-between\">" +
              "<h3 class=\"forum-post__title\">" + escapeHtml(item.titulo || "") + "</h3>" +
              deleteBtn +
            "</div>" +
            "<p class=\"forum-post__meta mono\">" + escapeHtml((item.tipo || "") + " • " + (item.anio || "") + " • " + (item.genero || "")) + " — subido por " + escapeHtml(item.usuario || "") + "</p>" +
            "<p class=\"forum-post__desc\">" + escapeHtml(item.descripcion || "") + "</p>" +
            "<div class=\"forum-post__opinions\">" +
              "<div class=\"forum-post__form\" style=\"align-items:center; gap:8px\">" +
                "<button class=\"mini-btn\" type=\"button\" data-action=\"like\" title=\"Aprobar\" style=\"display:flex; align-items:center; gap:4px; padding:6px 10px\"><span style=\"font-size:1.1em; pointer-events:none\">▲</span> <span class=\"vote-count\" data-likes>" + (item.likes||0) + "</span></button>" +
                "<button class=\"mini-btn\" type=\"button\" data-action=\"dislike\" title=\"Rechazar\" style=\"display:flex; align-items:center; gap:4px; padding:6px 10px\"><span style=\"font-size:1.1em; pointer-events:none\">▼</span> <span class=\"vote-count\" data-dislikes>" + (item.dislikes||0) + "</span></button>" +
                "<textarea class=\"forum-post__input\" rows=\"1\" maxlength=\"240\" placeholder=\"Escribe tu opinión...\" style=\"flex:1; margin-left:10px\"></textarea>" +
                "<button class=\"mini-btn\" type=\"button\" data-action=\"send\">Publicar</button>" +
              "</div>" +
              "<div class=\"forum-post__comments\" data-comments></div>" +
            "</div>" +
          "</div>";
        
        post.onclick = async function(e){
          var target = e.target;
          var action = target && target.getAttribute ? target.getAttribute("data-action") : null;
          
          if (action === "delete-post") {
              if (confirm("¿Seguro que quieres eliminar esta publicación del foro?")) {
                  try {
                      await apiForumDelete(item.id);
                      flashGlitch();
                      showToast("Publicación del foro eliminada");
                      await renderForum();
                  } catch(e) { alert("Error al eliminar: " + e.message); }
              }
              return;
          }
          
          if (target.classList.contains("forum-post__media")){
             openPoster({
               id: item.multimediaId || item.id,
               titulo: item.titulo,
               tipo: item.tipo,
               anio: item.anio,
               genero: item.genero,
               descripcion: item.descripcion,
               posterDataUrl: item.posterDataUrl,
               audioDataUrl: ""
             });
          }
        };
        forum.appendChild(post);

        var btnLike = post.querySelector("[data-action='like']");
        var btnDislike = post.querySelector("[data-action='dislike']");
        var numLikes = post.querySelector("[data-likes]");
        var numDislikes = post.querySelector("[data-dislikes]");

        btnLike.addEventListener("click", function(){
           apiForumVote(item.id, 1).then((res) => {
              item.likes = res.likes;
              item.dislikes = res.dislikes;
              numLikes.textContent = res.likes;
              numDislikes.textContent = res.dislikes;
              syncForumData().then(() => {
                updateGlobalMatchScores();
                renderTop10();
              });
           }).catch(function(e) {
              console.error(e);
              flashGlitch();
           });
        });
        btnDislike.addEventListener("click", function(){
           apiForumVote(item.id, -1).then((res) => {
              item.likes = res.likes;
              item.dislikes = res.dislikes;
              numLikes.textContent = res.likes;
              numDislikes.textContent = res.dislikes;
              syncForumData().then(() => {
                updateGlobalMatchScores();
                renderTop10();
              });
           }).catch(function(e) {
              console.error(e);
              flashGlitch();
           });
        });

        var commentsEl = post.querySelector("[data-comments]");
        var input = post.querySelector(".forum-post__input");
        var send = post.querySelector("[data-action='send']");
        var postId = item.id;

        function renderComments(list){
          commentsEl.innerHTML = "";
          if (!list || !list.length){
            commentsEl.innerHTML = "<div class=\"mono\" style=\"color:rgba(209,209,209,.55);font-size:12px\">Sin opiniones todavía.</div>";
            return;
          }
          list.slice().reverse().forEach(function(c){
            var el = document.createElement("div");
            el.className = "forum-comment";
            el.innerHTML =
              "<div class=\"forum-comment__meta mono\">" + escapeHtml(c.usuario || "") + " • " + escapeHtml(c.creadoEn || "") + "</div>" +
              "<div class=\"forum-comment__text\">" + escapeHtml(c.texto || "") + "</div>";
            commentsEl.appendChild(el);
          });
        }

        apiForumComments(postId).then(function(r){
          renderComments((r && r.comments) || []);
        }).catch(function(){
          renderComments([]);
        });

        if (send){
          send.addEventListener("click", function(){
            var txt = (input.value || "").trim();
            if (!txt) return;
            send.disabled = true;
            apiForumComment(postId, txt).then(function(){
              input.value = "";
              return apiForumComments(postId);
            }).then(function(r){
              renderComments((r && r.comments) || []);
              showToast("Opinión agregada!");
            }).catch(function(err){
              flashGlitch();
              alert("No se pudo publicar opinión: " + (err && err.message ? err.message : "error"));
            }).finally(function(){
              send.disabled = false;
            });
          });
        }
      });
    }catch(_){
      forum.innerHTML = "<p class=\"mono\" style=\"color:rgba(209,209,209,.65)\">No se pudo cargar el foro. (¿Servidor Java corriendo?)</p>";
    }
  }

  var editSourceView = null;

  function startEdit(item){
    var currentActive = [viewLogin, viewUserRegister, viewDash, viewRegistro, viewGaleria, viewForo, viewMyList].find(function(v){
      return v && v.classList.contains("view--active");
    });
    editSourceView = currentActive || viewGaleria;

    editMode.id = Number(item.id || 0);
    editMode.original = item;
    if (regTipo) regTipo.value = item.tipo || "Pelicula";
    if (regTitulo) regTitulo.value = item.titulo || "";
    if (regGenero) regGenero.value = item.genero || "";
    if (regDesc) regDesc.value = item.descripcion || "";
    if (regAnio) regAnio.value = String(item.anio || new Date().getFullYear());
    if (regDur) regDur.value = String(item.duracionMin || 92);
    if (regTemps) regTemps.value = String(item.temporadas || 1);
    if (regEpis) regEpis.value = String(item.episodios || 12);
    if (regStreamUrl) regStreamUrl.value = item.streamingUrl || "";
    selectedPosterDataUrl = item.posterDataUrl || "";
    selectedAudioDataUrl = item.audioDataUrl || "";
    if (posterPreview) {
      posterPreview.style.backgroundImage = selectedPosterDataUrl ? ("url('" + selectedPosterDataUrl.replace(/'/g, "%27") + "')") : "";
      applyPosterPreviewFit();
    }
    var prefs = getImagePrefs(item.id || 0);
    if (posterFit) posterFit.value = prefs.fit;
    if (posterFocus) posterFocus.value = prefs.focus;
    if (audioHint) audioHint.textContent = selectedAudioDataUrl ? "Audio cargado (existente)" : "Sin audio cargado.";
    if (audioPreview) audioPreview.style.backgroundImage = selectedAudioDataUrl ? "radial-gradient(circle at 30% 40%, rgba(120,255,180,.15), transparent 55%), radial-gradient(circle at 70% 70%, rgba(139,0,0,.14), transparent 62%)" : "";
    syncTipo();
    regStatus.textContent = "Editando ID #" + editMode.id + ". Guarda para actualizar.";
    setView(viewRegistro);
    updateActiveNavLink("btn-registro");
  }

  async function confirmDelete(item){
    var ok = confirm("¿Eliminar \"" + (item.titulo || "") + "\" del catálogo?");
    if (!ok) return;
    try{
      await apiEliminar(Number(item.id || 0));
      flashGlitch();
      showToast("¡Título eliminado del catálogo!");
      if (viewMyList && viewMyList.classList.contains("view--active")) {
        await renderMyList();
      } else {
        await renderGallery();
      }
    }catch(err){
      flashGlitch();
      alert("No se pudo eliminar: " + (err && err.message ? err.message : "error"));
    }
  }

  async function publishToForum(item){
    try{
      await apiForumPublish(Number(item.id || 0));
      flashGlitch();
      showToast("Compartido en el foro con éxito!");
    }catch(err){
      flashGlitch();
      showToast("No se pudo publicar en el foro", "error");
    }
  }

  function showFoundFootage(item){
    var foundFootage = $("found-footage");
    if (!foundFootage) return;
    var msg = item && item.metraje ? item.metraje : "Señal inestable…";
    foundFootage.textContent = "METRAJE ENCONTRADO: " + msg;
    foundFootage.hidden = false;
    setTimeout(function(){ if (foundFootage) foundFootage.hidden = true; }, 3500);
  }

  /* ================= Póster (linterna + nodos) ================= */
  var nodes = Array.prototype.slice.call(document.querySelectorAll(".node"));
  var panel = $("node-panel");
  var panelCode = $("node-panel-code");
  var panelClose = $("node-panel-close");
  var btnBack = $("btn-back");

  function setFlash(x,y){
    document.documentElement.style.setProperty("--mx", x + "px");
    document.documentElement.style.setProperty("--my", y + "px");
  }
  function updateLighting(x,y){
    var radius = 170;
    nodes.forEach(function(n){
      var r = n.getBoundingClientRect();
      var cx = r.left + r.width/2;
      var cy = r.top + r.height/2;
      var dx = x - cx, dy = y - cy;
      var dist = Math.sqrt(dx*dx + dy*dy);
      if (dist <= radius) n.classList.add("is-lit");
      else n.classList.remove("is-lit");
    });
  }
  document.addEventListener("mousemove", function(e){
    if (!viewPoster.classList.contains("view--active")) return;
    setFlash(e.clientX, e.clientY);
    updateLighting(e.clientX, e.clientY);
  });
  function showPanel(text){
    panelCode.textContent = text;
    panel.hidden = false;
  }
  function hidePanel(){ panel.hidden = true; }
  if (panelClose) panelClose.addEventListener("click", hidePanel);

  nodes.forEach(function(btn){
    btn.addEventListener("click", async function(){
      var key = btn.getAttribute("data-node");
      try{
        var info = await apiInfo();
        var sujeto = (info.sujetos || []).find(function(s){ return (s.nombre||"").toLowerCase().includes(key); });
        var reaccion = sujeto ? sujeto.reaccion : "señal incompleta";
        var estado = sujeto ? sujeto.estadoMarcado : "MARCA_ACTIVA";
        showPanel(
          "// Bloque 7 — Polimorfismo\n" +
          "persona.reaccionarASuceso();\n" +
          "// → \"" + reaccion + "\"\n\n" +
          "// Bloque 9 (protected) — Estado marcado\n" +
          "protected String estadoMarcado;\n" +
          "// → \"" + estado + "\"\n\n" +
          "// Bloque 10 — Investigable\n" +
          "archivo.analizarEvidencia();\n" +
          "// → \"" + info.evidencia + "\"\n\n" +
          "// Bloque 4/11 — Catálogo\n" +
          "CatalogoPeliculas items: List<Multimedia>;\n"
        );
      }catch(_){
        flashGlitch();
        showPanel("// Servidor no responde.\n// Inicia Java y vuelve a intentarlo.");
      }
    });
  });

  if (btnBack){
    btnBack.addEventListener("click", function(){
      hidePanel();
      if (typeof closeModal === "function") closeModal();
      
      if (lastView) {
        setView(lastView);
        if (lastView === viewGaleria) renderGallery();
        if (lastView === viewForo) renderForum();
        if (lastView === viewMyList) renderMyList();
      } else {
        setView(viewDash);
      }
    });
  }

  /* ================= CORRECCIÓN 3: DROPDOWN CLIC TOGGLE & OUTSIDE CLICK ================= */
  var profileContainer = $("nav-profile-container");
  var avatarBtn = $("nav-avatar");
  var dropdown = $("nav-dropdown");
  if (profileContainer && avatarBtn && dropdown) {
    avatarBtn.setAttribute("aria-expanded", "false");
    profileContainer.addEventListener("click", function(e) {
      e.stopPropagation();
      var active = dropdown.classList.contains("active");
      if (active) {
        dropdown.classList.remove("active");
        avatarBtn.setAttribute("aria-expanded", "false");
      } else {
        dropdown.classList.add("active");
        avatarBtn.setAttribute("aria-expanded", "true");
      }
    });
    document.addEventListener("click", function(e) {
      if (!profileContainer.contains(e.target)) {
        dropdown.classList.remove("active");
        avatarBtn.setAttribute("aria-expanded", "false");
      }
    });
  }

  /* ================= CORRECCIÓN 6: PASSWORD TOGGLE BOTONES OJO ================= */
  function setupPasswordToggle(inputEl, btnEl) {
    if (!inputEl || !btnEl) return;
    
    var eyeOpenSvg = `
      <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
    `;
    var eyeClosedSvg = `
      <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>
    `;

    btnEl.addEventListener("click", function(e) {
      e.stopPropagation();
      var isPass = inputEl.type === "password";
      inputEl.type = isPass ? "text" : "password";
      btnEl.innerHTML = isPass ? eyeOpenSvg : eyeClosedSvg;
      btnEl.style.color = isPass ? "#fff" : "rgba(160, 160, 176, 0.6)";
    });
  }
  setupPasswordToggle($("login-password"), $("login-toggle-pass"));
  setupPasswordToggle($("reg-pass"), $("reg-toggle-pass"));
  setupPasswordToggle($("reg-pass2"), $("reg2-toggle-pass"));

  window.addEventListener("load", function(){
    setFlash(window.innerWidth/2, window.innerHeight/2);
    initParticles();
  });
  setView(viewLogin);
})();
