<script lang="ts" setup>
import { PropType } from 'vue';

const toggleCheck = (payload: KeyboardEvent) => {
    if (payload.key === 'Enter' || payload.key === ' ') {
        props.toggle();
    }
}
const props = defineProps({
    active: { type: Boolean, default: false },
    activeText: { type: String, default: '' },
    disabledText: { type: String, default: '' },
    toggle: { type: Function as PropType<(payload?: MouseEvent) => void>, default: () => { } }
})

</script>

<template>
    <div :class="['outer', active ? 'active' : '']" @click="toggle" @keypress="toggleCheck" tabindex="0">
        <div class="wrapper">
            <div class="inner">
                <div class="circle"></div>
            </div>
        </div>
        <div class="copy">
            {{ active ? activeText : disabledText }}
        </div>
    </div>
</template>

<style lang="scss" scoped>
@import "@featherds/styles/mixins/typography";
@import "@featherds/styles/mixins/elevation";
@import "@featherds/styles/themes/variables";

.outer {
    display: flex;
    align-items: center;
    cursor: pointer;

    .copy {
        @include headline3();
        margin-left: var($spacing-l);
        color: var(--feather-shade-2);
    }

    .wrapper {
        width: 90px;
        height: 50px;
        border-radius: 30px;
        @include elevation(1);
        border: 2px solid var(--feather-shade-2);

        .circle {
            background-color: var(--feather-shade-2);
            width: 50px;
            height: 50px;
            border-radius: 50%;
            transition: transform ease-out 0.25s;
            transform: translateX(-1px);
        }


    }

    &.active {
        .copy {
            color: var(--feather-primary);
        }

        .wrapper {
            border: 2px solid var(--feather-primary);
        }

        .circle {
            background-color: var(--feather-primary);
            transform: translateX(41px);
        }
    }
}
</style>