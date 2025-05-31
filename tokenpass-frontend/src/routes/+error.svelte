<script>
    export let error;
  </script>
  
  <h1>Error {error.status}</h1>
  <p>{error.message ?? 'Something went wrong.'}</p>
  