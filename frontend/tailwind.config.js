/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          navy: '#09090b',
          blue: '#3b82f6',
          dark: '#18181b',
          border: '#27272a'
        }
      }
    },
  },
  plugins: [],
}
