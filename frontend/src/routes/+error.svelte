<script>
    export let error;
  </script>
  
  <style>
    .container {
      max-width: 400px;
      margin: 5rem auto;
      padding: 2rem;
      font-family: sans-serif;
      text-align: center;
    }
  </style>
  
  <div class="container">
    <h1>Error {error?.status}</h1>
    <p>{error?.message || 'Something went wrong.'}</p>
    <a href="/">← Go home</a>
  </div>
  