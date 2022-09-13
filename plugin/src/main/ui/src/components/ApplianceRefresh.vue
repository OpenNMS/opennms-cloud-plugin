<template>
    <br/>
    <FeatherButton v-on:click="getConfigurationStatus()">
        Check for New Appliances
    </FeatherButton>
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
import { ref } from 'vue';

const appliances = ref([{
  applianceLabel: 'virtual-appliance-1',
  nodeId: 4,
  nodeIpAddress: '192.168.3.1',
  nodeLocation: 'KanataOffice',
  nodeStatus: 'UP',
}])
/**
 * Attempt to retrieve application list.
 * Portions of this code borrowed from `CloudPlugin.vue`
 */
const getConfigurationStatus = async () => {
  const val = await fetch('/opennms/rest/plugin/cloud/appliance', { method: 'GET' })
  try {
    
    const jsonResponse = await val.json();
    console.log('val', val);
    console.log('jsonResponse', jsonResponse);
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