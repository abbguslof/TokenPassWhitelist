<script lang="ts">
	const brandName = import.meta.env.VITE_BRAND_NAME || 'Minecraft Server';
	const description = `Join the whitelist for ${brandName}.`;
</script>

<svelte:head>
	<title>{brandName}</title>
	
	<!-- Open Graph / Facebook -->
	<meta property="og:type" content="website" />
	<meta property="og:title" content="{brandName} Whitelist Invite" />
	<meta property="og:description" content="{description}" />
	<meta property="og:image" content="/favicon.png" />

	<!-- Twitter -->
	<meta property="twitter:card" content="summary" />
	<meta property="twitter:title" content="{brandName} Whitelist Invite" />
	<meta property="twitter:description" content="{description}" />
	<meta property="twitter:image" content="/favicon.png" />

	<!-- Theme Color for Discord Embeds -->
	<meta name="theme-color" content="#007cba" />
</svelte:head>

<slot />
