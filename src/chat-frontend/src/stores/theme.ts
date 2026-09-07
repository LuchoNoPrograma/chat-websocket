import { computed, ref, watch } from 'vue';
import { defineStore } from 'pinia';
import faviconSvg from '../../public/favicon.svg?raw';

type Theme = 'light' | 'dark';

const getInitialTheme = (): Theme => {
  const saved = window.localStorage.getItem('relay-theme');
  if (saved === 'light' || saved === 'dark') return saved;
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
};

export const useThemeStore = defineStore('theme', () => {
  const theme = ref<Theme>(getInitialTheme());
  const isDark = computed(() => theme.value === 'dark');

  const applyTheme = (value: Theme) => {
    document.documentElement.dataset.theme = value;
    document.documentElement.style.colorScheme = value;
    const colors = getComputedStyle(document.documentElement);
    const favicon = document.querySelector<HTMLLinkElement>('link[rel="icon"]');
    if (favicon) {
      const svg = faviconSvg.replaceAll('currentColor', colors.getPropertyValue('--lime').trim());
      favicon.href = `data:image/svg+xml,${encodeURIComponent(svg)}`;
    }
    document.querySelector<HTMLMetaElement>('meta[name="theme-color"]')?.setAttribute(
      'content', colors.getPropertyValue('--paper').trim()
    );
    window.localStorage.setItem('relay-theme', value);
  };

  const toggle = () => {
    theme.value = isDark.value ? 'light' : 'dark';
  };

  applyTheme(theme.value);
  watch(theme, applyTheme);

  return { theme, isDark, toggle };
});
