import js from '@eslint/js';
import pluginVue from 'eslint-plugin-vue';
import globals from 'globals';
import tseslint from 'typescript-eslint';

export default tseslint.config(
  {
    ignores: [
      'dist/**',
      'src/_mockApis/**',
      'src/assets/**',
      'src/components/**',
      'src/config.ts',
      'src/layouts/**',
      'src/router/**',
      'src/scss/**',
      'src/stores/**',
      'src/theme/**',
      'src/types/**',
      'src/utils/**',
      'src/views/**'
    ]
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...pluginVue.configs['flat/recommended'],
  {
    files: ['src/modern/**/*.{ts,vue}', 'src/main.ts', 'src/App.vue', 'vite.config.ts'],
    languageOptions: {
      globals: { ...globals.browser, ...globals.node },
      parserOptions: { parser: tseslint.parser }
    },
    rules: {
      'vue/multi-word-component-names': 'off',
      'vue/max-attributes-per-line': 'off',
      'vue/singleline-html-element-content-newline': 'off',
      'vue/html-self-closing': ['error', { html: { void: 'never', normal: 'always', component: 'always' } }]
    }
  }
);
