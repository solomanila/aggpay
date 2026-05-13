<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue';

const props = defineProps({
  start:       { type: String, default: '' },
  end:         { type: String, default: '' },
  placeholder: { type: String, default: '开始日期 ~ 结束日期' },
});
const emit = defineEmits(['update:start', 'update:end']);

const rootEl     = ref(null);
const popupEl    = ref(null);
const open       = ref(false);
const popupStyle = ref({});

const today    = new Date();
const navYear  = ref(today.getFullYear());
const navMonth = ref(today.getMonth()); // 0-indexed

const rightYear  = computed(() => navMonth.value === 11 ? navYear.value + 1 : navYear.value);
const rightMonth = computed(() => (navMonth.value + 1) % 12);

const hoverDate    = ref(null);
const selStart     = ref(null);
const selEnd       = ref(null);
const startTimeStr = ref('00:00:00');
const endTimeStr   = ref('23:59:59');

const DAYS = ['日','一','二','三','四','五','六'];

const pad2 = (n) => String(n).padStart(2, '0');

const fmtDate = (d) => !d ? '' :
  `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;

const parseVal = (val) => {
  if (!val) return null;
  const s = val.replace('T', ' ');
  const [datePart, timePart] = s.split(' ');
  const [y, m, d] = datePart.split('-').map(Number);
  if (!y || !m || !d) return null;
  return { date: new Date(y, m - 1, d), time: timePart || '00:00:00' };
};

const displayText = computed(() => {
  if (!props.start && !props.end) return '';
  const s = props.start ? props.start.replace('T', ' ') : '';
  const e = props.end   ? props.end.replace('T', ' ')   : '';
  if (!s && !e) return '';
  return `${s} ~ ${e}`;
});

// Build calendar cells (includes partial prev/next month days)
const buildCal = (year, month) => {
  const firstDow = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const cells = [];
  // Padding from prev month
  for (let i = firstDow - 1; i >= 0; i--) {
    const d = new Date(year, month, -i);
    cells.push({ date: d, outOfMonth: true });
  }
  // Current month
  for (let d = 1; d <= daysInMonth; d++) {
    cells.push({ date: new Date(year, month, d), outOfMonth: false });
  }
  // Padding to next month
  let n = 1;
  while (cells.length % 7 !== 0) {
    cells.push({ date: new Date(year, month + 1, n++), outOfMonth: true });
  }
  return cells;
};

const leftCells  = computed(() => buildCal(navYear.value,   navMonth.value));
const rightCells = computed(() => buildCal(rightYear.value, rightMonth.value));

const prevYear  = () => navYear.value--;
const prevMonth = () => {
  if (navMonth.value === 0) { navMonth.value = 11; navYear.value--; }
  else navMonth.value--;
};
const nextMonth = () => {
  if (navMonth.value === 11) { navMonth.value = 0; navYear.value++; }
  else navMonth.value++;
};
const nextYear  = () => navYear.value++;

const isSameDay = (a, b) => a && b &&
  a.getFullYear() === b.getFullYear() &&
  a.getMonth()    === b.getMonth()    &&
  a.getDate()     === b.getDate();

const onDayClick = (cell) => {
  if (!cell || cell.outOfMonth) return;
  const date = cell.date;
  if (!selStart.value || (selStart.value && selEnd.value)) {
    selStart.value   = date;
    selEnd.value     = null;
    startTimeStr.value = '00:00:00';
    endTimeStr.value   = '23:59:59';
  } else {
    if (date < selStart.value) {
      selEnd.value   = new Date(selStart.value);
      selStart.value = date;
    } else if (isSameDay(date, selStart.value)) {
      selEnd.value = date;
    } else {
      selEnd.value = date;
    }
  }
};

const effectiveEnd = computed(() => {
  if (selEnd.value) return selEnd.value;
  if (selStart.value && hoverDate.value && hoverDate.value > selStart.value) {
    return hoverDate.value;
  }
  return null;
});

const cellClass = (cell) => {
  const date = cell.date;
  const isStart = !!selStart.value && isSameDay(date, selStart.value);
  const isEnd   = !!selEnd.value   && isSameDay(date, selEnd.value);
  const isTod   = isSameDay(date, today);
  const eff     = effectiveEnd.value;
  const inRange = !!(selStart.value && eff && date > selStart.value && date < eff);
  return {
    'cal-day':      true,
    'out-of-month': cell.outOfMonth,
    'is-start':     isStart && !cell.outOfMonth,
    'is-end':       isEnd   && !cell.outOfMonth,
    'in-range':     inRange && !cell.outOfMonth,
    'is-today':     isTod   && !isStart && !isEnd && !cell.outOfMonth,
    'clickable':    !cell.outOfMonth,
  };
};

const initFromProps = () => {
  const s = parseVal(props.start);
  const e = parseVal(props.end);
  selStart.value     = s?.date ?? null;
  selEnd.value       = e?.date ?? null;
  startTimeStr.value = s?.time ?? '00:00:00';
  endTimeStr.value   = e?.time ?? '23:59:59';
  if (selStart.value) {
    navYear.value  = selStart.value.getFullYear();
    navMonth.value = selStart.value.getMonth();
  } else {
    navYear.value  = today.getFullYear();
    navMonth.value = today.getMonth();
  }
};

const updatePopupPosition = () => {
  if (!rootEl.value) return;
  const rect = rootEl.value.getBoundingClientRect();
  const viewportW = window.innerWidth;
  let left = rect.left;
  // Estimated popup width
  const popupW = 620;
  if (left + popupW > viewportW) {
    left = Math.max(0, viewportW - popupW - 8);
  }
  popupStyle.value = {
    position: 'fixed',
    top:  `${rect.bottom + 4}px`,
    left: `${left}px`,
    zIndex: 9999,
  };
};

const openPicker = () => {
  initFromProps();
  open.value = true;
  nextTick(updatePopupPosition);
};

const toggleOpen = () => {
  if (open.value) { open.value = false; }
  else openPicker();
};

const handleOutside = (e) => {
  if (!open.value) return;
  const root = rootEl.value;
  const popup = popupEl.value;
  if (root && !root.contains(e.target) && popup && !popup.contains(e.target)) {
    open.value = false;
  }
};

onMounted(()   => document.addEventListener('mousedown', handleOutside));
onUnmounted(() => document.removeEventListener('mousedown', handleOutside));

const onClear = (e) => {
  e.stopPropagation();
  selStart.value = null;
  selEnd.value   = null;
  emit('update:start', '');
  emit('update:end',   '');
};

const onConfirm = () => {
  const s = selStart.value ? `${fmtDate(selStart.value)} ${startTimeStr.value}` : '';
  const e = selEnd.value   ? `${fmtDate(selEnd.value)} ${endTimeStr.value}`     : '';
  emit('update:start', s);
  emit('update:end',   e);
  open.value = false;
};

const leftHeader  = computed(() =>
  `${navYear.value}-${pad2(navMonth.value + 1)}`);
const rightHeader = computed(() =>
  `${rightYear.value}-${pad2(rightMonth.value + 1)}`);

const startDateLabel = computed(() => fmtDate(selStart.value));
const endDateLabel   = computed(() => fmtDate(selEnd.value));
</script>

<template>
  <div class="dtrp" ref="rootEl">
    <!-- Trigger -->
    <div class="dtrp-trigger" @click="toggleOpen">
      <span v-if="displayText" class="dtrp-val">{{ displayText }}</span>
      <span v-else class="dtrp-ph">{{ placeholder }}</span>
      <button v-if="displayText" class="dtrp-clear" @click.stop="onClear">×</button>
      <span class="dtrp-icon">&#x25A1;</span>
    </div>

    <!-- Popup via Teleport to avoid overflow clip -->
    <Teleport to="body">
      <div v-if="open" ref="popupEl" class="dtrp-popup" :style="popupStyle">
        <!-- Dual calendars -->
        <div class="dtrp-cals">
          <!-- Left calendar -->
          <div class="dtrp-cal">
            <div class="cal-head">
              <button class="nav-btn" @click.stop="prevYear">«</button>
              <button class="nav-btn" @click.stop="prevMonth">‹</button>
              <span class="cal-title">{{ leftHeader }}</span>
              <span class="nav-btn invisible">›</span>
              <span class="nav-btn invisible">»</span>
            </div>
            <div class="cal-grid">
              <span v-for="d in DAYS" :key="d" class="wday">{{ d }}</span>
              <span
                v-for="(cell, i) in leftCells"
                :key="`l-${i}`"
                :class="cellClass(cell)"
                @click.stop="onDayClick(cell)"
                @mouseenter="!cell.outOfMonth && (hoverDate = cell.date)"
                @mouseleave="hoverDate = null"
              >{{ cell.date.getDate() }}</span>
            </div>
          </div>

          <!-- Right calendar -->
          <div class="dtrp-cal">
            <div class="cal-head">
              <span class="nav-btn invisible">«</span>
              <span class="nav-btn invisible">‹</span>
              <span class="cal-title">{{ rightHeader }}</span>
              <button class="nav-btn" @click.stop="nextMonth">›</button>
              <button class="nav-btn" @click.stop="nextYear">»</button>
            </div>
            <div class="cal-grid">
              <span v-for="d in DAYS" :key="d" class="wday">{{ d }}</span>
              <span
                v-for="(cell, i) in rightCells"
                :key="`r-${i}`"
                :class="cellClass(cell)"
                @click.stop="onDayClick(cell)"
                @mouseenter="!cell.outOfMonth && (hoverDate = cell.date)"
                @mouseleave="hoverDate = null"
              >{{ cell.date.getDate() }}</span>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div class="dtrp-footer">
          <div class="dtrp-time-item">
            <span class="dtrp-date-ico">&#x25A1;</span>
            <span class="dtrp-date-lbl">{{ startDateLabel || '—' }}</span>
            <span class="dtrp-clock-ico">&#9900;</span>
            <input class="dtrp-time-inp" v-model="startTimeStr" placeholder="00:00:00" />
          </div>
          <div class="dtrp-time-item">
            <span class="dtrp-date-ico">&#x25A1;</span>
            <span class="dtrp-date-lbl">{{ endDateLabel || '—' }}</span>
            <span class="dtrp-clock-ico">&#9900;</span>
            <input class="dtrp-time-inp" v-model="endTimeStr" placeholder="23:59:59" />
          </div>
          <button class="dtrp-confirm" @click.stop="onConfirm">确定</button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.dtrp {
  position: relative;
  display: inline-block;
}

.dtrp-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px;
  padding: 6px 10px;
  cursor: pointer;
  min-width: 260px;
  font-size: 13px;
  color: #fff;
  user-select: none;
}

.dtrp-val  { flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.dtrp-ph   { flex: 1; color: rgba(255, 255, 255, 0.35); }
.dtrp-icon { color: rgba(255, 255, 255, 0.4); font-size: 12px; }

.dtrp-clear {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  padding: 0 2px;
  font-size: 14px;
  line-height: 1;
}
.dtrp-clear:hover { color: #fff; }
</style>

<!-- Non-scoped popup styles (rendered in <body> via Teleport) -->
<style>
.dtrp-popup {
  background: #1a1b2e;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.6);
  padding: 16px;
  min-width: 580px;
  color: #fff;
  font-size: 13px;
}

.dtrp-cals {
  display: flex;
  gap: 24px;
}

.dtrp-cal { flex: 1; }

.cal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  gap: 4px;
}

.cal-title {
  flex: 1;
  text-align: center;
  font-weight: 600;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
}

.nav-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 6px;
  font-size: 14px;
  line-height: 1;
  display: inline-block;
}
.nav-btn:hover { background: rgba(255, 255, 255, 0.08); color: #fff; }
.nav-btn.invisible { visibility: hidden; }

.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}

.wday {
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  padding: 4px 0;
}

.cal-day {
  text-align: center;
  padding: 6px 2px;
  border-radius: 50%;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  transition: background 0.1s;
  cursor: default;
}

.cal-day.clickable {
  cursor: pointer;
}
.cal-day.clickable:hover {
  background: rgba(99, 102, 241, 0.3);
}

.cal-day.out-of-month {
  color: rgba(255, 255, 255, 0.2);
  cursor: default;
}

.cal-day.is-today {
  color: #818cf8;
  font-weight: 600;
}

.cal-day.is-start,
.cal-day.is-end {
  background: #4f46e5;
  color: #fff;
  border-radius: 50%;
  font-weight: 600;
}

.cal-day.in-range {
  background: rgba(79, 70, 229, 0.25);
  border-radius: 0;
}

/* Divider between calendars */
.dtrp-cals .dtrp-cal + .dtrp-cal {
  border-left: 1px solid rgba(255, 255, 255, 0.08);
  padding-left: 24px;
}

/* Footer */
.dtrp-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.dtrp-time-item {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
}

.dtrp-date-ico,
.dtrp-clock-ico {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
}

.dtrp-date-lbl {
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  min-width: 80px;
}

.dtrp-time-inp {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: #fff;
  padding: 4px 8px;
  border-radius: 8px;
  font-size: 13px;
  width: 90px;
  font-family: monospace;
}

.dtrp-confirm {
  background: #4f46e5;
  border: none;
  color: #fff;
  padding: 6px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  white-space: nowrap;
  margin-left: auto;
}
.dtrp-confirm:hover { background: #4338ca; }
</style>
