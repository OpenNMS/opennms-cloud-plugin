<template>
    <br/>
    
    <FeatherSnackbar v-model="notified">
      {{ notification }}
      <template v-slot:button>
        <FeatherButton @click="notified = false" text>dismiss</FeatherButton>
      </template>
    </FeatherSnackbar>
    <FeatherButton v-on:click="checkForAppliances()">
        Sync Appliance Inventory
    </FeatherButton>
    <FeatherSpinner v-if="loading"/> 
    <!-- Maybe turn this into a pill with the word "Synced" or "Complete" -->
    <FeatherIcon :icon="CheckCircle" v-if="appSync" />
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
import CheckCircle from "@featherds/icon/action/CheckCircle";
import { onMounted, ref } from 'vue';

//TODO: default date with empty data
const appliances = ref([{
  applianceLabel: 'virtual-appliance-1',
  nodeId: 4,
  nodeIpAddress: '192.168.3.1',
  nodeLocation: 'KanataOffice',
  nodeStatus: 'UP',
}]);

const loading = ref(false);
const appSync = ref(false);
const notified = ref(false);
const notification = ref('');

onMounted(async () => {
  //getConfigurationStatus();
});

const checkForAppliances = async() => {
  let response = { code : 0, message: '' }
  console.log('checking for appliances');
  loading.value = true;
  //Call Appliance manager
  console.log('spinner');
  await setTimeout(() => {
    console.log('faking a request');
    response = { code: 200, message: 'Appliances Synced' };
  }, 5000);
    
  console.log('we have data, needto display prompt for success');
  loading.value = false;

  if (response.code >= 200 && response.code <= 300) {
    console.log('maybe also replace spinner with checkmark');
    appSync.value = true;
    //TODO: move to store or use emits or something
    //along those lines
    notified.value = true;
    notification.value = response.message
  }

}
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
</style>