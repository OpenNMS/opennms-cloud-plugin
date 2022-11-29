<script setup lang="ts">
import { FeatherIcon } from "@featherds/icon";
import CheckCircle from '@featherds/icon/action/CheckCircle';
import Cancel from '@featherds/icon/action/Cancel';
import Cycle from '@featherds/icon/action/Cycle';

const props = defineProps({
    status: { type: String, default: 'activating' }
});

</script>

<template>
    <div v-if="status === 'activated'" class="activated statusbar">
        <FeatherIcon :icon="CheckCircle"/> Activated
    </div>
    <div v-else-if="status === 'activating'" class="activating statusbar">
        <FeatherIcon :icon="Cycle"/> Activating...
    </div>
    <div v-else class="deactivated statusbar">
        <FeatherIcon :icon="Cancel" /> Not activated
    </div>
</template>

<style scoped lang="scss">

.deactivated {
    background-color: rgba(117, 117, 117, 0.2);
    color: #757575;
    width: 115px;
}

.activating {
    background-color: rgba(0, 146, 199, 0.2);
    color: #0092C7;
    width: 105px;
}

.activated {
    background: rgba(11, 114, 12, 0.2);
    color: #0B720C;
    width: 91 px;
}

.statusbar {
    display: flex;
    flex-direction: row;
    justify-content: center;
    align-items: center;
    padding: 4px 6px;
    // border-radius: 0.5rem;
    height: 28px;
    border-radius: 4px;
    font-family: 'Inter';
    font-style: normal;
    font-weight: 500;
    font-size: 12px;
    line-height: 15px;
    margin-left: 20px;
    margin-top: 6px;

    /* identical to box height */
    font-feature-settings: 'tnum' on, 'lnum' on, 'cv05' on;
}

FeatherIcon { 
  margin-right: 10px;
}

</style>