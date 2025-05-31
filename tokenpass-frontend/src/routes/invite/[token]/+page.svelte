import { error } from '@sveltejs/kit';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ params, fetch }) => {
  const res = await fetch(`/api/check-token/${params.token}`);

  if (!res.ok) {
    throw error(404, 'Invalid or expired invite link.');
  }

  return { token: params.token };
};
