<template>
  <!-- Component to display messages from API-->
  <FeatherSnackbar v-model="notified" :center="true">
    {{ notification }}
    <template v-slot:button>
      <FeatherButton @click="notified = false" text>dismiss</FeatherButton>
    </template>
  </FeatherSnackbar>
  <FeatherSnackbar v-model="tableNotified" :center="true">
    {{ tableNotification }}
    <template v-slot:button>
      <FeatherButton @click="tableNotified = false" text>dismiss</FeatherButton>
    </template>
  </FeatherSnackbar>
  <br/>
  <div class="container">
    <!-- <FeatherSpinner class="label" v-if="loading"/> -->
    <FeatherChip v-if="loading" class="chip-checking">
        <div class="label">
          {{ loadingText }}
        </div>
    </FeatherChip>
    <FeatherChip v-if="error" class="chip-failed">
        <div class="label">
          {{ errorText }}
        </div>
    </FeatherChip>
    <FeatherTooltip
      title="Success"
    >      
      <FeatherChip v-if="appSync" class="chip-success">
          <template v-slot:icon class="label">
            <FeatherIcon :icon="checkCircle" class="my-primary-icon label" v-if="appSync" />
          </template>
          <div class="label">
            Sync Complete      
          </div>
      </FeatherChip>
    </FeatherTooltip>
    
    <FeatherButton v-on:click="configureAppliances()">
        Sync Appliance Inventory
    </FeatherButton>
  </div>
  <div id="appliance-table">
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
    <!-- <FeatherPagination :total="100" :page-size="10" :modelValue="1" /> -->
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

const error = ref(false);
const errorText = ref('Error!')
const loading = ref(false);
const loadingText = ref('Loading...');
const appSync = ref(false);
const notified = ref(false);
const notification = ref('');
const tableNotified = ref(false);
const tableNotification = ref('');

onMounted(async () => {
  getConfigurationStatus();
});

const configureAppliancesDummy = async() => {
  let response = { code : 0, message: '' }
  appSync.value = false;
  loading.value = true;
  loadingText.value = 'Syncing';
  //Call Appliance manager
  await setTimeout(async() => {
    console.log('faking a request');
    response = { code: 200, message: 'Appliances Synced' };

    if (response.code >= 200 && response.code < 300) {
      appSync.value = true;
      notified.value = true;
      notification.value = response.message

      loadingText.value = 'Refreshing Table';
      await setTimeout(async () => {
        loading.value = false;
        tableNotified.value = true;
        tableNotification.value = 'Table Refreshed';
      }, 1000);
    }
  }, 1000);

}

const configureAppliances = async () => {
  appSync.value = false;
  loading.value = true;
  loadingText.value = 'Syncing';
  
  try {
    const val = await fetch('/opennms/rest/plugin/cloud/appliance/configure', { method: 'POST' });
    const response = await val.json();
    if (response.status === 'success') {
      appSync.value = true;
      //TODO: move to store or use emits or something along those lines
      notified.value = true;
      notification.value = response.message || 'Sync completed'
      
      loadingText.value = 'Refreshing Table.';
      
    } else {
      console.log('no code', val);
    }
    // fire off a refresh of the table, wait for 2 seconds before doing so though
    // Just doing this in case behind the scenes theres a call that doesn't complete before our refresh does
    // should reconfigure to re-poll/refresh the table ever 30 seconds or so
    console.log('getting status')
    setTimeout(() => {
      console.log('getting status after two seconds')
      getConfigurationStatus();
    }, 2000);
  } catch (e) {
    error.value = true;
    errorText.value = 'Failed to Configure.'
    loading.value = false;
    console.error('Error executing configureAppliance: ', e);
  }
};

/**
 * Attempt to retrieve application list.
 * Portions of this code borrowed from `CloudPlugin.vue`
 */
const getConfigurationStatus = async () => {
  loadingText.value = 'Refreshing Table.';
  loading.value = true;
  const val = await fetch('/opennms/rest/plugin/cloud/appliance', { method: 'GET' })
  try {
    const jsonResponse = await val.json();
    appliances.value = jsonResponse;
    tableNotified.value = true;
    tableNotification.value = 'Table Refreshed.';
  } catch (e) {
    console.error('Error connecting to API', e);
    tableNotified.value = true;
    tableNotification.value = 'Table Refresh Failed.';
  } finally {
    loading.value = false;
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
  .label {
    color: var(--feather-surface);
  }
}
.chip-failed {
  background-color: var(--feather-error);
  .label {
    color: var(--feather-surface);
  }
}
.container {
  display: flex;
  justify-content: flex-end;
}

.start-item {
  align-self: flex-start;
}

.end-item {
  align-self: flex-end;
}
</style>
