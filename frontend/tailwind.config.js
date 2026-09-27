/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#F1EFFF',
          100: '#E4E1FF',
          200: '#C9C2FF',
          300: '#A99EFB',
          400: '#8A7BF2',
          500: '#6C5CE7',
          600: '#5B4BD5',
          700: '#4738B8',
        },
        accent: { 100: '#D7FBF2', 300: '#7FE9D2', 400: '#00D2A8', 600: '#00A886' },
        sun: { 100: '#FFF3D1', 300: '#FFDC85', 400: '#FFC93C', 600: '#D9A200' },
        coral: { 100: '#FFE1E1', 300: '#FFA3A3', 400: '#FF6B6B', 600: '#E04848' },
        sky: { 100: '#DCF3FE', 300: '#8FDCFB', 400: '#4FC3F7', 600: '#2196C9' },
        ink: { 400: '#9A9AB0', 600: '#4A4A68', 900: '#1B1B2F' },
        surface: { DEFAULT: '#FFFFFF', soft: '#F7F8FF', muted: '#EEF0FA' },
      },
      fontFamily: {
        sans: ['Inter', 'Segoe UI', 'system-ui', '-apple-system', 'sans-serif'],
      },
      borderRadius: { xl: '0.85rem', '2xl': '1.25rem', '3xl': '1.75rem' },
      boxShadow: {
        soft: '0 10px 30px -12px rgba(27, 27, 47, 0.18)',
        glow: '0 12px 40px -12px rgba(108, 92, 231, 0.55)',
      },
      keyframes: {
        'fade-in': { '0%': { opacity: 0, transform: 'translateY(6px)' }, '100%': { opacity: 1, transform: 'translateY(0)' } },
        'pop-in': { '0%': { opacity: 0, transform: 'scale(0.96)' }, '100%': { opacity: 1, transform: 'scale(1)' } },
        'slide-down': { '0%': { opacity: 0, transform: 'translateY(-14px)' }, '100%': { opacity: 1, transform: 'translateY(0)' } },
      },
      animation: {
        'fade-in': 'fade-in 220ms ease-out both',
        'pop-in': 'pop-in 180ms ease-out both',
        'slide-down': 'slide-down 200ms ease-out both',
      },
    },
  },
  plugins: [],
}
