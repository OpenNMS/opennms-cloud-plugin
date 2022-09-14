<template>
    <br/>
    <div class="container">
      <FeatherSnackbar v-model="notified" :center="true" :success="true">
        {{ notification }}
        <template v-slot:button>
          <FeatherButton @click="notified = false" text>dismiss</FeatherButton>
        </template>
      </FeatherSnackbar>
      <FeatherButton v-on:click="configureAppliances()">
          Sync Appliance Inventory
      </FeatherButton>
      <FeatherChip v-if="loading" class="chip-checking">
        <FeatherSpinner v-if="loading"/>
      </FeatherChip>
      <FeatherTooltip
        title="Success"
      >      
        <FeatherChip v-if="appSync" class="chip-success">
            <template v-slot:icon class="label">
              <FeatherIcon :icon="checkCircle" class="my-primary-icon label" v-if="appSync" />
            </template>
            <div class="label">
              Complete      
            </div>
        </FeatherChip>
      </FeatherTooltip>
    </div>
    <div>
      <table
        :class="{ 'tc1 tr2 tc4 tr6': true, hover: true }"
      >
        <thead>
          <tr>
            <th scope="col">
              Appliance Name
          </th>
            <th scope="col">Node Id</th>
            <th scope="col">IP Address</th>
            <th scope="col">Location</th>
            <th scope="col">Minion Status</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="appliance in appliances">
            <td>{{ appliance.applianceLabel }}</td>
            <td>{{ appliance.nodeId }}</td> 
            <td>{{ appliance.nodeIpAddress }}</td>
            <td>{{ appliance.nodeLocation }}</td>
            <td>{{ appliance.nodeStatus }}</td>
          </tr>
        </tbody>
      </table>
      <FeatherPagination :total="100" :page-size="10" :modelValue="1" />
    </div>
</template>

<script setup lang="ts">
import { FeatherButton } from '@featherds/button';
import { FeatherPagination } from "@featherds/pagination";
import { FeatherSnackbar } from '@featherds/snackbar'
import { FeatherSpinner } from "@featherds/progress";
import { FeatherIcon } from "@featherds/icon";
import { FeatherChip } from '@featherds/chips';
import {
  FeatherTooltip,
} from "@featherds/tooltip";

import CheckCircle from "@featherds/icon/action/CheckCircle";
import { computed, markRaw, onMounted, ref } from 'vue';

//TODO: default date with empty data
const appliances = ref([{
  applianceLabel: 'virtual-appliance-1',
  nodeId: 4,
  nodeIpAddress: '192.168.3.1',
  nodeLocation: 'KanataOffice',
  nodeStatus: 'UP',
}]);

const checkCircle = computed(() => {
  return markRaw(CheckCircle);
})

const loading = ref(false);
const appSync = ref(false);
const notified = ref(false);
const notification = ref('');

onMounted(async () => {
  getConfigurationStatus();
});

const configureAppliancesDummy = async() => {
  let response = { code : 0, message: '' }
  appSync.value = false;
  loading.value = true;
  //Call Appliance manager
  await setTimeout(async() => {
    console.log('faking a request');
    response = { code: 200, message: 'Appliances Synced' };
    loading.value = false;

    if (response.code >= 200 && response.code <= 300) {
      console.log('maybe also replace spinner with checkmark');
      appSync.value = true;
      //TODO: move to store or use emits or something
      //along those lines
      notified.value = true;
      notification.value = response.message
    }
  }, 1000);

}

const configureAppliances = async () => {
  appSync.value = false;
  const val = await fetch('/opennms/rest/plugin/cloud/appliance/configure', { method: 'POST' });
  try {
    const response = await val.json();
    if (response.code >= 200 && response.code <= 300) {
      console.log('maybe also replace spinner with checkmark');
      appSync.value = true;
      //TODO: move to store or use emits or something along those lines
      notified.value = true;
      notification.value = response.message || 'Sync completed'
      // fire off a refresh of the table
      getConfigurationStatus();
    }
  } catch (e) {
    console.error('Error executing configureAppliance: ', e);
  }
};

/**
 * Attempt to retrieve application list.
 * Portions of this code borrowed from `CloudPlugin.vue`
 */
const getConfigurationStatus = async () => {
  const val = await fetch('/opennms/rest/plugin/cloud/appliance', { method: 'GET' })
  try {
    const jsonResponse = await val.json();
    appliances.value = jsonResponse;
  } catch (e) {
    console.error('Error connecting to API', e);
  }
}
</script>

<style lang="scss" scoped>
@import "@featherds/table/scss/table";
table {
  width: 100%;
  @include table();
  @include row-select();
  &.hover {
    @include row-hover();
  }
  &.condensed {
    @include table-condensed();
  }
  &.striped {
    @include row-striped();
  }
}

@import "@featherds/styles/themes/variables";
.my-icon,
// .my-primary-icon {
//   font-size: 1.5rem;
//   color: var($secondary-text-on-surface);
// }

.my-primary-icon {
  color: var($primary);
}

.chip-1-example div.chip-list:first-of-type {
  max-width: 18.75rem;
  color: red; 
}
.chip-checking {
  background-color: var(--feather-warning);
  .label {
    color: var(--feather-surface);
  }
}
.chip-success {
  background-color: var(--feather-success);
  // margin-top: 50px;
  margin-left: 25px;;
  .label {
    color: var(--feather-surface);
  }
}
.container {
  display: flex;
  justify-content: flex-end;
}
</style>