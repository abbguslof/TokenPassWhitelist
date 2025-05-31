import adapter from '@sveltejs/adapter-node';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';

/** @type {import('@sveltejs/kit').Config} */
const config = {
    preprocess: vitePreprocess(),
    kit: {
        adapter: adapter()
    }
};	kit: {
o only supports some environments, see https://svelte.dev/docs/kit/adapter-auto for a list.
export default config;};

export default config;
