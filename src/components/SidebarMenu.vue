<script setup>
const props = defineProps({
  menuItems: {
    type: Array,
    default: () => []
  },
  lastSynced: {
    type: String,
    default: '刚刚'
  },
  activeParent: {
    type: String,
    default: ''
  },
  activeChild: {
    type: String,
    default: ''
  }
});

const emit = defineEmits(['select']);

const handleParentClick = (item) => {
  const isActive = item.id === props.activeParent;
  const hasChildren = Boolean(item.children?.length);

  if (hasChildren && isActive) {
    emit('select', { parentId: '', childId: '' });
    return;
  }

  const firstChild = item.children?.[0];
  emit('select', { parentId: item.id, childId: firstChild?.id });
};

const handleChildClick = (parentId, childId) => {
  emit('select', { parentId, childId });
};
</script>

<template>
  <aside class="sidebar">
    <nav>
      <div v-for="item in menuItems" :key="item.id" class="menu-group">
        <button
          class="menu-item"
          :class="{ active: item.id === activeParent }"
          type="button"
          @click="handleParentClick(item)"
        >
          <span class="bullet" aria-hidden="true" />
          <span>{{ item.label }}</span>
          <span v-if="item.badge" class="badge">{{ item.badge }}</span>
        </button>
        <div
          v-if="item.children?.length"
          class="submenu"
          :class="{ open: item.id === activeParent }"
        >
          <button
            v-for="child in item.children"
            :key="child.id"
            class="submenu-item"
            :class="{ active: child.id === activeChild }"
            type="button"
            @click="handleChildClick(item.id, child.id)"
          >
            <span>{{ child.label }}</span>
          </button>
        </div>
      </div>
    </nav>
    <div class="sidebar-footer">
      <p class="caption">数据刷新</p>
      <p class="value">{{ lastSynced }}</p>
      <p class="hint">若需强制刷新请在仪表板执行。</p>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 260px;
  min-width: 260px;
  flex-shrink: 0;
  padding: 32px 24px 24px;
  border-right: 1px solid rgba(255, 255, 255, 0.05);
  background: rgba(9, 10, 20, 0.9);
  backdrop-filter: blur(20px);
  display: flex;
  flex-direction: column;
  gap: 32px;
}

nav {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.menu-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.menu-item {
  border: none;
  padding: 12px 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.02);
  color: rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.menu-item .bullet {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: transparent;
  margin-right: 6px;
}

.menu-item span:first-of-type {
  flex: 1;
  text-align: left;
}

.menu-item.active {
  background: linear-gradient(120deg, rgba(127, 133, 249, 0.24), rgba(178, 145, 255, 0.24));
  color: #fff;
}

.menu-item.active .bullet {
  background: #8c8cfb;
}

.submenu {
  display: none;
  flex-direction: column;
  gap: 4px;
  padding-left: 16px;
}

.submenu.open {
  display: flex;
}

.submenu-item {
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.55);
  text-align: left;
  padding: 6px 12px 6px 20px;
  border-radius: 10px;
  font-size: 13px;
}

.submenu-item.active {
  color: #fff;
  background: rgba(140, 140, 251, 0.14);
}

.badge {
  font-size: 10px;
  text-transform: uppercase;
  background: rgba(255, 255, 255, 0.14);
  padding: 2px 6px;
  border-radius: 999px;
}

.sidebar-footer {
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.03);
}

.caption {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.value {
  font-size: 20px;
  font-weight: 600;
}

.hint {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

@media (max-width: 960px) {
  .sidebar {
    width: 100%;
    flex-direction: row;
    align-items: flex-start;
    gap: 16px;
    border-right: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  }

  nav {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .menu-group {
    flex: 1 1 220px;
  }

  .submenu {
    padding-left: 8px;
  }
}
</style>
