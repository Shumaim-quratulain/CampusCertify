const CATEGORIES = ['LEARN', 'BUILD', 'SHARE'];
const REQUIRED_POINTS = 6;

const el = (id) => document.getElementById(id);

async function api(path, options) {
  const response = await fetch(`/api${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!response.ok) {
    throw new Error(`${options?.method ?? 'GET'} ${path} failed with ${response.status}`);
  }
  return response.json();
}

function cell(row, text, className) {
  const td = document.createElement('td');
  td.textContent = text;
  if (className) {
    td.className = className;
  }
  row.appendChild(td);
  return td;
}

function renderActivities(activities) {
  const body = el('activityBody');
  body.replaceChildren();
  for (const activity of activities) {
    const row = document.createElement('tr');
    cell(row, activity.id, 'mono');
    cell(row, activity.name);
    const category = cell(row, activity.category);
    category.classList.add('tag', `tag-${activity.category.toLowerCase()}`);
    cell(row, activity.points, 'numeric');
    body.appendChild(row);
  }
}

function renderParticipants(participants) {
  const body = el('participantBody');
  body.replaceChildren();

  participants.forEach((participant) => {
    const row = document.createElement('tr');
    cell(row, participant.id, 'mono');

    const nameInput = document.createElement('input');
    nameInput.value = participant.name;
    nameInput.setAttribute('aria-label', `Name for ${participant.id}`);

    const activityInput = document.createElement('input');
    activityInput.value = participant.completedActivityIds.join(', ');
    activityInput.placeholder = 'none';
    activityInput.setAttribute('aria-label', `Completed activities for ${participant.id}`);

    const save = () => saveParticipant(participant.id, nameInput.value, activityInput.value);
    nameInput.addEventListener('change', save);
    activityInput.addEventListener('change', save);

    const nameCell = document.createElement('td');
    nameCell.appendChild(nameInput);
    row.appendChild(nameCell);

    const activityCell = document.createElement('td');
    activityCell.appendChild(activityInput);
    row.appendChild(activityCell);

    const actionCell = document.createElement('td');
    actionCell.className = 'numeric';
    const remove = document.createElement('button');
    remove.textContent = 'Remove';
    remove.className = 'link-danger';
    remove.addEventListener('click', () => deleteParticipant(participant.id));
    actionCell.appendChild(remove);
    row.appendChild(actionCell);

    body.appendChild(row);
  });
}

function categoryStrip(coveredCategories) {
  const strip = document.createElement('div');
  strip.className = 'strip';
  for (const category of CATEGORIES) {
    const segment = document.createElement('span');
    const covered = coveredCategories.includes(category);
    segment.className = `segment ${covered ? 'covered' : 'missing'} tag-${category.toLowerCase()}`;
    segment.textContent = category.charAt(0);
    segment.title = `${category}: ${covered ? 'covered' : 'missing'}`;
    strip.appendChild(segment);
  }
  return strip;
}

function renderResults(results) {
  const body = el('resultBody');
  body.replaceChildren();

  for (const result of results) {
    const row = document.createElement('tr');
    row.className = result.eligible ? 'row-eligible' : 'row-ineligible';

    const status = cell(row, result.eligible ? 'ELIGIBLE' : 'INELIGIBLE');
    status.classList.add('status', result.eligible ? 'status-yes' : 'status-no');

    cell(row, result.participantId, 'mono');
    cell(row, result.participantName);

    const progress = document.createElement('td');
    progress.appendChild(categoryStrip(result.coveredCategories));
    row.appendChild(progress);

    const points = cell(row, `${result.totalPoints} / ${REQUIRED_POINTS}`, 'numeric');
    points.classList.add(result.totalPoints >= REQUIRED_POINTS ? 'points-ok' : 'points-low');

    const reasons = document.createElement('td');
    if (result.failureReasons.length === 0) {
      reasons.textContent = '—';
      reasons.className = 'muted-text';
    } else {
      const list = document.createElement('ul');
      list.className = 'reasons';
      for (const reason of result.failureReasons) {
        const item = document.createElement('li');
        item.textContent = reason;
        list.appendChild(item);
      }
      reasons.appendChild(list);
    }
    row.appendChild(reasons);

    body.appendChild(row);
  }
}

function renderValidationErrors(errors) {
  const body = el('validationBody');
  body.replaceChildren();
  for (const error of errors) {
    const row = document.createElement('tr');
    cell(row, error.code, 'mono');
    cell(row, error.participantId || '—', 'mono');
    cell(row, error.offendingValue || '—', 'mono');
    body.appendChild(row);
  }
}

/** Single place that decides what is visible, so stale rows and counts cannot survive. */
function renderEvaluation(response) {
  const hasErrors = response.errors.length > 0;

  el('validationPanel').hidden = !hasErrors;
  el('resultsPanel').hidden = hasErrors;
  el('summaryPanel').hidden = hasErrors;
  el('emptyState').hidden = true;

  if (hasErrors) {
    renderValidationErrors(response.errors);
    el('resultBody').replaceChildren();
    el('eligibleCount').textContent = '0';
    el('ineligibleCount').textContent = '0';
    return;
  }

  renderResults(response.results);
  el('eligibleCount').textContent = response.summary.eligibleCount;
  el('ineligibleCount').textContent = response.summary.ineligibleCount;
}

/** Any edit invalidates the previous evaluation, so results and counts are wiped immediately. */
function clearEvaluation() {
  el('validationPanel').hidden = true;
  el('resultsPanel').hidden = true;
  el('summaryPanel').hidden = true;
  el('emptyState').hidden = false;
  el('resultBody').replaceChildren();
  el('validationBody').replaceChildren();
  el('eligibleCount').textContent = '0';
  el('ineligibleCount').textContent = '0';
}

function parseActivityTokens(text) {
  return text.split(',').map((token) => token.trim()).filter((token) => token.length > 0);
}

async function saveParticipant(id, name, activityText) {
  const participants = await api(`/participants/${encodeURIComponent(id)}`, {
    method: 'PUT',
    body: JSON.stringify({ id, name, completedActivityIds: parseActivityTokens(activityText) }),
  });
  renderParticipants(participants);
  clearEvaluation();
}

async function deleteParticipant(id) {
  const participants = await api(`/participants/${encodeURIComponent(id)}`, { method: 'DELETE' });
  renderParticipants(participants);
  clearEvaluation();
}

async function addParticipant() {
  const id = el('newId').value;
  const name = el('newName').value;
  const activities = parseActivityTokens(el('newActivities').value);

  const participants = await api('/participants', {
    method: 'POST',
    body: JSON.stringify({ id, name, completedActivityIds: activities }),
  });

  el('newId').value = '';
  el('newName').value = '';
  el('newActivities').value = '';
  renderParticipants(participants);
  clearEvaluation();
}

async function evaluate() {
  renderEvaluation(await api('/evaluate', { method: 'POST' }));
}

async function reset() {
  renderParticipants(await api('/reset', { method: 'POST' }));
  clearEvaluation();
}

async function init() {
  renderActivities(await api('/activities'));
  renderParticipants(await api('/participants'));
  clearEvaluation();

  el('evaluateBtn').addEventListener('click', evaluate);
  el('resetBtn').addEventListener('click', reset);
  el('addBtn').addEventListener('click', addParticipant);
}

init();
