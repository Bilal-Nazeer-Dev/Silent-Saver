// ============================================================
//  PIZZA PALACE — Main JavaScript
// ============================================================

/* ---------- Loader ---------- */
window.addEventListener('load', () => {
  setTimeout(() => {
    document.getElementById('loader').classList.add('hidden');
  }, 1400);
});

/* ---------- Nav scroll effect ---------- */
const nav = document.querySelector('nav');
window.addEventListener('scroll', () => {
  nav.classList.toggle('scrolled', window.scrollY > 40);
});

/* ---------- Hamburger ---------- */
const hamburger = document.querySelector('.hamburger');
const navLinks  = document.querySelector('.nav-links');
hamburger.addEventListener('click', () => {
  hamburger.classList.toggle('open');
  navLinks.classList.toggle('mobile-open');
});
// close when a link is clicked
navLinks.querySelectorAll('a').forEach(link => {
  link.addEventListener('click', () => {
    hamburger.classList.remove('open');
    navLinks.classList.remove('mobile-open');
  });
});

/* ---------- Scroll-reveal ---------- */
const revealEls = document.querySelectorAll('.reveal');
const observer  = new IntersectionObserver((entries) => {
  entries.forEach(e => {
    if (e.isIntersecting) {
      e.target.classList.add('visible');
      observer.unobserve(e.target);
    }
  });
}, { threshold: 0.12 });
revealEls.forEach(el => observer.observe(el));

/* ---------- Animated stat counters ---------- */
function animateCounter(el, target, duration = 1800) {
  let start = null;
  const step = (timestamp) => {
    if (!start) start = timestamp;
    const progress = Math.min((timestamp - start) / duration, 1);
    // ease-out
    const eased = 1 - Math.pow(1 - progress, 3);
    el.textContent = Math.floor(eased * target).toLocaleString();
    if (progress < 1) requestAnimationFrame(step);
  };
  requestAnimationFrame(step);
}

const counterObserver = new IntersectionObserver((entries) => {
  entries.forEach(e => {
    if (e.isIntersecting) {
      const el     = e.target;
      const target = parseInt(el.dataset.target, 10);
      animateCounter(el, target);
      counterObserver.unobserve(el);
    }
  });
}, { threshold: 0.5 });

document.querySelectorAll('.counter').forEach(el => counterObserver.observe(el));

/* ---------- Menu filter ---------- */
const filterBtns = document.querySelectorAll('.filter-btn');
const menuCards  = document.querySelectorAll('.menu-card');

filterBtns.forEach(btn => {
  btn.addEventListener('click', () => {
    filterBtns.forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    const filter = btn.dataset.filter;

    menuCards.forEach(card => {
      if (filter === 'all' || card.dataset.cat === filter) {
        card.classList.remove('hidden');
        card.style.animation = 'none';
        requestAnimationFrame(() => {
          card.style.animation = 'cardPop .35s ease both';
        });
      } else {
        card.classList.add('hidden');
      }
    });
  });
});

// add keyframe dynamically for card pop
const styleSheet = document.styleSheets[0];
styleSheet.insertRule(`@keyframes cardPop {
  from { opacity:0; transform:scale(.9) translateY(10px); }
  to   { opacity:1; transform:none; }
}`, styleSheet.cssRules.length);

/* ---------- Cart ---------- */
const cartItems   = [];
const cartPanel   = document.getElementById('cart-panel');
const cartCount   = document.getElementById('cart-count');
const cartList    = document.getElementById('cart-list');
const cartTotalEl = document.getElementById('cart-total');
const cartFloat   = document.getElementById('cart-floating');

function updateCartUI() {
  cartCount.textContent = cartItems.length;

  if (cartItems.length === 0) {
    cartList.innerHTML = '<p class="cart-empty">🍕  Your cart is empty.<br>Add some delicious items!</p>';
    cartTotalEl.textContent = '$0.00';
    return;
  }

  cartList.innerHTML = cartItems.map((item, i) => `
    <div class="cart-item">
      <span class="cart-item-emoji">${item.emoji}</span>
      <div class="cart-item-info">
        <h4>${item.name}</h4>
        <span>${item.desc.substring(0, 40)}…</span>
      </div>
      <span class="cart-item-price">$${item.price.toFixed(2)}</span>
      <button class="cart-item-remove" onclick="removeItem(${i})">✕</button>
    </div>
  `).join('');

  const total = cartItems.reduce((s, i) => s + i.price, 0);
  cartTotalEl.textContent = `$${total.toFixed(2)}`;
}

window.removeItem = function(index) {
  cartItems.splice(index, 1);
  updateCartUI();
};

document.querySelectorAll('.add-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    const card  = btn.closest('.menu-card');
    cartItems.push({
      name:  card.querySelector('h3').textContent,
      desc:  card.querySelector('p').textContent,
      price: parseFloat(card.querySelector('.card-price').textContent.replace('$', '')),
      emoji: card.querySelector('.card-img').textContent.trim(),
    });
    updateCartUI();
    // animate cart button
    cartFloat.style.animation = 'none';
    requestAnimationFrame(() => { cartFloat.style.animation = 'cartBounce .4s ease'; });
  });
});

cartFloat.addEventListener('click', () => cartPanel.classList.add('open'));
document.getElementById('cart-close').addEventListener('click', () => cartPanel.classList.remove('open'));

/* ---------- Reservation form ---------- */
document.getElementById('reservation-form').addEventListener('submit', (e) => {
  e.preventDefault();
  showToast('🎉 Table reserved! See you soon.');
  e.target.reset();
});

function showToast(msg) {
  const toast = document.getElementById('toast');
  toast.textContent = msg;
  toast.classList.add('show');
  setTimeout(() => toast.classList.remove('show'), 3500);
}

/* ---------- Checkout button ---------- */
document.getElementById('checkout-btn').addEventListener('click', () => {
  if (cartItems.length === 0) {
    showToast('🛒 Add items to your cart first!');
    return;
  }
  cartItems.length = 0;
  updateCartUI();
  cartPanel.classList.remove('open');
  showToast('✅ Order placed! Estimated delivery: 30 mins 🚀');
});
