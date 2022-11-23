<script setup lang="ts">
import { ref } from 'vue';

const status = ref('deactivated');

</script>

<template>
    <div v-if="status === 'deactivated'" class="deactivated statusbar">
        <FeatherIcon>Not activated</FeatherIcon>
    </div>
    <div v-if="status === 'activating'" class="activating">
        <FeatherIcon>Activating...</FeatherIcon>
    </div>
    <div v-if="status === 'activated'" class="activated">
        <FeatherIcon />
        Activated
    </div>
</template>

<style scoped lang="scss">

.deactivated {
    background-color: grey;
}

.activating {
    background-color: lightblue;
}

.activated {
    background-color: green;
}

.statsbar {
    border-radius: 1rem;
}

</style>