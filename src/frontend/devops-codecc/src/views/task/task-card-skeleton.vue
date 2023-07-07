<template>
  <div class="task-card"></div>
</template>

<style lang="postcss" scoped>
  .task-card {
    display: flex;
    align-items: center;
    position: relative;
    padding: 26px 40px;
    width: 1234px;
    height: 80px;
    border-radius: 2px;
    background: linear-gradient(90deg,rgba(190,190,190,.1) 25%,rgba(129,129,129,.12) 37%,rgba(190,190,190,.1) 63%);
    background-size: 400% 100%;
    animation: skeleton-loading 1.4s ease infinite;
    border: none;
  }
  @keyframes skeleton-loading {
    0% {
        background-position: 100% 50%
    }

    to {
        background-position: 0 50%
    }
  }
</style>
