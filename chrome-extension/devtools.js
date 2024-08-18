function debounce(fn, delay) {
  let timer;
  return function () {
    clearTimeout(timer);
    timer = setTimeout(() => {
      fn.apply(this, arguments);
    }, delay);
  };
}


function createUrl(file, line) {
  return [
    'http://localhost:61111',
    '/open',
    '?file=' + encodeURIComponent(file),
    '&line=' + encodeURIComponent(line)
  ].join('');
}

// nav
const requestsEle = document.getElementById('requests');
requestsEle.addEventListener('click', function (e) {
  if (e.target.tagName === 'A') {
    e.preventDefault();
    if(e.target.getAttribute('disabled')) {
      return;
    }
    e.target.setAttribute('disabled', 'disabled');
    fetch(e.target.href)
      .finally(() => {
        e.target.removeAttribute('disabled');
      });
  }
});

// clear
document.getElementById('clear').addEventListener('click', function () {
  requestsEle.innerHTML = '';
  updateCount();
});

// toggle
const toggleEle = document.getElementById('toggle');
toggleEle.addEventListener('click', function () {
  const isStop = this.getAttribute('data-stop') === 'true';
  this.setAttribute('data-stop', isStop ? 'false' : 'true');
  this.innerText = isStop ? 'Stop' : 'Start';
});

// search
const searchEle = document.getElementById('search');
searchEle.addEventListener('input', debounce(function () {
  const keyword = this.value.trim().toLowerCase();
  requestsEle.querySelectorAll('li').forEach(li => {
    const path = li.querySelector('.path').innerText.toLowerCase();
    const content = li.querySelector('pre').innerText.toLowerCase();
    const match = keyword.startsWith('!')
      ? path.includes(keyword.slice(1))
      : path.includes(keyword) || content.includes(keyword);
    if (match) {
      li.style.display = 'list-item';
    } else {
      li.style.display = 'none';
    }
  });
  updateCount();
}, 300));

// responseBodyVisible
const responseBodyVisibleEle = document.getElementById('responseBodyVisible');
responseBodyVisibleEle.addEventListener('change', function () {
  requestsEle.querySelectorAll('pre').forEach(pre => {
    pre.style.display = this.checked ? 'block' : 'none';
  });
  updateCount();
});

// getToggle, postToggle
const getToggleEle = document.getElementById('getToggle');
getToggleEle.addEventListener('change', function () {
  requestsEle.querySelectorAll('.method.get').forEach(span => {
    span.parentElement.style.display = this.checked ? 'list-item' : 'none';
  });
  updateCount();
});
const postToggleEle = document.getElementById('postToggle');
postToggleEle.addEventListener('change', function () {
  requestsEle.querySelectorAll('.method.post').forEach(span => {
    span.parentElement.style.display = this.checked ? 'list-item' : 'none';
  });
  updateCount();
});

// count
const countEle = document.getElementById('count');

function updateCount() {
  const liEle = requestsEle.querySelectorAll('li');
  countEle.innerText = `${[...liEle].filter(li => li.style.display !== 'none').length}/${liEle.length}`;
}

function calcResponseBodyVisible() {
  return responseBodyVisibleEle.checked ? 'block' : 'none';
}

function calcVisible(method, path, content) {
  const getToggle = getToggleEle.checked;
  const postToggle = postToggleEle.checked;
  const keyword = searchEle.value.trim().toLowerCase();

  const pathMatch = keyword.startsWith('!')
    ? !path.includes(keyword.slice(1))
    : path.includes(keyword);
  const contentMatch = keyword.startsWith('!')
    ? !content.includes(keyword.slice(1))
    : content.includes(keyword);

  return (
    (method === 'GET' && getToggle) || (method === 'POST' && postToggle)
  ) && (pathMatch || contentMatch) ? 'list-item' : 'none';
}


function createItem(time, method, path, fileUrl, content) {
  const itemEle = document.createElement('li');
  itemEle.style.display = calcVisible(method, path, content);
  itemEle.innerHTML = `<span class="time">${time}</span>
            ${method === 'GET' ? '<span class="method get">GET</span>' : '<span class="method post">POST</span>'}
            <a class="path" href="${fileUrl}">${path}</a>
            <pre style="display: ${calcResponseBodyVisible()}">${content}</pre>`;
  return itemEle;
}

chrome.devtools.network.onRequestFinished.addListener(function (request) {
  const {url, method} = request.request;
  const {headers} = request.response;

  const pathHeader = headers.find(header => header.name.toUpperCase() === 'X-SOURCE-PATH');
  const lineHeader = headers.find(header => header.name.toUpperCase() === 'X-SOURCE-LINE');
  if (pathHeader == null) {
    return;
  }

  const stop = toggleEle.getAttribute('data-stop');
  if (stop === 'true') {
    return;
  }

  request.getContent(body => {
    const openFileUrl = createUrl(pathHeader.value, lineHeader.value);
    requestsEle.prepend(createItem(new Date().toLocaleTimeString(), method, new URL(url).pathname, openFileUrl, body));
    updateCount();
  });
});


chrome.devtools.panels.create("API Requests", "icons/32.png", "devtools.html");
