<template>
  <div class="task-card"></div>
</template>

<style lang="postcss" scoped>
.task-card {
  position: relative;
  display: flex;
  width: 1234px;
  height: 80px;
  padding: 26px 40px;
  background: linear-gradient(
    90deg,
    rgb(190 190 190 / 10%) 25%,
    rgb(129 129 129 / 12%) 37%,
    rgb(190 190 190 / 10%) 63%
  );
  background-size: 400% 100%;
  border: none;
  border-radius: 2px;
  animation: skeleton-loading 1.4s ease infinite;
  align-items: center;
}

@keyframes skeleton-loading {
  0% {
    background-position: 100% 50%;
  }

  100% {
    background-position: 0 50%;
  }
}
</style>
