import re

with open('SvelteFrontend/src/routes/public-invite/[id]/+page.svelte', 'r') as f:
    content = f.read()

# 1. Add step variable and initialization
content = content.replace("let valid = false;\n\tlet hasPassword = false;", "let valid = false;\n\tlet hasPassword = false;\n\tlet step = 1;")
content = content.replace("hasPassword = data.hasPassword;", "hasPassword = data.hasPassword;\n\t\t\t\tstep = data.hasPassword ? 1 : 2;")

# 2. Add nextStep function
content = content.replace('function initializeCaptcha() {', """function nextStep() {
		if (password) step = 2;
	}

	/**
	 * Initializes the hCaptcha widget. 
""")

# 3. Modify HTML form
old_form = """		<form on:submit|preventDefault={submit}>
			<input type="text" placeholder="Minecraft Username" bind:value={username} required pattern="[a-zA-Z0-9_]{3,16}" title="Valid Minecraft username" disabled={submitting} />
			
			{#if hasPassword}
				<input type="password" placeholder="Invite Password" bind:value={password} required disabled={submitting} />
			{/if}

			<div class="captcha">
				<div class="h-captcha" bind:this={captchaContainer}></div>
				{#if !captchaLoaded}
					<p class="loading">Loading CAPTCHA...</p>
				{/if}
			</div>
			
			<button type="submit" disabled={submitting || !username || !captchaToken || (hasPassword && !password)}>
				{submitting ? 'Processing...' : 'Join Server'}
			</button>
		</form>"""

new_form = """		{#if step === 1}
			<form on:submit|preventDefault={nextStep}>
				<p>This invite is password-protected.</p>
				<input type="password" placeholder="Invite Password" bind:value={password} required />
				<button type="submit">Verify Password</button>
			</form>
		{:else}
			<form on:submit|preventDefault={submit}>
				<input type="text" placeholder="Minecraft Username" bind:value={username} required pattern="[a-zA-Z0-9_]{3,16}" title="Valid Minecraft username" disabled={submitting} />
				
				<div class="captcha">
					<div class="h-captcha" bind:this={captchaContainer}></div>
					{#if !captchaLoaded}
						<p class="loading">Loading CAPTCHA...</p>
					{/if}
				</div>
				
				<button type="submit" disabled={submitting || !username || !captchaToken}>
					{submitting ? 'Processing...' : 'Join Server'}
				</button>
			</form>
		{/if}"""

content = content.replace(old_form, new_form)

with open('SvelteFrontend/src/routes/public-invite/[id]/+page.svelte', 'w') as f:
    f.write(content)
