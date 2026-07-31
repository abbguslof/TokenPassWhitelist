<script lang="ts">
	import { goto } from '$app/navigation';
	
	const brandName = import.meta.env.VITE_BRAND_NAME;
	let password = '';
	let errorMsg = '';
	let loading = false;

	async function login() {
		loading = true;
		errorMsg = '';
		try {
			const res = await fetch('/admin/dashboard/api', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ action: 'invites', password, payload: {} })
			});
			
			if (res.ok) {
				localStorage.setItem('adminPassword', password);
				goto('/admin/dashboard');
			} else if (res.status === 401) {
				errorMsg = 'Invalid admin password';
			} else {
				const data = await res.json().catch(() => ({}));
				errorMsg = data.message || 'Failed to contact backend server';
			}
		} catch (e) {
			errorMsg = 'Network error occurred';
		} finally {
			loading = false;
		}
	}
</script>

<main class="container">
	<div class="header">
		<div class="admin-icon">🛡️</div>
		<h1>Admin Login</h1>
		<p class="subtitle">Enter the admin password to access the dashboard</p>
	</div>

	<form on:submit|preventDefault={login} class="invite-form">
		{#if errorMsg}
			<div class="error-msg">{errorMsg}</div>
		{/if}
		<div class="form-group">
			<label for="password">Admin Password</label>
			<input 
				id="password"
				type="password" 
				placeholder="Enter admin password" 
				bind:value={password} 
				required 
				disabled={loading}
			/>
		</div>

		<button type="submit" class="submit-btn" disabled={!password || loading}>
			{loading ? 'Logging in...' : 'Login'}
		</button>
	</form>
</main>

<style>
	.error-msg {
		color: #d32f2f;
		background: #ffebee;
		padding: 0.75rem;
		border-radius: 6px;
		font-size: 0.9rem;
		text-align: center;
		border: 1px solid #ffcdd2;
	}

	.container {
		max-width: 500px;
		margin: 5rem auto;
		padding: 3rem 2rem;
		background: white;
		border-radius: 10px;
		box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
		font-family: system-ui, sans-serif;
		animation: fadeIn 0.6s ease-out;
	}

	@keyframes fadeIn {
		from { opacity: 0; transform: translateY(20px); }
		to { opacity: 1; transform: translateY(0); }
	}

	.header {
		text-align: center;
		margin-bottom: 2.5rem;
	}

	.admin-icon {
		font-size: 3rem;
		margin-bottom: 1rem;
	}

	h1 {
		font-size: 2rem;
		margin-bottom: 0.5rem;
		color: #333;
		font-weight: 600;
	}

	.subtitle {
		color: #666;
		margin: 0;
		font-size: 1rem;
	}

	.invite-form {
		display: flex;
		flex-direction: column;
		gap: 1.5rem;
	}

	.form-group {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
	}

	label {
		font-weight: 500;
		color: #333;
		font-size: 0.9rem;
	}

	input {
		padding: 0.75rem;
		font-size: 1rem;
		border: 2px solid #e1e5e9;
		border-radius: 6px;
		transition: border-color 0.2s ease;
	}

	input:focus {
		outline: none;
		border-color: #007cba;
		box-shadow: 0 0 0 3px rgba(0, 124, 186, 0.1);
	}

	.submit-btn {
		padding: 0.875rem 1.5rem;
		font-size: 1rem;
		font-weight: 600;
		background: #007cba;
		color: white;
		border: none;
		border-radius: 6px;
		cursor: pointer;
		transition: all 0.2s ease;
		margin-top: 0.5rem;
	}

	.submit-btn:hover:not(:disabled) {
		background: #0056a3;
		transform: translateY(-1px);
		box-shadow: 0 4px 12px rgba(0, 124, 186, 0.3);
	}

	.submit-btn:disabled {
		background: #ccc;
		cursor: not-allowed;
	}

	@media (max-width: 640px) {
		.container {
			margin: 1rem;
			padding: 1.5rem 1rem;
		}
		.invite-form {
			width: 100%;
		}
	}
</style>
