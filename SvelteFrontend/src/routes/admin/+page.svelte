<script lang="ts">
	import { onMount } from 'svelte';
	
	const brandName = import.meta.env.VITE_BRAND_NAME;
	
	let password = '';
	let username = '';
	let inviter = '';
	let result: string | null = null;
	let error: string | null = null;
	let submitting = false;

	async function createInvite() {
		result = error = null;
		submitting = true;

		try {
			const res = await fetch('/admin/create', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ password, username, inviter })
			});

			const data = await res.json();
			if (res.ok) {
				result = data.link;
				// Clear form after successful creation
				username = '';
				inviter = '';
			} else {
				error = data.message || 'Error creating invite.';
			}
		} catch (err) {
			error = 'Network error occurred.';
		} finally {
			submitting = false;
		}
	}

	function copyToClipboard() {
		if (result) {
			navigator.clipboard.writeText(result);
			// Simple feedback
			const btn = document.querySelector('.copy-btn') as HTMLButtonElement;
			if (btn) {
				const originalText = btn.textContent;
				btn.textContent = 'Copied!';
				setTimeout(() => btn.textContent = originalText, 2000);
			}
		}
	}
</script>

<main class="container">
	<div class="header">
		<div class="admin-icon">🛡️</div>
		<h1>Admin Invite Panel</h1>
		<p class="subtitle">Create secure invite links for {brandName}</p>
	</div>

	<form on:submit|preventDefault={createInvite} class="invite-form">
		<div class="form-group">
			<label for="password">Admin Password</label>
			<input 
				id="password"
				type="password" 
				placeholder="Enter admin password" 
				bind:value={password} 
				required 
				disabled={submitting}
			/>
		</div>

		<div class="form-group">
			<label for="username">Target Username</label>
			<input 
				id="username"
				type="text" 
				placeholder="Minecraft username to whitelist" 
				bind:value={username} 
				required 
				disabled={submitting}
			/>
		</div>

		<div class="form-group">
			<label for="inviter">Inviter Name <span class="optional">(optional)</span></label>
			<input 
				id="inviter"
				type="text" 
				placeholder="Who is creating this invite?" 
				bind:value={inviter} 
				disabled={submitting}
			/>
		</div>

		<button type="submit" class="submit-btn" disabled={submitting || !password || !username}>
			{submitting ? 'Creating Invite...' : 'Generate Invite Link'}
		</button>
	</form>

	{#if result}
		<div class="result-section success">
			<div class="result-header">
				<div class="success-icon">✅</div>
				<h3>Invite Created Successfully!</h3>
			</div>
			<div class="invite-link">
				<label>Invite Link:</label>
				<div class="link-container">
					<code class="link">{result}</code>
					<button type="button" class="copy-btn" on:click={copyToClipboard}>
						📋 Copy
					</button>
				</div>
			</div>
		</div>
	{:else if error}
		<div class="result-section error">
			<div class="error-icon">❌</div>
			<p class="error-message">{error}</p>
		</div>
	{/if}
</main>

<style>
	.container {
		max-width: 500px;
		margin: 3rem auto;
		padding: 3rem 2rem;
		background: white;
		border-radius: 10px;
		box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
		font-family: system-ui, sans-serif;
		animation: fadeIn 0.6s ease-out;
	}

	@keyframes fadeIn {
		from {
			opacity: 0;
			transform: translateY(20px);
		}
		to {
			opacity: 1;
			transform: translateY(0);
		}
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

	.optional {
		color: #999;
		font-weight: normal;
		font-size: 0.8rem;
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

	input:disabled {
		background-color: #f8f9fa;
		color: #6c757d;
		cursor: not-allowed;
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
		transform: none;
		box-shadow: none;
	}

	.result-section {
		margin-top: 2rem;
		padding: 2rem;
		border-radius: 8px;
		animation: slideIn 0.4s ease-out;
	}

	@keyframes slideIn {
		from {
			opacity: 0;
			transform: translateY(10px);
		}
		to {
			opacity: 1;
			transform: translateY(0);
		}
	}

	.success {
		background: #f0f9f0;
		border: 1px solid #d4e8d4;
	}

	.error {
		background: #fef2f2;
		border: 1px solid #fed7d7;
		text-align: center;
	}

	.result-header {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		margin-bottom: 1.5rem;
	}

	.success-icon {
		font-size: 1.5rem;
	}

	.result-header h3 {
		margin: 0;
		color: #2d5a2d;
		font-size: 1.25rem;
	}

	.error-icon {
		font-size: 1.5rem;
		margin-bottom: 0.5rem;
	}

	.error-message {
		color: #c53030;
		margin: 0;
		font-weight: 500;
	}

	.invite-link label {
		display: block;
		margin-bottom: 0.75rem;
		color: #2d5a2d;
		font-weight: 500;
	}

	.link-container {
		display: flex;
		gap: 0.75rem;
		align-items: center;
	}

	.link {
		flex: 1;
		background: white;
		padding: 0.75rem;
		border: 1px solid #c6f6d5;
		border-radius: 4px;
		font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
		font-size: 0.85rem;
		color: #2d5a2d;
		word-break: break-all;
		line-height: 1.4;
	}

	.copy-btn {
		padding: 0.75rem 1rem;
		background: #38a169;
		color: white;
		border: none;
		border-radius: 4px;
		cursor: pointer;
		font-size: 0.9rem;
		font-weight: 500;
		transition: background 0.2s ease;
		white-space: nowrap;
	}

	.copy-btn:hover {
		background: #2f855a;
	}

	/* Responsive adjustments */
	@media (max-width: 640px) {
		.container {
			margin: 1rem;
			padding: 2rem 1.5rem;
		}

		.link-container {
			flex-direction: column;
			align-items: stretch;
		}

		.copy-btn {
			align-self: center;
		}
	}
</style>
